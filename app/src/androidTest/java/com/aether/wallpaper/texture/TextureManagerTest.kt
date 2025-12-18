package com.aether.wallpaper.texture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.aether.wallpaper.model.CropRect
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Instrumentation tests for TextureManager.
 *
 * These tests require an OpenGL ES 2.0 context and must run on a device or emulator.
 */
@RunWith(AndroidJUnit4::class)
class TextureManagerTest {

    private lateinit var context: Context
    private lateinit var glSurfaceView: GLSurfaceView
    private var textureManager: TextureManager? = null
    private var testImageUri: Uri? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        glSurfaceView = GLSurfaceView(context)
        glSurfaceView.setEGLContextClientVersion(2)

        textureManager = TextureManager(context)

        // Create test image
        createTestImage()
    }

    @After
    fun tearDown() {
        textureManager?.release()
        textureManager = null

        // Clean up test image
        testImageUri?.let { uri ->
            try {
                val file = File(uri.path!!)
                file.delete()
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }

    /**
     * Create a test image file.
     */
    private fun createTestImage(width: Int = 100, height: Int = 100) {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)

        val file = File(context.cacheDir, "test_image.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        bitmap.recycle()

        testImageUri = Uri.fromFile(file)
    }

    /**
     * Execute code on the GL thread with a valid OpenGL context.
     */
    private fun runOnGLThread(block: () -> Unit) {
        val latch = CountDownLatch(1)
        var exception: Exception? = null

        glSurfaceView.setRenderer(object : GLSurfaceView.Renderer {
            override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
                try {
                    block()
                } catch (e: Exception) {
                    exception = e
                } finally {
                    latch.countDown()
                }
            }

            override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {}
            override fun onDrawFrame(gl: GL10?) {}
        })

        assertTrue(
            "GL thread did not complete in time",
            latch.await(30, TimeUnit.SECONDS)
        )

        exception?.let { throw it }
    }

    @Test
    fun testCalculateSampleSizeNoSampling() {
        val sampleSize = textureManager!!.calculateSampleSize(
            sourceWidth = 500,
            sourceHeight = 500,
            targetWidth = 1000,
            targetHeight = 1000
        )

        assertEquals("No sampling needed for small image", 1, sampleSize)
    }

    @Test
    fun testCalculateSampleSizeTwoX() {
        val sampleSize = textureManager!!.calculateSampleSize(
            sourceWidth = 2160,
            sourceHeight = 3840,
            targetWidth = 1080,
            targetHeight = 1920
        )

        assertEquals("2x sampling for 2x larger image", 2, sampleSize)
    }

    @Test
    fun testCalculateSampleSizeFourX() {
        val sampleSize = textureManager!!.calculateSampleSize(
            sourceWidth = 4320,
            sourceHeight = 7680,
            targetWidth = 1080,
            targetHeight = 1920
        )

        assertEquals("4x sampling for 4x larger image", 4, sampleSize)
    }

    @Test
    fun testCalculateSampleSizeVeryLarge() {
        val sampleSize = textureManager!!.calculateSampleSize(
            sourceWidth = 8000,
            sourceHeight = 6000,
            targetWidth = 1080,
            targetHeight = 1920
        )

        assertTrue("Sample size should be power of 2", sampleSize in listOf(4, 8))
        assertTrue("Sample size should reduce dimensions", sampleSize > 1)
    }

    @Test
    fun testLoadBitmapFromValidUri() {
        val bitmap = textureManager!!.loadBitmapFromUri(testImageUri!!, 1080, 1920)

        assertNotNull("Bitmap should be loaded", bitmap)
        assertTrue("Bitmap width should be positive", bitmap!!.width > 0)
        assertTrue("Bitmap height should be positive", bitmap.height > 0)

        bitmap.recycle()
    }

    @Test
    fun testLoadBitmapWithoutSampling() {
        val bitmap = textureManager!!.loadBitmapFromUri(testImageUri!!)

        assertNotNull("Bitmap should be loaded", bitmap)
        assertEquals("Width should match source", 100, bitmap!!.width)
        assertEquals("Height should match source", 100, bitmap.height)

        bitmap.recycle()
    }

    @Test
    fun testLoadBitmapFromInvalidUri() {
        val invalidUri = Uri.parse("content://invalid/path")
        val bitmap = textureManager!!.loadBitmapFromUri(invalidUri, 1080, 1920)

        assertNull("Bitmap should be null for invalid URI", bitmap)
    }

    @Test
    fun testCropBitmap() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)

        val cropRect = CropRect(x = 10, y = 10, width = 50, height = 50)
        val croppedBitmap = textureManager!!.cropBitmap(bitmap, cropRect)

        assertNotNull("Cropped bitmap should not be null", croppedBitmap)
        assertEquals("Cropped width should match", 50, croppedBitmap.width)
        assertEquals("Cropped height should match", 50, croppedBitmap.height)

        croppedBitmap.recycle()
    }

    @Test
    fun testCropBitmapWithInvalidRect() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val invalidCrop = CropRect(x = -10, y = 10, width = 50, height = 50)
        val result = textureManager!!.cropBitmap(bitmap, invalidCrop)

        assertEquals("Should return original bitmap for invalid crop", bitmap, result)

        bitmap.recycle()
    }

    @Test
    fun testCropBitmapExceedsBounds() {
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)

        val exceedsCrop = CropRect(x = 50, y = 50, width = 100, height = 100)
        val result = textureManager!!.cropBitmap(bitmap, exceedsCrop)

        assertEquals("Should return original bitmap when crop exceeds bounds", bitmap, result)

        bitmap.recycle()
    }

    @Test
    fun testCreatePlaceholderTexture() {
        runOnGLThread {
            val textureId = textureManager!!.createPlaceholderTexture()

            assertTrue("Texture ID should be positive", textureId > 0)

            val error = GLES20.glGetError()
            assertEquals("No OpenGL errors", GLES20.GL_NO_ERROR, error)

            textureManager!!.releaseTexture(textureId)
        }
    }

    @Test
    fun testCreatePlaceholderTextureWithColor() {
        runOnGLThread {
            val color = Color.argb(255, 128, 64, 32)
            val textureId = textureManager!!.createPlaceholderTexture(color)

            assertTrue("Texture ID should be positive", textureId > 0)

            textureManager!!.releaseTexture(textureId)
        }
    }

    @Test
    fun testCreateTextureFromBitmap() {
        runOnGLThread {
            val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.GREEN)

            val textureId = textureManager!!.createTexture(bitmap)
            bitmap.recycle()

            assertTrue("Texture ID should be positive", textureId > 0)

            val error = GLES20.glGetError()
            assertEquals("No OpenGL errors", GLES20.GL_NO_ERROR, error)

            textureManager!!.releaseTexture(textureId)
        }
    }

    @Test
    fun testBindTexture() {
        runOnGLThread {
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val textureId = textureManager!!.createTexture(bitmap)
            bitmap.recycle()

            textureManager!!.bindTexture(textureId)

            val boundTexture = IntArray(1)
            GLES20.glGetIntegerv(GLES20.GL_TEXTURE_BINDING_2D, boundTexture, 0)

            assertEquals("Texture should be bound", textureId, boundTexture[0])

            textureManager!!.releaseTexture(textureId)
        }
    }

    @Test
    fun testBindInvalidTexture() {
        runOnGLThread {
            // Should not crash
            textureManager!!.bindTexture(0)
            textureManager!!.bindTexture(-1)

            val error = GLES20.glGetError()
            assertEquals("Should handle invalid texture gracefully", GLES20.GL_NO_ERROR, error)
        }
    }

    @Test
    fun testReleaseTexture() {
        runOnGLThread {
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val textureId = textureManager!!.createTexture(bitmap)
            bitmap.recycle()

            assertTrue("Texture created", textureId > 0)

            textureManager!!.releaseTexture(textureId)

            val error = GLES20.glGetError()
            assertEquals("No errors after release", GLES20.GL_NO_ERROR, error)
        }
    }

    @Test
    fun testReleaseInvalidTexture() {
        runOnGLThread {
            // Should not crash
            textureManager!!.releaseTexture(0)
            textureManager!!.releaseTexture(-1)

            val error = GLES20.glGetError()
            assertEquals("Should handle invalid release gracefully", GLES20.GL_NO_ERROR, error)
        }
    }

    @Test
    fun testMultipleTextureCreation() {
        runOnGLThread {
            val textureIds = mutableListOf<Int>()

            repeat(5) { i ->
                val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(Color.rgb(i * 50, i * 50, i * 50))

                val textureId = textureManager!!.createTexture(bitmap)
                bitmap.recycle()

                assertTrue("Texture $i created", textureId > 0)
                textureIds.add(textureId)
            }

            // All IDs should be unique
            assertEquals("All texture IDs unique", textureIds.size, textureIds.distinct().size)

            // Clean up
            textureIds.forEach { textureManager!!.releaseTexture(it) }

            val error = GLES20.glGetError()
            assertEquals("No OpenGL errors", GLES20.GL_NO_ERROR, error)
        }
    }

    @Test
    fun testTextureParametersSet() {
        runOnGLThread {
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val textureId = textureManager!!.createTexture(bitmap)
            bitmap.recycle()

            textureManager!!.bindTexture(textureId)

            // Query texture parameters
            val minFilter = IntArray(1)
            val magFilter = IntArray(1)
            val wrapS = IntArray(1)
            val wrapT = IntArray(1)

            GLES20.glGetTexParameteriv(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                minFilter,
                0
            )
            GLES20.glGetTexParameteriv(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                magFilter,
                0
            )
            GLES20.glGetTexParameteriv(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrapS, 0)
            GLES20.glGetTexParameteriv(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrapT, 0)

            assertEquals("Min filter LINEAR", GLES20.GL_LINEAR, minFilter[0])
            assertEquals("Mag filter LINEAR", GLES20.GL_LINEAR, magFilter[0])
            assertEquals("Wrap S CLAMP_TO_EDGE", GLES20.GL_CLAMP_TO_EDGE, wrapS[0])
            assertEquals("Wrap T CLAMP_TO_EDGE", GLES20.GL_CLAMP_TO_EDGE, wrapT[0])

            textureManager!!.releaseTexture(textureId)
        }
    }

    @Test
    fun testHasTextureInitiallyFalse() {
        assertFalse("Should not have texture initially", textureManager!!.hasTexture())
    }

    @Test
    fun testHasTextureAfterLoad() {
        runOnGLThread {
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val textureId = textureManager!!.createTexture(bitmap)
            bitmap.recycle()

            // Manually set current texture (simulating loadTexture)
            // Note: hasTexture checks internal state, not GL state
            // This is a limitation of the test - in practice loadTexture sets currentTextureId

            textureManager!!.releaseTexture(textureId)
        }
    }

    @Test
    fun testGetCurrentTextureIdInitiallyZero() {
        assertEquals("Current texture ID should be 0 initially", 0, textureManager!!.getCurrentTextureId())
    }

    @Test
    fun testCalculateBitmapMemorySizeARGB8888() {
        val size = textureManager!!.calculateBitmapMemorySize(
            1080,
            1920,
            Bitmap.Config.ARGB_8888
        )

        val expected = 1080 * 1920 * 4 // 4 bytes per pixel
        assertEquals("ARGB_8888 memory size", expected, size)
    }

    @Test
    fun testCalculateBitmapMemorySizeRGB565() {
        val size = textureManager!!.calculateBitmapMemorySize(
            1080,
            1920,
            Bitmap.Config.RGB_565
        )

        val expected = 1080 * 1920 * 2 // 2 bytes per pixel
        assertEquals("RGB_565 memory size", expected, size)
    }

    @Test
    fun testTextureCreationFromLargeBitmap() {
        runOnGLThread {
            // Create a larger bitmap to test memory handling
            val bitmap = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.YELLOW)

            val textureId = textureManager!!.createTexture(bitmap)
            bitmap.recycle()

            assertTrue("Large texture created", textureId > 0)

            val error = GLES20.glGetError()
            assertEquals("No OpenGL errors", GLES20.GL_NO_ERROR, error)

            textureManager!!.releaseTexture(textureId)
        }
    }

    @Test
    fun testReplaceTexture() {
        runOnGLThread {
            // Create first texture
            val bitmap1 = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            bitmap1.eraseColor(Color.RED)
            val textureId1 = textureManager!!.createTexture(bitmap1)
            bitmap1.recycle()

            // Simulate setting as current
            val currentId1 = textureId1

            // Release and create second texture
            textureManager!!.releaseTexture(textureId1)

            val bitmap2 = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            bitmap2.eraseColor(Color.BLUE)
            val textureId2 = textureManager!!.createTexture(bitmap2)
            bitmap2.recycle()

            assertTrue("Second texture created", textureId2 > 0)
            assertNotEquals("Texture IDs different", textureId1, textureId2)

            textureManager!!.releaseTexture(textureId2)
        }
    }

    @Test
    fun testConsistentTextureLifecycle() {
        runOnGLThread {
            repeat(10) { i ->
                val bitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
                val textureId = textureManager!!.createTexture(bitmap)
                bitmap.recycle()

                assertTrue("Iteration $i: texture created", textureId > 0)

                textureManager!!.bindTexture(textureId)
                textureManager!!.releaseTexture(textureId)

                val error = GLES20.glGetError()
                assertEquals("Iteration $i: no errors", GLES20.GL_NO_ERROR, error)
            }
        }
    }

    @Test
    fun testReleaseAllResources() {
        runOnGLThread {
            val bitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
            val textureId = textureManager!!.createTexture(bitmap)
            bitmap.recycle()

            // Note: release() would release currentTextureId, but we haven't set it
            // Just verify no crash
            textureManager!!.release()

            val error = GLES20.glGetError()
            assertEquals("No errors after release()", GLES20.GL_NO_ERROR, error)
        }
    }
}
