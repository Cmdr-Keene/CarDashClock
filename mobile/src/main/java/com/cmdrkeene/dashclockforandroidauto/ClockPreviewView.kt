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

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            invalidate()
            handler.postDelayed(this, 1000)
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
        
        // Ensure hardware acceleration is off for shadow layer if needed, 
        // but for a simple glow on modern devices it usually works fine.
        // setLayerType(LAYER_TYPE_SOFTWARE, null) 

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

        // Reset shadow layer for markers
        paint.clearShadowLayer()

        // Draw face
        paint.color = faceColor
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 6f
        if (isGlowEnabled) paint.setShadowLayer(15f, 0f, 0f, faceColor)
        canvas.drawCircle(centerX, centerY, radius, paint)

        // Draw hour markings
        paint.strokeWidth = 3f
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30).toDouble())
            val startX = centerX + (radius * 0.88f * sin(angle)).toFloat()
            val startY = centerY - (radius * 0.88f * cos(angle)).toFloat()
            val endX = centerX + (radius * 0.95f * sin(angle)).toFloat()
            val endY = centerY - (radius * 0.95f * cos(angle)).toFloat()
            canvas.drawLine(startX, startY, endX, endY, paint)
        }

        // Get current time for live animation
        val calendar = Calendar.getInstance()
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        // Hour hand
        paint.color = handColor
        paint.strokeWidth = 10f
        if (isGlowEnabled) paint.setShadowLayer(8f, 0f, 0f, handColor)
        drawHand(canvas, centerX, centerY, radius * 0.5f, ((hours + minutes / 60f) * 30).toDouble(), paint)

        // Minute hand
        paint.strokeWidth = 7f
        drawHand(canvas, centerX, centerY, radius * 0.75f, ((minutes + seconds / 60f) * 6).toDouble(), paint)

        // Second hand
        paint.color = secondHandColor
        paint.strokeWidth = 3f
        if (isGlowEnabled) paint.setShadowLayer(15f, 0f, 0f, secondHandColor)
        drawHand(canvas, centerX, centerY, radius * 0.85f, (seconds * 6).toDouble(), paint)

        // Center dot
        paint.style = Paint.Style.FILL
        paint.color = faceColor
        if (isGlowEnabled) paint.setShadowLayer(5f, 0f, 0f, faceColor)
        canvas.drawCircle(centerX, centerY, 8f, paint)
    }

    private fun drawHand(canvas: Canvas, cx: Float, cy: Float, length: Float, angleDegrees: Double, paint: Paint) {
        val angleRadians = Math.toRadians(angleDegrees)
        val stopX = cx + (length * sin(angleRadians)).toFloat()
        val stopY = cy - (length * cos(angleRadians)).toFloat()
        canvas.drawLine(cx, cy, stopX, stopY, paint)
    }
}
