package io.devoma.facedetectiontest

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.volley.Request.Method.POST
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import io.devoma.facedetectiontest.camerax.CameraManager
import io.devoma.facedetectiontest.databinding.ActivityMainBinding
import io.devoma.facedetectiontest.faceDetection.FaceAnalyzer
import io.devoma.facedetectiontest.misc.*
import io.reactivex.disposables.CompositeDisposable
import org.json.JSONObject

@androidx.camera.core.ExperimentalGetImage
@androidx.camera.core.ExperimentalUseCaseGroup
@androidx.camera.lifecycle.ExperimentalUseCaseGroupLifecycle
class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private var disposables: CompositeDisposable? = null
    private lateinit var cameraManager: CameraManager
    private lateinit var response: JSONObject
    private lateinit var frameSize: Size
    private val imageArrayDeque = ArrayDeque<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        createCameraManager()
        if (allPermissionsGranted()) {
            // Avoid null finder view
            Handler(Looper.getMainLooper()).postDelayed({ cameraManager.startCamera() }, 2000)
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
    }

    override fun onResume() {
        super.onResume()
        disposables = CompositeDisposable()

        val faceAnalyzer = cameraManager.getAnalyzer() as FaceAnalyzer
        disposables += faceAnalyzer.onNothing()
            .subscribe {
                binding.message.text = resources.getString(
                    R.string.no_face_detected
                )
                if (!::frameSize.isInitialized) {
                    val bitmap = it.image!!
                        .toBitMap(this)
                        .crop(it.cropRect)
                        .rotate(it.imageInfo.rotationDegrees.toFloat())
                    frameSize = Size(bitmap.width, bitmap.height) // Image size sent to server
                    Log.d("Resolution", "${bitmap.height} x ${bitmap.width}")
                    initGraphicOverlay() // TODO show spinner & fetch server response
                }
            }
        disposables += faceAnalyzer.onFaceDetected()
            .subscribe {
                binding.message.text = resources.getString(
                    R.string.make_sure_your_face_is_within_the_circle
                )
                // TODO remove-this
                showPreview(
                    it.image!!
                        .toBitMap(this)
                        .crop(it.cropRect)
                        .rotate(it.imageInfo.rotationDegrees.toFloat())
                        .flip(),
                    binding.preview
                )
            }
        disposables += faceAnalyzer.onFaceDetectedInBounds()
            .subscribe {
                binding.message.text = resources.getString(
                    R.string.point_your_nose_to_the_square
                )
                val base64EncodedJpeg = it.image!!
                    .toBitMap(this)
                    .crop(it.cropRect)
                    .rotate(it.imageInfo.rotationDegrees.toFloat())
                    .flip()
                    .base64Encoded()
                // TODO Compress / Use frame / Save to list
                // TODO remove-this
                showPreview(base64EncodedJpeg.decodeToBitmap(), binding.previewEncoded)
            }
        disposables += faceAnalyzer.onNoseDetectedInBounds()
            .subscribe {
                binding.message.text = resources.getString(R.string.yay_nose_detected)
                // TODO Stop camera and verify user
            }
    }

    override fun onPause() {
        super.onPause()
        disposables?.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraManager.cameraExecutor.shutdown()
    }

    private fun createCameraManager() {
        cameraManager = CameraManager(
            context = this,
            finderView = binding.viewFinder,
            lifecycleOwner = this,
            graphicOverlay = binding.graphicOverlay
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                cameraManager.startCamera()
            } else {
                Toast.makeText(
                    this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
            }
        }
    }

    private fun initGraphicOverlay() {
        binding.progressIndicator.show()
        val requestQueue = Volley.newRequestQueue(this)
        val jsonRequest = JSONObject().apply {
            put("userId", "0688291904")
            put("imageWidth", frameSize.width)
            put("imageHeight", frameSize.height)
        }
        val listener = Response.Listener<JSONObject> { response ->
            this.response = response
            setOverlayGraphicPosition(response)
            Toast.makeText(this, "Coordinates applied!", Toast.LENGTH_LONG).show()
            Log.d(TAG, "Response: $response")
            requestQueue.stop()
            binding.progressIndicator.hide()
        }
        val request = JsonObjectRequest(
            POST, AWS_API_URL, jsonRequest, listener, null
        )
        requestQueue.add(request)
        requestQueue.start()
    }

    private fun setOverlayGraphicPosition(serverResponse: JSONObject) {
        val areaHeight = serverResponse.getInt("areaHeight").toFloat()
        val areaWidth = serverResponse.getInt("areaWidth").toFloat()
        val areaLeft = serverResponse.getInt("areaLeft").toFloat()
        val areaTop = serverResponse.getInt("areaTop").toFloat()
        val noseLeft = serverResponse.getInt("noseLeft").toFloat()
        val noseTop = serverResponse.getInt("noseTop").toFloat()
        val noseHeight = serverResponse.getInt("noseHeight").toFloat()
        val noseWidth = serverResponse.getInt("noseWidth").toFloat()

        binding.graphicOverlay.run {
            this.areaWidth = this@MainActivity.translateX(areaWidth)
            this.areaHeight = this@MainActivity.translateY(areaHeight)
            this.areaLeft = this@MainActivity.translateX(areaLeft)
            this.areaTop = this@MainActivity.translateY(areaTop)
            this.noseLeft = this@MainActivity.translateX(noseLeft)
            this.noseTop = this@MainActivity.translateY(noseTop)
            this.noseHeight = noseHeight
            this.noseWidth = noseWidth
        }
    }

    private fun translateX(x: Float): Float {
        val imageWidth = frameSize.width.toFloat()
//        val preview = binding.viewFinder.getChildAt(0)
//        val previewWidth = preview.height * preview.scaleY
        val previewWidth = binding.viewFinder.width // ScaleType.FILL_CENTER
        return ((previewWidth / imageWidth) * x)
    }

    private fun translateY(y: Float): Float {
        val imageHeight = frameSize.height.toFloat()
        val preview = binding.viewFinder.getChildAt(0)
        val previewHeight = preview.height * preview.scaleY
        return ((previewHeight / imageHeight) * y)
    }

    // TODO remove-this
    private fun showPreview(bitmap: Bitmap, preview: AppCompatImageView) {
        if (!::response.isInitialized) return
        val noseLeft = response.getInt("noseLeft")
        val noseTop = response.getInt("noseTop")
        val noseHeight = response.getInt("noseHeight")
        val noseWidth = response.getInt("noseWidth")

        preview.setImageBitmap(
            bitmap.apply {
                Canvas(this).drawRect(
                    Rect(noseLeft, noseTop, noseWidth + noseLeft, noseTop + noseHeight),
                    Paint().apply { color = Color.YELLOW }
                )
            }
        )
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private const val AWS_API_URL =
            "https://3s6irq4jm5.execute-api.us-east-2.amazonaws.com/Prod/challenge/start"
    }
}