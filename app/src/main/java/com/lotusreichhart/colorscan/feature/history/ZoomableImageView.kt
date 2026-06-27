package com.lotusreichhart.colorscan.feature.history

import android.content.Context
import android.graphics.Matrix
import android.graphics.PointF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ScaleGestureDetector

class ZoomableImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : androidx.appcompat.widget.AppCompatImageView(context, attrs, defStyleAttr) {

    private var scaleMatrix = Matrix()
    private var mode = 0 // 0: NONE, 1: DRAG, 2: ZOOM

    private var lastTouch = PointF()
    private var startTouch = PointF()
    
    private var saveScale = 1f
    private var minScale = 1f
    private var maxScale = 4f
    
    private var mScaleDetector: ScaleGestureDetector
    private var mGestureDetector: GestureDetector
    private val matrixValues = FloatArray(9)

    private var viewWidth = 0
    private var viewHeight = 0
    private var origWidth = 0f
    private var origHeight = 0f

    init {
        super.setScaleType(ScaleType.MATRIX)
        mScaleDetector = ScaleGestureDetector(context, ScaleListener())
        mGestureDetector = GestureDetector(context, GestureListener())
        
        setOnTouchListener { _, event ->
            mScaleDetector.onTouchEvent(event)
            mGestureDetector.onTouchEvent(event)
            val currentTouch = PointF(event.x, event.y)

            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouch.set(currentTouch)
                    startTouch.set(lastTouch)
                    mode = 1 // DRAG
                }
                MotionEvent.ACTION_POINTER_DOWN -> {
                    mode = 2 // ZOOM
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = 0
                    val xDiff = Math.abs(currentTouch.x - startTouch.x).toInt()
                    val yDiff = Math.abs(currentTouch.y - startTouch.y).toInt()
                    if (xDiff < 3 && yDiff < 3) {
                        performClick()
                    }
                }
                MotionEvent.ACTION_MOVE -> {
                    if (mode == 1) { // DRAG
                        val deltaX = currentTouch.x - lastTouch.x
                        val deltaY = currentTouch.y - lastTouch.y
                        val fixTransX = getFixDragTrans(deltaX, viewWidth.toFloat(), origWidth * saveScale)
                        val fixTransY = getFixDragTrans(deltaY, viewHeight.toFloat(), origHeight * saveScale)
                        scaleMatrix.postTranslate(fixTransX, fixTransY)
                        limitDrag()
                        lastTouch.set(currentTouch.x, currentTouch.y)
                    }
                }
            }
            imageMatrix = scaleMatrix
            invalidate()
            true
        }
    }

    private fun getFixDragTrans(delta: Float, viewSize: Float, contentSize: Float): Float {
        if (contentSize <= viewSize) {
            return 0f
        }
        return delta
    }

    private fun limitDrag() {
        scaleMatrix.getValues(matrixValues)
        val transX = matrixValues[Matrix.MTRANS_X]
        val transY = matrixValues[Matrix.MTRANS_Y]

        val fixTransX = getFixTrans(transX, viewWidth.toFloat(), origWidth * saveScale)
        val fixTransY = getFixTrans(transY, viewHeight.toFloat(), origHeight * saveScale)

        if (fixTransX != 0f || fixTransY != 0f) {
            scaleMatrix.postTranslate(fixTransX, fixTransY)
        }
    }

    private fun getFixTrans(trans: Float, viewSize: Float, contentSize: Float): Float {
        val minTrans: Float
        val maxTrans: Float

        if (contentSize <= viewSize) {
            minTrans = 0f
            maxTrans = viewSize - contentSize
        } else {
            minTrans = viewSize - contentSize
            maxTrans = 0f
        }

        if (trans < minTrans) return -trans + minTrans
        if (trans > maxTrans) return -trans + maxTrans
        return 0f
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        viewHeight = MeasureSpec.getSize(heightMeasureSpec)

        val drawable = drawable ?: return
        if (drawable.intrinsicWidth == 0 || drawable.intrinsicHeight == 0) return

        val imgWidth = drawable.intrinsicWidth
        val imgHeight = drawable.intrinsicHeight

        val scaleX = viewWidth.toFloat() / imgWidth
        val scaleY = viewHeight.toFloat() / imgHeight
        val scale = Math.min(scaleX, scaleY)

        scaleMatrix.setScale(scale, scale)

        val redundantYSpace = viewHeight.toFloat() - (scale * imgHeight.toFloat())
        val redundantXSpace = viewWidth.toFloat() - (scale * imgWidth.toFloat())

        scaleMatrix.postTranslate(redundantXSpace / 2, redundantYSpace / 2)

        origWidth = imgWidth.toFloat() * scale
        origHeight = imgHeight.toFloat() * scale
        saveScale = 1f
        imageMatrix = scaleMatrix
    }

    private inner class ScaleListener : ScaleGestureDetector.SimpleOnScaleGestureListener() {
        override fun onScale(detector: ScaleGestureDetector): Boolean {
            var mScaleFactor = detector.scaleFactor
            val origScale = saveScale
            saveScale *= mScaleFactor
            if (saveScale > maxScale) {
                saveScale = maxScale
                mScaleFactor = maxScale / origScale
            } else if (saveScale < minScale) {
                saveScale = minScale
                mScaleFactor = minScale / origScale
            }

            if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight) {
                scaleMatrix.postScale(mScaleFactor, mScaleFactor, viewWidth / 2f, viewHeight / 2f)
            } else {
                scaleMatrix.postScale(mScaleFactor, mScaleFactor, detector.focusX, detector.focusY)
            }

            limitDrag()
            return true
        }
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            val targetScale = if (saveScale > minScale) minScale else maxScale
            val mScaleFactor = targetScale / saveScale
            saveScale = targetScale

            if (origWidth * saveScale <= viewWidth || origHeight * saveScale <= viewHeight) {
                scaleMatrix.postScale(mScaleFactor, mScaleFactor, viewWidth / 2f, viewHeight / 2f)
            } else {
                scaleMatrix.postScale(mScaleFactor, mScaleFactor, e.x, e.y)
            }
            limitDrag()
            imageMatrix = scaleMatrix
            invalidate()
            return true
        }
    }
}
