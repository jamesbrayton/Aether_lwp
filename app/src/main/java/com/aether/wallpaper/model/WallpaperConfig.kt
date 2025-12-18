package com.aether.wallpaper.model

/**
 * Complete wallpaper configuration.
 *
 * Stores background image, particle effect layers, and global settings.
 * Serialized to JSON and saved in SharedPreferences.
 */
data class WallpaperConfig(
    val background: BackgroundConfig? = null,
    val layers: List<LayerConfig> = emptyList(),
    val globalSettings: GlobalSettings = GlobalSettings()
) {
    /**
     * Validate the configuration.
     * @return true if valid, false otherwise
     */
    fun validate(): Boolean {
        // Validate all layers
        return layers.all { it.validate() }
    }
}

/**
 * Background image configuration.
 */
data class BackgroundConfig(
    val uri: String,
    val crop: CropRect? = null
)

/**
 * Crop rectangle for background image.
 */
data class CropRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    /**
     * Validate crop rectangle.
     */
    fun validate(): Boolean {
        return x >= 0 && y >= 0 && width > 0 && height > 0
    }
}

/**
 * Individual particle effect layer configuration.
 */
data class LayerConfig(
    val shaderId: String,
    val order: Int,
    val enabled: Boolean = true,
    val opacity: Float = 1.0f,
    val depth: Float = 0.5f,
    val params: Map<String, Any> = emptyMap()
) {
    /**
     * Validate layer configuration.
     * @return true if valid, false otherwise
     */
    fun validate(): Boolean {
        // Opacity must be between 0.0 and 1.0
        if (opacity < 0.0f || opacity > 1.0f) return false

        // Depth must be between 0.0 and 1.0
        if (depth < 0.0f || depth > 1.0f) return false

        // Order should be non-negative
        if (order < 0) return false

        // Shader ID must not be empty
        if (shaderId.isBlank()) return false

        return true
    }
}

/**
 * Global wallpaper settings.
 */
data class GlobalSettings(
    val targetFps: Int = 60,
    val gyroscopeEnabled: Boolean = false
) {
    /**
     * Validate global settings.
     */
    fun validate(): Boolean {
        // FPS should be positive and reasonable
        return targetFps in 1..120
    }
}
