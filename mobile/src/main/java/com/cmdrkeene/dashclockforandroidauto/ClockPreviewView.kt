package com.cmdrkeene.dashclockforandroidauto

import android.content.Context
import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class ClockPreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var faceColor: Int = Color.WHITE
    var handColor: Int = Color.WHITE
    var secondHandColor: Int = Color.RED
    var clockBackgroundColor: Int = Color.BLACK
    var overlayColor: Int = Color.TRANSPARENT
    var backgroundBitmap: Bitmap? = null
    var isDarkMode: Boolean = false
    var isGlowEnabled: Boolean = false

    private val hourHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 10f
    }
    private val minuteHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 7f
    }
    private val secondHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 3f
    }
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6f
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    
    private val calendar = Calendar.getInstance()

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            if (isShown) {
                invalidate()
            }
            // Align with the start of the next second
            val currentTime = System.currentTimeMillis()
            val delay = 1000 - (currentTime % 1000)
            handler.postDelayed(this, delay)
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        handler.post(updateRunnable)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        handler.removeCallbacks(updateRunnable)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        
        val width = width.toFloat()
        val height = height.toFloat()
        val centerX = width / 2
        val centerY = height / 2
        val radius = Math.min(width, height) / 2 * 0.85f

        // Draw background
        backgroundBitmap?.let { bitmap ->
            val scale = Math.max(width / bitmap.width, height / bitmap.height)
            val scaledWidth = bitmap.width * scale
            val scaledHeight = bitmap.height * scale
            val left = (width - scaledWidth) / 2
            val top = (height - scaledHeight) / 2
            canvas.drawBitmap(bitmap, null, RectF(left, top, left + scaledWidth, top + scaledHeight), null)
            
            if (overlayColor != Color.TRANSPARENT) {
                canvas.drawColor(overlayColor)
            }
        } ?: canvas.drawColor(clockBackgroundColor)

        // Draw face
        facePaint.color = faceColor
        if (isGlowEnabled) facePaint.setShadowLayer(15f, 0f, 0f, faceColor) else facePaint.clearShadowLayer()
        canvas.drawCircle(centerX, centerY, radius, facePaint)

        // Draw hour markings
        markerPaint.color = faceColor
        markerPaint.clearShadowLayer()
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30).toDouble())
            val startX = centerX + (radius * 0.88f * sin(angle)).toFloat()
            val startY = centerY - (radius * 0.88f * cos(angle)).toFloat()
            val endX = centerX + (radius * 0.95f * sin(angle)).toFloat()
            val endY = centerY - (radius * 0.95f * cos(angle)).toFloat()
            canvas.drawLine(startX, startY, endX, endY, markerPaint)
        }

        // Get current time for live animation
        calendar.setTimeInMillis(System.currentTimeMillis())
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        // Hour hand
        hourHandPaint.color = handColor
        if (isGlowEnabled) hourHandPaint.setShadowLayer(8f, 0f, 0f, handColor) else hourHandPaint.clearShadowLayer()
        drawHand(canvas, centerX, centerY, radius * 0.5f, ((hours + minutes / 60f) * 30).toDouble(), hourHandPaint)

        // Minute hand
        minuteHandPaint.color = handColor
        if (isGlowEnabled) minuteHandPaint.setShadowLayer(8f, 0f, 0f, handColor) else minuteHandPaint.clearShadowLayer()
        drawHand(canvas, centerX, centerY, radius * 0.75f, ((minutes + seconds / 60f) * 6).toDouble(), minuteHandPaint)

        // Second hand
        secondHandPaint.color = secondHandColor
        if (isGlowEnabled) secondHandPaint.setShadowLayer(15f, 0f, 0f, secondHandColor) else secondHandPaint.clearShadowLayer()
        drawHand(canvas, centerX, centerY, radius * 0.85f, (seconds * 6).toDouble(), secondHandPaint)

        // Center dot
        dotPaint.color = faceColor
        if (isGlowEnabled) dotPaint.setShadowLayer(5f, 0f, 0f, faceColor) else dotPaint.clearShadowLayer()
        canvas.drawCircle(centerX, centerY, 8f, dotPaint)
    }

    private fun drawHand(canvas: Canvas, cx: Float, cy: Float, length: Float, angleDegrees: Double, paint: Paint) {
        val angleRadians = Math.toRadians(angleDegrees)
        val stopX = cx + (length * sin(angleRadians)).toFloat()
        val stopY = cy - (length * cos(angleRadians)).toFloat()
        canvas.drawLine(cx, cy, stopX, stopY, paint)
    }
}
