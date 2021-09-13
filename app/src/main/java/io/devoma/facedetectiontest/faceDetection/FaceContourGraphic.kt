package io.devoma.facedetectiontest.faceDetection

import android.graphics.*
import androidx.annotation.ColorInt
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import io.devoma.facedetectiontest.views.OvalGraphicOverlay

class FaceContourGraphic(
    private val overlay: OvalGraphicOverlay,
    private val face: Face,
    private val imageRect: Rect
) : OvalGraphicOverlay.Graphic() {

    private val facePositionPaint: Paint
    private val idPaint: Paint
    private val boxPaint: Paint

    init {
        val selectedColor = Color.WHITE

        facePositionPaint = Paint()
        facePositionPaint.color = selectedColor

        idPaint = Paint()
        idPaint.color = selectedColor
        idPaint.textSize = ID_TEXT_SIZE

        boxPaint = Paint()
        boxPaint.color = selectedColor
        boxPaint.style = Paint.Style.STROKE
        boxPaint.strokeWidth = BOX_STROKE_WIDTH
    }

    private fun Canvas.drawFace(facePosition: Int, @ColorInt selectedColor: Int) {
        val contour = face.getContour(facePosition)
        val path = Path()
        contour?.points?.forEachIndexed { index, pointF ->
            if (index == 0) {
                path.moveTo(
                    overlay.translateX(pointF.x),
                    overlay.translateY(pointF.y)
                )
            }
            path.lineTo(
                overlay.translateX(pointF.x),
                overlay.translateY(pointF.y)
            )
        }
        val paint = Paint().apply {
            color = selectedColor
            style = Paint.Style.STROKE
            strokeWidth = BOX_STROKE_WIDTH
        }
        drawPath(path, paint)
    }

    override fun draw(canvas: Canvas?) {

        val rect = overlay.calculateRect(
            imageRect.height().toFloat(),
            imageRect.width().toFloat(),
            face.boundingBox
        )
        canvas?.drawRect(rect, boxPaint)

        val contours = face.allContours

        contours.forEach {
            it.points.forEach { point ->
                val px = overlay.translateX(point.x)
                val py = overlay.translateY(point.y)
                canvas?.drawCircle(px, py, FACE_POSITION_RADIUS, facePositionPaint)
            }
        }

        // face
        canvas?.drawFace(FaceContour.FACE, Color.BLUE)

        // left eye
        canvas?.drawFace(FaceContour.LEFT_EYEBROW_TOP, Color.RED)
        canvas?.drawFace(FaceContour.LEFT_EYE, Color.BLACK)
        canvas?.drawFace(FaceContour.LEFT_EYEBROW_BOTTOM, Color.CYAN)

        // right eye
        canvas?.drawFace(FaceContour.RIGHT_EYE, Color.DKGRAY)
        canvas?.drawFace(FaceContour.RIGHT_EYEBROW_BOTTOM, Color.GRAY)
        canvas?.drawFace(FaceContour.RIGHT_EYEBROW_TOP, Color.GREEN)

        // nose
        canvas?.drawFace(FaceContour.NOSE_BOTTOM, Color.LTGRAY)
        canvas?.drawFace(FaceContour.NOSE_BRIDGE, Color.MAGENTA)

        // rip
        canvas?.drawFace(FaceContour.LOWER_LIP_BOTTOM, Color.WHITE)
        canvas?.drawFace(FaceContour.LOWER_LIP_TOP, Color.YELLOW)
        canvas?.drawFace(FaceContour.UPPER_LIP_BOTTOM, Color.GREEN)
        canvas?.drawFace(FaceContour.UPPER_LIP_TOP, Color.CYAN)
    }

    companion object {
        private const val FACE_POSITION_RADIUS = 4.0f
        private const val ID_TEXT_SIZE = 30.0f
        private const val BOX_STROKE_WIDTH = 5.0f
    }

}