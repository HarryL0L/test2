package com.localguard.gallery.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.os.Build
import android.util.Size
import com.localguard.gallery.ml.NsfwClassifier
import com.localguard.gallery.ml.NsfwPrediction

/** Turns a gallery item into one or more bitmaps and classifies them on the device. */
class MediaScanner(private val context: Context, private val classifier: NsfwClassifier) {

    fun scan(item: MediaItem): NsfwPrediction? =
        if (item.isVideo) scanVideo(item) else scanImage(item)

    private fun scanImage(item: MediaItem): NsfwPrediction? {
        val bitmap = decodeImage(item) ?: return null
        return try {
            classifier.classify(bitmap)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeImage(item: MediaItem): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            // Small, fast thumbnail from MediaStore; plenty for a 224px model.
            context.contentResolver.loadThumbnail(item.uri, Size(THUMB, THUMB), null)
        } else {
            val source = ImageDecoder.createSource(context.contentResolver, item.uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                val scale = THUMB.toFloat() / maxOf(info.size.width, info.size.height)
                if (scale < 1f) {
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1),
                    )
                }
            }
        }
    }.getOrNull()?.let(::toSoftware)

    /** Samples several frames across the video and keeps the most explicit one. */
    private fun scanVideo(item: MediaItem): NsfwPrediction? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, item.uri)
            val durationUs = (retriever
                .extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L) * 1000
            val times = if (durationUs <= 0) {
                listOf(0L)
            } else {
                (1..VIDEO_FRAMES).map { durationUs * it / (VIDEO_FRAMES + 1) }
            }
            times.mapNotNull { t -> frameAt(retriever, t) }
                .map { frame ->
                    try {
                        classifier.classify(frame)
                    } finally {
                        frame.recycle()
                    }
                }
                .maxByOrNull { it.porn + it.hentai + it.sexy }
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }

    private fun frameAt(retriever: MediaMetadataRetriever, timeUs: Long): Bitmap? = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            retriever.getScaledFrameAtTime(
                timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, THUMB, THUMB,
            )
        } else {
            retriever.getFrameAtTime(timeUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
        }
    }.getOrNull()?.let(::toSoftware)

    private fun toSoftware(b: Bitmap): Bitmap =
        if (b.config == Bitmap.Config.HARDWARE) {
            b.copy(Bitmap.Config.ARGB_8888, false).also { b.recycle() }
        } else {
            b
        }

    companion object {
        private const val THUMB = 320
        private const val VIDEO_FRAMES = 6
    }
}
