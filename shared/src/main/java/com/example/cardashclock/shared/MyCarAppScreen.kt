package com.example.cardashclock.shared

import android.graphics.Rect
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
import android.os.PowerManager
import android.os.SystemClock
import android.content.Context
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import java.io.InputStream
import java.util.Calendar
import kotlin.math.cos
import kotlin.math.sin

class MyCarAppScreen(carContext: CarContext) : Screen(carContext), SurfaceCallback, DefaultLifecycleObserver {

    private val handler = Handler(Looper.getMainLooper())
    private var surfaceContainer: SurfaceContainer? = null
    private val settingsManager = SettingsManager(carContext)
    
    private var cachedBgUri: String? = null
    private var cachedBitmap: Bitmap? = null
    private var staticBitmap: Bitmap? = null
    private var isStaticDirty = true
    private var stableArea: Rect? = null

    // Cache settings to avoid repeated SharedPreferences access in the render loop
    private var faceColor: Int = 0
    private var handColor: Int = 0
    private var secondHandColor: Int = 0
    private var bgImageUri: String? = null
    private var backgroundColor: Int = 0
    private var overlayColor: Int = 0
    private var isGlowEnabled: Boolean = false
    private var isDarkMode: Boolean = false

    // Trigonometry lookup tables for 3600 positions (0.1 degree precision)
    private val sinLookup = FloatArray(3601)
    private val cosLookup = FloatArray(3601)

    private fun updateSettingsCache() {
        val dark = carContext.isDarkMode
        isDarkMode = dark
        faceColor = settingsManager.getFaceColor(dark)
        handColor = settingsManager.getHandColor(dark)
        secondHandColor = settingsManager.getSecondHandColor(dark)
        bgImageUri = settingsManager.getBgImageUri(dark)
        backgroundColor = settingsManager.getBackgroundColor(dark)
        overlayColor = settingsManager.getOverlayColor(dark)
        isGlowEnabled = settingsManager.isGlowEnabled(dark)
        isStaticDirty = true
    }

