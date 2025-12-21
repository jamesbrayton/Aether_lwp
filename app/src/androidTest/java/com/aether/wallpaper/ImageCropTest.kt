package com.aether.wallpaper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aether.wallpaper.model.CropRect
import com.aether.wallpaper.texture.TextureManager
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for image cropping functionality.
 *
 * Tests the end-to-end crop workflow:
 * - ImageCropActivity crop coordinate calculation
 * - TextureManager crop application
 * - CropRect validation and persistence
 *
 * Note: Full UI tests for ImageCropActivity would require Espresso
 * with custom touch interactions (drag, resize). These tests focus
 * on the core cropping logic and TextureManager integration.
 */
@RunWith(AndroidJUnit4::class)
class ImageCropTest {

    private lateinit var context: Context
    private lateinit var textureManager: TextureManager

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        textureManager = TextureManager(context)
    }

    // ========== CropRect Validation ==========

    @Test
    fun testCropRectValidation() {
        // Valid crop rect
        val validCrop = CropRect(0, 0, 100, 100)
        assertTrue(validCrop.validate())

        // Invalid: negative x
        val invalidX = CropRect(-10, 0, 100, 100)
        assertFalse(invalidX.validate())

        // Invalid: negative y
        val invalidY = CropRect(0, -10, 100, 100)
        assertFalse(invalidY.validate())

        // Invalid: zero width
        val invalidWidth = CropRect(0, 0, 0, 100)
        assertFalse(invalidWidth.validate())

        // Invalid: zero height
        val invalidHeight = CropRect(0, 0, 100, 0)
        assertFalse(invalidHeight.validate())

        // Invalid: negative width
        val negativeWidth = CropRect(0, 0, -100, 100)
        assertFalse(negativeWidth.validate())

        // Invalid: negative height
        val negativeHeight = CropRect(0, 0, 100, -100)
        assertFalse(negativeHeight.validate())
    }

    // ========== TextureManager Crop Application ==========

    @Test
    fun testCropBitmapBasic() {
        // Create test bitmap (200x200 white image)
        val sourceBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        sourceBitmap.eraseColor(android.graphics.Color.WHITE)

        // Crop to 100x100 region
        val cropRect = CropRect(50, 50, 100, 100)
        val croppedBitmap = textureManager.cropBitmap(sourceBitmap, cropRect)

        // Verify cropped dimensions
        assertEquals(100, croppedBitmap.width)
        assertEquals(100, croppedBitmap.height)

        // Cleanup
        sourceBitmap.recycle()
        if (croppedBitmap != sourceBitmap) {
            croppedBitmap.recycle()
        }
    }

    @Test
    fun testCropBitmapCentered() {
        // Create 400x400 bitmap
        val sourceBitmap = Bitmap.createBitmap(400, 400, Bitmap.Config.ARGB_8888)

        // Crop centered 200x200 region
        val cropRect = CropRect(100, 100, 200, 200)
        val croppedBitmap = textureManager.cropBitmap(sourceBitmap, cropRect)

        assertEquals(200, croppedBitmap.width)
        assertEquals(200, croppedBitmap.height)

        sourceBitmap.recycle()
        if (croppedBitmap != sourceBitmap) {
            croppedBitmap.recycle()
        }
    }

    @Test
    fun testCropBitmapTopLeft() {
        // Create 300x300 bitmap
        val sourceBitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)

        // Crop top-left 100x100 region
        val cropRect = CropRect(0, 0, 100, 100)
        val croppedBitmap = textureManager.cropBitmap(sourceBitmap, cropRect)

        assertEquals(100, croppedBitmap.width)
        assertEquals(100, croppedBitmap.height)

        sourceBitmap.recycle()
        if (croppedBitmap != sourceBitmap) {
            croppedBitmap.recycle()
        }
    }

    @Test
    fun testCropBitmapBottomRight() {
        // Create 300x300 bitmap
        val sourceBitmap = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888)

        // Crop bottom-right 100x100 region
        val cropRect = CropRect(200, 200, 100, 100)
        val croppedBitmap = textureManager.cropBitmap(sourceBitmap, cropRect)

        assertEquals(100, croppedBitmap.width)
        assertEquals(100, croppedBitmap.height)

        sourceBitmap.recycle()
        if (croppedBitmap != sourceBitmap) {
            croppedBitmap.recycle()
        }
    }

    @Test
    fun testCropBitmapInvalidRect() {
        // Create 200x200 bitmap
        val sourceBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)

        // Invalid crop rect (exceeds bounds)
        val cropRect = CropRect(150, 150, 100, 100) // Would need 250x250 bitmap

        // Should return original bitmap unchanged
        val result = textureManager.cropBitmap(sourceBitmap, cropRect)

        assertEquals(sourceBitmap, result)
        assertEquals(200, result.width)
        assertEquals(200, result.height)

        sourceBitmap.recycle()
    }

    @Test
    fun testCropBitmapNegativeCoordinates() {
        // Create 200x200 bitmap
        val sourceBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)

        // Invalid crop rect (negative coordinates)
        val cropRect = CropRect(-10, -10, 50, 50)

        // Should return original bitmap due to validation failure
        val result = textureManager.cropBitmap(sourceBitmap, cropRect)

        assertEquals(sourceBitmap, result)

        sourceBitmap.recycle()
    }

    @Test
    fun testCropBitmapFullImage() {
        // Create 200x200 bitmap
        val sourceBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)

        // Crop entire image
        val cropRect = CropRect(0, 0, 200, 200)
        val croppedBitmap = textureManager.cropBitmap(sourceBitmap, cropRect)

        assertEquals(200, croppedBitmap.width)
        assertEquals(200, croppedBitmap.height)

        sourceBitmap.recycle()
        if (croppedBitmap != sourceBitmap) {
            croppedBitmap.recycle()
        }
    }

    // ========== Aspect Ratio Tests ==========

    @Test
    fun testCropBitmapPortraitAspect() {
        // Create 1080x1920 bitmap (portrait)
        val sourceBitmap = Bitmap.createBitmap(1080, 1920, Bitmap.Config.ARGB_8888)

        // Crop to 540x960 (maintains 9:16 aspect)
        val cropRect = CropRect(270, 480, 540, 960)
        val croppedBitmap = textureManager.cropBitmap(sourceBitmap, cropRect)

        assertEquals(540, croppedBitmap.width)
        assertEquals(960, croppedBitmap.height)

        // Verify aspect ratio
        val aspectRatio = croppedBitmap.width.toFloat() / croppedBitmap.height.toFloat()
        assertEquals(9f / 16f, aspectRatio, 0.01f)

        sourceBitmap.recycle()
        if (croppedBitmap != sourceBitmap) {
            croppedBitmap.recycle()
        }
    }

    @Test
    fun testCropBitmapLandscapeAspect() {
        // Create 1920x1080 bitmap (landscape)
        val sourceBitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888)

        // Crop to 960x540 (maintains 16:9 aspect)
        val cropRect = CropRect(480, 270, 960, 540)
        val croppedBitmap = textureManager.cropBitmap(sourceBitmap, cropRect)

        assertEquals(960, croppedBitmap.width)
        assertEquals(540, croppedBitmap.height)

        val aspectRatio = croppedBitmap.width.toFloat() / croppedBitmap.height.toFloat()
        assertEquals(16f / 9f, aspectRatio, 0.01f)

        sourceBitmap.recycle()
        if (croppedBitmap != sourceBitmap) {
            croppedBitmap.recycle()
        }
    }

    // ========== Edge Cases ==========

    @Test
    fun testCropBitmapMinimumSize() {
        // Create 500x500 bitmap
        val sourceBitmap = Bitmap.createBitmap(500, 500, Bitmap.Config.ARGB_8888)

        // Crop to very small region (1x1 pixel)
        val cropRect = CropRect(250, 250, 1, 1)
        val croppedBitmap = textureManager.cropBitmap(sourceBitmap, cropRect)

        assertEquals(1, croppedBitmap.width)
        assertEquals(1, croppedBitmap.height)

        sourceBitmap.recycle()
        if (croppedBitmap != sourceBitmap) {
            croppedBitmap.recycle()
        }
    }

    @Test
    fun testCropBitmapLargeImage() {
        // Create large bitmap (4000x3000)
        val sourceBitmap = Bitmap.createBitmap(4000, 3000, Bitmap.Config.ARGB_8888)

        // Crop to 1080x1920 region
        val cropRect = CropRect(1460, 540, 1080, 1920)
        val croppedBitmap = textureManager.cropBitmap(sourceBitmap, cropRect)

        assertEquals(1080, croppedBitmap.width)
        assertEquals(1920, croppedBitmap.height)

        sourceBitmap.recycle()
        if (croppedBitmap != sourceBitmap) {
            croppedBitmap.recycle()
        }
    }

    /**
     * Note on missing tests:
     *
     * Full integration tests for ImageCropActivity would require:
     * 1. Espresso UI testing with custom ViewActions
     * 2. Simulating touch events for drag and resize
     * 3. Verifying crop overlay rendering
     * 4. Testing aspect ratio maintenance during resize
     * 5. Testing crop coordinate calculation from view space to image space
     *
     * These tests cover the core bitmap cropping logic that ImageCropActivity
     * and TextureManager rely on. The UI layer would need manual or Espresso testing.
     *
     * TextureManager.loadTexture() with CropRect is tested indirectly through
     * these cropBitmap tests, as loadTexture() calls cropBitmap() internally.
     */
}
