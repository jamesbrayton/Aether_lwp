package com.aether.wallpaper.renderer

import android.content.Context
import android.opengl.GLES20
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aether.wallpaper.GLTestContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation tests for GLRenderer.
 *
 * These tests require an OpenGL ES 2.0 context and must run on a device or emulator.
 * Uses direct EGL context creation (via GLTestContext) for reliable headless testing.
 */
@RunWith(AndroidJUnit4::class)
class GLRendererTest {

    private lateinit var context: Context
    private lateinit var glContext: GLTestContext
    private var testRenderer: GLRenderer? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        glContext = GLTestContext()
    }

    @After
    fun tearDown() {
        testRenderer?.release()
        testRenderer = null
        glContext.destroy()
    }

    /**
     * Execute code on the GL thread with a valid OpenGL context.
     * This is now synchronous and runs on the test thread with EGL context made current.
     */
    private fun runOnGLThread(block: () -> Unit) {
        glContext.runOnGLThread(block)
    }

    @Test
    fun testRendererInitialization() {
        runOnGLThread {
            testRenderer = GLRenderer(context)

            // Simulate surface creation
            testRenderer!!.onSurfaceCreated(null, null)

            // Verify no OpenGL errors
            val error = GLES20.glGetError()
            assertEquals("No OpenGL errors should occur during init", GLES20.GL_NO_ERROR, error)
        }
    }

    @Test
    fun testSurfaceChangedSetsViewport() {
        runOnGLThread {
            testRenderer = GLRenderer(context)
            testRenderer!!.onSurfaceCreated(null, null)

            // Change surface size
            testRenderer!!.onSurfaceChanged(null, 1080, 1920)

            // Verify viewport was set (we can't directly query it, but we can check for errors)
            val error = GLES20.glGetError()
            assertEquals("onSurfaceChanged should not produce errors", GLES20.GL_NO_ERROR, error)
        }
    }

    @Test
    fun testDrawFrameExecutesWithoutErrors() {
        runOnGLThread {
            testRenderer = GLRenderer(context)
            testRenderer!!.onSurfaceCreated(null, null)
            testRenderer!!.onSurfaceChanged(null, 1080, 1920)

            // Draw a frame
            testRenderer!!.onDrawFrame(null)

            // Verify no OpenGL errors
            val error = GLES20.glGetError()
            assertEquals("onDrawFrame should not produce errors", GLES20.GL_NO_ERROR, error)
        }
    }

    @Test
    fun testMultipleFramesRender() {
        runOnGLThread {
            testRenderer = GLRenderer(context)
            testRenderer!!.onSurfaceCreated(null, null)
            testRenderer!!.onSurfaceChanged(null, 1080, 1920)

            // Render 10 frames
            repeat(10) {
                testRenderer!!.onDrawFrame(null)

                val error = GLES20.glGetError()
                assertEquals("Frame $it should render without errors", GLES20.GL_NO_ERROR, error)
            }
        }
    }

    @Test
    fun testElapsedTimeProgresses() {
        runOnGLThread {
            testRenderer = GLRenderer(context)
            testRenderer!!.onSurfaceCreated(null, null)
            testRenderer!!.onSurfaceChanged(null, 1080, 1920)

            val initialTime = testRenderer!!.getElapsedTime()

            // Sleep for 100ms
            Thread.sleep(100)

            // Draw a frame
            testRenderer!!.onDrawFrame(null)

            val afterTime = testRenderer!!.getElapsedTime()

            // Elapsed time should have increased
            assertTrue("Elapsed time should increase", afterTime > initialTime)
            assertTrue("Elapsed time should be >= 0.1s", afterTime >= 0.1f)
        }
    }

    @Test
    fun testTimeNeverDecreases() {
        runOnGLThread {
            testRenderer = GLRenderer(context)
            testRenderer!!.onSurfaceCreated(null, null)
            testRenderer!!.onSurfaceChanged(null, 1080, 1920)

            var previousTime = 0.0f

            // Render 20 frames and check time always increases
            repeat(20) {
                testRenderer!!.onDrawFrame(null)

                val currentTime = testRenderer!!.getElapsedTime()
                assertTrue(
                    "Time should never decrease (frame $it)",
                    currentTime >= previousTime
                )
                previousTime = currentTime

                // Small delay between frames
                Thread.sleep(10)
            }
        }
    }

    @Test
    fun testFPSCalculation() {
        runOnGLThread {
            testRenderer = GLRenderer(context)
            testRenderer!!.onSurfaceCreated(null, null)
            testRenderer!!.onSurfaceChanged(null, 1080, 1920)

            // Render frames with known timing
            val frameCount = 30
            val delayMs = 16L // ~60fps

            repeat(frameCount) {
                testRenderer!!.onDrawFrame(null)
                Thread.sleep(delayMs)
            }

            val fps = testRenderer!!.getFPS()

            // FPS should be reasonable (accounting for overhead)
            assertTrue("FPS should be > 0", fps > 0f)
            assertTrue("FPS should be < 120", fps < 120f) // Sanity check
        }
    }

    @Test
    fun testShaderProgramActive() {
        runOnGLThread {
            testRenderer = GLRenderer(context)
            testRenderer!!.onSurfaceCreated(null, null)
            testRenderer!!.onSurfaceChanged(null, 1080, 1920)
            testRenderer!!.onDrawFrame(null)

            // Get current program
            val programId = IntArray(1)
            GLES20.glGetIntegerv(GLES20.GL_CURRENT_PROGRAM, programId, 0)

            // Should have a program active
            assertTrue("Shader program should be active", programId[0] > 0)
        }
    }

    @Test
    fun testVertexAttributesEnabled() {
        runOnGLThread {
            testRenderer = GLRenderer(context)
            testRenderer!!.onSurfaceCreated(null, null)
            testRenderer!!.onSurfaceChanged(null, 1080, 1920)

            // This test just ensures rendering doesn't crash
            // Actual vertex data validation would require shader inspection
            testRenderer!!.onDrawFrame(null)

            val error = GLES20.glGetError()
            assertEquals("Vertex rendering should not produce errors", GLES20.GL_NO_ERROR, error)
        }
    }

    @Test
    fun testResourceCleanup() {
        runOnGLThread {
            testRenderer = GLRenderer(context)
            testRenderer!!.onSurfaceCreated(null, null)
            testRenderer!!.onSurfaceChanged(null, 1080, 1920)
            testRenderer!!.onDrawFrame(null)

            // Release resources
            testRenderer!!.release()

            // Verify no errors during cleanup
            val error = GLES20.glGetError()
            assertEquals("Resource cleanup should not produce errors", GLES20.GL_NO_ERROR, error)
        }
    }

    @Test
    fun testMultipleSurfaceChanges() {
        runOnGLThread {
            testRenderer = GLRenderer(context)
            testRenderer!!.onSurfaceCreated(null, null)

            // Change surface size multiple times (simulating rotation)
            testRenderer!!.onSurfaceChanged(null, 1080, 1920) // Portrait
            testRenderer!!.onDrawFrame(null)

            testRenderer!!.onSurfaceChanged(null, 1920, 1080) // Landscape
            testRenderer!!.onDrawFrame(null)

            testRenderer!!.onSurfaceChanged(null, 1440, 2560) // Larger screen
            testRenderer!!.onDrawFrame(null)

            // All size changes should work without errors
            val error = GLES20.glGetError()
            assertEquals("Multiple surface changes should not produce errors", GLES20.GL_NO_ERROR, error)
        }
    }

    @Test
    fun testConsistentRendering() {
        runOnGLThread {
            testRenderer = GLRenderer(context)
            testRenderer!!.onSurfaceCreated(null, null)
            testRenderer!!.onSurfaceChanged(null, 1080, 1920)

            // Render 100 frames continuously
            repeat(100) { frameNum ->
                testRenderer!!.onDrawFrame(null)

                val error = GLES20.glGetError()
                assertEquals(
                    "Frame $frameNum should render without errors",
                    GLES20.GL_NO_ERROR,
                    error
                )
            }
        }
    }

    @Test
    fun testCustomShaderFiles() {
        runOnGLThread {
            // Test with explicit shader file names
            testRenderer = GLRenderer(
                context,
                vertexShaderFile = "vertex_shader.vert",
                fragmentShaderFile = "test.frag"
            )

            testRenderer!!.onSurfaceCreated(null, null)
            testRenderer!!.onSurfaceChanged(null, 1080, 1920)
            testRenderer!!.onDrawFrame(null)

            val error = GLES20.glGetError()
            assertEquals("Custom shader files should load correctly", GLES20.GL_NO_ERROR, error)
        }
    }

    @Test
    fun testFrameCountIncreases() {
        runOnGLThread {
            testRenderer = GLRenderer(context)
            testRenderer!!.onSurfaceCreated(null, null)
            testRenderer!!.onSurfaceChanged(null, 1080, 1920)

            // Get initial FPS (should be 0 or very low)
            val initialFps = testRenderer!!.getFPS()

            // Render several frames
            repeat(10) {
                testRenderer!!.onDrawFrame(null)
                Thread.sleep(10)
            }

            // FPS should have increased
            val afterFps = testRenderer!!.getFPS()
            assertTrue("FPS should increase after rendering frames", afterFps > initialFps)
        }
    }

    @Test
    fun testBackgroundTextureCreated() {
        runOnGLThread {
            testRenderer = GLRenderer(context)
            testRenderer!!.onSurfaceCreated(null, null)

            // Placeholder texture should be created
            // We can't directly verify the texture, but onDrawFrame should work
            testRenderer!!.onSurfaceChanged(null, 1080, 1920)
            testRenderer!!.onDrawFrame(null)

            val error = GLES20.glGetError()
            assertEquals("Background texture should be usable", GLES20.GL_NO_ERROR, error)
        }
    }
}
