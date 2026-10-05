package com.localguard.gallery.ml

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect

object ImagePreprocessor {

    /** Center-crops [source] to a square, scales it to [size] and returns planar RGB floats in 0..1. */
    fun toChwFloats(source: Bitmap, size: Int): FloatArray {
        val side = minOf(source.width, source.height)
        val left = (source.width - side) / 2
        val top = (source.height - side) / 2
        val scaled = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        Canvas(scaled).drawBitmap(
            source,
            Rect(left, top, left + side, top + side),
            Rect(0, 0, size, size),
            Paint(Paint.FILTER_BITMAP_FLAG),
        )

        val pixels = IntArray(size * size)
        scaled.getPixels(pixels, 0, size, 0, 0, size, size)
        scaled.recycle()

        val plane = size * size
        val out = FloatArray(3 * plane)
        for (i in 0 until plane) {
            val c = pixels[i]
            out[i] = ((c shr 16) and 0xFF) / 255f
            out[plane + i] = ((c shr 8) and 0xFF) / 255f
            out[2 * plane + i] = (c and 0xFF) / 255f
        }
        return out
    }
}
