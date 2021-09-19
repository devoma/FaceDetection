package io.devoma.facedetectiontest.misc

import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import androidx.camera.view.PreviewView

/**
 * Returns a correction matrix that can be used to translate [ImageProxy] x, y coordinates
 * to [PreviewView] x, y coordinates.
 */
fun PreviewView.getCorrectionMatrix(imageProxy: ImageProxy): Matrix {
    val cropRect = imageProxy.cropRect
    val rotationDegrees = imageProxy.imageInfo.rotationDegrees
    val matrix = Matrix()

    // A float array of the source vertices (crop rect) in clockwise order.
    val source = floatArrayOf(
        cropRect.left.toFloat(),
        cropRect.top.toFloat(),
        cropRect.right.toFloat(),
        cropRect.top.toFloat(),
        cropRect.right.toFloat(),
        cropRect.bottom.toFloat(),
        cropRect.left.toFloat(),
        cropRect.bottom.toFloat()
    )

    // A float array of the destination vertices in clockwise order.
    val destination = floatArrayOf(
        0f,
        0f,
        width.toFloat(),
        0f,
        width.toFloat(),
        height.toFloat(),
        0f,
        height.toFloat()
    )

    // The destination vertexes need to be shifted based on rotation degrees. The
    // rotation degree represents the clockwise rotation needed to correct the image.

    // Each vertex is represented by 2 float numbers in the vertices array.
    val vertexSize = 2
    // The destination needs to be shifted 1 vertex for every 90° rotation.
    val shiftOffset = rotationDegrees / 90 * vertexSize
    val tempArray = destination.clone()
    for (toIndex in source.indices) {
        val fromIndex = (toIndex + shiftOffset) % source.size
        destination[toIndex] = tempArray[fromIndex]
    }
    matrix.setPolyToPoly(source, 0, destination, 0, 4)
    return matrix
}