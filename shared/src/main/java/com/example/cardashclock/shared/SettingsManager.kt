package com.example.cardashclock.shared

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("car_dash_clock_prefs", Context.MODE_PRIVATE)

    companion object {
        const val KEY_FACE_COLOR_DAY = "face_color_day"
        const val KEY_FACE_COLOR_NIGHT = "face_color_night"
        const val KEY_HAND_COLOR_DAY = "hand_color_day"
        const val KEY_HAND_COLOR_NIGHT = "hand_color_night"
        const val KEY_SECOND_HAND_COLOR_DAY = "second_hand_color_day"
        const val KEY_SECOND_HAND_COLOR_NIGHT = "second_hand_color_night"
        const val KEY_BG_IMAGE_URI_DAY = "bg_image_uri_day"
        const val KEY_BG_IMAGE_URI_NIGHT = "bg_image_uri_night"
        const val KEY_BG_COLOR_DAY = "bg_color_day"
        const val KEY_BG_COLOR_NIGHT = "bg_color_night"
        const val KEY_OVERLAY_COLOR_DAY = "overlay_color_day"
        const val KEY_OVERLAY_COLOR_NIGHT = "overlay_color_night"
        const val KEY_GLOW_DAY = "glow_day"
        const val KEY_GLOW_NIGHT = "glow_night"
    }

    fun getFaceColor(isDarkMode: Boolean): Int {
        val key = if (isDarkMode) KEY_FACE_COLOR_NIGHT else KEY_FACE_COLOR_DAY
        val default = if (isDarkMode) Color.WHITE else Color.BLACK
        return prefs.getInt(key, default)
    }

    fun setFaceColor(color: Int, isDarkMode: Boolean) {
        val key = if (isDarkMode) KEY_FACE_COLOR_NIGHT else KEY_FACE_COLOR_DAY
        prefs.edit().putInt(key, color).apply()
    }

    fun getHandColor(isDarkMode: Boolean): Int {
        val key = if (isDarkMode) KEY_HAND_COLOR_NIGHT else KEY_HAND_COLOR_DAY
        val default = if (isDarkMode) Color.WHITE else Color.BLACK
        return prefs.getInt(key, default)
    }

    fun setHandColor(color: Int, isDarkMode: Boolean) {
        val key = if (isDarkMode) KEY_HAND_COLOR_NIGHT else KEY_HAND_COLOR_DAY
        prefs.edit().putInt(key, color).apply()
    }

    fun getSecondHandColor(isDarkMode: Boolean): Int {
        val key = if (isDarkMode) KEY_SECOND_HAND_COLOR_NIGHT else KEY_SECOND_HAND_COLOR_DAY
        return prefs.getInt(key, Color.RED)
    }

    fun setSecondHandColor(color: Int, isDarkMode: Boolean) {
        val key = if (isDarkMode) KEY_SECOND_HAND_COLOR_NIGHT else KEY_SECOND_HAND_COLOR_DAY
        prefs.edit().putInt(key, color).apply()
    }

    fun getBgImageUri(isDarkMode: Boolean): String? {
        val key = if (isDarkMode) KEY_BG_IMAGE_URI_NIGHT else KEY_BG_IMAGE_URI_DAY
        return prefs.getString(key, null)
    }

    fun setBgImageUri(uri: String?, isDarkMode: Boolean) {
        val key = if (isDarkMode) KEY_BG_IMAGE_URI_NIGHT else KEY_BG_IMAGE_URI_DAY
        prefs.edit().putString(key, uri).apply()
    }

    fun getBackgroundColor(isDarkMode: Boolean): Int {
        val key = if (isDarkMode) KEY_BG_COLOR_NIGHT else KEY_BG_COLOR_DAY
        val default = if (isDarkMode) Color.parseColor("#121212") else Color.parseColor("#F5F5F5")
        return prefs.getInt(key, default)
    }

    fun setBackgroundColor(color: Int, isDarkMode: Boolean) {
        val key = if (isDarkMode) KEY_BG_COLOR_NIGHT else KEY_BG_COLOR_DAY
        prefs.edit().putInt(key, color).apply()
    }

    fun getOverlayColor(isDarkMode: Boolean): Int {
        val key = if (isDarkMode) KEY_OVERLAY_COLOR_NIGHT else KEY_OVERLAY_COLOR_DAY
        val default = if (isDarkMode) Color.parseColor("#33000000") else Color.parseColor("#1AFFFFFF")
        return prefs.getInt(key, default)
    }

    fun setOverlayColor(color: Int, isDarkMode: Boolean) {
        val key = if (isDarkMode) KEY_OVERLAY_COLOR_NIGHT else KEY_OVERLAY_COLOR_DAY
        prefs.edit().putInt(key, color).apply()
    }

    fun isGlowEnabled(isDarkMode: Boolean): Boolean {
        val key = if (isDarkMode) KEY_GLOW_NIGHT else KEY_GLOW_DAY
        return prefs.getBoolean(key, false)
    }

    fun setGlowEnabled(enabled: Boolean, isDarkMode: Boolean) {
        val key = if (isDarkMode) KEY_GLOW_NIGHT else KEY_GLOW_DAY
        prefs.edit().putBoolean(key, enabled).apply()
    }

    fun resetColors() {
        prefs.edit()
            .remove(KEY_FACE_COLOR_DAY)
            .remove(KEY_FACE_COLOR_NIGHT)
            .remove(KEY_HAND_COLOR_DAY)
            .remove(KEY_HAND_COLOR_NIGHT)
            .remove(KEY_SECOND_HAND_COLOR_DAY)
            .remove(KEY_SECOND_HAND_COLOR_NIGHT)
            .remove(KEY_BG_COLOR_DAY)
            .remove(KEY_BG_COLOR_NIGHT)
            .remove(KEY_OVERLAY_COLOR_DAY)
            .remove(KEY_OVERLAY_COLOR_NIGHT)
            .remove(KEY_GLOW_DAY)
            .remove(KEY_GLOW_NIGHT)
            .apply()
    }

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}
