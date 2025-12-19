package com.aether.wallpaper

import android.net.Uri
import android.service.wallpaper.WallpaperService
import android.util.Log
import android.view.SurfaceHolder
import com.aether.wallpaper.config.ConfigManager
import com.aether.wallpaper.renderer.GLRenderer
import com.aether.wallpaper.shader.ShaderRegistry

/**
 * Live Wallpaper Service for Aether wallpaper.
 *
 * This service integrates all Phase 1 components:
 * - ConfigManager: Loads wallpaper configuration
 * - ShaderRegistry: Discovers available shaders
 * - ShaderLoader: Compiles GLSL shaders (via GLRenderer)
 * - GLRenderer: Renders frames at 60fps
 * - TextureManager: Loads background images (via GLRenderer)
 *
 * Lifecycle:
 * 1. System calls onCreateEngine() when wallpaper is set
 * 2. Engine initializes components in onCreate()
 * 3. onSurfaceCreated() starts GL rendering thread with Engine's SurfaceHolder
 * 4. Renderer loads configuration and shaders
 * 5. Rendering loop runs continuously at 60fps
 * 6. Engine pauses/resumes based on visibility
 * 7. Resources released on onSurfaceDestroyed()
 *
 * Features:
 * - Dynamic shader loading based on configuration
 * - Parameter values from configuration applied to uniforms
 * - Background image loading from URI
 * - Visibility handling (pause when screen off)
 * - Clean resource management
 * - Manual EGL setup for proper wallpaper integration
 */
class AetherWallpaperService : WallpaperService() {

    companion object {
        private const val TAG = "AetherWallpaperService"
    }

    override fun onCreateEngine(): Engine {
        return AetherEngine()
    }

    /**
     * Wallpaper engine that manages OpenGL rendering.
     *
     * Uses manual EGL setup with a custom GL thread instead of GLSurfaceView.
     * This is the correct pattern for OpenGL ES wallpapers.
     */
    inner class AetherEngine : Engine() {

        private var glRenderer: GLWallpaperRenderer? = null
        private var renderer: GLRenderer? = null
        private var configManager: ConfigManager? = null
        private var shaderRegistry: ShaderRegistry? = null

        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)

            // Initialize components
            configManager = ConfigManager(this@AetherWallpaperService)
            shaderRegistry = ShaderRegistry(this@AetherWallpaperService)

            // Discover available shaders
            shaderRegistry?.discoverShaders()
        }

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)

            Log.d(TAG, "Engine.onSurfaceCreated")

            // Load configuration
            val config = configManager?.loadConfig()

            // Create renderer with configuration
            config?.let {
                // Get the first enabled layer's shader, or use test.frag as fallback
                val firstEnabledLayer = it.layers.firstOrNull { layer -> layer.enabled }
                val fragmentShaderPath = if (firstEnabledLayer != null) {
                    val shader = shaderRegistry?.getShaderById(firstEnabledLayer.shaderId)
                    shader?.fragmentShaderPath ?: "shaders/test.frag"
                } else {
                    "shaders/test.frag"
                }

                // Strip "shaders/" prefix since ShaderLoader.loadShaderFromAssets() adds it
                val fragmentShaderFile = fragmentShaderPath.removePrefix("shaders/")

                renderer = GLRenderer(
                    this@AetherWallpaperService,
                    "vertex_shader.vert",  // Just filename, ShaderLoader adds "shaders/" prefix
                    fragmentShaderFile
                )

                // Set background configuration if available
                it.background?.uri?.let { uriString ->
                    try {
                        val uri = Uri.parse(uriString)
                        Log.d(TAG, "Setting background: uri=$uri, crop=${it.background.crop}")
                        renderer?.setBackgroundConfig(uri, it.background.crop)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse background URI: $uriString", e)
                    }
                }

                // Create GL rendering thread with wallpaper's surface holder
                glRenderer = GLWallpaperRenderer(holder, renderer!!)
                glRenderer?.start()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)
            // Surface size changes are handled automatically by the GL thread
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)

            // Stop GL rendering thread
            glRenderer?.stop()
            glRenderer = null
            renderer = null
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                // Resume rendering when visible
                glRenderer?.start()
            } else {
                // Pause rendering when not visible (saves battery)
                glRenderer?.stop()
            }
        }

        override fun onDestroy() {
            super.onDestroy()

            // Clean up resources
            glRenderer?.stop()
            glRenderer = null
            renderer = null
            configManager = null
            shaderRegistry = null
        }
    }
}
