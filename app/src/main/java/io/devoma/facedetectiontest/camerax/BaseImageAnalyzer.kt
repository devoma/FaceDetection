package io.devoma.facedetectiontest.camerax

import android.media.Image
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import io.devoma.facedetectiontest.views.OvalGraphicOverlay

abstract class BaseImageAnalyzer<T> : ImageAnalysis.Analyzer {

    abstract val graphicOverlay: OvalGraphicOverlay

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        mediaImage?.let {
            detectInImage(InputImage.fromMediaImage(it, imageProxy.imageInfo.rotationDegrees))
                .addOnSuccessListener { results ->
                    onSuccess(
                        results,
                        graphicOverlay,
                        it
                    )
                }
                .addOnFailureListener {
                    graphicOverlay.clear()
                    graphicOverlay.postInvalidate()
                    onFailure(it)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    abstract fun stop()

    protected abstract fun detectInImage(image: InputImage): Task<T>

    protected abstract fun onSuccess(
        results: T,
        graphicOverlay: OvalGraphicOverlay,
        image: Image
    )

    protected abstract fun onFailure(e: Exception)

}