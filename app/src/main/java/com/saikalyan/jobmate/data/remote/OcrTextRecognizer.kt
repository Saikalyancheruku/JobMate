package com.saikalyan.jobmate.data.remote

import android.media.Image
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.PixelFormat
import java.nio.ByteBuffer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.Closeable
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class OcrTextRecognizer @Inject constructor() : Closeable {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognize(image: Image): String = suspendCancellableCoroutine { continuation ->
        val closed = AtomicBoolean(false)
        val bitmap = try {
            if (image.format == PixelFormat.RGBA_8888) rgbaToBitmap(image) else null
        } catch (failure: Exception) {
            image.close()
            continuation.resumeWithException(failure)
            return@suspendCancellableCoroutine
        }
        fun closeResources() {
            if (closed.compareAndSet(false, true)) {
                image.close()
                bitmap?.recycle()
            }
        }
        val input = try {
            if (image.format == ImageFormat.YUV_420_888) InputImage.fromMediaImage(image, 0)
            else bitmap?.let { InputImage.fromBitmap(it, 0) }
                ?: throw IllegalArgumentException("Unsupported screen image format: ${image.format}")
        }
        catch (failure: Exception) {
            closeResources()
            continuation.resumeWithException(failure)
            return@suspendCancellableCoroutine
        }
        try {
            recognizer.process(input)
                .addOnSuccessListener { result -> closeResources(); if (continuation.isActive) continuation.resume(result.text) }
                .addOnFailureListener { failure -> closeResources(); if (continuation.isActive) continuation.resumeWithException(failure) }
        } catch (failure: Exception) {
            closeResources()
            continuation.resumeWithException(failure)
        }
    }

    private fun rgbaToBitmap(image: Image): Bitmap {
        val plane = image.planes.firstOrNull() ?: throw IllegalArgumentException("Screen image has no pixel plane.")
        val pixelStride = plane.pixelStride
        require(pixelStride == 4) { "Unexpected RGBA pixel stride: $pixelStride" }
        val rowByteCount = image.width * pixelStride
        val packedPixels = ByteBuffer.allocateDirect(rowByteCount * image.height)
        val source = plane.buffer.duplicate()
        val row = ByteArray(rowByteCount)
        for (y in 0 until image.height) {
            source.position(y * plane.rowStride)
            source.get(row, 0, rowByteCount)
            packedPixels.put(row)
        }
        packedPixels.flip()
        return Bitmap.createBitmap(image.width, image.height, Bitmap.Config.ARGB_8888).apply {
            copyPixelsFromBuffer(packedPixels)
        }
    }

    override fun close() { recognizer.close() }
}
