package com.cmdrkeene.dashclockforandroidauto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.util.TypedValue
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cardashclock.shared.SettingsManager
import com.google.android.material.color.DynamicColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.android.material.tabs.TabLayout

class MainActivity : AppCompatActivity() {
    private lateinit var settingsManager: SettingsManager
    private var isEditModeNight = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            settingsManager.setBgImageUri(it.toString(), isEditModeNight)
            updatePreviews()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        DynamicColors.applyToActivityIfAvailable(this)
        super.onCreate(savedInstanceState)
        settingsManager = SettingsManager(this)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        setupTabs()

        findViewById<Button>(R.id.btn_pick_face_color).setOnClickListener {
            showColorPicker("Choose Face Color", settingsManager.getFaceColor(isEditModeNight), showAlpha = false) { color ->
                settingsManager.setFaceColor(color, isEditModeNight)
                updatePreviews()
            }
        }

        findViewById<Button>(R.id.btn_pick_hand_color).setOnClickListener {
            showColorPicker("Choose Hour/Min Hand Color", settingsManager.getHandColor(isEditModeNight), showAlpha = false) { color ->
                settingsManager.setHandColor(color, isEditModeNight)
                updatePreviews()
            }
        }

        findViewById<Button>(R.id.btn_pick_second_hand_color).setOnClickListener {
            showColorPicker("Choose Second Hand Color", settingsManager.getSecondHandColor(isEditModeNight), showAlpha = false) { color ->
                settingsManager.setSecondHandColor(color, isEditModeNight)
                updatePreviews()
            }
        }

        findViewById<Button>(R.id.btn_pick_bg_image).setOnClickListener {
            pickImage.launch("image/*")
        }

        findViewById<Button>(R.id.btn_clear_bg_image).setOnClickListener {
            settingsManager.setBgImageUri(null, isEditModeNight)
            updatePreviews()
        }

        findViewById<Button>(R.id.btn_pick_overlay_color).setOnClickListener {
            showColorPicker("Choose Image Overlay Tint", settingsManager.getOverlayColor(isEditModeNight), showAlpha = true) { color ->
                settingsManager.setOverlayColor(color, isEditModeNight)
                updatePreviews()
            }
        }

        findViewById<Button>(R.id.btn_pick_bg_color).setOnClickListener {
            showColorPicker("Choose Background Color", settingsManager.getBackgroundColor(isEditModeNight), showAlpha = false) { color ->
                settingsManager.setBackgroundColor(color, isEditModeNight)
                settingsManager.setBgImageUri(null, isEditModeNight)
                updatePreviews()
            }
        }

