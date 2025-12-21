package com.aether.wallpaper.renderer

import android.content.Context
import android.opengl.GLES20
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumentation test for FBOManager.
 *
 * Tests framebuffer object creation, binding, texture management, and resource cleanup.
 * Requires a real OpenGL ES context.
 */
@RunWith(AndroidJUnit4::class)
class FBOManagerTest {

    private lateinit var context: Context
    private lateinit var glTestUtils: GLTestUtils
    private lateinit var fboManager: FBOManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        glTestUtils = GLTestUtils(context)
        glTestUtils.setupGLContext(1080, 1920)
        fboManager = FBOManager()
    }

    @After
    fun tearDown() {
        fboManager.release()
        glTestUtils.tearDownGLContext()
    }

    @Test
    fun testCreateFBOWithTextureAttachment() {
        // When: Create FBO
        val fboInfo = fboManager.createFBO("layer1", 1080, 1920)

        // Then: FBO is created successfully
        assertNotNull("FBO should be created", fboInfo)
        assertNotEquals("FBO ID should be valid", 0, fboInfo!!.fboId)
        assertNotEquals("Texture ID should be valid", 0, fboInfo.textureId)
        assertEquals("Width should match", 1080, fboInfo.width)
        assertEquals("Height should match", 1920, fboInfo.height)

        // Verify FBO is complete
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboInfo.fboId)
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        assertEquals(
            "FBO should be complete",
            GLES20.GL_FRAMEBUFFER_COMPLETE,
            status
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    @Test
    fun testBindAndUnbindFBO() {
        // Given: FBO created
        val fboInfo = fboManager.createFBO("layer1", 1080, 1920)
        assertNotNull(fboInfo)

        // When: Bind FBO
        val bound = fboManager.bindFBO("layer1")

        // Then: FBO is bound
        assertTrue("bindFBO should return true", bound)

        val boundFBO = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, boundFBO, 0)
        assertEquals("FBO should be bound", fboInfo!!.fboId, boundFBO[0])

        // When: Unbind FBO
        fboManager.unbindFBO()

        // Then: Default framebuffer is bound
        GLES20.glGetIntegerv(GLES20.GL_FRAMEBUFFER_BINDING, boundFBO, 0)
        assertEquals("Default framebuffer should be bound", 0, boundFBO[0])
    }

    @Test
    fun testBindNonexistentFBO() {
        // When: Try to bind nonexistent FBO
        val bound = fboManager.bindFBO("nonexistent")

        // Then: Returns false
        assertFalse("bindFBO should return false for nonexistent FBO", bound)
    }

    @Test
    fun testGetTextureID() {
        // Given: Multiple FBOs created
        val fboInfo1 = fboManager.createFBO("layer1", 1080, 1920)
        val fboInfo2 = fboManager.createFBO("layer2", 1080, 1920)
        val fboInfo3 = fboManager.createFBO("layer3", 1080, 1920)

        assertNotNull(fboInfo1)
        assertNotNull(fboInfo2)
        assertNotNull(fboInfo3)

        // When: Get texture IDs
        val texture1 = fboManager.getTexture("layer1")
        val texture2 = fboManager.getTexture("layer2")
        val texture3 = fboManager.getTexture("layer3")
        val textureNonexistent = fboManager.getTexture("nonexistent")

        // Then: Correct texture IDs are returned
        assertEquals("Layer1 texture should match", fboInfo1!!.textureId, texture1)
        assertEquals("Layer2 texture should match", fboInfo2!!.textureId, texture2)
        assertEquals("Layer3 texture should match", fboInfo3!!.textureId, texture3)
        assertEquals("Nonexistent layer should return 0", 0, textureNonexistent)

        // Verify texture IDs are unique
        assertNotEquals("Texture IDs should be unique", texture1, texture2)
        assertNotEquals("Texture IDs should be unique", texture2, texture3)
        assertNotEquals("Texture IDs should be unique", texture1, texture3)
    }

    @Test
    fun testResizeRecreatesFBOs() {
        // Given: FBOs created at 1080x1920
        fboManager.createFBO("layer1", 1080, 1920)
        fboManager.createFBO("layer2", 1080, 1920)
        fboManager.createFBO("layer3", 1080, 1920)

        val originalTexture1 = fboManager.getTexture("layer1")
        val originalTexture2 = fboManager.getTexture("layer2")

        assertNotEquals(0, originalTexture1)
        assertNotEquals(0, originalTexture2)

        // When: Resize to 1920x1080 (landscape)
        fboManager.resize(1920, 1080)

        // Then: New textures are created
        val newTexture1 = fboManager.getTexture("layer1")
        val newTexture2 = fboManager.getTexture("layer2")
        val newTexture3 = fboManager.getTexture("layer3")

        // Textures should be recreated (different IDs)
        assertNotEquals("Texture should be recreated", originalTexture1, newTexture1)
        assertNotEquals("Texture should be recreated", originalTexture2, newTexture2)

        // New textures should be valid
        assertNotEquals(0, newTexture1)
        assertNotEquals(0, newTexture2)
        assertNotEquals(0, newTexture3)
    }

    @Test
    fun testReleaseAllResources() {
        // Given: FBOs created
        fboManager.createFBO("layer1", 1080, 1920)
        fboManager.createFBO("layer2", 1080, 1920)
        fboManager.createFBO("layer3", 1080, 1920)

        val texture1Before = fboManager.getTexture("layer1")
        assertNotEquals(0, texture1Before)

        // When: Release all resources
        fboManager.release()

        // Then: All resources are cleared
        val texture1After = fboManager.getTexture("layer1")
        val texture2After = fboManager.getTexture("layer2")
        val texture3After = fboManager.getTexture("layer3")

        assertEquals("Texture should be cleared", 0, texture1After)
        assertEquals("Texture should be cleared", 0, texture2After)
        assertEquals("Texture should be cleared", 0, texture3After)
    }

    @Test
    fun testMultipleFBOsWithUniqueIDs() {
        // When: Create multiple FBOs
        val fboInfo1 = fboManager.createFBO("layer1", 1080, 1920)
        val fboInfo2 = fboManager.createFBO("layer2", 1080, 1920)
        val fboInfo3 = fboManager.createFBO("layer3", 1080, 1920)

        assertNotNull(fboInfo1)
        assertNotNull(fboInfo2)
        assertNotNull(fboInfo3)

        // Then: All FBO IDs are unique
        assertNotEquals(fboInfo1!!.fboId, fboInfo2!!.fboId)
        assertNotEquals(fboInfo2.fboId, fboInfo3!!.fboId)
        assertNotEquals(fboInfo1.fboId, fboInfo3.fboId)

        // And: All texture IDs are unique
        assertNotEquals(fboInfo1.textureId, fboInfo2.textureId)
        assertNotEquals(fboInfo2.textureId, fboInfo3.textureId)
        assertNotEquals(fboInfo1.textureId, fboInfo3.textureId)

        // And: All are valid (non-zero)
        assertTrue(fboInfo1.fboId > 0)
        assertTrue(fboInfo2.fboId > 0)
        assertTrue(fboInfo3.fboId > 0)
        assertTrue(fboInfo1.textureId > 0)
        assertTrue(fboInfo2.textureId > 0)
        assertTrue(fboInfo3.textureId > 0)
    }

    @Test
    fun testTextureParameters() {
        // Given: FBO created
        val fboInfo = fboManager.createFBO("layer1", 1080, 1920)
        assertNotNull(fboInfo)

        // When: Bind texture and check parameters
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboInfo!!.textureId)

        val minFilter = IntArray(1)
        val magFilter = IntArray(1)
        val wrapS = IntArray(1)
        val wrapT = IntArray(1)

        GLES20.glGetTexParameteriv(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, minFilter, 0)
        GLES20.glGetTexParameteriv(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, magFilter, 0)
        GLES20.glGetTexParameteriv(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, wrapS, 0)
        GLES20.glGetTexParameteriv(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, wrapT, 0)

        // Then: Texture has correct parameters
        assertEquals("Min filter should be LINEAR", GLES20.GL_LINEAR, minFilter[0])
        assertEquals("Mag filter should be LINEAR", GLES20.GL_LINEAR, magFilter[0])
        assertEquals("Wrap S should be CLAMP_TO_EDGE", GLES20.GL_CLAMP_TO_EDGE, wrapS[0])
        assertEquals("Wrap T should be CLAMP_TO_EDGE", GLES20.GL_CLAMP_TO_EDGE, wrapT[0])

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }
}
