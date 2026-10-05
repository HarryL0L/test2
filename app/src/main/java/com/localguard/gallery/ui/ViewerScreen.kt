package com.localguard.gallery.ui

import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.localguard.gallery.data.MediaItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewerScreen(item: MediaItem, state: GalleryState, onClose: () -> Unit) {
    val shouldBlur = state.shouldBlur(item)
    var revealed by rememberSaveable(item.cacheKey) { mutableStateOf(false) }
    val hidden = shouldBlur && !revealed
    val prediction = state.predictions[item.cacheKey]

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        when {
            hidden -> MediaThumbnail(
                item = item,
                blurred = true,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            item.isVideo -> AndroidView(
                factory = { ctx ->
                    VideoView(ctx).apply {
                        setMediaController(MediaController(ctx).also { it.setAnchorView(this) })
                        setVideoURI(item.uri)
                        setOnPreparedListener { start() }
                    }
                },
                onRelease = { it.stopPlayback() },
                modifier = Modifier.fillMaxSize().align(Alignment.Center),
            )
            else -> AsyncImage(
                model = item.uri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }

        if (hidden) {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(Icons.Filled.VisibilityOff, null, tint = Color.White, modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(12.dp))
                Text(
                    when (state.verdict(item)) {
                        Verdict.Flagged -> "Sensitive content"
                        Verdict.Error -> "Couldn't scan this item"
                        else -> "Not scanned yet"
                    },
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                )
                prediction?.let {
                    Text(
                        "Explicit: ${pct(it.porn + it.hentai)} · Suggestive: ${pct(it.sexy)}",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Spacer(Modifier.height(20.dp))
                FilledTonalButton(onClick = { revealed = true }) {
                    Icon(Icons.Filled.Visibility, null)
                    Text("  Show anyway")
                }
            }
        }

        TopAppBar(
            title = { Text(item.displayName, maxLines = 1) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (shouldBlur && revealed) {
                    IconButton(onClick = { revealed = false }) {
                        Icon(Icons.Filled.VisibilityOff, contentDescription = "Hide again")
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black.copy(alpha = 0.4f),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White,
                actionIconContentColor = Color.White,
            ),
        )
    }
}

private fun pct(v: Float) = "${(v * 100).toInt()}%"
