package com.aether.wallpaper.shader

import android.content.Context
import android.util.Log
import com.aether.wallpaper.model.ShaderDescriptor

/**
 * Registry for discovering and cataloging available shader effects.
 *
 * Scans the assets/shaders/ directory for .frag files, parses their embedded
 * metadata using ShaderMetadataParser, and maintains a catalog of available shaders.
 *
 * Usage:
 * ```
 * val registry = ShaderRegistry(context)
 * val shaders = registry.discoverShaders()
 * val snowShader = registry.getShaderById("snow")
 * ```
 */
class ShaderRegistry(private val context: Context) {

    private val parser = ShaderMetadataParser()
    private val descriptors = mutableMapOf<String, ShaderDescriptor>()

    companion object {
        private const val TAG = "ShaderRegistry"
        private const val SHADERS_DIR = "shaders"
    }

    /**
     * Discovers all shader files in assets/shaders/ and parses their metadata.
     *
     * @return List of successfully parsed ShaderDescriptor objects
     */
    fun discoverShaders(): List<ShaderDescriptor> {
        descriptors.clear()

        try {
            val shaderFiles = context.assets.list(SHADERS_DIR) ?: emptyArray()
            Log.d(TAG, "Found ${shaderFiles.size} files in $SHADERS_DIR: ${shaderFiles.joinToString()}")

            for (filename in shaderFiles) {
                Log.d(TAG, "Processing file: $filename")
                if (filename.endsWith(".frag")) {
                    try {
                        val filePath = "$SHADERS_DIR/$filename"
                        Log.d(TAG, "Loading shader source from: $filePath")
                        val shaderSource = loadShaderSource(filePath)
                        Log.d(TAG, "Shader source loaded (${shaderSource.length} bytes), parsing metadata...")
                        val descriptor = parser.parse(shaderSource, filePath)
                        Log.d(TAG, "Parsed shader: id=${descriptor.id}, name=${descriptor.name}, params=${descriptor.parameters.size}")

                        if (validateShader(descriptor)) {
                            descriptors[descriptor.id] = descriptor
                            Log.i(TAG, "✓ Successfully discovered shader: ${descriptor.getSummary()}")
                        } else {
                            Log.w(TAG, "✗ Shader validation failed: $filename")
                        }
                    } catch (e: ShaderParseException) {
                        Log.e(TAG, "✗ Failed to parse shader $filename: ${e.message}", e)
                        e.printStackTrace()
                        // Continue with other shaders - don't let one bad shader break discovery
                    } catch (e: Exception) {
                        Log.e(TAG, "✗ Unexpected error parsing shader $filename: ${e.message}", e)
                        e.printStackTrace()
                    }
                } else {
                    Log.d(TAG, "Skipping non-.frag file: $filename")
                }
            }

            Log.i(TAG, "=== Shader discovery complete. Found ${descriptors.size} valid shaders: ${descriptors.keys.joinToString()} ===")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to discover shaders: ${e.message}", e)
            e.printStackTrace()
        }

        return descriptors.values.toList()
    }

    /**
     * Retrieves a shader descriptor by its unique ID.
     *
     * @param id The shader ID (e.g., "snow", "rain")
     * @return ShaderDescriptor if found, null otherwise
     */
    fun getShaderById(id: String): ShaderDescriptor? {
        return descriptors[id]
    }

    /**
     * Returns all discovered shaders.
     *
     * @return List of all ShaderDescriptor objects
     */
    fun getAllShaders(): List<ShaderDescriptor> {
        return descriptors.values.toList()
    }

    /**
     * Loads shader source code from assets.
     *
     * @param filePath Path relative to assets directory (e.g., "shaders/snow.frag")
     * @return Shader source code as string
     */
    private fun loadShaderSource(filePath: String): String {
        return context.assets.open(filePath).use { inputStream ->
            inputStream.bufferedReader().use { it.readText() }
        }
    }

    /**
     * Validates that a shader descriptor is complete and correct.
     *
     * @param descriptor The ShaderDescriptor to validate
     * @return true if valid, false otherwise
     */
    private fun validateShader(descriptor: ShaderDescriptor): Boolean {
        return try {
            descriptor.validate()
            true
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "Shader validation failed for ${descriptor.id}: ${e.message}")
            false
        }
    }
}
