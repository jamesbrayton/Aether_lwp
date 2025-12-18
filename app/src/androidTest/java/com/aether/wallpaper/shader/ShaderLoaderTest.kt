package com.aether.wallpaper.shader

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Instrumentation tests for ShaderLoader.
 *
 * These tests require an OpenGL ES 2.0 context and must run on a device or emulator.
 */
@RunWith(AndroidJUnit4::class)
class ShaderLoaderTest {

    private lateinit var context: Context
    private lateinit var shaderLoader: ShaderLoader
    private lateinit var glSurfaceView: GLSurfaceView
    private val latch = CountDownLatch(1)
    private var testException: Exception? = null
    private var testResult: Any? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        shaderLoader = ShaderLoader(context)

        // Create a GLSurfaceView to get an OpenGL context
        glSurfaceView = GLSurfaceView(context)
        glSurfaceView.setEGLContextClientVersion(2)
    }

    @After
    fun tearDown() {
        // Clean up any OpenGL resources created during tests
        runOnGLThread {
            // Any cleanup if needed
        }
    }

    /**
     * Execute code on the GL thread with a valid OpenGL context.
     */
    private fun runOnGLThread(block: () -> Unit) {
        val localLatch = CountDownLatch(1)
        var localException: Exception? = null

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                try {
                    block()
                } catch (e: Exception) {
                    localException = e
                } finally {
                    localLatch.countDown()
                }
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {}
            override fun onDrawFrame(gl: GL10?) {}
        })

        // Wait for GL thread to execute
        assertTrue(
            "GL thread did not complete in time",
            localLatch.await(5, TimeUnit.SECONDS)
        )

        // Rethrow any exception that occurred on GL thread
        localException?.let { throw it }
    }

    @Test
    fun testLoadShaderFromAssets_vertexShader() {
        val source = shaderLoader.loadShaderFromAssets("vertex_shader.vert")

        assertNotNull("Shader source should not be null", source)
        assertTrue("Shader source should not be empty", source.isNotEmpty())
        assertTrue(
            "Vertex shader should contain 'a_position' attribute",
            source.contains("a_position")
        )
    }

    @Test
    fun testLoadShaderFromAssets_fragmentShader() {
        val source = shaderLoader.loadShaderFromAssets("test.frag")

        assertNotNull("Shader source should not be null", source)
        assertTrue("Shader source should not be empty", source.isNotEmpty())
        assertTrue(
            "Fragment shader should contain standard uniforms",
            source.contains("u_time") && source.contains("u_resolution")
        )
    }

    @Test
    fun testLoadShaderFromAssets_withMetadataComments() {
        val source = shaderLoader.loadShaderFromAssets("test.frag")

        // Verify that metadata comments are included in the source
        assertTrue(
            "Shader should include metadata comments",
            source.contains("@shader") || source.contains("/**")
        )
    }

    @Test(expected = IOException::class)
    fun testLoadShaderFromAssets_missingFile() {
        shaderLoader.loadShaderFromAssets("missing_file.frag")
    }

    @Test
    fun testCompileShader_validVertexShader() {
        val vertexSource = shaderLoader.loadShaderFromAssets("vertex_shader.vert")

        runOnGLThread {
            val shaderId = shaderLoader.compileShader(vertexSource, GLES20.GL_VERTEX_SHADER)

            assertTrue("Shader ID should be greater than 0", shaderId > 0)

            // Verify it's actually a shader object
            val isShader = GLES20.glIsShader(shaderId)
            assertTrue("OpenGL should recognize this as a shader", isShader)

            // Clean up
            GLES20.glDeleteShader(shaderId)
        }
    }

    @Test
    fun testCompileShader_validFragmentShader() {
        val fragmentSource = shaderLoader.loadShaderFromAssets("test.frag")

        runOnGLThread {
            val shaderId = shaderLoader.compileShader(fragmentSource, GLES20.GL_FRAGMENT_SHADER)

            assertTrue("Shader ID should be greater than 0", shaderId > 0)

            // Verify it's actually a shader object
            val isShader = GLES20.glIsShader(shaderId)
            assertTrue("OpenGL should recognize this as a shader", isShader)

            // Clean up
            GLES20.glDeleteShader(shaderId)
        }
    }

    @Test
    fun testCompileShader_withMetadataComments() {
        // Test that GLSL compiler ignores JavaDoc-style comments
        val fragmentSource = shaderLoader.loadShaderFromAssets("test.frag")

        runOnGLThread {
            // Should compile successfully despite metadata comments
            val shaderId = shaderLoader.compileShader(fragmentSource, GLES20.GL_FRAGMENT_SHADER)

            assertTrue("Shader with metadata should compile successfully", shaderId > 0)

            // Clean up
            GLES20.glDeleteShader(shaderId)
        }
    }

    @Test
    fun testCompileShader_invalidSyntax() {
        val invalidSource = """
            precision mediump float;
            void main() {
                // Missing semicolon
                gl_FragColor = vec4(1.0, 0.0, 0.0, 1.0)
            }
        """.trimIndent()

        runOnGLThread {
            try {
                shaderLoader.compileShader(invalidSource, GLES20.GL_FRAGMENT_SHADER)
                fail("Should have thrown ShaderCompilationException")
            } catch (e: ShaderCompilationException) {
                // Expected exception
                assertNotNull("Error log should not be null", e.errorLog)
                assertTrue("Error log should not be empty", e.errorLog.isNotEmpty())
                assertEquals(
                    "Shader type should be FRAGMENT",
                    ShaderCompilationException.ShaderType.FRAGMENT,
                    e.shaderType
                )
            }
        }
    }

    @Test
    fun testLinkProgram_validShaders() {
        val vertexSource = shaderLoader.loadShaderFromAssets("vertex_shader.vert")
        val fragmentSource = shaderLoader.loadShaderFromAssets("test.frag")

        runOnGLThread {
            val vertexShader = shaderLoader.compileShader(vertexSource, GLES20.GL_VERTEX_SHADER)
            val fragmentShader = shaderLoader.compileShader(fragmentSource, GLES20.GL_FRAGMENT_SHADER)

            val programId = shaderLoader.linkProgram(vertexShader, fragmentShader)

            assertTrue("Program ID should be greater than 0", programId > 0)

            // Verify it's actually a program object
            val isProgram = GLES20.glIsProgram(programId)
            assertTrue("OpenGL should recognize this as a program", isProgram)

            // Verify link status
            val linkStatus = IntArray(1)
            GLES20.glGetProgramiv(programId, GLES20.GL_LINK_STATUS, linkStatus, 0)
            assertEquals("Program should be successfully linked", GLES20.GL_TRUE, linkStatus[0])

            // Clean up
            GLES20.glDeleteShader(vertexShader)
            GLES20.glDeleteShader(fragmentShader)
            GLES20.glDeleteProgram(programId)
        }
    }

    @Test
    fun testCreateProgram_fromAssetFiles() {
        runOnGLThread {
            val programId = shaderLoader.createProgram("vertex_shader.vert", "test.frag")

            assertTrue("Program ID should be greater than 0", programId > 0)

            // Verify it's a valid program
            val isProgram = GLES20.glIsProgram(programId)
            assertTrue("Should create a valid program", isProgram)

            // Clean up
            GLES20.glDeleteProgram(programId)
        }
    }

    @Test
    fun testCreateProgram_uniformLocations() {
        runOnGLThread {
            val programId = shaderLoader.createProgram("vertex_shader.vert", "test.frag")

            // Query standard uniform locations
            val timeLocation = GLES20.glGetUniformLocation(programId, "u_time")
            val resolutionLocation = GLES20.glGetUniformLocation(programId, "u_resolution")
            val textureLocation = GLES20.glGetUniformLocation(programId, "u_backgroundTexture")

            assertTrue("u_time uniform should exist", timeLocation >= 0)
            assertTrue("u_resolution uniform should exist", resolutionLocation >= 0)
            assertTrue("u_backgroundTexture uniform should exist", textureLocation >= 0)

            // Clean up
            GLES20.glDeleteProgram(programId)
        }
    }

    @Test
    fun testCreateProgram_attributeLocations() {
        runOnGLThread {
            val programId = shaderLoader.createProgram("vertex_shader.vert", "test.frag")

            // Query attribute location
            val positionLocation = GLES20.glGetAttribLocation(programId, "a_position")

            assertTrue("a_position attribute should exist", positionLocation >= 0)

            // Clean up
            GLES20.glDeleteProgram(programId)
        }
    }

    @Test(expected = IOException::class)
    fun testCreateProgram_missingVertexShader() {
        shaderLoader.createProgram("missing.vert", "test.frag")
    }

    @Test(expected = IOException::class)
    fun testCreateProgram_missingFragmentShader() {
        shaderLoader.createProgram("vertex_shader.vert", "missing.frag")
    }

    @Test
    fun testShaderCompilationException_vertexShader() {
        val invalidVertexSource = "invalid glsl code;"

        runOnGLThread {
            try {
                shaderLoader.compileShader(invalidVertexSource, GLES20.GL_VERTEX_SHADER)
                fail("Should have thrown ShaderCompilationException")
            } catch (e: ShaderCompilationException) {
                assertEquals(
                    "Should identify as vertex shader error",
                    ShaderCompilationException.ShaderType.VERTEX,
                    e.shaderType
                )
                assertTrue(
                    "Exception message should mention vertex shader",
                    e.message?.contains("Vertex") == true
                )
            }
        }
    }

    @Test
    fun testShaderCompilationException_fragmentShader() {
        val invalidFragmentSource = "invalid glsl code;"

        runOnGLThread {
            try {
                shaderLoader.compileShader(invalidFragmentSource, GLES20.GL_FRAGMENT_SHADER)
                fail("Should have thrown ShaderCompilationException")
            } catch (e: ShaderCompilationException) {
                assertEquals(
                    "Should identify as fragment shader error",
                    ShaderCompilationException.ShaderType.FRAGMENT,
                    e.shaderType
                )
                assertTrue(
                    "Exception message should mention fragment shader",
                    e.message?.contains("Fragment") == true
                )
            }
        }
    }

    @Test
    fun testNoOpenGLErrors() {
        val vertexSource = shaderLoader.loadShaderFromAssets("vertex_shader.vert")
        val fragmentSource = shaderLoader.loadShaderFromAssets("test.frag")

        runOnGLThread {
            val programId = shaderLoader.createProgram("vertex_shader.vert", "test.frag")

            // Verify no OpenGL errors occurred
            val error = GLES20.glGetError()
            assertEquals("No OpenGL errors should occur", GLES20.GL_NO_ERROR, error)

            // Clean up
            GLES20.glDeleteProgram(programId)
        }
    }
}
