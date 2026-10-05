package com.localguard.gallery.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.localguard.gallery.data.GuardSettings
import com.localguard.gallery.data.MediaItem
import com.localguard.gallery.data.MediaRepository
import com.localguard.gallery.data.MediaScanner
import com.localguard.gallery.data.ScanCache
import com.localguard.gallery.data.SettingsStore
import com.localguard.gallery.ml.NsfwClassifier
import com.localguard.gallery.ml.NsfwPrediction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll

enum class Verdict { Pending, Safe, Flagged, Error }

data class GalleryState(
    val items: List<MediaItem> = emptyList(),
    val predictions: Map<String, NsfwPrediction> = emptyMap(),
    val failed: Set<String> = emptySet(),
    val loading: Boolean = false,
    val scanned: Int = 0,
    val settings: GuardSettings = GuardSettings(),
    val modelError: String? = null,
) {
    fun verdict(item: MediaItem): Verdict {
        val p = predictions[item.cacheKey]
            ?: return if (item.cacheKey in failed) Verdict.Error else Verdict.Pending
        return if (p.score(settings.blurSuggestive) >= settings.sensitivity.threshold) {
            Verdict.Flagged
        } else {
            Verdict.Safe
        }
    }

    fun shouldBlur(item: MediaItem): Boolean = when (verdict(item)) {
        Verdict.Flagged -> true
        Verdict.Pending, Verdict.Error -> settings.blurUntilScanned
        Verdict.Safe -> false
    }

    val flaggedCount: Int get() = items.count { verdict(it) == Verdict.Flagged }
}

class GalleryViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = MediaRepository(app)
    private val cache = ScanCache(app)
    private val settingsStore = SettingsStore(app)
    private var scanner: MediaScanner? = null
    private var classifier: NsfwClassifier? = null
    private var scanJob: Job? = null

    private val _state = MutableStateFlow(GalleryState(settings = settingsStore.load()))
    val state: StateFlow<GalleryState> = _state.asStateFlow()

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true) }
            val items = repository.loadAll()
            val known = items.mapNotNull { item -> cache.get(item.cacheKey)?.let { item.cacheKey to it } }
                .toMap()
            _state.update {
                it.copy(items = items, predictions = known, scanned = known.size, loading = false)
            }
            startScan(items)
        }
    }

    private fun startScan(items: List<MediaItem>) {
        scanJob?.cancel()
        scanJob = viewModelScope.launch(Dispatchers.Default) {
            val scanner = ensureScanner() ?: return@launch
            val todo = items.filter { cache.get(it.cacheKey) == null }
            val permits = Semaphore(PARALLELISM)
            // Scan in chunks so results stream into the UI and are flushed to disk regularly.
            for (chunk in todo.chunked(CHUNK)) {
                val results = chunk.map { item ->
                    async {
                        permits.withPermit { item to runCatching { scanner.scan(item) }.getOrNull() }
                    }
                }.awaitAll()
                val ok = results.mapNotNull { (item, p) -> p?.let { item.cacheKey to it } }.toMap()
                val bad = results.filter { it.second == null }.map { it.first.cacheKey }
                withContext(Dispatchers.IO) { cache.putAll(ok) }
                _state.update {
                    it.copy(
                        predictions = it.predictions + ok,
                        failed = it.failed + bad,
                        scanned = it.scanned + results.size,
                    )
                }
            }
        }
    }

    private fun ensureScanner(): MediaScanner? {
        scanner?.let { return it }
        return try {
            val c = NsfwClassifier(getApplication())
            classifier = c
            MediaScanner(getApplication(), c).also { scanner = it }
        } catch (e: Throwable) {
            _state.update { it.copy(modelError = e.message ?: e.javaClass.simpleName) }
            null
        }
    }

    fun updateSettings(transform: (GuardSettings) -> GuardSettings) {
        _state.update {
            val s = transform(it.settings)
            settingsStore.save(s)
            it.copy(settings = s)
        }
    }

    fun rescanAll() {
        cache.clear()
        _state.update { it.copy(predictions = emptyMap(), failed = emptySet(), scanned = 0) }
        startScan(_state.value.items)
    }

    override fun onCleared() {
        scanJob?.cancel()
        classifier?.close()
    }

    private companion object {
        const val PARALLELISM = 2
        const val CHUNK = 12
    }
}