    // Pre-initialize Paint objects to avoid allocations in draw loop
    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
    }
    private val handPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val secondHandPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val centerDotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val markerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    
    private val calendar = Calendar.getInstance()

    private val updateRunnable = object : Runnable {
        override fun run() {
            if (surfaceContainer == null) return
            
            // Suggestion 3: Frame Skipping when idle
            // If the lifecycle is not STARTED or RESUMED, we can skip or slow down updates.
            val lifecycleState = lifecycle.currentState
            if (!lifecycleState.isAtLeast(Lifecycle.State.STARTED)) {
                handler.postDelayed(this, 5000) // Check again in 5 seconds
                return
            }

            render()
            
            // Calculate delay until the exact start of the next second or minute
            val powerManager = carContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
            val isPowerSave = powerManager?.isPowerSaveMode ?: false
            
            val currentTime = System.currentTimeMillis()
            val uptimeMillis = SystemClock.uptimeMillis()
            
            val nextTickOffset = if (isPowerSave) {
                // If in power save mode, update at the start of the next minute
                60000 - (currentTime % 60000)
            } else {
                // Otherwise update at the start of the next second
                1000 - (currentTime % 1000)
            }
            
            handler.postAtTime(this, uptimeMillis + nextTickOffset)
        }
    }

    init {
        // Initialize lookup tables
        for (i in 0..3600) {
            val rad = Math.toRadians(i / 10.0)
            sinLookup[i] = sin(rad).toFloat()
            cosLookup[i] = cos(rad).toFloat()
        }

        // Register the surface callback to receive surface lifecycle events
        carContext.getCarService(AppManager::class.java)
            .setSurfaceCallback(this)
        
        // Add lifecycle observer
        lifecycle.addObserver(this)
    }
    
    private fun getSin(degrees: Double): Float {
        val d = (degrees % 360 + 360) % 360
        return sinLookup[(d * 10).toInt()]
    }

    private fun getCos(degrees: Double): Float {
        val d = (degrees % 360 + 360) % 360
        return cosLookup[(d * 10).toInt()]
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
        handler.removeCallbacks(updateRunnable)
        this.surfaceContainer = surfaceContainer
        this.stableArea = null
        isStaticDirty = true
        updateSettingsCache()
        render()
        handler.post(updateRunnable)
    }

    override fun onVisibleAreaChanged(visibleArea: Rect) {
    }

    override fun onStableAreaChanged(stableArea: Rect) {
        this.stableArea = stableArea
        isStaticDirty = true
        render()
    }

    override fun onSurfaceDestroyed(surfaceContainer: SurfaceContainer) {
        this.surfaceContainer = null
        handler.removeCallbacks(updateRunnable)
        
        // Suggestion 1: Explicit Memory Recycling
        staticBitmap?.recycle()
        staticBitmap = null
        cachedBitmap?.recycle()
        cachedBitmap = null
        cachedBgUri = null
    }

    private fun render() {
        val container = surfaceContainer ?: return
        val surface = container.surface ?: return
        if (!surface.isValid) return

        // Update cache if dark mode changed
        if (isDarkMode != carContext.isDarkMode) {
            updateSettingsCache()
        }

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
        val area = stableArea ?: Rect(0, 0, canvas.width, canvas.height)
        val height = canvas.height.toFloat()
        
        // Use stable area for horizontal centering, but full surface for vertical centering
        // to avoid jumping when the ActionStrip (Exit button) appears.
        val centerX = (area.left + area.right).toFloat() / 2
        val centerY = height / 2
        val radius = Math.min(area.width(), canvas.height) / 2 * 0.8f

        // Suggestion 1: Static Layer Caching
        if (isStaticDirty || staticBitmap == null || staticBitmap?.width != canvas.width || staticBitmap?.height != canvas.height) {
            rebuildStaticLayer(canvas.width, canvas.height, centerX, centerY, radius)
            isStaticDirty = false
        }

        staticBitmap?.let {
            canvas.drawBitmap(it, 0f, 0f, null)
        }

        calendar.setTimeInMillis(System.currentTimeMillis())
        val hours = calendar.get(Calendar.HOUR)
        val minutes = calendar.get(Calendar.MINUTE)
        val seconds = calendar.get(Calendar.SECOND)

        val powerManager = carContext.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val isPowerSave = powerManager?.isPowerSaveMode ?: false

        // Hour hand
        handPaint.color = handColor
        handPaint.strokeWidth = 12f
        if (isGlowEnabled) handPaint.setShadowLayer(8f, 0f, 0f, handColor) else handPaint.clearShadowLayer()
        drawHand(canvas, centerX, centerY, radius * 0.5f, ((hours + minutes / 60f) * 30).toDouble(), handPaint)

        // Minute hand
        handPaint.strokeWidth = 8f
        drawHand(canvas, centerX, centerY, radius * 0.75f, ((minutes + seconds / 60f) * 6).toDouble(), handPaint)

        // Second hand (skip in power save mode to save battery)
        if (!isPowerSave) {
            secondHandPaint.color = secondHandColor
            secondHandPaint.strokeWidth = 4f
            if (isGlowEnabled) secondHandPaint.setShadowLayer(8f, 0f, 0f, secondHandColor) else secondHandPaint.clearShadowLayer()
            drawHand(canvas, centerX, centerY, radius * 0.85f, (seconds * 6).toDouble(), secondHandPaint)
        }

        // Draw center dot
        centerDotPaint.color = faceColor
        if (isGlowEnabled) centerDotPaint.setShadowLayer(5f, 0f, 0f, faceColor) else centerDotPaint.clearShadowLayer()
        canvas.drawCircle(centerX, centerY, 10f, centerDotPaint)
    }

    private fun rebuildStaticLayer(width: Int, height: Int, centerX: Float, centerY: Float, radius: Float) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val staticCanvas = Canvas(bitmap)

        // Clear with background color
        staticCanvas.drawColor(backgroundColor)

        // Draw background image centered and scaled to fill the entire surface
        if (bgImageUri != null) {
            if (bgImageUri != cachedBgUri) {
                loadBackgroundBitmap(bgImageUri!!)
            }
            cachedBitmap?.let { bgBitmap ->
                val scale = Math.max(width.toFloat() / bgBitmap.width, height.toFloat() / bgBitmap.height)
                val scaledWidth = bgBitmap.width * scale
                val scaledHeight = bgBitmap.height * scale
                val left = (width - scaledWidth) / 2
                val top = (height - scaledHeight) / 2
                staticCanvas.drawBitmap(bgBitmap, null, android.graphics.RectF(left, top, left + scaledWidth, top + scaledHeight), null)
                
                if (overlayColor != android.graphics.Color.TRANSPARENT) {
                    staticCanvas.drawColor(overlayColor)
                }
            }
        } else {
            cachedBitmap = null
            cachedBgUri = null
        }

        // Update paint properties
        facePaint.color = faceColor
        if (isGlowEnabled) facePaint.setShadowLayer(8f, 0f, 0f, faceColor) else facePaint.clearShadowLayer()

        // Draw clock face outer circle
        staticCanvas.drawCircle(centerX, centerY, radius, facePaint)

        // Draw hour markings
        markerPaint.color = faceColor
        for (i in 0 until 12) {
            val angleDegrees = (i * 30).toDouble()
            val sinVal = getSin(angleDegrees)
            val cosVal = getCos(angleDegrees)
            val startX = centerX + (radius * 0.85f * sinVal)
            val startY = centerY - (radius * 0.85f * cosVal)
            val endX = centerX + (radius * 0.95f * sinVal)
            val endY = centerY - (radius * 0.95f * cosVal)
            staticCanvas.drawLine(startX, startY, endX, endY, markerPaint)
        }
        
        staticBitmap?.recycle()
        staticBitmap = bitmap
    }

    private fun loadBackgroundBitmap(uriString: String) {
        try {
            val uri = Uri.parse(uriString)
            
            // First decode with inJustDecodeBounds=true to check dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            carContext.contentResolver.openInputStream(uri)?.use { 
                BitmapFactory.decodeStream(it, null, options)
            }

            // Calculate inSampleSize to downsample if the image is much larger than the screen
            // A typical car screen is around 1000-2000 pixels wide.
            // If we don't have surfaceContainer yet, we default to a reasonable maximum like 1920.
            val targetWidth = surfaceContainer?.width ?: 1920
            val targetHeight = surfaceContainer?.height ?: 1080
            
            options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
            options.inJustDecodeBounds = false
            
            carContext.contentResolver.openInputStream(uri)?.use { 
                cachedBitmap = BitmapFactory.decodeStream(it, null, options)
            }
            cachedBgUri = uriString
        } catch (e: Exception) {
            Log.e("MyCarAppScreen", "Error loading background bitmap", e)
            cachedBitmap = null
            cachedBgUri = null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun drawHand(canvas: Canvas, cx: Float, cy: Float, length: Float, angleDegrees: Double, paint: Paint) {
        val sinVal = getSin(angleDegrees)
        val cosVal = getCos(angleDegrees)
        val stopX = cx + (length * sinVal)
        val stopY = cy - (length * cosVal)
        canvas.drawLine(cx, cy, stopX, stopY, paint)
    }
}
