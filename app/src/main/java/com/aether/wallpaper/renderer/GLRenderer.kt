package com.aether.wallpaper.renderer

import android.content.Context
import android.net.Uri
import android.opengl.GLES20
import android.util.Log
import com.aether.wallpaper.model.CropRect
import com.aether.wallpaper.shader.ShaderLoader
import com.aether.wallpaper.texture.TextureManager
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * OpenGL ES 2.0 renderer for fullscreen particle effects.
 *
 * Renders a fullscreen quad with GLSL fragment shaders.
 * Manages standard uniforms and frame timing for 60fps rendering.
 */
class GLRenderer(
    private val context: Context,
    private val vertexShaderFile: String = "vertex_shader.vert",
    private val fragmentShaderFile: String = "test.frag"
) : android.opengl.GLSurfaceView.Renderer {

    // Shader program and uniform locations
    private var shaderProgram: Int = 0
    private val uniformLocations = mutableMapOf<String, Int>()

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

        // Initialize shader loader
        shaderLoader = ShaderLoader(context)

        // Initialize texture manager
        textureManager = TextureManager(context)

        // Load and compile shaders
        Log.d(TAG, "Loading shaders: vertex=$vertexShaderFile, fragment=$fragmentShaderFile")
        try {
            shaderProgram = shaderLoader.createProgram(vertexShaderFile, fragmentShaderFile)
        } catch (e: Exception) {
            throw RuntimeException("Failed to create shader program", e)
        }

        // Get attribute and uniform locations
        positionHandle = GLES20.glGetAttribLocation(shaderProgram, "a_position")

        // Get standard uniform locations
        uniformLocations["u_time"] = GLES20.glGetUniformLocation(shaderProgram, "u_time")
        uniformLocations["u_resolution"] = GLES20.glGetUniformLocation(shaderProgram, "u_resolution")
        uniformLocations["u_backgroundTexture"] = GLES20.glGetUniformLocation(shaderProgram, "u_backgroundTexture")
        uniformLocations["u_gyroOffset"] = GLES20.glGetUniformLocation(shaderProgram, "u_gyroOffset")
        uniformLocations["u_depthValue"] = GLES20.glGetUniformLocation(shaderProgram, "u_depthValue")

        // Create fullscreen quad vertex buffer
        createVertexBuffer()

        // Create placeholder background texture
        createPlaceholderTexture()

        // Initialize timing
        startTime = System.currentTimeMillis()
        lastFrameTime = startTime
        frameCount = 0

        checkGLError("onSurfaceCreated")
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d(TAG, "onSurfaceChanged: ${width}x${height}")

        // Set viewport to match screen size
        GLES20.glViewport(0, 0, width, height)

        // Store screen dimensions
        screenWidth = width
        screenHeight = height

        // Load background texture if configured and not already loaded
        if (!backgroundTextureLoaded && backgroundUri != null) {
            loadBackgroundTexture(backgroundUri!!, backgroundCropRect)
            backgroundTextureLoaded = true
        }

        checkGLError("onSurfaceChanged")
    }

    override fun onDrawFrame(gl: GL10?) {
        // Clear screen
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // Use shader program
        GLES20.glUseProgram(shaderProgram)

        // Calculate elapsed time
        val currentTime = System.currentTimeMillis()
        val elapsedTime = (currentTime - startTime) / 1000.0f

        // Set standard uniforms
        setStandardUniforms(elapsedTime)

        // Render fullscreen quad
        renderFullscreenQuad()

        // Update frame timing
        lastFrameTime = currentTime
        frameCount++

        checkGLError("onDrawFrame")
    }

    /**
     * Set all standard uniforms required by shaders.
     */
    private fun setStandardUniforms(time: Float) {
        // u_time - elapsed time in seconds
        uniformLocations["u_time"]?.let { location ->
            if (location >= 0) {
                GLES20.glUniform1f(location, time)
            }
        }

        // u_resolution - screen dimensions
        uniformLocations["u_resolution"]?.let { location ->
            if (location >= 0) {
                GLES20.glUniform2f(location, screenWidth.toFloat(), screenHeight.toFloat())
            }
        }

        // u_backgroundTexture - background image (placeholder for now)
        uniformLocations["u_backgroundTexture"]?.let { location ->
            if (location >= 0) {
                GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, backgroundTextureId)
                GLES20.glUniform1i(location, 0) // Texture unit 0
            }
        }

        // u_gyroOffset - gyroscope offset (zero for Phase 1)
        uniformLocations["u_gyroOffset"]?.let { location ->
            if (location >= 0) {
                GLES20.glUniform2f(location, 0.0f, 0.0f)
            }
        }

        // u_depthValue - layer depth (zero for Phase 1)
        uniformLocations["u_depthValue"]?.let { location ->
            if (location >= 0) {
                GLES20.glUniform1f(location, 0.0f)
            }
        }
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
        if (shaderProgram != 0) {
            GLES20.glDeleteProgram(shaderProgram)
            shaderProgram = 0
        }

        if (backgroundTextureId != 0) {
            val textureIds = intArrayOf(backgroundTextureId)
            GLES20.glDeleteTextures(1, textureIds, 0)
            backgroundTextureId = 0
        }

        vertexBuffer = null
    }
}
