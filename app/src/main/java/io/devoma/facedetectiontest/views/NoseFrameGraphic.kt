package io.devoma.facedetectiontest.views

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

/**
 * Draws a nose frame rect when a face is detected within the [OvalGraphicOverlay].
 */
class NoseFrameGraphic(
    private val noseLeft: Float,
    private val noseTop: Float,
    private val noseWidth: Float,
    private val noseHeight: Float,
    strokeWidth: Float
) : OvalGraphicOverlay.Graphic() {

    var strokeColor = Color.RED

    private val boxPaint = Paint()

    init {
        boxPaint.color = strokeColor
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = strokeWidth

        // See: https://developer.android.com/reference/android/graphics/Rect
        require(noseLeft <= (noseLeft + noseWidth))
        require(noseTop <= (noseTop + noseHeight))
    }

    override fun draw(canvas: Canvas?) {
        val noseRect = Rect().apply {
            top = noseTop.toInt()
            left = noseLeft.toInt()
            right = (noseWidth + left).toInt()
            bottom = (noseHeight + top).toInt()
        }
        canvas?.drawRect(noseRect, boxPaint)
    }
}