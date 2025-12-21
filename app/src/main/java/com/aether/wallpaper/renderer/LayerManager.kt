package com.aether.wallpaper.renderer

import android.content.Context
import android.opengl.GLES20
import android.util.Log
import com.aether.wallpaper.model.LayerConfig
import com.aether.wallpaper.shader.ShaderLoader

/**
 * Manages shader programs for multiple effect layers.
 *
 * Responsibilities:
 * - Cache compiled shader programs to avoid recompilation
 * - Provide enabled layers sorted by render order
 * - Handle shader compilation failures gracefully
 * - Manage program lifecycle (creation and deletion)
 *
 * Usage:
 * ```
 * val layerManager = LayerManager(context, shaderLoader, config.layers)
 * val program = layerManager.getOrCreateProgram("snow", vertexShaderId)
 * val enabledLayers = layerManager.getEnabledLayers()
 * layerManager.release()
 * ```
 */
class LayerManager(
    private val context: Context,
    private val shaderLoader: ShaderLoader,
    private var layers: List<LayerConfig>
) {

    companion object {
        private const val TAG = "LayerManager"
    }

    // Cache of shader ID → compiled program handle
    private val programCache = mutableMapOf<String, Int>()

    /**
     * Get or create a shader program for the given shader ID.
     *
     * Programs are cached on first creation. Subsequent calls with the same
     * shader ID return the cached program without recompilation.
     *
     * @param shaderId The shader identifier (e.g., "snow", "rain")
     * @param vertexShaderId The compiled vertex shader ID (reused across all programs)
     * @return Program handle, or 0 if compilation fails
     */
    fun getOrCreateProgram(shaderId: String, vertexShaderId: Int): Int {
        // Check cache first
        programCache[shaderId]?.let {
            Log.d(TAG, "Returning cached program for shader: $shaderId (program=$it)")
            return it
        }

        // Compile shader if not cached
        return try {
            Log.d(TAG, "Compiling shader: $shaderId")

            // Load fragment shader source
            val fragmentShaderPath = "$shaderId.frag"
            val fragmentSource = shaderLoader.loadShaderFromAssets(fragmentShaderPath)

            // Compile fragment shader
            val fragmentShaderId = shaderLoader.compileShader(
                fragmentSource,
                GLES20.GL_FRAGMENT_SHADER
            )

            // Link program
            val program = shaderLoader.linkProgram(vertexShaderId, fragmentShaderId)

            // Delete fragment shader (no longer needed after linking)
            GLES20.glDeleteShader(fragmentShaderId)

            // Cache program
            programCache[shaderId] = program

            Log.d(TAG, "Successfully compiled and cached shader: $shaderId (program=$program)")
            program

        } catch (e: Exception) {
            Log.e(TAG, "Failed to compile shader: $shaderId", e)
            0 // Return invalid program handle
        }
    }

    /**
     * Get all enabled layers sorted by render order.
     *
     * Returns only layers where `enabled = true`, sorted by the `order` field.
     * Lower order values are rendered first (background), higher values last (foreground).
     *
     * @return List of enabled layers sorted by order
     */
    fun getEnabledLayers(): List<LayerConfig> {
        return layers
            .filter { it.enabled }
            .sortedBy { it.order }
    }

    /**
     * Update the layer configuration.
     *
     * This allows dynamic changes to the layer list without recreating the LayerManager.
     * Note: This does not invalidate the program cache - cached programs remain valid.
     *
     * @param newLayers New layer configuration
     */
    fun updateLayers(newLayers: List<LayerConfig>) {
        Log.d(TAG, "Updating layers: ${newLayers.size} layers")
        layers = newLayers
    }

    /**
     * Release all cached shader programs.
     *
     * Deletes all GL program objects and clears the cache.
     * Should be called when the GL context is destroyed.
     */
    fun release() {
        Log.d(TAG, "Releasing ${programCache.size} cached shader programs")

        programCache.values.forEach { program ->
            if (program != 0) {
                GLES20.glDeleteProgram(program)
                Log.d(TAG, "Deleted shader program: $program")
            }
        }

        programCache.clear()
    }

    /**
     * Get the number of cached programs.
     *
     * Primarily for testing and debugging.
     *
     * @return Number of programs in cache
     */
    fun getCachedProgramCount(): Int = programCache.size
}
