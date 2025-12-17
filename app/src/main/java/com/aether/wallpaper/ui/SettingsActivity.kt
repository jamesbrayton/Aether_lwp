package com.aether.wallpaper.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.aether.wallpaper.R

/**
 * Main settings activity for configuring the Aether live wallpaper.
 * Provides UI for:
 * - Background image selection and cropping
 * - Effect selection (from ShaderRegistry)
 * - Layer management (add/remove/reorder/configure)
 * - Dynamic parameter controls (generated from shader metadata)
 * - Live wallpaper preview
 */
class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: Set up view binding
        // TODO: Initialize ShaderRegistry and discover available shaders
        // TODO: Initialize ConfigManager
        // TODO: Set up RecyclerView adapters for effects and layers
        // TODO: Implement background image selection
        // TODO: Implement effect add/remove logic
        // TODO: Implement dynamic parameter UI generation
    }

    // TODO: Implement selectBackgroundImage()
    // TODO: Implement openCropActivity(uri)
    // TODO: Implement addEffectLayer(shaderId)
    // TODO: Implement removeLayer(position)
    // TODO: Implement showParameterControls(descriptor)
    // TODO: Implement applyWallpaper()
}
