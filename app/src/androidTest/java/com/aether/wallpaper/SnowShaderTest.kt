package com.aether.wallpaper

import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aether.wallpaper.shader.ParameterType
import com.aether.wallpaper.shader.ShaderLoader
import com.aether.wallpaper.shader.ShaderMetadataParser
import com.aether.wallpaper.shader.ShaderRegistry
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Integration tests for Snow shader effect.
 *
 * Tests shader discovery, metadata parsing, compilation, uniform locations,
 * and rendering behavior.
 *
 * Requires OpenGL ES 2.0 context - must run as instrumentation tests.
 */
@RunWith(AndroidJUnit4::class)
class SnowShaderTest {

    private lateinit var context: Context
    private lateinit var glSurfaceView: GLSurfaceView
    private lateinit var shaderRegistry: ShaderRegistry
    private lateinit var shaderLoader: ShaderLoader
    private lateinit var metadataParser: ShaderMetadataParser

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        shaderRegistry = ShaderRegistry(context)
        shaderLoader = ShaderLoader(context)
        metadataParser = ShaderMetadataParser()

        glSurfaceView = GLSurfaceView(context)
        glSurfaceView.setEGLContextClientVersion(2)
    }

    @After
    fun teardown() {
        // GLSurfaceView cleanup handled by test framework
    }

    // ========== Shader Discovery ==========

    @Test
    fun testSnowShaderDiscoveredByRegistry() {
        val descriptors = shaderRegistry.discoverShaders()

        val snowShader = descriptors.find { it.id == "snow" }
        assertNotNull("Snow shader should be discovered", snowShader)
        assertEquals("Falling Snow", snowShader!!.name)
        assertEquals("1.0.0", snowShader.version)
    }

    @Test
    fun testSnowShaderCanBeRetrievedById() {
        shaderRegistry.discoverShaders()

        val snowShader = shaderRegistry.getShaderById("snow")
        assertNotNull("Snow shader should be retrievable by ID", snowShader)
        assertEquals("snow", snowShader!!.id)
    }

    // ========== Metadata Parsing ==========

    @Test
    fun testSnowShaderMetadataContainsRequiredTags() {
        val shaderSource = context.assets.open("shaders/snow.frag").bufferedReader().use { it.readText() }
        val descriptor = metadataParser.parse(shaderSource, "shaders/snow.frag")

        assertEquals("Falling Snow", descriptor.name)
        assertEquals("snow", descriptor.id)
        assertEquals("1.0.0", descriptor.version)
        assertEquals("Aether Team", descriptor.author)
        assertEquals("MIT", descriptor.license)
        assertTrue("Description should mention snow", descriptor.description.contains("snow", ignoreCase = true))
        assertTrue("Tags should include winter", descriptor.tags.contains("winter"))
        assertTrue("Tags should include weather", descriptor.tags.contains("weather"))
    }

    @Test
    fun testSnowShaderDeclaresStandardUniforms() {
        val shaderSource = context.assets.open("shaders/snow.frag").bufferedReader().use { it.readText() }

        // Verify standard uniforms are declared
        assertTrue("Should declare u_backgroundTexture", shaderSource.contains("uniform sampler2D u_backgroundTexture"))
        assertTrue("Should declare u_time", shaderSource.contains("uniform float u_time"))
        assertTrue("Should declare u_resolution", shaderSource.contains("uniform vec2 u_resolution"))
        assertTrue("Should declare u_gyroOffset", shaderSource.contains("uniform vec2 u_gyroOffset"))
        assertTrue("Should declare u_depthValue", shaderSource.contains("uniform float u_depthValue"))
    }

    @Test
    fun testSnowShaderDefinesCustomParameters() {
        val shaderSource = context.assets.open("shaders/snow.frag").bufferedReader().use { it.readText() }
        val descriptor = metadataParser.parse(shaderSource, "shaders/snow.frag")

        assertEquals("Should have 3 custom parameters", 3, descriptor.parameters.size)

        // Verify u_particleCount
        val particleCount = descriptor.parameters.find { it.id == "u_particleCount" }
        assertNotNull("u_particleCount should be defined", particleCount)
        assertEquals(ParameterType.FLOAT, particleCount!!.type)
        assertEquals(100.0f, particleCount.defaultValue)
        assertEquals(10.0f, particleCount.minValue)
        assertEquals(200.0f, particleCount.maxValue)
        assertEquals("Particle Count", particleCount.name)
        assertTrue("Description should mention particles", particleCount.description.contains("particles", ignoreCase = true))

        // Verify u_speed
        val speed = descriptor.parameters.find { it.id == "u_speed" }
        assertNotNull("u_speed should be defined", speed)
        assertEquals(ParameterType.FLOAT, speed!!.type)
        assertEquals(1.0f, speed.defaultValue)
        assertEquals(0.1f, speed.minValue)
        assertEquals(3.0f, speed.maxValue)
        assertEquals("Fall Speed", speed.name)

        // Verify u_driftAmount
        val drift = descriptor.parameters.find { it.id == "u_driftAmount" }
        assertNotNull("u_driftAmount should be defined", drift)
        assertEquals(ParameterType.FLOAT, drift!!.type)
        assertEquals(0.5f, drift.defaultValue)
        assertEquals(0.0f, drift.minValue)
        assertEquals(1.0f, drift.maxValue)
        assertEquals("Lateral Drift", drift.name)
    }

    // ========== Shader Compilation ==========

    @Test
    fun testSnowShaderCompilesWithoutErrors() {
        val latch = CountDownLatch(1)
        var compilationSuccess = false
        var programId = 0

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                try {
                    programId = shaderLoader.createProgram("vertex_shader.vert", "snow.frag")
                    compilationSuccess = programId > 0
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    latch.countDown()
                }
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {}
            override fun onDrawFrame(gl: GL10?) {}
        })

        assertTrue("Shader compilation timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Snow shader should compile successfully", compilationSuccess)
        assertTrue("Program ID should be > 0", programId > 0)
    }

    @Test
    fun testSnowShaderMetadataCommentsIgnoredByCompiler() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                try {
                    // Metadata comments should not cause compilation errors
                    val programId = shaderLoader.createProgram("vertex_shader.vert", "snow.frag")
                    GLES20.glUseProgram(programId)
                    val error = GLES20.glGetError()
                    noErrors = (error == GLES20.GL_NO_ERROR)
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    latch.countDown()
                }
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {}
            override fun onDrawFrame(gl: GL10?) {}
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Metadata comments should be ignored by GLSL compiler", noErrors)
    }

    // ========== Uniform Locations ==========

    @Test
    fun testSnowShaderStandardUniformsAccessible() {
        val latch = CountDownLatch(1)
        val uniformLocations = mutableMapOf<String, Int>()

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                try {
                    val programId = shaderLoader.createProgram("vertex_shader.vert", "snow.frag")
                    GLES20.glUseProgram(programId)

                    uniformLocations["u_backgroundTexture"] = GLES20.glGetUniformLocation(programId, "u_backgroundTexture")
                    uniformLocations["u_time"] = GLES20.glGetUniformLocation(programId, "u_time")
                    uniformLocations["u_resolution"] = GLES20.glGetUniformLocation(programId, "u_resolution")
                    uniformLocations["u_gyroOffset"] = GLES20.glGetUniformLocation(programId, "u_gyroOffset")
                    uniformLocations["u_depthValue"] = GLES20.glGetUniformLocation(programId, "u_depthValue")
                } finally {
                    latch.countDown()
                }
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {}
            override fun onDrawFrame(gl: GL10?) {}
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("u_backgroundTexture should be accessible", uniformLocations["u_backgroundTexture"]!! >= 0)
        assertTrue("u_time should be accessible", uniformLocations["u_time"]!! >= 0)
        assertTrue("u_resolution should be accessible", uniformLocations["u_resolution"]!! >= 0)
        assertTrue("u_gyroOffset should be accessible", uniformLocations["u_gyroOffset"]!! >= 0)
        assertTrue("u_depthValue should be accessible", uniformLocations["u_depthValue"]!! >= 0)
    }

    @Test
    fun testSnowShaderCustomParameterUniformsAccessible() {
        val latch = CountDownLatch(1)
        val uniformLocations = mutableMapOf<String, Int>()

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                try {
                    val programId = shaderLoader.createProgram("vertex_shader.vert", "snow.frag")
                    GLES20.glUseProgram(programId)

                    uniformLocations["u_particleCount"] = GLES20.glGetUniformLocation(programId, "u_particleCount")
                    uniformLocations["u_speed"] = GLES20.glGetUniformLocation(programId, "u_speed")
                    uniformLocations["u_driftAmount"] = GLES20.glGetUniformLocation(programId, "u_driftAmount")
                } finally {
                    latch.countDown()
                }
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {}
            override fun onDrawFrame(gl: GL10?) {}
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("u_particleCount should be accessible", uniformLocations["u_particleCount"]!! >= 0)
        assertTrue("u_speed should be accessible", uniformLocations["u_speed"]!! >= 0)
        assertTrue("u_driftAmount should be accessible", uniformLocations["u_driftAmount"]!! >= 0)
    }

    // ========== Rendering Behavior ==========

    @Test
    fun testSnowShaderRendersWithoutErrors() {
        val latch = CountDownLatch(1)
        var renderSuccess = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0
            private val vertices = floatArrayOf(
                -1f, -1f,
                1f, -1f,
                -1f, 1f,
                1f, -1f,
                1f, 1f,
                -1f, 1f
            )

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "snow.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    // Set uniforms
                    val timeLocation = GLES20.glGetUniformLocation(programId, "u_time")
                    val resolutionLocation = GLES20.glGetUniformLocation(programId, "u_resolution")
                    val particleCountLocation = GLES20.glGetUniformLocation(programId, "u_particleCount")
                    val speedLocation = GLES20.glGetUniformLocation(programId, "u_speed")
                    val driftLocation = GLES20.glGetUniformLocation(programId, "u_driftAmount")

                    GLES20.glUniform1f(timeLocation, 1.0f)
                    GLES20.glUniform2f(resolutionLocation, 1080f, 1920f)
                    GLES20.glUniform1f(particleCountLocation, 100f)
                    GLES20.glUniform1f(speedLocation, 1.0f)
                    GLES20.glUniform1f(driftLocation, 0.5f)

                    // Create placeholder background texture
                    val textureIds = IntArray(1)
                    GLES20.glGenTextures(1, textureIds, 0)
                    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureIds[0])

                    val textureLocation = GLES20.glGetUniformLocation(programId, "u_backgroundTexture")
                    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
                    GLES20.glUniform1i(textureLocation, 0)

                    val error = GLES20.glGetError()
                    renderSuccess = (error == GLES20.GL_NO_ERROR)
                } finally {
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Snow shader should render without OpenGL errors", renderSuccess)
    }

    @Test
    fun testSnowShaderHandlesZeroParticles() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "snow.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    // Set particle count to 0
                    val particleCountLocation = GLES20.glGetUniformLocation(programId, "u_particleCount")
                    GLES20.glUniform1f(particleCountLocation, 0f)

                    val error = GLES20.glGetError()
                    noErrors = (error == GLES20.GL_NO_ERROR)
                } finally {
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Snow shader should handle zero particles", noErrors)
    }

    @Test
    fun testSnowShaderHandlesMinimumSpeed() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "snow.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    // Set minimum speed
                    val speedLocation = GLES20.glGetUniformLocation(programId, "u_speed")
                    GLES20.glUniform1f(speedLocation, 0.1f)

                    val error = GLES20.glGetError()
                    noErrors = (error == GLES20.GL_NO_ERROR)
                } finally {
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Snow shader should handle minimum speed", noErrors)
    }

    @Test
    fun testSnowShaderHandlesMaximumSpeed() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "snow.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    // Set maximum speed
                    val speedLocation = GLES20.glGetUniformLocation(programId, "u_speed")
                    GLES20.glUniform1f(speedLocation, 3.0f)

                    val error = GLES20.glGetError()
                    noErrors = (error == GLES20.GL_NO_ERROR)
                } finally {
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Snow shader should handle maximum speed", noErrors)
    }

    @Test
    fun testSnowShaderHandlesMaximumParticleCount() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "snow.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    // Set maximum particle count
                    val particleCountLocation = GLES20.glGetUniformLocation(programId, "u_particleCount")
                    GLES20.glUniform1f(particleCountLocation, 200f)

                    val error = GLES20.glGetError()
                    noErrors = (error == GLES20.GL_NO_ERROR)
                } finally {
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Snow shader should handle maximum particle count", noErrors)
    }

    @Test
    fun testSnowShaderHandlesNoDrift() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "snow.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    // Set drift to 0 (no lateral movement)
                    val driftLocation = GLES20.glGetUniformLocation(programId, "u_driftAmount")
                    GLES20.glUniform1f(driftLocation, 0.0f)

                    val error = GLES20.glGetError()
                    noErrors = (error == GLES20.GL_NO_ERROR)
                } finally {
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Snow shader should handle no drift", noErrors)
    }

    @Test
    fun testSnowShaderHandlesMaximumDrift() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "snow.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    // Set maximum drift
                    val driftLocation = GLES20.glGetUniformLocation(programId, "u_driftAmount")
                    GLES20.glUniform1f(driftLocation, 1.0f)

                    val error = GLES20.glGetError()
                    noErrors = (error == GLES20.GL_NO_ERROR)
                } finally {
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Snow shader should handle maximum drift", noErrors)
    }

    @Test
    fun testSnowShaderMultipleFramesConsistent() {
        val latch = CountDownLatch(1)
        var framesRendered = 0
        var allFramesSuccessful = true

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "snow.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    if (framesRendered < 10) {
                        // Update time for animation
                        val timeLocation = GLES20.glGetUniformLocation(programId, "u_time")
                        GLES20.glUniform1f(timeLocation, framesRendered * 0.016f)

                        val error = GLES20.glGetError()
                        if (error != GLES20.GL_NO_ERROR) {
                            allFramesSuccessful = false
                        }

                        framesRendered++
                    } else {
                        latch.countDown()
                    }
                } catch (e: Exception) {
                    allFramesSuccessful = false
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertEquals("Should render 10 frames", 10, framesRendered)
        assertTrue("All frames should render successfully", allFramesSuccessful)
    }
}
