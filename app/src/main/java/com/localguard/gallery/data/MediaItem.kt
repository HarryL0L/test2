package com.localguard.gallery.data

import android.net.Uri

data class MediaItem(
    val id: Long,
    val uri: Uri,
    val isVideo: Boolean,
    val dateModified: Long,
    val displayName: String,
) {
    /** Changes whenever the file is edited, so stale scan results are not reused. */
    val cacheKey: String get() = "${if (isVideo) "v" else "i"}$id:$dateModified"
}
