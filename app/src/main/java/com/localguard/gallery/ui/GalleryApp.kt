package com.localguard.gallery.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.localguard.gallery.data.MediaItem

private fun mediaPermissions(): Array<String> = when {
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
    )
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> arrayOf(
        Manifest.permission.READ_MEDIA_IMAGES,
        Manifest.permission.READ_MEDIA_VIDEO,
    )
    else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
}

@Composable
fun GalleryApp(vm: GalleryViewModel = viewModel()) {
    val context = LocalContext.current
    fun hasAccess() = mediaPermissions().any {
        ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
    }
    var granted by remember { mutableStateOf(hasAccess()) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { granted = hasAccess() }

    LaunchedEffect(granted) { if (granted) vm.refresh() }

    if (!granted) {
        PermissionScreen { launcher.launch(mediaPermissions()) }
        return
    }

    val state by vm.state.collectAsStateWithLifecycle()
    var openKey by rememberSaveable { mutableStateOf<String?>(null) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    val open = openKey?.let { key -> state.items.firstOrNull { it.cacheKey == key } }

    AnimatedContent(targetState = Triple(open, showSettings, Unit), label = "screen") { (item, settings, _) ->
        when {
            item != null -> {
                BackHandler { openKey = null }
                ViewerScreen(item = item, state = state, onClose = { openKey = null })
            }
            settings -> {
                BackHandler { showSettings = false }
                SettingsScreen(
                    settings = state.settings,
                    onChange = vm::updateSettings,
                    onRescan = vm::rescanAll,
                    onBack = { showSettings = false },
                )
            }
            else -> GridScreen(
                state = state,
                onOpen = { openKey = it.cacheKey },
                onSettings = { showSettings = true },
            )
        }
    }
}

@Composable
private fun PermissionScreen(onRequest: () -> Unit) {
    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                Icons.Filled.Shield,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(72.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text("Guard Gallery", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(12.dp))
            Text(
                "Your photos and videos are checked for explicit content entirely on this phone. " +
                    "The app has no internet permission, so nothing ever leaves your device.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(32.dp))
            Button(onClick = onRequest) { Text("Allow access to photos & videos") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GridScreen(
    state: GalleryState,
    onOpen: (MediaItem) -> Unit,
    onSettings: () -> Unit,
) {
    var onlyFlagged by rememberSaveable { mutableStateOf(false) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val visible = if (onlyFlagged) {
        state.items.filter { state.verdict(it) == Verdict.Flagged }
    } else {
        state.items
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text("Gallery") },
                actions = {
                    IconButton(onClick = onSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding)) {
            ScanStatus(state)
            Row(Modifier.padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !onlyFlagged, onClick = { onlyFlagged = false }, label = {
                    Text("All (${state.items.size})")
                })
                FilterChip(selected = onlyFlagged, onClick = { onlyFlagged = true }, label = {
                    Text("Blurred (${state.flaggedCount})")
                })
            }
            if (state.items.isEmpty() && !state.loading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No photos or videos found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            LazyVerticalGrid(
                columns = GridCells.Adaptive(110.dp),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(visible, key = { it.cacheKey }) { item ->
                    GridCell(item, state.shouldBlur(item), state.verdict(item), onClick = { onOpen(item) })
                }
            }
        }
    }
}

@Composable
private fun ScanStatus(state: GalleryState) {
    val total = state.items.size
    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        when {
            state.modelError != null -> Text(
                "Detection model failed to load: ${state.modelError}",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            total > 0 && state.scanned < total -> {
                Text(
                    "Scanning on device… ${state.scanned} / $total",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(
                    progress = { state.scanned.toFloat() / total },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            total > 0 -> Text(
                "All $total items scanned locally · ${state.flaggedCount} blurred",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun GridCell(item: MediaItem, blurred: Boolean, verdict: Verdict, onClick: () -> Unit) {
    Box(
        Modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
    ) {
        MediaThumbnail(item = item, blurred = blurred, modifier = Modifier.fillMaxSize())
        if (item.isVideo) {
            Icon(
                Icons.Filled.PlayCircle,
                contentDescription = "Video",
                tint = Color.White,
                modifier = Modifier.align(Alignment.BottomEnd).padding(6.dp).size(20.dp),
            )
        }
        if (blurred) {
            Icon(
                Icons.Filled.VisibilityOff,
                contentDescription = if (verdict == Verdict.Flagged) "Sensitive content" else "Not scanned yet",
                tint = Color.White,
                modifier = Modifier.align(Alignment.Center).size(28.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensitivityPicker(selected: com.localguard.gallery.data.Sensitivity, onSelect: (com.localguard.gallery.data.Sensitivity) -> Unit) {
    val options = com.localguard.gallery.data.Sensitivity.entries
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(option.label) }
        }
    }
}
