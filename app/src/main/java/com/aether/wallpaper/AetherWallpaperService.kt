package com.aether.wallpaper

import android.opengl.GLSurfaceView
import android.service.wallpaper.WallpaperService
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
 * 2. Engine creates GLSurfaceView with OpenGL ES 2.0 context
 * 3. Renderer loads configuration and shaders
 * 4. Rendering loop runs continuously at 60fps
 * 5. Engine pauses/resumes based on visibility
 * 6. Resources released on destroy
 *
 * Features:
 * - Dynamic shader loading based on configuration
 * - Parameter values from configuration applied to uniforms
 * - Background image loading from URI
 * - Visibility handling (pause when screen off)
 * - Clean resource management
 */
class AetherWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine {
        return AetherEngine()
    }

    /**
     * Wallpaper engine that manages OpenGL rendering.
     *
     * Extends WallpaperService.Engine and integrates GLSurfaceView
     * for hardware-accelerated rendering.
     */
    inner class AetherEngine : Engine() {

        private var glSurfaceView: WallpaperGLSurfaceView? = null
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

            // Load configuration
            val config = configManager?.loadConfig()

            // Create GLSurfaceView for wallpaper
            glSurfaceView = WallpaperGLSurfaceView(this@AetherWallpaperService, this)
            glSurfaceView?.setEGLContextClientVersion(2) // OpenGL ES 2.0

            // Create renderer with configuration
            config?.let {
                // Get the first enabled layer's shader, or use test.frag as fallback
                val firstEnabledLayer = it.layers.firstOrNull { layer -> layer.enabled }
                val fragmentShaderFile = if (firstEnabledLayer != null) {
                    val shader = shaderRegistry?.getShaderById(firstEnabledLayer.shaderId)
                    shader?.fragmentShaderPath ?: "shaders/test.frag"
                } else {
                    "shaders/test.frag"
                }

                renderer = GLRenderer(
                    this@AetherWallpaperService,
                    "shaders/vertex_shader.vert",
                    fragmentShaderFile
                )
                glSurfaceView?.setRenderer(renderer)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                // Resume rendering when visible
                glSurfaceView?.onResume()
            } else {
                // Pause rendering when not visible (saves battery)
                glSurfaceView?.onPause()
            }
        }

        override fun onDestroy() {
            super.onDestroy()

            // Clean up resources
            glSurfaceView?.onDestroy()
            renderer = null
            glSurfaceView = null
            configManager = null
            shaderRegistry = null
        }
    }

    /**
     * Custom GLSurfaceView for live wallpaper.
     *
     * Integrates GLSurfaceView with WallpaperService by overriding
     * getHolder() to return the wallpaper's SurfaceHolder.
     */
    class WallpaperGLSurfaceView(
        wallpaperService: AetherWallpaperService,
        engine: WallpaperService.Engine
    ) : GLSurfaceView(wallpaperService) {

        private val wallpaperSurfaceHolder: SurfaceHolder = engine.surfaceHolder

        override fun getHolder(): SurfaceHolder {
            // Return the wallpaper's surface holder instead of creating a new one
            return wallpaperSurfaceHolder
        }

        fun onDestroy() {
            // Clean up GLSurfaceView resources
            // The actual cleanup is handled by the renderer
        }
    }
}
