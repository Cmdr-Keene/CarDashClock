package com.example.cardashclock.shared

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.car.app.AppManager
import androidx.car.app.CarContext
import androidx.car.app.Screen
import androidx.car.app.SurfaceCallback
import androidx.car.app.SurfaceContainer
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.CarIcon
import androidx.car.app.model.Template
import androidx.car.app.navigation.model.NavigationTemplate
import androidx.core.graphics.drawable.IconCompat
import java.io.InputStream
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class MyCarAppScreen(carContext: CarContext) : Screen(carContext), SurfaceCallback {

    private val handler = Handler(Looper.getMainLooper())
    private var surfaceContainer: SurfaceContainer? = null
    private val settingsManager = SettingsManager(carContext)
    
    private var cachedBgUri: String? = null
    private var cachedBitmap: Bitmap? = null

    private val updateRunnable = object : Runnable {
        override fun run() {
            render()
            // Update every 1000ms to save battery life
            handler.postDelayed(this, 1000)
        }
    }

    init {
        // Register the surface callback to receive surface lifecycle events
        carContext.getCarService(AppManager::class.java)
            .setSurfaceCallback(this)
    }

    override fun onGetTemplate(): Template {
        // Use NavigationTemplate which supports a background surface.
        // Replace the default BACK action with a custom "Exit" action using an 'X' icon.
        return NavigationTemplate.Builder()
            .setActionStrip(
                ActionStrip.Builder()
                    .addAction(
                        Action.Builder()
                            .setIcon(
                                CarIcon.Builder(
                                    IconCompat.createWithResource(
                                        carContext,
                                        android.R.drawable.ic_menu_close_clear_cancel
                                    )
                                ).build()
                            )
                            .setOnClickListener {
                                carContext.finishCarApp()
                            }
                            .build()
                    )
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

        try {
            val canvas: Canvas = surface.lockCanvas(null) ?: return
            try {
                drawClock(canvas)
            } finally {
                surface.unlockCanvasAndPost(canvas)
            }
        } catch (e: Exception) {
            Log.e("MyCarAppScreen", "Error locking canvas", e)
        }
    }

    private fun drawClock(canvas: Canvas) {
        val width = canvas.width.toFloat()
        val height = canvas.height.toFloat()
        val centerX = width / 2
        val centerY = height / 2
        val radius = Math.min(width, height) / 2 * 0.8f

        // --- Automatic Night Mode Logic ---
        val isDarkMode = carContext.isDarkMode
        
        // --- User Settings ---
        val faceColor = settingsManager.getFaceColor(isDarkMode)
        val handColor = settingsManager.getHandColor(isDarkMode)
        val secondHandColor = settingsManager.getSecondHandColor(isDarkMode)
        val bgImageUri = settingsManager.getBgImageUri(isDarkMode)
        val backgroundColor = settingsManager.getBackgroundColor(isDarkMode)
        val overlayColor = settingsManager.getOverlayColor(isDarkMode)
        val isGlowEnabled = settingsManager.isGlowEnabled(isDarkMode)

        // Draw background
        if (bgImageUri != null) {
            if (bgImageUri != cachedBgUri) {
                loadBackgroundBitmap(bgImageUri)
            }
            cachedBitmap?.let { bitmap ->
                val scale = Math.max(width / bitmap.width, height / bitmap.height)
                val scaledWidth = bitmap.width * scale
                val scaledHeight = bitmap.height * scale
                val left = (width - scaledWidth) / 2
                val top = (height - scaledHeight) / 2
                canvas.drawBitmap(bitmap, null, android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight), null)
                
                if (overlayColor != android.graphics.Color.TRANSPARENT) {
                    canvas.drawColor(overlayColor)
                }
            } ?: canvas.drawColor(backgroundColor)
        } else {
            canvas.drawColor(backgroundColor)
            cachedBitmap = null
            cachedBgUri = null
        }

        val paint = Paint().apply {
            color = faceColor
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 8f
            if (isGlowEnabled) setShadowLayer(8f, 0f, 0f, faceColor)
        }

        // Draw clock face outer circle
        canvas.drawCircle(centerX, centerY, radius, paint)

        // Draw hour markings
        paint.strokeWidth = 4f
        paint.clearShadowLayer()
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

        // Hour hand
        paint.color = handColor
        paint.strokeWidth = 12f
        if (isGlowEnabled) paint.setShadowLayer(8f, 0f, 0f, handColor)
        drawHand(canvas, centerX, centerY, radius * 0.5f, ((hours + minutes / 60f) * 30).toDouble(), paint)

        // Minute hand
        paint.strokeWidth = 8f
        drawHand(canvas, centerX, centerY, radius * 0.75f, ((minutes + seconds / 60f) * 6).toDouble(), paint)

        // Second hand (standard 1-second ticks for battery efficiency)
        paint.color = secondHandColor
        paint.strokeWidth = 4f
        if (isGlowEnabled) paint.setShadowLayer(8f, 0f, 0f, secondHandColor)
        drawHand(canvas, centerX, centerY, radius * 0.85f, (seconds * 6).toDouble(), paint)

        // Draw center dot
        paint.style = Paint.Style.FILL
        paint.color = faceColor
        if (isGlowEnabled) paint.setShadowLayer(5f, 0f, 0f, faceColor)
        canvas.drawCircle(centerX, centerY, 10f, paint)
    }

    private fun loadBackgroundBitmap(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            val inputStream: InputStream? = carContext.contentResolver.openInputStream(uri)
            cachedBitmap = BitmapFactory.decodeStream(inputStream)
            cachedBgUri = uriString
        } catch (e: Exception) {
            Log.e("MyCarAppScreen", "Error loading background bitmap", e)
            cachedBitmap = null
            cachedBgUri = null
        }
    }

    private fun drawHand(canvas: Canvas, cx: Float, cy: Float, length: Float, angleDegrees: Double, paint: Paint) {
        val angleRadians = Math.toRadians(angleDegrees)
        val stopX = cx + (length * sin(angleRadians)).toFloat()
        val stopY = cy - (length * cos(angleRadians)).toFloat()
        canvas.drawLine(cx, cy, stopX, stopY, paint)
    }
}
