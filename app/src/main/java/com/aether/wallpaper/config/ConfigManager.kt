package com.aether.wallpaper.config

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.aether.wallpaper.model.WallpaperConfig
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException

/**
 * Manages wallpaper configuration persistence.
 *
 * Stores configuration as JSON in SharedPreferences.
 * Provides save, load, and default configuration methods.
 */
class ConfigManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val gson: Gson = Gson()

    companion object {
        private const val TAG = "ConfigManager"
        private const val PREFS_NAME = "aether_wallpaper_config"
        private const val KEY_CONFIG = "wallpaper_config"
    }

    /**
     * Save wallpaper configuration to SharedPreferences.
     *
     * @param config The configuration to save
     * @return true if save succeeded, false otherwise
     */
    fun saveConfig(config: WallpaperConfig): Boolean {
        return try {
            // Validate before saving
            if (!config.validate()) {
                Log.e(TAG, "Configuration validation failed")
                return false
            }

            // Serialize to JSON
            val json = gson.toJson(config)

            // Save to SharedPreferences
            prefs.edit()
                .putString(KEY_CONFIG, json)
                .apply()

            Log.d(TAG, "Configuration saved successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save configuration", e)
            false
        }
    }

    /**
     * Load wallpaper configuration from SharedPreferences.
     *
     * @return The loaded configuration, or default config if none exists or load fails
     */
    fun loadConfig(): WallpaperConfig {
        return try {
            // Get JSON from SharedPreferences
            val json = prefs.getString(KEY_CONFIG, null)

            if (json == null) {
                Log.d(TAG, "No saved configuration found, returning default")
                return getDefaultConfig()
            }

            // Deserialize from JSON
            val config = gson.fromJson(json, WallpaperConfig::class.java)

            // Validate loaded config
            if (!config.validate()) {
                Log.w(TAG, "Loaded configuration is invalid, returning default")
                return getDefaultConfig()
            }

            Log.d(TAG, "Configuration loaded successfully")
            config
        } catch (e: JsonSyntaxException) {
            Log.e(TAG, "Invalid JSON in saved configuration, returning default", e)
            getDefaultConfig()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load configuration, returning default", e)
            getDefaultConfig()
        }
    }

    /**
     * Get default wallpaper configuration.
     *
     * @return A valid default configuration with no background or layers
     */
    fun getDefaultConfig(): WallpaperConfig {
        return WallpaperConfig(
            background = null,
            layers = emptyList(),
            globalSettings = com.aether.wallpaper.model.GlobalSettings()
        )
    }

    /**
     * Check if a configuration has been saved.
     *
     * @return true if a configuration exists in SharedPreferences
     */
    fun hasConfig(): Boolean {
        return prefs.contains(KEY_CONFIG)
    }

    /**
     * Clear saved configuration.
     */
    fun clearConfig() {
        prefs.edit()
            .remove(KEY_CONFIG)
            .apply()
        Log.d(TAG, "Configuration cleared")
    }
}
