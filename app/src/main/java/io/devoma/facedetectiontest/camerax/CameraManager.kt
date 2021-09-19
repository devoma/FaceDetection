package io.devoma.facedetectiontest.camerax

import android.content.Context
import android.util.DisplayMetrics
import android.util.Log
import android.util.Rational
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.MutableLiveData
import io.devoma.facedetectiontest.MainActivity
import io.devoma.facedetectiontest.faceDetection.FaceContourDetectionProcessor
import io.devoma.facedetectiontest.views.OvalGraphicOverlay
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

@androidx.camera.core.ExperimentalGetImage
@androidx.camera.core.ExperimentalUseCaseGroup
@androidx.camera.lifecycle.ExperimentalUseCaseGroupLifecycle
class CameraManager(
    private val context: Context,
    private val finderView: PreviewView,
    private val lifecycleOwner: LifecycleOwner,
    private val graphicOverlay: OvalGraphicOverlay
) {

    private var preview: Preview? = null
    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageAnalyzer: ImageAnalysis? = null
    private lateinit var faceContourDetectionProcessor: FaceContourDetectionProcessor

    lateinit var cameraExecutor: ExecutorService
    lateinit var imageCapture: ImageCapture
    lateinit var metrics: DisplayMetrics

    var rotation: Float = 0f
    var cameraSelectorOption = CameraSelector.LENS_FACING_FRONT

    init {
        createNewExecutor()
    }

    private fun createNewExecutor() {
        cameraExecutor = Executors.newSingleThreadExecutor()
    }

    fun getAnalyzer(): ImageAnalysis.Analyzer {
        if (!::faceContourDetectionProcessor.isInitialized) {
            faceContourDetectionProcessor = FaceContourDetectionProcessor(graphicOverlay)
        }
        return faceContourDetectionProcessor
    }

    private fun setCameraConfig(
        cameraProvider: ProcessCameraProvider?,
        cameraSelector: CameraSelector,
        useCaseGroup: UseCaseGroup
    ) {
        try {
            cameraProvider?.unbindAll()
            camera = cameraProvider?.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                useCaseGroup
            )
            preview?.setSurfaceProvider(finderView.surfaceProvider)
        } catch (e: Exception) {
            Log.e(TAG, "Use case binding failed", e)
        }
    }

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                cameraProvider = cameraProviderFuture.get()

                // Get screen metrics used to setup camera for full screen resolution
                metrics = DisplayMetrics().also { finderView.display.getRealMetrics(it) }
                Log.d(TAG, "Screen metrics: ${metrics.widthPixels} x ${metrics.heightPixels}")

                val screenAspectRatio = aspectRatio(metrics.widthPixels, metrics.heightPixels)
                Log.d(TAG, "Preview aspect ratio: $screenAspectRatio")

                val rotation = finderView.display.rotation

                // CameraSelector
                val cameraSelector = CameraSelector.Builder()
                    .requireLensFacing(cameraSelectorOption)
                    .build()

                // Preview
                preview = Preview.Builder()
                    // We request aspect ratio but no resolution
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(rotation)
                    .build()

                // ImageCapture
                imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    // We request aspect ratio but no resolution to match preview config, but letting
                    // CameraX optimize for whatever specific resolution best fits our use cases
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(rotation)
                    .build()

                // ImageAnalysis
                imageAnalyzer = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    // We request aspect ratio but no resolution
                    .setTargetAspectRatio(screenAspectRatio)
                    .setTargetRotation(rotation)
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, getAnalyzer())
                    }

                val viewPort = finderView.viewPort!!

                val useCaseGroup = UseCaseGroup.Builder()
                    .addUseCase(preview!!)
                    .addUseCase(imageAnalyzer!!)
                    .addUseCase(imageCapture)
                    .setViewPort(viewPort)
                    .build()

                setCameraConfig(cameraProvider, cameraSelector, useCaseGroup)

            }, ContextCompat.getMainExecutor(context)
        )
    }

    fun changeCameraSelector() {
        cameraProvider?.unbindAll()
        cameraSelectorOption =
            if (cameraSelectorOption == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
            else CameraSelector.LENS_FACING_BACK
        graphicOverlay.toggleSelector()
        startCamera()
    }

    fun isHorizontalMode(): Boolean {
        return rotation == 90f || rotation == 270f
    }

    fun isFrontMode(): Boolean {
        return cameraSelectorOption == CameraSelector.LENS_FACING_FRONT
    }

    private val onCaptureImage = MutableLiveData<ImageProxy>()

    /**
     *  [androidx.camera.core.ImageAnalysis.Builder] requires enum value of
     *  [androidx.camera.core.AspectRatio]. Currently it has values of 4:3 & 16:9.
     *
     *  Detecting the most suitable ratio for dimensions provided in @params by counting absolute
     *  of preview ratio to one of the provided values.
     *
     *  @param width - preview width
     *  @param height - preview height
     *  @return suitable aspect ratio
     */
    private fun aspectRatio(width: Int, height: Int): Int {
        val previewRatio = max(width, height).toDouble() / min(width, height)
        if (abs(previewRatio - RATIO_4_3_VALUE) <= abs(previewRatio - RATIO_16_9_VALUE)) {
            return AspectRatio.RATIO_4_3
        }
        return AspectRatio.RATIO_16_9
    }

    /**
     * Returns a [Rational] for provided [AspectRatio]
     *
     * @param aspectRatio - AspectRatio
     * @return rational for aspectRatio
     */
    private fun aspectRatioToRational(aspectRatio: Int): Rational {
        val numerator = if (aspectRatio == AspectRatio.RATIO_16_9) 16 else 4
        val denominator = if (aspectRatio == AspectRatio.RATIO_16_9) 9 else 3
        return Rational(numerator, denominator)
    }

    /**
     * Returns an MutableLiveData that emits after a successful image capture.
     */
    fun onImageCaptured() = onCaptureImage

    /**
     * Initiates image capture.
     */
    fun captureImage() {
        imageCapture.takePicture(cameraExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                (context as MainActivity).runOnUiThread {
                    onCaptureImage.value = image
                }
                super.onCaptureSuccess(image)
            }
        })
    }

    companion object {
        private const val RATIO_4_3_VALUE = 4.0 / 3.0
        private const val RATIO_16_9_VALUE = 16.0 / 9.0
        private const val TAG = "CameraXBasic"
    }

}