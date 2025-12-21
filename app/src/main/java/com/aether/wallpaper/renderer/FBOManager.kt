package com.aether.wallpaper.renderer

import android.opengl.GLES20
import android.util.Log

/**
 * Manages framebuffer objects (FBOs) for multi-layer rendering.
 *
 * Each layer renders to its own FBO with an attached texture.
 * The compositor shader then blends all layer textures to the screen.
 *
 * Responsibilities:
 * - Create FBOs with RGBA8 texture attachments
 * - Bind/unbind FBOs for rendering
 * - Provide texture IDs for compositor
 * - Handle screen resizing (recreate FBOs)
 * - Clean up GL resources on release
 *
 * Usage:
 * ```
 * val fboManager = FBOManager()
 * fboManager.createFBO("layer1", 1080, 1920)
 * fboManager.bindFBO("layer1")
 * // render to FBO...
 * fboManager.unbindFBO()
 * val textureId = fboManager.getTexture("layer1")
 * fboManager.release()
 * ```
 */
class FBOManager {

    companion object {
        private const val TAG = "FBOManager"
    }

    /**
     * Information about a framebuffer object.
     *
     * @property fboId OpenGL framebuffer object ID
     * @property textureId OpenGL texture ID attached to the FBO
     * @property width Texture width in pixels
     * @property height Texture height in pixels
     */
    data class FBOInfo(
        val fboId: Int,
        val textureId: Int,
        val width: Int,
        val height: Int
    )

    // Map of layer ID → FBO info
    private val fboMap = mutableMapOf<String, FBOInfo>()

    /**
     * Create a framebuffer object with RGBA8 texture attachment.
     *
     * @param layerId Unique identifier for this layer
     * @param width Texture width in pixels
     * @param height Texture height in pixels
     * @return FBOInfo on success, null on failure
     */
    fun createFBO(layerId: String, width: Int, height: Int): FBOInfo? {
        try {
            // Generate texture
            val textureIds = IntArray(1)
            GLES20.glGenTextures(1, textureIds, 0)
            val textureId = textureIds[0]

            if (textureId == 0) {
                Log.e(TAG, "Failed to generate texture for layer: $layerId")
                return null
            }

            // Configure texture
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

            // Create empty texture with RGBA8 format
            GLES20.glTexImage2D(
                GLES20.GL_TEXTURE_2D,
                0,                              // mip level
                GLES20.GL_RGBA,                 // internal format
                width,
                height,
                0,                              // border (must be 0)
                GLES20.GL_RGBA,                 // format
                GLES20.GL_UNSIGNED_BYTE,        // type
                null                            // no initial data
            )

            // Set texture parameters
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            )

            // Generate FBO
            val fboIds = IntArray(1)
            GLES20.glGenFramebuffers(1, fboIds, 0)
            val fboId = fboIds[0]

            if (fboId == 0) {
                Log.e(TAG, "Failed to generate FBO for layer: $layerId")
                GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
                return null
            }

            // Attach texture to FBO
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId)
            GLES20.glFramebufferTexture2D(
                GLES20.GL_FRAMEBUFFER,
                GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D,
                textureId,
                0
            )

            // Check framebuffer completeness
            val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
            if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
                Log.e(TAG, "FBO incomplete for layer $layerId: status=$status")
                GLES20.glDeleteFramebuffers(1, intArrayOf(fboId), 0)
                GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
                return null
            }

            // Unbind FBO
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

            // Store FBO info
            val fboInfo = FBOInfo(fboId, textureId, width, height)
            fboMap[layerId] = fboInfo

            Log.d(TAG, "Created FBO for layer $layerId: fbo=$fboId, texture=$textureId, size=${width}x${height}")
            return fboInfo

        } catch (e: Exception) {
            Log.e(TAG, "Exception creating FBO for layer $layerId", e)
            return null
        }
    }

    /**
     * Bind an FBO for rendering.
     *
     * Subsequent GL draw calls will render to this FBO's texture.
     *
     * @param layerId The layer ID
     * @return true if FBO was bound, false if layer not found
     */
    fun bindFBO(layerId: String): Boolean {
        val fboInfo = fboMap[layerId]
        if (fboInfo == null) {
            Log.w(TAG, "Cannot bind FBO: layer $layerId not found")
            return false
        }

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboInfo.fboId)
        return true
    }

    /**
     * Unbind FBO and restore default framebuffer.
     *
     * Subsequent GL draw calls will render to the screen.
     */
    fun unbindFBO() {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    }

    /**
     * Get the texture ID for a layer.
     *
     * The texture ID can be bound and sampled in shaders.
     *
     * @param layerId The layer ID
     * @return Texture ID, or 0 if layer not found
     */
    fun getTexture(layerId: String): Int {
        return fboMap[layerId]?.textureId ?: 0
    }

    /**
     * Get FBO information for a layer.
     *
     * @param layerId The layer ID
     * @return FBOInfo, or null if layer not found
     */
    fun getFBOInfo(layerId: String): FBOInfo? {
        return fboMap[layerId]
    }

    /**
     * Resize all FBOs to new dimensions.
     *
     * This is called when the surface changes size (e.g., screen rotation).
     * All existing FBOs are deleted and recreated at the new size.
     *
     * @param width New width in pixels
     * @param height New height in pixels
     */
    fun resize(width: Int, height: Int) {
        Log.d(TAG, "Resizing FBOs to ${width}x${height}")

        // Save layer IDs before releasing
        val layerIds = fboMap.keys.toList()

        // Release existing FBOs
        release()

        // Recreate FBOs at new size
        layerIds.forEach { layerId ->
            createFBO(layerId, width, height)
        }
    }

    /**
     * Release all FBO resources.
     *
     * Deletes all framebuffer objects and textures.
     * Should be called when the GL context is destroyed.
     */
    fun release() {
        Log.d(TAG, "Releasing ${fboMap.size} FBOs")

        fboMap.values.forEach { fboInfo ->
            // Delete FBO
            GLES20.glDeleteFramebuffers(1, intArrayOf(fboInfo.fboId), 0)
            Log.d(TAG, "Deleted FBO: ${fboInfo.fboId}")

            // Delete texture
            GLES20.glDeleteTextures(1, intArrayOf(fboInfo.textureId), 0)
            Log.d(TAG, "Deleted texture: ${fboInfo.textureId}")
        }

        fboMap.clear()
    }

    /**
     * Get the number of FBOs currently managed.
     *
     * Primarily for testing and debugging.
     *
     * @return Number of FBOs
     */
    fun getFBOCount(): Int = fboMap.size
}
