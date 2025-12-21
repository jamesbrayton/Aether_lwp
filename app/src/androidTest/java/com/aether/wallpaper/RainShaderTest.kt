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
 * Integration tests for Rain shader effect.
 *
 * Tests shader discovery, metadata parsing, compilation, uniform locations,
 * and rendering behavior for the rain effect.
 *
 * Requires OpenGL ES 2.0 context - must run as instrumentation tests.
 */
@RunWith(AndroidJUnit4::class)
class RainShaderTest {

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
    fun testRainShaderDiscoveredByRegistry() {
        val descriptors = shaderRegistry.discoverShaders()

        val rainShader = descriptors.find { it.id == "rain" }
        assertNotNull("Rain shader should be discovered", rainShader)
        assertEquals("Falling Rain", rainShader!!.name)
        assertEquals("1.0.0", rainShader.version)
    }

    @Test
    fun testRainShaderCanBeRetrievedById() {
        shaderRegistry.discoverShaders()

        val rainShader = shaderRegistry.getShaderById("rain")
        assertNotNull("Rain shader should be retrievable by ID", rainShader)
        assertEquals("rain", rainShader!!.id)
    }

    // ========== Metadata Parsing ==========

    @Test
    fun testRainShaderMetadataContainsRequiredTags() {
        val shaderSource = context.assets.open("shaders/rain.frag").bufferedReader().use { it.readText() }
        val descriptor = metadataParser.parse(shaderSource, "shaders/rain.frag")

        assertEquals("Falling Rain", descriptor.name)
        assertEquals("rain", descriptor.id)
        assertEquals("1.0.0", descriptor.version)
        assertEquals("Aether Team", descriptor.author)
        assertEquals("MIT", descriptor.license)
        assertTrue("Description should mention rain", descriptor.description.contains("rain", ignoreCase = true))
        assertTrue("Tags should include weather", descriptor.tags.contains("weather"))
        assertTrue("Tags should include rain", descriptor.tags.contains("rain"))
        assertTrue("Tags should include storm", descriptor.tags.contains("storm"))
    }

    @Test
    fun testRainShaderDeclaresStandardUniforms() {
        val shaderSource = context.assets.open("shaders/rain.frag").bufferedReader().use { it.readText() }

        // Verify standard uniforms are declared
        assertTrue("Should declare u_backgroundTexture", shaderSource.contains("uniform sampler2D u_backgroundTexture"))
        assertTrue("Should declare u_time", shaderSource.contains("uniform float u_time"))
        assertTrue("Should declare u_resolution", shaderSource.contains("uniform vec2 u_resolution"))
        assertTrue("Should declare u_gyroOffset", shaderSource.contains("uniform vec2 u_gyroOffset"))
        assertTrue("Should declare u_depthValue", shaderSource.contains("uniform float u_depthValue"))
    }

    @Test
    fun testRainShaderDefinesCustomParameters() {
        val shaderSource = context.assets.open("shaders/rain.frag").bufferedReader().use { it.readText() }
        val descriptor = metadataParser.parse(shaderSource, "shaders/rain.frag")

        assertEquals("Should have 4 custom parameters", 4, descriptor.parameters.size)

        // Verify u_particleCount
        val particleCount = descriptor.parameters.find { it.id == "u_particleCount" }
        assertNotNull("u_particleCount should be defined", particleCount)
        assertEquals(ParameterType.FLOAT, particleCount!!.type)
        assertEquals(100.0f, particleCount.defaultValue)
        assertEquals(50.0f, particleCount.minValue)
        assertEquals(150.0f, particleCount.maxValue)
        assertEquals("Raindrop Count", particleCount.name)
        assertTrue("Description should mention rain", particleCount.description.contains("rain", ignoreCase = true))

        // Verify u_speed
        val speed = descriptor.parameters.find { it.id == "u_speed" }
        assertNotNull("u_speed should be defined", speed)
        assertEquals(ParameterType.FLOAT, speed!!.type)
        assertEquals(2.0f, speed.defaultValue)
        assertEquals(1.0f, speed.minValue)
        assertEquals(3.0f, speed.maxValue)
        assertEquals("Fall Speed", speed.name)

        // Verify u_angle
        val angle = descriptor.parameters.find { it.id == "u_angle" }
        assertNotNull("u_angle should be defined", angle)
        assertEquals(ParameterType.FLOAT, angle!!.type)
        assertEquals(70.0f, angle.defaultValue)
        assertEquals(60.0f, angle.minValue)
        assertEquals(80.0f, angle.maxValue)
        assertEquals("Rain Angle", angle.name)

        // Verify u_streakLength
        val streakLength = descriptor.parameters.find { it.id == "u_streakLength" }
        assertNotNull("u_streakLength should be defined", streakLength)
        assertEquals(ParameterType.FLOAT, streakLength!!.type)
        assertEquals(0.03f, streakLength.defaultValue)
        assertEquals(0.01f, streakLength.minValue)
        assertEquals(0.05f, streakLength.maxValue)
        assertEquals("Streak Length", streakLength.name)
    }

