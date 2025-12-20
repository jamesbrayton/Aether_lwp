package com.aether.wallpaper.texture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLUtils
import android.util.Log
import androidx.exifinterface.media.ExifInterface
import com.aether.wallpaper.model.CropRect
import java.io.FileNotFoundException
import java.io.IOException

/**
 * Manages OpenGL texture loading and lifecycle.
 *
 * Handles bitmap loading from ContentResolver URIs, efficient sampling for large images,
 * OpenGL texture creation and upload, and texture lifecycle management.
 */
class TextureManager(private val context: Context) {

    private var currentTextureId: Int = 0

    companion object {
        private const val TAG = "TextureManager"

        // Fallback sample sizes for OOM recovery
        private val FALLBACK_SAMPLE_SIZES = intArrayOf(2, 4, 8, 16)
    }

    /**
     * Load a bitmap from a content URI with optional sampling.
     *
     * @param uri Content URI pointing to the image
     * @param targetWidth Target width for sampling (0 = no sampling)
     * @param targetHeight Target height for sampling (0 = no sampling)
     * @return Decoded Bitmap, or null on error
     */
    fun loadBitmapFromUri(uri: Uri, targetWidth: Int = 0, targetHeight: Int = 0): Bitmap? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: run {
                    Log.e(TAG, "Failed to open input stream for URI: $uri")
                    return null
                }

