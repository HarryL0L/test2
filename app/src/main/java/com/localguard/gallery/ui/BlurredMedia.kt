package com.localguard.gallery.ui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.size.Precision
import coil.size.Size
import com.localguard.gallery.data.MediaItem

/**
 * Draws a thumbnail of [item]. When [blurred] is true the image is decoded at a tiny
 * resolution (so explicit detail never reaches the screen, on any Android version)
 * and additionally run through a real Gaussian blur on Android 12+.
 */
@Composable
fun MediaThumbnail(
    item: MediaItem,
    blurred: Boolean,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
) {
    val context = LocalContext.current
    val request = ImageRequest.Builder(context)
        .data(item.uri)
        .apply {
            if (item.isVideo) decoderFactory(VideoFrameDecoder.Factory())
            if (blurred) {
                size(Size(BLUR_DECODE_PX, BLUR_DECODE_PX))
                precision(Precision.EXACT)
                memoryCacheKey("blur:${item.cacheKey}")
            }
        }
        .crossfade(!blurred)
        .build()

    Box(modifier.clipToBounds().background(Color.DarkGray)) {
        AsyncImage(
            model = request,
            contentDescription = item.displayName,
            contentScale = contentScale,
            filterQuality = if (blurred) FilterQuality.High else FilterQuality.Low,
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (blurred && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        Modifier.blur(24.dp)
                    } else {
                        Modifier
                    },
                ),
        )
    }
}

private const val BLUR_DECODE_PX = 12
