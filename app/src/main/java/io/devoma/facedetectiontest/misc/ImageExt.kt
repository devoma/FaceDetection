package io.devoma.facedetectiontest.misc

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.Image
import androidx.camera.core.ImageProxy
import io.devoma.facedetectiontest.utils.YuvToRgbConverter


/**
 * Returns a [Bitmap] for [Image] using [YuvToRgbConverter]
 */
fun Image.toBitMap(context: Context): Bitmap {
    val yuvToRgbConverter = YuvToRgbConverter(context)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    yuvToRgbConverter.yuvToRgb(this, bitmap)
    return bitmap
}

/**
 * Returns a [Bitmap] for [ImageProxy]. Only works with JPEG
 */
fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    buffer.rewind()
    val bytes = ByteArray(buffer.capacity())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}