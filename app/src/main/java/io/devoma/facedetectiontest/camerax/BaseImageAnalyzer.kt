package io.devoma.facedetectiontest.camerax

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import io.devoma.facedetectiontest.misc.crop
import io.devoma.facedetectiontest.misc.toBitMap
import io.devoma.facedetectiontest.views.OvalGraphicOverlay

abstract class BaseImageAnalyzer<T> : ImageAnalysis.Analyzer {

    abstract val graphicOverlay: OvalGraphicOverlay

    @androidx.camera.core.ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        mediaImage?.let { image ->
            // TODO This is inefficient. Should use YUV_420_888 format for camera2 API
            val inputImage = InputImage.fromBitmap(
                image.toBitMap(graphicOverlay.context).crop(imageProxy.cropRect),
                imageProxy.imageInfo.rotationDegrees
            )
            detectInImage(inputImage)
                .addOnSuccessListener { results ->
                    onSuccess(
                        results,
                        graphicOverlay,
                        imageProxy
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
        imageProxy: ImageProxy
    )

    protected abstract fun onFailure(e: Exception)

}