            inputStream.use { stream ->
                // First pass: decode bounds only
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeStream(stream, null, options)

                val sourceWidth = options.outWidth
                val sourceHeight = options.outHeight

                if (sourceWidth <= 0 || sourceHeight <= 0) {
                    Log.e(TAG, "Invalid image dimensions: ${sourceWidth}x${sourceHeight}")
                    return null
                }

                // Calculate sample size if target dimensions provided
                val sampleSize = if (targetWidth > 0 && targetHeight > 0) {
                    calculateSampleSize(sourceWidth, sourceHeight, targetWidth, targetHeight)
                } else {
                    1
                }

                Log.d(TAG, "Loading image ${sourceWidth}x${sourceHeight} with sample size $sampleSize")

                // Second pass: decode with sampling
                val bitmap = decodeBitmapWithFallback(uri, sampleSize)

                // Apply EXIF orientation correction
                bitmap?.let { applyExifOrientation(it, uri) }
            }
        } catch (e: FileNotFoundException) {
            Log.e(TAG, "File not found for URI: $uri", e)
            null
        } catch (e: IOException) {
            Log.e(TAG, "IO error loading bitmap from URI: $uri", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading bitmap from URI: $uri", e)
            null
        }
    }

    /**
     * Decode bitmap with fallback sample sizes for OOM recovery.
     */
    private fun decodeBitmapWithFallback(uri: Uri, initialSampleSize: Int): Bitmap? {
        var sampleSize = initialSampleSize

        // Try initial sample size
        try {
            return decodeBitmapWithSampleSize(uri, sampleSize)
        } catch (e: OutOfMemoryError) {
            Log.w(TAG, "OOM with sample size $sampleSize, trying fallback sizes")
        }

        // Try fallback sample sizes
        for (fallbackSize in FALLBACK_SAMPLE_SIZES) {
            if (fallbackSize <= sampleSize) continue // Already tried smaller

            try {
                Log.d(TAG, "Attempting fallback sample size: $fallbackSize")
                return decodeBitmapWithSampleSize(uri, fallbackSize)
            } catch (e: OutOfMemoryError) {
                Log.w(TAG, "OOM with fallback sample size $fallbackSize")
            }
        }

        Log.e(TAG, "Failed to decode bitmap even with maximum sampling")
        return null
    }

    /**
     * Decode bitmap with specific sample size.
     */
    private fun decodeBitmapWithSampleSize(uri: Uri, sampleSize: Int): Bitmap? {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null

        return inputStream.use { stream ->
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888 // Preserve quality and alpha
            }
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    /**
     * Calculate appropriate sample size for efficient memory usage.
     *
     * Sample size is a power of 2 that results in dimensions at least as large as target.
     * inSampleSize = 1: no sampling
     * inSampleSize = 2: width/height reduced by 2x
     * inSampleSize = 4: width/height reduced by 4x
     *
     * @param sourceWidth Source image width
     * @param sourceHeight Source image height
     * @param targetWidth Target width
     * @param targetHeight Target height
     * @return Sample size (power of 2)
     */
    fun calculateSampleSize(
        sourceWidth: Int,
        sourceHeight: Int,
        targetWidth: Int,
        targetHeight: Int
    ): Int {
        var inSampleSize = 1

        if (sourceWidth > targetWidth || sourceHeight > targetHeight) {
            val halfWidth = sourceWidth / 2
            val halfHeight = sourceHeight / 2

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width
            while ((halfWidth / inSampleSize) >= targetWidth &&
                (halfHeight / inSampleSize) >= targetHeight
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Apply EXIF orientation correction to bitmap.
     */
    private fun applyExifOrientation(bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return bitmap

            inputStream.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                val matrix = Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    else -> return bitmap // No rotation needed
                }

                val rotatedBitmap = Bitmap.createBitmap(
                    bitmap,
                    0,
                    0,
                    bitmap.width,
                    bitmap.height,
                    matrix,
                    true
                )

                if (rotatedBitmap != bitmap) {
                    bitmap.recycle()
                }

                rotatedBitmap
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to apply EXIF orientation, using original bitmap", e)
            bitmap
        }
    }

    /**
     * Crop bitmap to specified rectangle.
     *
     * @param bitmap Source bitmap
     * @param cropRect Crop region
     * @return Cropped bitmap, or original if crop is invalid
     */
    fun cropBitmap(bitmap: Bitmap, cropRect: CropRect): Bitmap {
        // Validate crop bounds
        if (!cropRect.validate()) {
            Log.e(TAG, "Invalid crop rectangle: $cropRect")
            return bitmap
        }

        if (cropRect.x + cropRect.width > bitmap.width ||
            cropRect.y + cropRect.height > bitmap.height
        ) {
            Log.e(
                TAG,
                "Crop rectangle exceeds bitmap bounds: $cropRect vs ${bitmap.width}x${bitmap.height}"
            )
            return bitmap
        }

        return try {
            val croppedBitmap = Bitmap.createBitmap(
                bitmap,
                cropRect.x,
                cropRect.y,
                cropRect.width,
                cropRect.height
            )

            if (croppedBitmap != bitmap) {
                bitmap.recycle()
            }

            croppedBitmap
        } catch (e: Exception) {
            Log.e(TAG, "Failed to crop bitmap", e)
            bitmap
        }
    }

    /**
     * Create OpenGL texture from bitmap.
     *
     * Must be called on GL thread.
     *
     * @param bitmap Source bitmap
     * @return Texture ID, or 0 on error
     */
    fun createTexture(bitmap: Bitmap): Int {
        val textureIds = IntArray(1)

        // Generate texture
        GLES20.glGenTextures(1, textureIds, 0)
        val textureId = textureIds[0]

        if (textureId == 0) {
            Log.e(TAG, "Failed to generate texture")
            return 0
        }

        // Bind texture
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

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

        // Upload bitmap to texture
        // Note: UV flip is handled in shader (uv.y = 1.0 - uv.y)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            Log.e(TAG, "OpenGL error uploading texture: $error")
            GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
            return 0
        }

        // Unbind
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        Log.d(TAG, "Created texture $textureId from ${bitmap.width}x${bitmap.height} bitmap")
        return textureId
    }

    /**
     * Create a 1x1 placeholder texture.
     *
     * Must be called on GL thread.
     *
     * @param color ARGB color value (default: black)
     * @return Texture ID
     */
    fun createPlaceholderTexture(color: Int = 0xFF000000.toInt()): Int {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, color)

        val textureId = createTexture(bitmap)
        bitmap.recycle()

        return textureId
    }

    /**
     * Bind texture for rendering.
     *
     * Must be called on GL thread.
     *
     * @param textureId Texture ID to bind
     */
    fun bindTexture(textureId: Int) {
        if (textureId <= 0) {
            Log.w(TAG, "Attempted to bind invalid texture ID: $textureId")
            return
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
    }

    /**
     * Release texture and free GPU memory.
     *
     * Must be called on GL thread.
     *
     * @param textureId Texture ID to release
     */
    fun releaseTexture(textureId: Int) {
        if (textureId <= 0) {
            return
        }

        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
        Log.d(TAG, "Released texture $textureId")

        if (textureId == currentTextureId) {
            currentTextureId = 0
        }
    }

    /**
     * Load texture from URI and set as current.
     *
     * Releases previous texture if it exists.
     * Must be called on GL thread for texture creation.
     *
     * @param uri Content URI
     * @param targetWidth Target width for sampling
     * @param targetHeight Target height for sampling
     * @param cropRect Optional crop region
     * @return Texture ID, or 0 on error
     */
    fun loadTexture(
        uri: Uri,
        targetWidth: Int,
        targetHeight: Int,
        cropRect: CropRect? = null
    ): Int {
        // Load bitmap (can be off GL thread)
        var bitmap = loadBitmapFromUri(uri, targetWidth, targetHeight) ?: run {
            Log.e(TAG, "Failed to load bitmap from URI")
            return 0
        }

        // Apply crop if specified
        if (cropRect != null) {
            bitmap = cropBitmap(bitmap, cropRect)
        }

        // Release old texture
        if (currentTextureId > 0) {
            releaseTexture(currentTextureId)
        }

        // Create new texture (must be on GL thread)
        val textureId = createTexture(bitmap)
        bitmap.recycle()

        if (textureId > 0) {
            currentTextureId = textureId
        }

        return textureId
    }

    /**
     * Check if a texture is currently loaded.
     */
    fun hasTexture(): Boolean {
        return currentTextureId > 0
    }

    /**
     * Get current texture ID.
     *
     * @return Current texture ID, or 0 if none
     */
    fun getCurrentTextureId(): Int {
        return currentTextureId
    }

    /**
     * Calculate bitmap memory size in bytes.
     *
     * @param width Bitmap width
     * @param height Bitmap height
     * @param config Bitmap config
     * @return Memory size in bytes
     */
    fun calculateBitmapMemorySize(width: Int, height: Int, config: Bitmap.Config): Int {
        val bytesPerPixel = when (config) {
            Bitmap.Config.ARGB_8888 -> 4
            Bitmap.Config.RGB_565 -> 2
            Bitmap.Config.ARGB_4444 -> 2
            Bitmap.Config.ALPHA_8 -> 1
            else -> 4 // Default to ARGB_8888
        }

        return width * height * bytesPerPixel
    }

    /**
     * Release all resources.
     */
    fun release() {
        if (currentTextureId > 0) {
            releaseTexture(currentTextureId)
        }
    }
}
