package io.devoma.facedetectiontest.views

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect

class NoseFrameGraphic(
    private val noseRect: Rect,
    strokeWidth: Float
) : OvalGraphicOverlay.Graphic() {

    var strokeColor = Color.RED

    private val boxPaint = Paint()

    init {
        boxPaint.color = strokeColor
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = strokeWidth
    }

    override fun draw(canvas: Canvas?) {
        canvas?.drawRect(noseRect, boxPaint)
    }
}