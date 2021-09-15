package io.devoma.facedetectiontest

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Size
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.devoma.facedetectiontest.camerax.CameraManager
import io.devoma.facedetectiontest.databinding.ActivityMainBinding
import io.devoma.facedetectiontest.misc.toPx

class MainActivity : AppCompatActivity() {
    private val binding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }
    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        createCameraManager()

        if (allPermissionsGranted()) {
            cameraManager.startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        setOverlayGraphicPosition()

        binding.graphicOverlay.onFaceDetected()
            .observe(this, {
                if (it.first) {
                    binding.message.text = resources.getString(
                        R.string.make_sure_your_face_is_within_the_circle
                    )
                    // TODO Face is detected. Do anything with mediaImageBitmap here
                    val mediaImageBitmap = it.second
                } else {
                    binding.message.text = resources.getString(R.string.no_face_detected)
                }
            })
        binding.graphicOverlay.onFaceDetectedInBounds()
            .observe(this, {
                if (it.first) {
                    binding.message.text =
                        resources.getString(R.string.point_your_nose_to_the_square)
                    // TODO Face is detected. Do anything with mediaImageBitmap here
                    val mediaImageBitmap = it.second
                }
            })
        binding.graphicOverlay.onNoseDetectedInBounds()
            .observe(this, {
                if (it.first) {
                    binding.message.text = resources.getString(R.string.yay_nose_detected)
                }
            })
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

    // TODO(Change as required. Do not remove.)
    private fun setOverlayGraphicPosition() {
        val metrics = DisplayMetrics().apply {
            windowManager.defaultDisplay.getRealMetrics(this)
        }
        val deviceWidth = metrics.widthPixels
        val deviceHeight = metrics.heightPixels

        val noseFrameLeft = 171F // TODO Fetch this from server
        val noseFrameTop = 251F // TODO Fetch this from server
        binding.graphicOverlay.run {
            areaWidth = deviceWidth * OVAL_FRAME_WIDTH_MULTIPLIER
            areaHeight = areaWidth * OVAL_FRAME_HEIGHT_MULTIPLIER // 5:4 aspect ratio
            areaLeft = deviceWidth * OVAL_FRAME_LEFT_MULTIPLIER
            areaTop = deviceHeight * OVAL_FRAME_TOP_MULTIPLIER
            noseTop = deviceHeight / (SERVER_DEVICE_RESOLUTION.height / noseFrameTop)
            noseLeft = deviceWidth / (SERVER_DEVICE_RESOLUTION.width / noseFrameLeft)
            noseHeight = 20.toPx
            noseWidth = 20.toPx
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

        private const val OVAL_FRAME_WIDTH_MULTIPLIER = 3F / 4F
        private const val OVAL_FRAME_HEIGHT_MULTIPLIER = 5F / 4F
        private const val OVAL_FRAME_LEFT_MULTIPLIER = 1F / 8F
        private const val OVAL_FRAME_TOP_MULTIPLIER = 1F / 5F
        private val SERVER_DEVICE_RESOLUTION = Size(480, 640)
    }
}