        val glowSwitch = findViewById<MaterialSwitch>(R.id.switch_glow)
        glowSwitch.isChecked = settingsManager.isGlowEnabled(isEditModeNight)
        glowSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsManager.setGlowEnabled(isChecked, isEditModeNight)
            updatePreviews()
        }

        findViewById<Button>(R.id.btn_reset).setOnClickListener {
            settingsManager.resetColors()
            glowSwitch.isChecked = settingsManager.isGlowEnabled(isEditModeNight)
            updatePreviews()
        }

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupPreviewClickListeners()
        updatePreviews()
    }

    private fun setupPreviewClickListeners() {
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        findViewById<View>(R.id.card_preview_day).setOnClickListener {
            tabLayout.getTabAt(0)?.select()
        }
        findViewById<View>(R.id.card_preview_night).setOnClickListener {
            tabLayout.getTabAt(1)?.select()
        }
    }

    private fun setupTabs() {
        val tabLayout = findViewById<TabLayout>(R.id.tab_layout)
        val glowSwitch = findViewById<MaterialSwitch>(R.id.switch_glow)
        val dayCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_preview_day)
        val nightCard = findViewById<com.google.android.material.card.MaterialCardView>(R.id.card_preview_night)
        
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isEditModeNight = tab?.position == 1
                // Sync glow switch state with current mode
                glowSwitch.isChecked = settingsManager.isGlowEnabled(isEditModeNight)
                
                // Update selection visual
                if (isEditModeNight) {
                    nightCard.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics).toInt()
                    nightCard.cardElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics)
                    dayCard.strokeWidth = 0
                    dayCard.cardElevation = 0f
                } else {
                    dayCard.strokeWidth = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3f, resources.displayMetrics).toInt()
                    dayCard.cardElevation = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 8f, resources.displayMetrics)
                    nightCard.strokeWidth = 0
                    nightCard.cardElevation = 0f
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun updatePreviews() {
        val previewDay = findViewById<ClockPreviewView>(R.id.preview_day)
        val previewNight = findViewById<ClockPreviewView>(R.id.preview_night)

        // Update Day Preview
        previewDay.isDarkMode = false
        previewDay.isGlowEnabled = settingsManager.isGlowEnabled(false)
        previewDay.faceColor = settingsManager.getFaceColor(false)
        previewDay.handColor = settingsManager.getHandColor(false)
        previewDay.secondHandColor = settingsManager.getSecondHandColor(false)
        previewDay.clockBackgroundColor = settingsManager.getBackgroundColor(false)
        previewDay.overlayColor = settingsManager.getOverlayColor(false)
        previewDay.backgroundBitmap = loadBitmapFromUri(settingsManager.getBgImageUri(false))
        previewDay.invalidate()

        // Update Night Preview
        previewNight.isDarkMode = true
        previewNight.isGlowEnabled = settingsManager.isGlowEnabled(true)
        previewNight.faceColor = settingsManager.getFaceColor(true)
        previewNight.handColor = settingsManager.getHandColor(true)
        previewNight.secondHandColor = settingsManager.getSecondHandColor(true)
        previewNight.clockBackgroundColor = settingsManager.getBackgroundColor(true)
        previewNight.overlayColor = settingsManager.getOverlayColor(true)
        previewNight.backgroundBitmap = loadBitmapFromUri(settingsManager.getBgImageUri(true))
        previewNight.invalidate()
    }

    private fun loadBitmapFromUri(uriString: String?): android.graphics.Bitmap? {
        if (uriString == null) return null
        return try {
            val inputStream = contentResolver.openInputStream(Uri.parse(uriString))
            BitmapFactory.decodeStream(inputStream)
        } catch (e: Exception) {
            null
        }
    }

    private fun showColorPicker(title: String, initialColor: Int, showAlpha: Boolean, onColorSelected: (Int) -> Unit) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_color_picker, null)
        val preview = dialogView.findViewById<View>(R.id.color_preview)
        val alphaSeek = dialogView.findViewById<SeekBar>(R.id.alpha_seekbar)
        val hueSeek = dialogView.findViewById<SeekBar>(R.id.hue_seekbar)
        val satSeek = dialogView.findViewById<SeekBar>(R.id.saturation_seekbar)
        val valSeek = dialogView.findViewById<SeekBar>(R.id.value_seekbar)
        
        val alphaContainer = dialogView.findViewById<View>(R.id.container_alpha)
        val alphaGradientView = dialogView.findViewById<View>(R.id.alpha_gradient)
        val hueGradientView = dialogView.findViewById<View>(R.id.hue_gradient)
        val satGradientView = dialogView.findViewById<View>(R.id.sat_gradient)
        val valGradientView = dialogView.findViewById<View>(R.id.val_gradient)

        alphaContainer.visibility = if (showAlpha) View.VISIBLE else View.GONE
        dialogView.findViewById<View>(R.id.label_alpha).visibility = if (showAlpha) View.VISIBLE else View.GONE

        val hsv = FloatArray(3)
        Color.colorToHSV(initialColor, hsv)
        val initialAlpha = if (showAlpha) Color.alpha(initialColor) else 255
        
        alphaSeek.progress = initialAlpha
        hueSeek.progress = hsv[0].toInt()
        satSeek.progress = (hsv[1] * 100).toInt()
        valSeek.progress = (hsv[2] * 100).toInt()

        val hueColors = IntArray(361) { i -> Color.HSVToColor(floatArrayOf(i.toFloat(), 1f, 1f)) }
        hueGradientView.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, hueColors).apply { cornerRadius = 8f }

        fun updateGradients() {
            val currentColor = Color.HSVToColor(hsv)
            if (showAlpha) {
                val alphaColors = intArrayOf(Color.TRANSPARENT, currentColor)
                alphaGradientView.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, alphaColors).apply { cornerRadius = 8f }
            }
            val satColors = intArrayOf(Color.HSVToColor(floatArrayOf(hsv[0], 0f, hsv[2])), Color.HSVToColor(floatArrayOf(hsv[0], 1f, hsv[2])))
            satGradientView.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, satColors).apply { cornerRadius = 8f }
            val valColors = intArrayOf(Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], 0f)), Color.HSVToColor(floatArrayOf(hsv[0], hsv[1], 1f)))
            valGradientView.background = GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, valColors).apply { cornerRadius = 8f }
        }

        fun updateColor() {
            hsv[0] = hueSeek.progress.toFloat()
            hsv[1] = satSeek.progress / 100f
            hsv[2] = valSeek.progress / 100f
            val alpha = if (showAlpha) alphaSeek.progress else 255
            preview.setBackgroundColor(Color.HSVToColor(alpha, hsv))
            updateGradients()
        }

        val listener = object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(s: SeekBar?, p: Int, f: Boolean) = updateColor()
            override fun onStartTrackingTouch(s: SeekBar?) {}
            override fun onStopTrackingTouch(s: SeekBar?) {}
        }

        alphaSeek.setOnSeekBarChangeListener(listener)
        hueSeek.setOnSeekBarChangeListener(listener)
        satSeek.setOnSeekBarChangeListener(listener)
        valSeek.setOnSeekBarChangeListener(listener)
        
        updateColor()

        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(dialogView)
            .setPositiveButton("Select") { _, _ ->
                val alpha = if (showAlpha) alphaSeek.progress else 255
                onColorSelected(Color.HSVToColor(alpha, hsv))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
