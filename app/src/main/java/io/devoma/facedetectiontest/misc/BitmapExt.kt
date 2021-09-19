package io.devoma.facedetectiontest.misc

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat.JPEG
import android.graphics.Matrix
import android.graphics.Rect
import android.util.Base64
import java.io.ByteArrayOutputStream


/**
 * Rotate a bitmap by given [degrees]
 */
fun Bitmap.rotate(degrees: Float): Bitmap {
    val matrix = Matrix()
    matrix.preRotate(degrees)
    val rotatedBitmap = Bitmap.createBitmap(
        this, 0, 0, width, height, matrix, true
    )
    recycle()
    return rotatedBitmap
}

/**
 * Returns a flipped/mirrored bitmap
 */
fun Bitmap.flip(): Bitmap {
    val matrix = Matrix()
    matrix.preScale(-1f, 1f)
    val flippedBitmap = Bitmap.createBitmap(
        this, 0, 0, width, height, matrix, true
    )
    recycle()
    return flippedBitmap
}

/**
 * Crop a [Bitmap]
 */
fun Bitmap.crop(cropRect: Rect): Bitmap {
    if (cropRect.width() > width || cropRect.height() > height) {
        throw Exception("Crop rect size exceeds the source image.")
    }
    val croppedBitmap = Bitmap.createBitmap(
        this, cropRect.left, cropRect.top, cropRect.width(), cropRect.height()
    )
    recycle()
    return croppedBitmap
}

/**
 * Encodes a [Bitmap] in [Base64]
 */
fun Bitmap.base64Encoded(): String {
    val byteArrayOutputStream = ByteArrayOutputStream()
    compress(JPEG, 100, byteArrayOutputStream)
    val byteArray: ByteArray = byteArrayOutputStream.toByteArray()
    return Base64.encodeToString(
        byteArray,
        Base64.DEFAULT
    )
}