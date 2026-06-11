package com.example.cardashclock.shared

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Handler
import android.os.Looper
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class MyCarAppScreen(carContext: CarContext) : Screen(carContext), SurfaceCallback {

    private val handler = Handler(Looper.getMainLooper())
    private var surfaceContainer: SurfaceContainer? = null
    private val updateRunnable = object : Runnable {
        override fun run() {
            render()
            handler.postDelayed(this, 1000)
        }
    }

    init {
        // Register the surface callback to receive surface lifecycle events
        carContext.getCarService(AppManager::class.java)
            .setSurfaceCallback(this)
    }

    override fun onGetTemplate(): Template {
        // Use NavigationTemplate which supports a background surface
        return NavigationTemplate.Builder()
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(Action.APP_ICON)
                    .build()
            )
            .build()
    }

    override fun onSurfaceAvailable(surfaceContainer: SurfaceContainer) {
        this.surfaceContainer = surfaceContainer
        render()
        handler.post(updateRunnable)
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        this.surfaceContainer = null
        handler.removeCallbacks(updateRunnable)
    }

    private fun render() {
        val container = surfaceContainer ?: return
        val surface = container.surface ?: return
        if (!surface.isValid) return

        // For Car App Library, it's recommended to use lockCanvas or lockHardwareCanvas
        val canvas: Canvas = surface.lockCanvas(null)
        try {
            drawClock(canvas)
        } finally {
            surface.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawClock(canvas: Canvas) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        val centerX = width / 2
        val centerY = height / 2
        val radius = Math.min(width, height) / 2 * 0.8f

        // Clear background with a dark color
        canvas.drawColor(Color.parseColor("#121212"))

        val paint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 8f
        }

        // Draw clock face outer circle
        canvas.drawCircle(centerX, centerY, radius, paint)

        // Draw hour markings
        paint.strokeWidth = 4f
        for (i in 0 until 12) {
            val angle = Math.toRadians((i * 30).toDouble())
            val startX = centerX + (radius * 0.85f * sin(angle)).toFloat()
            val startY = centerY - (radius * 0.85f * cos(angle)).toFloat()
            val endX = centerX + (radius * 0.95f * sin(angle)).toFloat()
            val endY = centerY - (radius * 0.95f * cos(angle)).toFloat()
            canvas.drawLine(startX, startY, endX, endY, paint)
        }

        val calendar = Calendar.getInstance()
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        // Draw hour hand
        paint.strokeWidth = 12f
        drawHand(canvas, centerX, centerY, radius * 0.5f, ((hours + minutes / 60f) * 30).toDouble(), paint)

        // Draw minute hand
        paint.strokeWidth = 8f
        drawHand(canvas, centerX, centerY, radius * 0.75f, ((minutes + seconds / 60f) * 6).toDouble(), paint)

        // Draw second hand
        paint.color = Color.RED
        paint.strokeWidth = 4f
        drawHand(canvas, centerX, centerY, radius * 0.85f, (seconds * 6).toDouble(), paint)

        // Draw center dot
        paint.style = Paint.Style.FILL
        paint.color = Color.WHITE
        canvas.drawCircle(centerX, centerY, 10f, paint)
    }

    private fun drawHand(canvas: Canvas, cx: Float, cy: Float, length: Float, angleDegrees: Double, paint: Paint) {
        val angleRadians = Math.toRadians(angleDegrees)
        val stopX = cx + (length * sin(angleRadians)).toFloat()
        val stopY = cy - (length * cos(angleRadians)).toFloat()
        canvas.drawLine(cx, cy, stopX, stopY, paint)
    }
}
