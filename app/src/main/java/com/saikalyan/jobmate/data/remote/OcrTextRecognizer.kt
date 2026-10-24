package com.saikalyan.jobmate.data.remote

import android.media.Image
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
        fun closeImage() { if (closed.compareAndSet(false, true)) image.close() }
        val input = try { InputImage.fromMediaImage(image, 0) }
        catch (failure: Exception) {
            closeImage()
            continuation.resumeWithException(failure)
            return@suspendCancellableCoroutine
        }
        try {
            recognizer.process(input)
                .addOnSuccessListener { result -> closeImage(); if (continuation.isActive) continuation.resume(result.text) }
                .addOnFailureListener { failure -> closeImage(); if (continuation.isActive) continuation.resumeWithException(failure) }
        } catch (failure: Exception) {
            closeImage()
            continuation.resumeWithException(failure)
        }
    }

    override fun close() { recognizer.close() }
}
