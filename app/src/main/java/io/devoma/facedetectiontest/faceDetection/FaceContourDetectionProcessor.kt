package io.devoma.facedetectiontest.faceDetection

import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import io.devoma.facedetectiontest.camerax.BaseImageAnalyzer
import io.devoma.facedetectiontest.views.OvalGraphicOverlay
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import java.io.IOException

/**
 * Interface for Face detection
 */
interface FaceAnalyzer {
    /**
     * Returns an Observable that emits when no face is detected. The emission is an [ImageProxy].
     */
    fun onNothing(): Observable<ImageProxy>

    /**
     * Returns an Observable that emits when a face is detected. The emission is an [ImageProxy].
     */
    fun onFaceDetected(): Observable<ImageProxy>

    /**
     * Returns an Observable that emits when a face is enclosed in oval frame bounds.
     * The emission is an [ImageProxy].
     */
    fun onFaceDetectedInBounds(): Observable<ImageProxy>

    /**
     * Returns an Observable that emits when nose tip is enclosed in nose frame bounds.
     * The emission is an [ImageProxy].
     */
    fun onNoseDetectedInBounds(): Observable<ImageProxy>
}

class FaceContourDetectionProcessor(private val view: OvalGraphicOverlay) :
    BaseImageAnalyzer<List<Face>>(), FaceAnalyzer {

    private val realTimeOpts = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
        .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
        .build()

    private val detector = FaceDetection.getClient(realTimeOpts)

    private val onNothing = PublishSubject.create<ImageProxy>()
    private val onFaceDetected = PublishSubject.create<ImageProxy>()
    private val onFaceDetectedInBounds = PublishSubject.create<ImageProxy>()
    private val onNoseDetectedInBounds = PublishSubject.create<ImageProxy>()

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
        imageProxy: ImageProxy
    ) {
        val isFaceDetected = results.isNotEmpty()
        graphicOverlay.clear()

        if (isFaceDetected) {
            onFaceDetected.onNext(imageProxy)
            graphicOverlay.setFaceDetected(true)

            // Check if face is with in oval frame bounds
            val face = results.first() // Only single face detection is supported
            val imageRect = imageProxy.cropRect
            val faceRect = graphicOverlay.calculateRect(
                height = imageRect.height().toFloat(),
                width = imageRect.width().toFloat(),
                boundingBoxT = face.boundingBox
            )
            if (graphicOverlay.getOvalFrameRect().contains(faceRect)) {
                onFaceDetectedInBounds.onNext(imageProxy)
                graphicOverlay.setFaceDetectedInBounds(true)
            }
            if (checkNoseEnclosed(face)) {
                onNoseDetectedInBounds.onNext(imageProxy)
                graphicOverlay.setNoseDetectedInBounds(true)
            }
        } else {
            onNothing.onNext(imageProxy)
            graphicOverlay.setFaceDetected(false)
            graphicOverlay.setFaceDetectedInBounds(false)
            graphicOverlay.setNoseDetectedInBounds(false)
        }

        graphicOverlay.postInvalidate()
    }

    override fun onFailure(e: Exception) {
        Log.w(TAG, "Face Detector failed.$e")
    }

    /**
     * Returns an Observable that emits when no face is detected. The emission is an [ImageProxy].
     */
    override fun onNothing(): Observable<ImageProxy> = onNothing

    /**
     * Returns an Observable that emits when a face is detected. The emission is an [ImageProxy].
     */
    override fun onFaceDetected(): Observable<ImageProxy> = onFaceDetected

    /**
     * Returns an Observable that emits when a face is enclosed in oval frame bounds.
     * The emission is an [ImageProxy].
     */
    override fun onFaceDetectedInBounds(): Observable<ImageProxy> = onFaceDetectedInBounds

    /**
     * Returns an Observable that emits when nose tip is enclosed in nose frame bounds.
     * The emission is an [ImageProxy].
     */
    override fun onNoseDetectedInBounds(): Observable<ImageProxy> = onNoseDetectedInBounds

    // Returns true if nose tip is enclosed in nose frame
    private fun checkNoseEnclosed(face: Face): Boolean {
        val noseBridgeContour = face.getContour(FaceContour.NOSE_BRIDGE)
        val noseTipF = noseBridgeContour?.points?.last() ?: return false
        return graphicOverlay.getNoseFrameRect().contains(
            graphicOverlay.translateX(noseTipF.x).toInt(),
            graphicOverlay.translateY(noseTipF.y).toInt()
        )
    }

    companion object {
        private const val TAG = "FaceDetectorProcessor"
    }
}