    @Test
    fun testRainShaderDefaultSpeedFasterThanSnow() {
        val rainSource = context.assets.open("shaders/rain.frag").bufferedReader().use { it.readText() }
        val rainDescriptor = metadataParser.parse(rainSource, "shaders/rain.frag")

        val snowSource = context.assets.open("shaders/snow.frag").bufferedReader().use { it.readText() }
        val snowDescriptor = metadataParser.parse(snowSource, "shaders/snow.frag")

        val rainSpeed = rainDescriptor.parameters.find { it.id == "u_speed" }!!.defaultValue as Float
        val snowSpeed = snowDescriptor.parameters.find { it.id == "u_speed" }!!.defaultValue as Float

        assertTrue("Rain default speed should be faster than snow", rainSpeed > snowSpeed)
    }

    // ========== Shader Compilation ==========

    @Test
    fun testRainShaderCompilesWithoutErrors() {
        val latch = CountDownLatch(1)
        var compilationSuccess = false
        var programId = 0

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                try {
                    programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
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
        assertTrue("Rain shader should compile successfully", compilationSuccess)
        assertTrue("Program ID should be > 0", programId > 0)
    }

    @Test
    fun testRainShaderMetadataCommentsIgnoredByCompiler() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                try {
                    // Metadata comments should not cause compilation errors
                    val programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
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
    fun testRainShaderStandardUniformsAccessible() {
        val latch = CountDownLatch(1)
        val uniformLocations = mutableMapOf<String, Int>()

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                try {
                    val programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
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
    fun testRainShaderCustomParameterUniformsAccessible() {
        val latch = CountDownLatch(1)
        val uniformLocations = mutableMapOf<String, Int>()

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                try {
                    val programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
                    GLES20.glUseProgram(programId)

                    uniformLocations["u_particleCount"] = GLES20.glGetUniformLocation(programId, "u_particleCount")
                    uniformLocations["u_speed"] = GLES20.glGetUniformLocation(programId, "u_speed")
                    uniformLocations["u_angle"] = GLES20.glGetUniformLocation(programId, "u_angle")
                    uniformLocations["u_streakLength"] = GLES20.glGetUniformLocation(programId, "u_streakLength")
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
        assertTrue("u_angle should be accessible", uniformLocations["u_angle"]!! >= 0)
        assertTrue("u_streakLength should be accessible", uniformLocations["u_streakLength"]!! >= 0)
    }

    // ========== Rendering Behavior ==========

    @Test
    fun testRainShaderRendersWithoutErrors() {
        val latch = CountDownLatch(1)
        var renderSuccess = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
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
                    val angleLocation = GLES20.glGetUniformLocation(programId, "u_angle")
                    val streakLengthLocation = GLES20.glGetUniformLocation(programId, "u_streakLength")

                    GLES20.glUniform1f(timeLocation, 1.0f)
                    GLES20.glUniform2f(resolutionLocation, 1080f, 1920f)
                    GLES20.glUniform1f(particleCountLocation, 100f)
                    GLES20.glUniform1f(speedLocation, 2.0f)
                    GLES20.glUniform1f(angleLocation, 70.0f)
                    GLES20.glUniform1f(streakLengthLocation, 0.03f)

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
        assertTrue("Rain shader should render without OpenGL errors", renderSuccess)
    }

    @Test
    fun testRainShaderHandlesZeroParticles() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
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
        assertTrue("Rain shader should handle zero particles", noErrors)
    }

    @Test
    fun testRainShaderHandlesMinimumSpeed() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    // Set minimum speed
                    val speedLocation = GLES20.glGetUniformLocation(programId, "u_speed")
                    GLES20.glUniform1f(speedLocation, 1.0f)

                    val error = GLES20.glGetError()
                    noErrors = (error == GLES20.GL_NO_ERROR)
                } finally {
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Rain shader should handle minimum speed", noErrors)
    }

    @Test
    fun testRainShaderHandlesMaximumSpeed() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
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
        assertTrue("Rain shader should handle maximum speed", noErrors)
    }

    @Test
    fun testRainShaderHandlesMaximumParticleCount() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    // Set maximum particle count
                    val particleCountLocation = GLES20.glGetUniformLocation(programId, "u_particleCount")
                    GLES20.glUniform1f(particleCountLocation, 150f)

                    val error = GLES20.glGetError()
                    noErrors = (error == GLES20.GL_NO_ERROR)
                } finally {
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Rain shader should handle maximum particle count", noErrors)
    }

    @Test
    fun testRainShaderHandlesMinimumAngle() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    // Set minimum angle (more horizontal)
                    val angleLocation = GLES20.glGetUniformLocation(programId, "u_angle")
                    GLES20.glUniform1f(angleLocation, 60.0f)

                    val error = GLES20.glGetError()
                    noErrors = (error == GLES20.GL_NO_ERROR)
                } finally {
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Rain shader should handle minimum angle", noErrors)
    }

