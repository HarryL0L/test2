package com.localguard.gallery.data

import android.content.Context
import com.localguard.gallery.ml.NsfwPrediction
import java.io.File

/**
 * Persists predictions in app-private storage so each file is only classified once.
 * Line format: cacheKey|drawings|hentai|neutral|porn|sexy
 */
class ScanCache(context: Context) {

    private val file = File(context.filesDir, "scan_cache.txt")
    private val entries = HashMap<String, NsfwPrediction>()

    init {
        if (file.exists()) {
            file.forEachLine { line ->
                val parts = line.split('|')
                if (parts.size == 6) {
                    val v = parts.drop(1).map { it.toFloatOrNull() ?: return@forEachLine }
                    entries[parts[0]] = NsfwPrediction(v[0], v[1], v[2], v[3], v[4])
                }
            }
        }
    }

    @Synchronized
    fun get(key: String): NsfwPrediction? = entries[key]

    @Synchronized
    fun putAll(results: Map<String, NsfwPrediction>) {
        if (results.isEmpty()) return
        entries.putAll(results)
        file.appendText(
            results.entries.joinToString(separator = "") { (k, p) ->
                "$k|${p.drawings}|${p.hentai}|${p.neutral}|${p.porn}|${p.sexy}\n"
            },
        )
    }

    @Synchronized
    fun clear() {
        entries.clear()
        file.delete()
    }
}
