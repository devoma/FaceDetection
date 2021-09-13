package io.devoma.facedetectiontest

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.devoma.facedetectiontest.camerax.CameraManager
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var cameraManager: CameraManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createCameraManager()

        if (allPermissionsGranted()) {
            cameraManager.startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }
        setOverlayGraphicPosition()

        graphicOverlay.onFaceDetected()
            .observe(this, { faceDetected ->
                val stringResource = if (faceDetected) {
                    R.string.make_sure_your_face_is_within_the_circle
                } else {
                    R.string.no_face_detected
                }
                message.text = resources.getString(stringResource)
            })
        graphicOverlay.onFaceDetectedInBounds()
            .observe(this, { faceDetectedInBounds ->
                if (faceDetectedInBounds) {
                    message.text = resources.getString(R.string.point_your_nose_to_the_square)
                }
            })
    }

    private fun createCameraManager() {
        cameraManager = CameraManager(
            context = this,
            finderView = viewFinder,
            lifecycleOwner = this,
            graphicOverlay = graphicOverlay
        )
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
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

    // TODO(Remove this method or change as needed)
    private fun setOverlayGraphicPosition() {
        graphicOverlay.run {
            noFaceDetectedColor = Color.TRANSPARENT // Default value is transparent as well
            faceDetectedColor = Color.RED // Default value is red as well
            faceDetectedInBoundsColor = Color.GREEN // Default value is green as well
            areaHeight = 1000F
            areaWidth = 800F
            areaLeft = 120F
            areaTop = 500F
            noseTop = 800F
            noseLeft = 400F
            noseHeight = 60F
            noseWidth = 60F
        }
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}