    @Test
    fun testRainShaderHandlesMaximumAngle() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    // Set maximum angle (more vertical)
                    val angleLocation = GLES20.glGetUniformLocation(programId, "u_angle")
                    GLES20.glUniform1f(angleLocation, 80.0f)

                    val error = GLES20.glGetError()
                    noErrors = (error == GLES20.GL_NO_ERROR)
                } finally {
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Rain shader should handle maximum angle", noErrors)
    }

    @Test
    fun testRainShaderHandlesMinimumStreakLength() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    // Set minimum streak length
                    val streakLengthLocation = GLES20.glGetUniformLocation(programId, "u_streakLength")
                    GLES20.glUniform1f(streakLengthLocation, 0.01f)

                    val error = GLES20.glGetError()
                    noErrors = (error == GLES20.GL_NO_ERROR)
                } finally {
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Rain shader should handle minimum streak length", noErrors)
    }

    @Test
    fun testRainShaderHandlesMaximumStreakLength() {
        val latch = CountDownLatch(1)
        var noErrors = false

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
                GLES20.glUseProgram(programId)
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
                GLES20.glViewport(0, 0, width, height)
            }

            override fun onDrawFrame(gl: GL10?) {
                try {
                    // Set maximum streak length
                    val streakLengthLocation = GLES20.glGetUniformLocation(programId, "u_streakLength")
                    GLES20.glUniform1f(streakLengthLocation, 0.05f)

                    val error = GLES20.glGetError()
                    noErrors = (error == GLES20.GL_NO_ERROR)
                } finally {
                    latch.countDown()
                }
            }
        })

        assertTrue("Test timed out", latch.await(10, TimeUnit.SECONDS))
        assertTrue("Rain shader should handle maximum streak length", noErrors)
    }

    @Test
    fun testRainShaderMultipleFramesConsistent() {
        val latch = CountDownLatch(1)
        var framesRendered = 0
        var allFramesSuccessful = true

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            private var programId = 0

            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                programId = shaderLoader.createProgram("vertex_shader.vert", "rain.frag")
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
