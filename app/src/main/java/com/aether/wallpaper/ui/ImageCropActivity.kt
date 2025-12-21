package com.aether.wallpaper.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.aether.wallpaper.R
import com.google.android.material.button.MaterialButton
import java.io.IOException

/**
 * Activity for cropping background images to screen aspect ratio.
 *
 * Intent extras:
 * - IMAGE_URI (String): URI of image to crop
 *
 * Result extras:
 * - CROP_X (Int): X coordinate of crop region
 * - CROP_Y (Int): Y coordinate of crop region
 * - CROP_WIDTH (Int): Width of crop region
 * - CROP_HEIGHT (Int): Height of crop region
 *
 * Result codes:
 * - RESULT_OK: Crop completed successfully
 * - RESULT_CANCELED: User canceled or error occurred
 */
class ImageCropActivity : AppCompatActivity() {

    private lateinit var cropImageView: CropImageView
    private lateinit var doneButton: MaterialButton
    private lateinit var cancelButton: MaterialButton

    private var imageUri: Uri? = null

    companion object {
        private const val TAG = "ImageCropActivity"
        const val EXTRA_IMAGE_URI = "IMAGE_URI"
        const val EXTRA_CROP_X = "CROP_X"
        const val EXTRA_CROP_Y = "CROP_Y"
        const val EXTRA_CROP_WIDTH = "CROP_WIDTH"
        const val EXTRA_CROP_HEIGHT = "CROP_HEIGHT"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_crop)

        cropImageView = findViewById(R.id.cropImageView)
        doneButton = findViewById(R.id.doneButton)
        cancelButton = findViewById(R.id.cancelButton)

        // Get image URI from intent
        val uriString = intent.getStringExtra(EXTRA_IMAGE_URI)
        if (uriString == null) {
            Log.e(TAG, "No image URI provided")
            Toast.makeText(this, "Error: No image selected", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        imageUri = Uri.parse(uriString)

        // Load and display image
        loadImage(imageUri!!)

        // Set up buttons
        doneButton.setOnClickListener {
            finishWithCrop()
        }

        cancelButton.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    /**
     * Load image from URI with sampling and EXIF handling.
     */
    private fun loadImage(uri: Uri) {
        try {
            // First pass: get image dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, options)
            }

            val sourceWidth = options.outWidth
            val sourceHeight = options.outHeight

            if (sourceWidth <= 0 || sourceHeight <= 0) {
                Log.e(TAG, "Invalid image dimensions")
                showErrorAndFinish("Invalid image")
                return
            }

            // Calculate sample size to fit in view (max 2048x2048 for memory)
            val maxDimension = 2048
            val sampleSize = calculateSampleSize(sourceWidth, sourceHeight, maxDimension, maxDimension)

            // Second pass: decode with sampling
            val sampledBitmap = contentResolver.openInputStream(uri)?.use { stream ->
                val decodeOptions = BitmapFactory.Options().apply {
                    inSampleSize = sampleSize
                }
                BitmapFactory.decodeStream(stream, null, decodeOptions)
            }

            if (sampledBitmap == null) {
                Log.e(TAG, "Failed to decode image")
                showErrorAndFinish("Failed to load image")
                return
            }

            // Apply EXIF orientation
            val rotatedBitmap = applyExifOrientation(sampledBitmap, uri)

            // Set bitmap in crop view
            cropImageView.setImageBitmap(rotatedBitmap)

        } catch (e: IOException) {
            Log.e(TAG, "IO error loading image", e)
            showErrorAndFinish("Error loading image")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading image", e)
            showErrorAndFinish("Error loading image")
        }
    }

    /**
     * Calculate sample size for efficient memory usage.
     */
    private fun calculateSampleSize(srcWidth: Int, srcHeight: Int, targetWidth: Int, targetHeight: Int): Int {
        var sampleSize = 1

        if (srcWidth > targetWidth || srcHeight > targetHeight) {
            val halfWidth = srcWidth / 2
            val halfHeight = srcHeight / 2

            while ((halfWidth / sampleSize) >= targetWidth && (halfHeight / sampleSize) >= targetHeight) {
                sampleSize *= 2
            }
        }

        return sampleSize
    }

    /**
     * Apply EXIF orientation to bitmap.
     */
    private fun applyExifOrientation(bitmap: Bitmap, uri: Uri): Bitmap {
        try {
            contentResolver.openInputStream(uri)?.use { stream ->
                val exif = ExifInterface(stream)
                val orientation = exif.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )

                val matrix = android.graphics.Matrix()
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                    ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                    ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                    ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                    ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                    else -> return bitmap
                }

                return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            }
        } catch (e: IOException) {
            Log.w(TAG, "Failed to read EXIF data, using image as-is", e)
        }

        return bitmap
    }

    /**
     * Finish activity with crop coordinates.
     */
    private fun finishWithCrop() {
        val coords = cropImageView.getCropCoordinates()

        if (coords == null) {
            Log.e(TAG, "Failed to get crop coordinates")
            Toast.makeText(this, "Error calculating crop", Toast.LENGTH_SHORT).show()
            setResult(RESULT_CANCELED)
            finish()
            return
        }

        val resultIntent = Intent().apply {
            putExtra(EXTRA_CROP_X, coords[0])
            putExtra(EXTRA_CROP_Y, coords[1])
            putExtra(EXTRA_CROP_WIDTH, coords[2])
            putExtra(EXTRA_CROP_HEIGHT, coords[3])
        }

        Log.d(TAG, "Crop completed: x=${coords[0]}, y=${coords[1]}, w=${coords[2]}, h=${coords[3]}")

        setResult(RESULT_OK, resultIntent)
        finish()
    }

    /**
     * Show error message and finish activity.
     */
    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        setResult(RESULT_CANCELED)
        finish()
    }
}
