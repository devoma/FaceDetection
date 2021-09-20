package io.devoma.facedetectiontest.views

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.CameraSelector
import kotlin.math.ceil

/**
 * Displays an Oval shaped frame
 */
class OvalGraphicOverlay(
    context: Context,
    attrs: AttributeSet?
) : View(context, attrs) {

    var areaLeft = 0F
        set(value) {
            field = value
            postInvalidate()
        }
    var areaTop = 0F
        set(value) {
            field = value
            postInvalidate()
        }
    var areaWidth = 0F
        set(value) {
            field = value
            postInvalidate()
        }
    var areaHeight = 0F
        set(value) {
            field = value
            postInvalidate()
        }
    var minFaceAreaPercent = 50F
        set(value) {
            field = value
            postInvalidate()
        }
    var strokeWidth = 8F
        set(value) {
            field = value
            postInvalidate()
        }

    /** Change this to change stroke color displayed when a face is detected within frame bounds. */
    var faceDetectedInBoundsColor = Color.GREEN
        set(value) {
            field = value
            postInvalidate()
        }

    /** Change this to change stroke color displayed when a face is detected. */
    var faceDetectedColor = Color.RED
        set(value) {
            field = value
            postInvalidate()
        }

    /** Change this to change stroke color displayed when no face is detected. */
    var noFaceDetectedColor = Color.TRANSPARENT
        set(value) {
            field = value
            postInvalidate()
        }

    /** Alpha value for the [InvertedRectDrawable]. */
    var overlayOpacity = 80
        set(value) {
            field = value
            postInvalidate()
        }

    // Nose frame parameters
    var noseLeft: Float = 241f
        set(value) {
            field = value
            postInvalidate()
        }
    var noseTop = 181f
        set(value) {
            field = value
            postInvalidate()
        }
    var noseWidth = 20f
        set(value) {
            field = value
            postInvalidate()
        }
    var noseHeight = 20f
        set(value) {
            field = value
            postInvalidate()
        }
    var noseStrokeWidth = 4f
        set(value) {
            field = value
            postInvalidate()
        }

    private val lock = Any()
    private val graphics: MutableList<Graphic> = ArrayList()
    var mScale: Float? = null
    var mOffsetX: Float? = null
    var mOffsetY: Float? = null
    var cameraSelector: Int = CameraSelector.LENS_FACING_FRONT
    lateinit var processBitmap: Bitmap
    lateinit var processCanvas: Canvas

    private val ovalPaint: Paint = Paint()

    init {
        require(areaLeft <= (areaLeft + areaWidth))
        require(areaTop <= (areaTop + areaHeight))

        require(noseLeft <= (noseLeft + noseWidth))
        require(noseTop <= (noseTop + noseHeight))

        ovalPaint.color = noFaceDetectedColor
        ovalPaint.style = Paint.Style.STROKE
        ovalPaint.strokeWidth = strokeWidth
    }

    /**
     * Returns [RectF] for oval frame
     */
    fun getOvalFrameRect() = RectF().apply {
        top = areaTop
        left = areaLeft
        bottom = areaTop + areaHeight
        right = areaLeft + areaWidth
    }

    /**
     * Returns [RectF] for nose frame
     */
    fun getNoseFrameRect() = Rect().apply {
        top = noseTop.toInt()
        left = noseLeft.toInt()
        right = (noseWidth + left).toInt()
        bottom = (noseHeight + top).toInt()
    }

    private val ovalShapeDrawable = InvertedRectDrawable()

    override fun onDraw(canvas: Canvas) {
        synchronized(lock) {
            initProcessCanvas()
            graphics.forEach {
                it.draw(canvas)
                it.draw(processCanvas)
            }
        }
        ovalShapeDrawable.run {
            alpha = overlayOpacity
            draw(canvas)
        }
        canvas.drawOval(getOvalFrameRect(), ovalPaint)
    }

    /**
     * Set this true whenever a face is detected.
     */
    fun setFaceDetected(faceDetected: Boolean) {
        if (faceDetected) {
            ovalPaint.color = faceDetectedColor
        } else {
            ovalPaint.color = noFaceDetectedColor
        }
        postInvalidate()
    }

    /**
     * Set this true when a face is found to be with in the oval frame boundaries.
     */
    fun setFaceDetectedInBounds(faceInBounds: Boolean) {
        if (faceInBounds) {
            ovalPaint.color = faceDetectedInBoundsColor
            add(
                NoseFrameGraphic(
                    noseRect = getNoseFrameRect(),
                    strokeWidth = noseStrokeWidth
                )
            )
        } else {
            ovalPaint.color = noFaceDetectedColor
            clear()
        }
        postInvalidate()
    }

    /**
     * Set this true when nose is enclosed within the nose frame.
     */
    fun setNoseDetectedInBounds(faceInBounds: Boolean) {
        // TODO do something here. Maybe change nose frame colors
    }

    fun isFrontMode() = cameraSelector == CameraSelector.LENS_FACING_FRONT

    fun toggleSelector() {
        cameraSelector =
            if (cameraSelector == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT
            else CameraSelector.LENS_FACING_BACK
    }

    fun clear() {
        synchronized(lock) { graphics.clear() }
        postInvalidate()
    }

    fun add(graphic: Graphic) {
        synchronized(lock) { graphics.add(graphic) }
    }

    fun remove(graphic: Graphic) {
        synchronized(lock) { graphics.remove(graphic) }
        postInvalidate()
    }

    private fun initProcessCanvas() {
        processBitmap = Bitmap.createBitmap(measuredWidth, measuredHeight, Bitmap.Config.ARGB_8888)
        processCanvas = Canvas(processBitmap)
    }

    /**
     * Inverted Rect drawable for [OvalGraphicOverlay]
     */
    inner class InvertedRectDrawable(private val color: Int = Color.BLACK) : Drawable() {

        var paint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = this@InvertedRectDrawable.color
        }
        private val path = Path()

        override fun draw(canvas: Canvas) {
            path.reset()
            path.addOval(getOvalFrameRect(), Path.Direction.CW)
            path.fillType = Path.FillType.INVERSE_EVEN_ODD

            canvas.clipPath(path)
            canvas.drawPaint(paint)
        }

        override fun setAlpha(alpha: Int) {
            paint.alpha = alpha
        }

        override fun setColorFilter(colorFilter: ColorFilter?) {
            paint.colorFilter = colorFilter
        }

        override fun getOpacity(): Int {
            return PixelFormat.TRANSLUCENT
        }
    }

    fun calculateRect(height: Float, width: Float, boundingBoxT: Rect): RectF {

        // for land scape
        fun isLandScapeMode(): Boolean {
            return context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

        fun whenLandScapeModeWidth(): Float {
            return when (isLandScapeMode()) {
                true -> width
                false -> height
            }
        }

        fun whenLandScapeModeHeight(): Float {
            return when (isLandScapeMode()) {
                true -> height
                false -> width
            }
        }

        val scaleX = this.width.toFloat() / whenLandScapeModeWidth()
        val scaleY = this.height.toFloat() / whenLandScapeModeHeight()
        val scale = scaleX.coerceAtLeast(scaleY)
        mScale = scale

        // Calculate offset (we need to center the overlay on the target)
        val offsetX = (this.width.toFloat() - ceil(whenLandScapeModeWidth() * scale)) / 2.0f
        val offsetY =
            (this.height.toFloat() - ceil(whenLandScapeModeHeight() * scale)) / 2.0f

        mOffsetX = offsetX
        mOffsetY = offsetY

        val mappedBox = RectF().apply {
            left = boundingBoxT.right * scale + offsetX
            top = boundingBoxT.top * scale + offsetY
            right = boundingBoxT.left * scale + offsetX
            bottom = boundingBoxT.bottom * scale + offsetY
        }

        // for front mode
        if (isFrontMode()) {
            val centerX = this.width.toFloat() / 2
            mappedBox.apply {
                left = centerX + (centerX - left)
                right = centerX - (right - centerX)
            }
        }
        return mappedBox
    }

    fun translateX(horizontal: Float): Float {
        return if (mScale != null && mOffsetX != null && !isFrontMode()) {
            (horizontal * mScale!!) + mOffsetX!!
        } else if (mScale != null && mOffsetX != null && isFrontMode()) {
            val centerX = width.toFloat() / 2
            centerX - ((horizontal * mScale!!) + mOffsetX!! - centerX)
        } else {
            horizontal
        }
    }

    fun translateY(vertical: Float): Float {
        return if (mScale != null && mOffsetY != null) {
            (vertical * mScale!!) + mOffsetY!!
        } else {
            vertical
        }
    }

    /**
     * Represents a Graphic.
     */
    abstract class Graphic {

        abstract fun draw(canvas: Canvas?)

    }
}