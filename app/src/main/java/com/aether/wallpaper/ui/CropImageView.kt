package com.aether.wallpaper.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Custom view for cropping images to screen aspect ratio.
 *
 * Features:
 * - Displays full image scaled to fit view
 * - Crop overlay with device screen aspect ratio
 * - Drag to reposition crop region
 * - Corner handles to resize (maintaining aspect ratio)
 * - Returns crop coordinates in image space
 *
 * Simplified implementation for MVP - basic drag and resize only.
 */
class CropImageView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var bitmap: Bitmap? = null
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    private val dimPaint = Paint().apply {
        color = Color.argb(128, 0, 0, 0) // 50% black
    }
    private val handlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    // Image display properties
    private var imageMatrix = Matrix()
    private var imageRect = RectF()

    // Crop overlay properties (in view coordinates)
    private var cropRect = RectF()
    private val screenAspectRatio: Float
    private val handleRadius = 40f
    private val minCropSize = 200f

    // Touch handling
    private enum class TouchMode { NONE, DRAG, RESIZE_TL, RESIZE_TR, RESIZE_BL, RESIZE_BR }
    private var touchMode = TouchMode.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    init {
        // Get device screen aspect ratio
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        wm.defaultDisplay.getRealMetrics(metrics)
        screenAspectRatio = metrics.widthPixels.toFloat() / metrics.heightPixels.toFloat()
    }

    /**
     * Set the image to crop.
     */
    fun setImageBitmap(bitmap: Bitmap) {
        this.bitmap = bitmap
        requestLayout()
        invalidate()
    }

    /**
     * Get crop coordinates in image space.
     * Returns (x, y, width, height) in pixels relative to original bitmap.
     */
    fun getCropCoordinates(): IntArray? {
        val bmp = bitmap ?: return null

        // Convert crop rect from view coordinates to image coordinates
        val invMatrix = Matrix()
        if (!imageMatrix.invert(invMatrix)) {
            return null
        }

        val points = floatArrayOf(
            cropRect.left, cropRect.top,
            cropRect.right, cropRect.bottom
        )
        invMatrix.mapPoints(points)

        val x = points[0].toInt().coerceIn(0, bmp.width)
        val y = points[1].toInt().coerceIn(0, bmp.height)
        val right = points[2].toInt().coerceIn(0, bmp.width)
        val bottom = points[3].toInt().coerceIn(0, bmp.height)

        val width = (right - x).coerceAtLeast(1)
        val height = (bottom - y).coerceAtLeast(1)

        return intArrayOf(x, y, width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        updateImageMatrix()
        initializeCropRect()
    }

    /**
     * Calculate matrix to fit image within view.
     */
    private fun updateImageMatrix() {
        val bmp = bitmap ?: return
        if (width == 0 || height == 0) return

        imageMatrix.reset()

        val viewWidth = width.toFloat()
        val viewHeight = height.toFloat()
        val bmpWidth = bmp.width.toFloat()
        val bmpHeight = bmp.height.toFloat()

        // Scale to fit view (letterbox/pillarbox)
        val scale = min(viewWidth / bmpWidth, viewHeight / bmpHeight)
        val scaledWidth = bmpWidth * scale
        val scaledHeight = bmpHeight * scale

        // Center in view
        val dx = (viewWidth - scaledWidth) / 2f
        val dy = (viewHeight - scaledHeight) / 2f

        imageMatrix.postScale(scale, scale)
        imageMatrix.postTranslate(dx, dy)

        // Calculate image rect in view coordinates
        imageRect.set(0f, 0f, bmpWidth, bmpHeight)
        imageMatrix.mapRect(imageRect)
    }

    /**
     * Initialize crop rect with screen aspect ratio, centered and maximized.
     */
    private fun initializeCropRect() {
        if (imageRect.isEmpty) return

        // Calculate maximum crop size with screen aspect ratio
        val maxWidth = imageRect.width()
        val maxHeight = imageRect.height()

        var cropWidth: Float
        var cropHeight: Float

        if (maxWidth / maxHeight > screenAspectRatio) {
            // Image is wider than screen aspect
            cropHeight = maxHeight
            cropWidth = cropHeight * screenAspectRatio
        } else {
            // Image is taller than screen aspect
            cropWidth = maxWidth
            cropHeight = cropWidth / screenAspectRatio
        }

        // Ensure minimum size
        cropWidth = cropWidth.coerceAtLeast(minCropSize)
        cropHeight = cropHeight.coerceAtLeast(minCropSize / screenAspectRatio)

        // Center within image rect
        val left = imageRect.centerX() - cropWidth / 2f
        val top = imageRect.centerY() - cropHeight / 2f

        cropRect.set(left, top, left + cropWidth, top + cropHeight)
        constrainCropRect()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Draw image
        bitmap?.let {
            canvas.drawBitmap(it, imageMatrix, paint)
        }

        if (cropRect.isEmpty) return

        // Draw dimmed overlay outside crop region
        val path = Path().apply {
            addRect(0f, 0f, width.toFloat(), height.toFloat(), Path.Direction.CW)
            addRect(cropRect, Path.Direction.CCW)
        }
        canvas.drawPath(path, dimPaint)

        // Draw crop border
        canvas.drawRect(cropRect, overlayPaint)

        // Draw corner handles
        drawHandle(canvas, cropRect.left, cropRect.top)
        drawHandle(canvas, cropRect.right, cropRect.top)
        drawHandle(canvas, cropRect.left, cropRect.bottom)
        drawHandle(canvas, cropRect.right, cropRect.bottom)
    }

    private fun drawHandle(canvas: Canvas, x: Float, y: Float) {
        canvas.drawCircle(x, y, handleRadius, handlePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                lastTouchX = event.x
                lastTouchY = event.y
                touchMode = getTouchMode(event.x, event.y)
                return touchMode != TouchMode.NONE
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastTouchX
                val dy = event.y - lastTouchY

                when (touchMode) {
                    TouchMode.DRAG -> {
                        cropRect.offset(dx, dy)
                        constrainCropRect()
                    }
                    TouchMode.RESIZE_TL -> resizeCorner(dx, dy, left = true, top = true)
                    TouchMode.RESIZE_TR -> resizeCorner(dx, dy, left = false, top = true)
                    TouchMode.RESIZE_BL -> resizeCorner(dx, dy, left = true, top = false)
                    TouchMode.RESIZE_BR -> resizeCorner(dx, dy, left = false, top = false)
                    TouchMode.NONE -> return false
                }

                lastTouchX = event.x
                lastTouchY = event.y
                invalidate()
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                touchMode = TouchMode.NONE
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    private fun getTouchMode(x: Float, y: Float): TouchMode {
        // Check corner handles first
        if (isNearPoint(x, y, cropRect.left, cropRect.top)) return TouchMode.RESIZE_TL
        if (isNearPoint(x, y, cropRect.right, cropRect.top)) return TouchMode.RESIZE_TR
        if (isNearPoint(x, y, cropRect.left, cropRect.bottom)) return TouchMode.RESIZE_BL
        if (isNearPoint(x, y, cropRect.right, cropRect.bottom)) return TouchMode.RESIZE_BR

        // Check if inside crop rect for dragging
        if (cropRect.contains(x, y)) return TouchMode.DRAG

        return TouchMode.NONE
    }

    private fun isNearPoint(x: Float, y: Float, px: Float, py: Float): Boolean {
        val touchRadius = handleRadius * 1.5f
        return abs(x - px) < touchRadius && abs(y - py) < touchRadius
    }

    private fun resizeCorner(dx: Float, dy: Float, left: Boolean, top: Boolean) {
        // Resize while maintaining aspect ratio
        val oldRect = RectF(cropRect)

        if (left) {
            cropRect.left += dx
        } else {
            cropRect.right += dx
        }

        // Adjust height to maintain aspect ratio
        val newWidth = cropRect.width()
        val newHeight = newWidth / screenAspectRatio

        if (top) {
            cropRect.top = cropRect.bottom - newHeight
        } else {
            cropRect.bottom = cropRect.top + newHeight
        }

        // Enforce minimum size
        if (cropRect.width() < minCropSize || cropRect.height() < minCropSize / screenAspectRatio) {
            cropRect.set(oldRect)
            return
        }

        // Constrain to image bounds
        if (!imageRect.contains(cropRect)) {
            cropRect.set(oldRect)
        }
    }

    private fun constrainCropRect() {
        // Ensure crop rect stays within image bounds
        if (cropRect.left < imageRect.left) {
            cropRect.offset(imageRect.left - cropRect.left, 0f)
        }
        if (cropRect.top < imageRect.top) {
            cropRect.offset(0f, imageRect.top - cropRect.top)
        }
        if (cropRect.right > imageRect.right) {
            cropRect.offset(imageRect.right - cropRect.right, 0f)
        }
        if (cropRect.bottom > imageRect.bottom) {
            cropRect.offset(0f, imageRect.bottom - cropRect.bottom)
        }
    }
}
