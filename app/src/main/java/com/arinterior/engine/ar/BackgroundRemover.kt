package com.arinterior.engine.ar

import android.content.Context
import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit Subject Segmentation for background removal.
 * On-device, free, no API calls needed.
 */
class BackgroundRemover(context: Context) {

    private val segmenter = SubjectSegmentation.getClient(
        SubjectSegmenterOptions.Builder()
            .enableForegroundBitmap()
            .build()
    )

    /**
     * Remove background from bitmap, return foreground only with transparent background.
     */
    suspend fun removeBackground(bitmap: Bitmap): Bitmap {
        return suspendCancellableCoroutine { continuation ->
            val inputImage = InputImage.fromBitmap(bitmap, 0)

            segmenter.process(inputImage)
                .addOnSuccessListener { result ->
                    val foregroundBitmap = result.foregroundBitmap
                    if (foregroundBitmap != null) {
                        continuation.resume(foregroundBitmap)
                    } else {
                        // Fallback: return original if segmentation fails
                        continuation.resume(bitmap)
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resumeWithException(e)
                }
        }
    }

    fun close() {
        segmenter.close()
    }
}
