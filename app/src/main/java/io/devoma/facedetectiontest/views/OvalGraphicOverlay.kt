package io.devoma.facedetectiontest.views

import android.content.Context
import android.content.res.Configuration
import android.graphics.*
import android.graphics.drawable.Drawable
import android.media.Image
import android.util.AttributeSet
import android.view.View
import androidx.camera.core.CameraSelector
import androidx.lifecycle.MutableLiveData
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceContour
import io.devoma.facedetectiontest.R
import io.devoma.facedetectiontest.utils.toBitMap
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlin.math.ceil

/**
 * Displays an Oval shaped frame
 */
class OvalGraphicOverlay(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    // Oval frame getters and setters
    var areaLeft = 185F
        set(value) {
            field = value
            postInvalidate()
        }
    var areaTop = 60F
        set(value) {
            field = value
            postInvalidate()
        }
    var areaWidth = 270F
        set(value) {
            field = value
            postInvalidate()
        }
    var areaHeight = 360F
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
    var noseWidth = 100f
        set(value) {
            field = value
            postInvalidate()
        }
    var noseHeight = 100f
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

    private val ovalRect = RectF()
    private val ovalPaint: Paint = Paint()

    private val onFaceDetected = MutableLiveData<Pair<Boolean, Bitmap>>()
    private val onFaceDetectedInBounds = MutableLiveData<Pair<Boolean, Bitmap>>()
    private val onNoseDetectedInBounds = MutableLiveData<Pair<Boolean, Bitmap>>()

    init {
        context.obtainStyledAttributes(
            attrs, R.styleable.OvalGraphicOverlay
        ).apply {
            // Oval frame
            areaLeft = getFloat(R.styleable.OvalGraphicOverlay_areaLeft, areaLeft)
            areaTop = getFloat(R.styleable.OvalGraphicOverlay_areaTop, areaTop)
            areaWidth = getFloat(R.styleable.OvalGraphicOverlay_areaWidth, areaWidth)
            areaHeight = getFloat(R.styleable.OvalGraphicOverlay_areaHeight, areaHeight)
            strokeWidth = getDimension(R.styleable.OvalGraphicOverlay_strokeSize, strokeWidth)
            minFaceAreaPercent = getFloat(
                R.styleable.OvalGraphicOverlay_minFaceAreaPercent, minFaceAreaPercent
            )
            // Nose frame
            noseLeft = getFloat(R.styleable.OvalGraphicOverlay_noseLeft, noseLeft)
            noseTop = getFloat(R.styleable.OvalGraphicOverlay_noseTop, noseTop)
            noseWidth = getFloat(R.styleable.OvalGraphicOverlay_noseWidth, noseWidth)
            noseHeight = getFloat(R.styleable.OvalGraphicOverlay_noseHeight, noseHeight)
            noseStrokeWidth =
                getDimension(R.styleable.OvalGraphicOverlay_noseStrokeSize, noseStrokeWidth)
            recycle()
        }

        // See: https://developer.android.com/reference/android/graphics/Rect
        require(areaLeft <= (areaLeft + areaWidth))
        require(areaTop <= (areaTop + areaHeight))

        require(noseLeft <= (noseLeft + noseWidth))
        require(noseTop <= (noseTop + noseHeight))

        ovalPaint.color = noFaceDetectedColor
        ovalPaint.style = Paint.Style.STROKE
        ovalPaint.strokeWidth = strokeWidth
    }

    /**
     * Returns an MutableLiveData that emits whenever a face is detected.
     */
    fun onFaceDetected() = onFaceDetected

    /**
     * Returns an MutableLiveData that emits when a face is detected with in oval frame
     */
    fun onFaceDetectedInBounds() = onFaceDetectedInBounds

    /**
     * Returns a MutableLiveData that emits when nose is detected with in the nose frame.
     */
    fun onNoseDetectedInBounds() = onNoseDetectedInBounds

    /**
     * Called when a face is detected.
     */
    fun onFaceDetected(results: List<Face>, image: Image?) {
        val imageRect = image?.cropRect
        if (results.isNotEmpty()) {
            val face = results.first() // Only one face can be detected when using contours.
            setFaceDetected(true)
            if (imageRect != null) {
                val imageBitmap = image.toBitMap(context)
                onFaceDetected.value = Pair(true, imageBitmap)
                val faceRect = calculateRect(
                    height = imageRect.height().toFloat(),
                    width = imageRect.width().toFloat(),
                    boundingBoxT = face.boundingBox
                )
                if (ovalRect.contains(faceRect)) {
                    setFaceDetectedInBounds(true)
                    onFaceDetectedInBounds.value = Pair(true, imageBitmap)
                }
                if (checkNoseEnclosed(face, calculateNoseRect())) {
                    onNoseDetectedInBounds.value = Pair(true, imageBitmap)
                }
            }
        } else {
            setFaceDetectedInBounds(false)
            setFaceDetected(false)
        }
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
        ovalRect.run {
            top = areaTop
            left = areaLeft
            bottom = areaTop + areaHeight
            right = areaLeft + areaWidth
            canvas.drawOval(this, ovalPaint)
        }
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
                    noseRect = calculateNoseRect(),
                    strokeWidth = noseStrokeWidth
                )
            )
        } else {
            ovalPaint.color = noFaceDetectedColor
            clear()
        }
        postInvalidate()
    }

    private fun calculateNoseRect() = Rect().apply {
        top = noseTop.toInt()
        left = noseLeft.toInt()
        right = (noseWidth + left).toInt()
        bottom = (noseHeight + top).toInt()
    }

    /**
     * Returns true if nose tip is enclosed in [noseRect]
     */
    private fun checkNoseEnclosed(face: Face, noseRect: Rect): Boolean {
        val noseBridgeContour = face.getContour(FaceContour.NOSE_BRIDGE)
        val noseTipF = noseBridgeContour?.points?.last() ?: return false
        val noseRectContainsNoseTip = noseRect.contains(
            translateX(noseTipF.x).toInt(),
            translateY(noseTipF.y).toInt()
        )
        if (noseRectContainsNoseTip) {
            return true
        }
        return false
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
            path.addOval(ovalRect, Path.Direction.CW)
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