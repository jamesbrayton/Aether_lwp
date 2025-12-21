package com.aether.wallpaper.renderer

import android.content.Context
import android.net.Uri
import android.opengl.GLES20
import android.util.Log
import com.aether.wallpaper.model.CropRect
import com.aether.wallpaper.model.WallpaperConfig
import com.aether.wallpaper.shader.ShaderLoader
import com.aether.wallpaper.texture.TextureManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 2.0 renderer for multi-layer particle effects.
 *
 * Implements multi-pass rendering:
 * 1. Render each layer to its own FBO
 * 2. Composite all layers with the compositor shader
 *
 * Manages LayerManager, FBOManager, and compositor pipeline.
 */
class GLRenderer(
    private val context: Context,
    private val vertexShaderFile: String = "vertex_shader.vert",
    private var wallpaperConfig: WallpaperConfig
) : android.opengl.GLSurfaceView.Renderer {

    // Multi-layer rendering components
    private lateinit var layerManager: LayerManager
    private lateinit var fboManager: FBOManager
    private var vertexShaderId: Int = 0
    private var compositorProgram: Int = 0
    private val compositorUniforms = mutableMapOf<String, Int>()

    // Vertex buffer for fullscreen quad
    private var vertexBuffer: FloatBuffer? = null
    private var positionHandle: Int = 0

    // Screen dimensions
    private var screenWidth: Int = 0
    private var screenHeight: Int = 0

    // Frame timing
    private var startTime: Long = 0
    private var lastFrameTime: Long = 0
    private var frameCount: Int = 0

    // ShaderLoader instance
    private lateinit var shaderLoader: ShaderLoader

    // TextureManager instance
    private lateinit var textureManager: TextureManager

    // Background texture ID
    private var backgroundTextureId: Int = 0

    // Background configuration (URI and crop rect)
    private var backgroundUri: Uri? = null
    private var backgroundCropRect: CropRect? = null
    private var backgroundTextureLoaded: Boolean = false

    companion object {
        private const val TAG = "GLRenderer"
        // Fullscreen quad vertices: 2 triangles covering (-1,-1) to (1,1)
        // Triangle 1: (-1,-1), (1,-1), (-1,1)
        // Triangle 2: (-1,1), (1,-1), (1,1)
        private val FULLSCREEN_QUAD_VERTICES = floatArrayOf(
            // Triangle 1
            -1.0f, -1.0f, 0.0f,  // Bottom-left
             1.0f, -1.0f, 0.0f,  // Bottom-right
            -1.0f,  1.0f, 0.0f,  // Top-left

            // Triangle 2
            -1.0f,  1.0f, 0.0f,  // Top-left
             1.0f, -1.0f, 0.0f,  // Bottom-right
             1.0f,  1.0f, 0.0f   // Top-right
        )

        private const val COORDS_PER_VERTEX = 3
        private const val VERTEX_COUNT = 6
        private const val VERTEX_STRIDE = COORDS_PER_VERTEX * 4 // 4 bytes per float
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // Initialize OpenGL state
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)

        // Initialize managers
        shaderLoader = ShaderLoader(context)
        textureManager = TextureManager(context)

        // Compile vertex shader once (reused for all programs)
        Log.d(TAG, "Compiling vertex shader: $vertexShaderFile")
        val vertexSource = shaderLoader.loadShaderFromAssets(vertexShaderFile)
        vertexShaderId = shaderLoader.compileShader(vertexSource, GLES20.GL_VERTEX_SHADER)

        // Initialize layer manager
        layerManager = LayerManager(context, shaderLoader, wallpaperConfig.layers)

        // Compile all layer shaders
        Log.d(TAG, "Compiling layer shaders...")
        for (layer in layerManager.getEnabledLayers()) {
            val program = layerManager.getOrCreateProgram(layer.shaderId, vertexShaderId)
            if (program == 0) {
                Log.w(TAG, "Failed to compile shader: ${layer.shaderId}")
            } else {
                Log.d(TAG, "Compiled shader: ${layer.shaderId} -> program $program")
            }
        }

        // Compile compositor shader
        Log.d(TAG, "Compiling compositor shader")
        compositorProgram = shaderLoader.createProgram(vertexShaderFile, "compositor.frag")

        // Cache compositor uniform locations
        cacheCompositorUniforms()

        // Get attribute location
        positionHandle = GLES20.glGetAttribLocation(compositorProgram, "a_position")

        // Create fullscreen quad vertex buffer
        createVertexBuffer()

        // Create placeholder background texture
        createPlaceholderTexture()

        // Reset texture loaded flag
        backgroundTextureLoaded = false

        // Initialize timing
        startTime = System.currentTimeMillis()
        lastFrameTime = startTime
        frameCount = 0

        checkGLError("onSurfaceCreated")
    }

    /**
     * Cache uniform locations for compositor shader.
     */
    private fun cacheCompositorUniforms() {
        compositorUniforms["u_backgroundTexture"] =
            GLES20.glGetUniformLocation(compositorProgram, "u_backgroundTexture")
        compositorUniforms["u_resolution"] =
            GLES20.glGetUniformLocation(compositorProgram, "u_resolution")
        compositorUniforms["u_layerCount"] =
            GLES20.glGetUniformLocation(compositorProgram, "u_layerCount")

        // Cache layer texture and opacity uniform locations
        for (i in 0..4) {
            compositorUniforms["u_layer$i"] =
                GLES20.glGetUniformLocation(compositorProgram, "u_layer$i")
            compositorUniforms["u_opacity$i"] =
                GLES20.glGetUniformLocation(compositorProgram, "u_opacity$i")
        }

        Log.d(TAG, "Cached ${compositorUniforms.size} compositor uniforms")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: ${width}x${height}")

        // Set viewport to match screen size
        GLES20.glViewport(0, 0, width, height)

        // Store screen dimensions
        screenWidth = width
        screenHeight = height

        // Initialize FBO manager
        fboManager = FBOManager()

        // Create FBOs for each enabled layer
        Log.d(TAG, "Creating FBOs for ${layerManager.getEnabledLayers().size} layers")
        for ((index, layer) in layerManager.getEnabledLayers().withIndex()) {
            val layerId = "layer_$index"
            val fboInfo = fboManager.createFBO(layerId, width, height)
            if (fboInfo == null) {
                Log.e(TAG, "FBO creation failed for ${layer.shaderId}")
            } else {
                Log.d(TAG, "Created FBO for ${layer.shaderId}: texture=${fboInfo.textureId}")
            }
        }

        // Load background texture if configured and not already loaded
        if (!backgroundTextureLoaded && backgroundUri != null) {
            loadBackgroundTexture(backgroundUri!!, backgroundCropRect)
            backgroundTextureLoaded = true
        }

        checkGLError("onSurfaceChanged")
    }

    override fun onDrawFrame(gl: GL10?) {
        val currentTime = System.currentTimeMillis()
        val elapsedTime = (currentTime - startTime) / 1000.0f
        val enabledLayers = layerManager.getEnabledLayers()

        // PHASE 1: Render each layer to its FBO
        for ((index, layer) in enabledLayers.withIndex()) {
            val layerId = "layer_$index"
            val program = layerManager.getOrCreateProgram(layer.shaderId, vertexShaderId)

            if (program == 0) {
                Log.w(TAG, "Skipping layer ${layer.shaderId} - no program")
                continue
            }

            // Bind layer FBO
            if (!fboManager.bindFBO(layerId)) {
                Log.w(TAG, "Failed to bind FBO for $layerId")
                continue
            }

            // Clear FBO
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

            // Use layer shader
            GLES20.glUseProgram(program)

            // Set uniforms for this layer
            setLayerUniforms(program, layer, elapsedTime)

            // Render fullscreen quad to FBO
            renderFullscreenQuad()

            // Unbind FBO
            fboManager.unbindFBO()
        }

        // PHASE 2: Composite all layers to screen
        compositeLayersToScreen(enabledLayers)

        // Update frame timing
        lastFrameTime = currentTime
        frameCount++

        checkGLError("onDrawFrame")
    }

    /**
     * Set uniforms for a specific layer.
     */
    private fun setLayerUniforms(program: Int, layer: com.aether.wallpaper.model.LayerConfig, time: Float) {
        // Get uniform locations for this program
        val uTime = GLES20.glGetUniformLocation(program, "u_time")
        val uResolution = GLES20.glGetUniformLocation(program, "u_resolution")
        val uBackground = GLES20.glGetUniformLocation(program, "u_backgroundTexture")
        val uGyro = GLES20.glGetUniformLocation(program, "u_gyroOffset")
        val uDepth = GLES20.glGetUniformLocation(program, "u_depthValue")

        // Set standard uniforms
        if (uTime >= 0) GLES20.glUniform1f(uTime, time)
        if (uResolution >= 0) GLES20.glUniform2f(uResolution, screenWidth.toFloat(), screenHeight.toFloat())
        if (uGyro >= 0) GLES20.glUniform2f(uGyro, 0.0f, 0.0f) // Phase 2: gyroscope
        if (uDepth >= 0) GLES20.glUniform1f(uDepth, layer.depth)

        // Bind background texture (texture unit 0)
        if (uBackground >= 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureId)
            GLES20.glUniform1i(uBackground, 0)
        }

        // Set layer-specific parameters
        layer.params.forEach { (name, value) ->
            val location = GLES20.glGetUniformLocation(program, name)
            if (location >= 0) {
                val floatValue = when (value) {
                    is Number -> value.toFloat()
                    else -> value.toString().toFloatOrNull() ?: 0.0f
                }
                GLES20.glUniform1f(location, floatValue)
            }
        }
    }

    /**
     * Composite all layer textures to the screen.
     */
    private fun compositeLayersToScreen(layers: List<com.aether.wallpaper.model.LayerConfig>) {
        // Bind screen framebuffer
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Use compositor shader
        GLES20.glUseProgram(compositorProgram)

        // Set background texture (unit 0)
        compositorUniforms["u_backgroundTexture"]?.let { loc ->
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureId)
            GLES20.glUniform1i(loc, 0)
        }

        // Set resolution
        compositorUniforms["u_resolution"]?.let { loc ->
            GLES20.glUniform2f(loc, screenWidth.toFloat(), screenHeight.toFloat())
        }

        // Bind layer textures (units 1-5)
        for ((index, layer) in layers.withIndex()) {
            if (index >= 5) break

            val layerId = "layer_$index"
            val textureId = fboManager.getTexture(layerId)

            // Bind texture to unit 1+index
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1 + index)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

            // Set sampler uniform
            compositorUniforms["u_layer$index"]?.let { loc ->
                GLES20.glUniform1i(loc, 1 + index)
            }

            // Set opacity uniform
            compositorUniforms["u_opacity$index"]?.let { loc ->
                GLES20.glUniform1f(loc, layer.opacity)
            }
        }

        // Set layer count
        compositorUniforms["u_layerCount"]?.let { loc ->
            GLES20.glUniform1i(loc, layers.size)
        }

        // Render final composite
        renderFullscreenQuad()
    }

    /**
     * Render the fullscreen quad.
     */
    private fun renderFullscreenQuad() {
        vertexBuffer?.let { buffer ->
            // Enable vertex attribute array
            GLES20.glEnableVertexAttribArray(positionHandle)

            // Set vertex attribute pointer
            buffer.position(0)
            GLES20.glVertexAttribPointer(
                positionHandle,
                COORDS_PER_VERTEX,
                GLES20.GL_FLOAT,
                false,
                VERTEX_STRIDE,
                buffer
            )

            // Draw the quad (2 triangles = 6 vertices)
            GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, VERTEX_COUNT)

            // Disable vertex attribute array
            GLES20.glDisableVertexAttribArray(positionHandle)
        }
    }

    /**
     * Create vertex buffer for fullscreen quad.
     */
    private fun createVertexBuffer() {
        // Allocate buffer
        val byteBuffer = ByteBuffer.allocateDirect(FULLSCREEN_QUAD_VERTICES.size * 4)
        byteBuffer.order(ByteOrder.nativeOrder())

        vertexBuffer = byteBuffer.asFloatBuffer()
        vertexBuffer?.put(FULLSCREEN_QUAD_VERTICES)
        vertexBuffer?.position(0)
    }

    /**
     * Create a placeholder 1x1 white texture.
     * Will be replaced with actual background image in future components.
     */
    private fun createPlaceholderTexture() {
        val textureIds = IntArray(1)
        GLES20.glGenTextures(1, textureIds, 0)
        backgroundTextureId = textureIds[0]

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureId)

        // Set texture parameters
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)

        // Create 1x1 white pixel
        val pixel = ByteBuffer.allocateDirect(4)
        pixel.put(0, 255.toByte()) // R
        pixel.put(1, 255.toByte()) // G
        pixel.put(2, 255.toByte()) // B
        pixel.put(3, 255.toByte()) // A

        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D,
            0,
            GLES20.GL_RGBA,
            1,
            1,
            0,
            GLES20.GL_RGBA,
            GLES20.GL_UNSIGNED_BYTE,
            pixel
        )
    }

    /**
     * Check for OpenGL errors and throw exception if found.
     */
    private fun checkGLError(operation: String) {
        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            throw RuntimeException("$operation: glError $error")
        }
    }

    /**
     * Get current frames per second.
     */
    fun getFPS(): Float {
        val elapsedTime = (System.currentTimeMillis() - startTime) / 1000.0f
        return if (elapsedTime > 0) frameCount / elapsedTime else 0f
    }

    /**
     * Get elapsed time in seconds since surface creation.
     */
    fun getElapsedTime(): Float {
        return (System.currentTimeMillis() - startTime) / 1000.0f
    }

    /**
     * Set background configuration.
     *
     * Can be called from any thread. The texture will be loaded on the GL thread
     * when the surface is ready.
     *
     * @param uri Content URI of the background image
     * @param cropRect Optional crop rectangle
     */
    fun setBackgroundConfig(uri: Uri?, cropRect: CropRect? = null) {
        Log.d(TAG, "setBackgroundConfig: uri=$uri")
        backgroundUri = uri
        backgroundCropRect = cropRect
        backgroundTextureLoaded = false
    }

    /**
     * Load background texture from URI with optional crop.
     *
     * This must be called on the GL thread (e.g., from onSurfaceCreated or after it).
     *
     * @param uri Content URI of the background image
     * @param cropRect Optional crop rectangle
     */
    private fun loadBackgroundTexture(uri: Uri, cropRect: CropRect? = null) {
        Log.d(TAG, "Loading background texture from URI: $uri")

        try {
            // Release old placeholder texture
            if (backgroundTextureId != 0) {
                val oldIds = intArrayOf(backgroundTextureId)
                GLES20.glDeleteTextures(1, oldIds, 0)
                backgroundTextureId = 0
            }

            // Load new texture
            backgroundTextureId = textureManager.loadTexture(
                uri,
                screenWidth,
                screenHeight,
                cropRect
            )

            if (backgroundTextureId > 0) {
                Log.d(TAG, "Background texture loaded successfully: ID=$backgroundTextureId")
            } else {
                Log.e(TAG, "Failed to load background texture, creating placeholder")
                createPlaceholderTexture()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error loading background texture", e)
            createPlaceholderTexture()
        }
    }

    /**
     * Release OpenGL resources.
     */
    fun release() {
        // Release compositor program
        if (compositorProgram != 0) {
            GLES20.glDeleteProgram(compositorProgram)
            compositorProgram = 0
        }

        // Release vertex shader
        if (vertexShaderId != 0) {
            GLES20.glDeleteShader(vertexShaderId)
            vertexShaderId = 0
        }

        // Release layer manager (destroys all layer programs)
        if (::layerManager.isInitialized) {
            layerManager.release()
        }

        // Release FBO manager (destroys all FBOs)
        if (::fboManager.isInitialized) {
            fboManager.release()
        }

        // Release background texture
        if (backgroundTextureId != 0) {
            GLES20.glDeleteTextures(1, intArrayOf(backgroundTextureId), 0)
            backgroundTextureId = 0
        }

        vertexBuffer = null
    }

    /**
     * Remove old setShaderParameters method - no longer needed.
     * Parameters are now set per-layer in setLayerUniforms().
     */
}
