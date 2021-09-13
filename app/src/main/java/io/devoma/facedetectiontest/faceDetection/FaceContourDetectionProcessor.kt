package io.devoma.facedetectiontest.faceDetection

import android.media.Image
import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import io.devoma.facedetectiontest.camerax.BaseImageAnalyzer
import io.devoma.facedetectiontest.views.OvalGraphicOverlay
import java.io.IOException

class FaceContourDetectionProcessor(private val view: OvalGraphicOverlay) :
    BaseImageAnalyzer<List<Face>>() {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
//        .enableTracking()
        .build()

    private val detector = FaceDetection.getClient(realTimeOpts)

    override val graphicOverlay: OvalGraphicOverlay
        get() = view

    override fun detectInImage(image: InputImage): Task<List<Face>> {
        return detector.process(image)
    }

    override fun stop() {
        try {
            detector.close()
        } catch (e: IOException) {
            Log.e(TAG, "Exception thrown while trying to close Face Detector: $e")
        }
    }

    override fun onSuccess(
        results: List<Face>,
        graphicOverlay: OvalGraphicOverlay,
        image: Image
    ) {
        graphicOverlay.clear()
        graphicOverlay.onFaceDetected(results, image)
        // TODO(Uncomment the following to display face contours or remove if not needed)
//        results.forEach {
//            val faceGraphic = FaceContourGraphic(graphicOverlay, it, image.cropRect)
//            graphicOverlay.add(faceGraphic)
//        }
        graphicOverlay.postInvalidate()
    }

    override fun onFailure(e: Exception) {
        Log.w(TAG, "Face Detector failed.$e")
    }

    companion object {
        private const val TAG = "FaceDetectorProcessor"
    }

}