Feature: Texture Management
  As the wallpaper renderer
  I want to efficiently load and manage OpenGL textures from user images
  So that background images display correctly without consuming excessive memory

  Background:
    Given TextureManager is initialized
    And OpenGL context is available

  Scenario: Load image from content URI
    Given a content URI pointing to a valid image
    When the image is loaded from the URI
    Then a Bitmap is returned
    And the Bitmap has valid dimensions
    And the Bitmap format is supported

  Scenario: Decode bitmap with sampling for large images
    Given an image larger than the target resolution
    When the image is loaded with the target dimensions
    Then the Bitmap is sampled down to fit the target
    And memory usage is minimized
    And the aspect ratio is preserved

  Scenario: Decode bitmap without sampling for small images
    Given an image smaller than the target resolution
    When the image is loaded with the target dimensions
    Then the Bitmap is loaded at full resolution
    And no upscaling occurs

  Scenario: Calculate appropriate sample size for large image
    Given an image of 4000x3000 pixels
    And a target resolution of 1080x1920
    When the sample size is calculated
    Then the sample size is 2 (inSampleSize = 2)
    And the resulting bitmap will be ~2000x1500

  Scenario: Calculate sample size for very large image
    Given an image of 8000x6000 pixels
    And a target resolution of 1080x1920
    When the sample size is calculated
    Then the sample size is 4 (inSampleSize = 4)
    And the resulting bitmap will be ~2000x1500

  Scenario: Create OpenGL texture from bitmap
    Given a valid Bitmap
    When the texture is created in OpenGL
    Then a valid texture ID is returned
    And the texture ID is greater than 0
    And the texture is bound to GL_TEXTURE_2D
    And texture parameters are set (min/mag filter, wrap mode)

  Scenario: Upload bitmap data to OpenGL texture
    Given a Bitmap and texture ID
    When texImage2D is called
    Then the bitmap data is uploaded to the GPU
    And no OpenGL errors occur
    And the texture is usable for rendering

  Scenario: Set texture filtering parameters
    Given a texture is created
    When texture parameters are configured
    Then GL_TEXTURE_MIN_FILTER is set to GL_LINEAR
    And GL_TEXTURE_MAG_FILTER is set to GL_LINEAR
    And GL_TEXTURE_WRAP_S is set to GL_CLAMP_TO_EDGE
    And GL_TEXTURE_WRAP_T is set to GL_CLAMP_TO_EDGE

  Scenario: Bind texture for rendering
    Given a texture ID exists
    When the texture is bound
    Then glBindTexture is called with GL_TEXTURE_2D
    And the texture ID is bound
    And the texture is active for rendering

  Scenario: Release texture resources
    Given a texture ID exists
    When the texture is released
    Then glDeleteTextures is called
    And the texture ID is invalidated
    And GPU memory is freed

  Scenario: Handle invalid content URI
    Given an invalid content URI
    When the image is loaded
    Then null is returned
    And an error is logged
    And no exception is thrown

  Scenario: Handle missing file from URI
    Given a content URI pointing to a non-existent file
    When the image is loaded
    Then null is returned
    And FileNotFoundException is handled gracefully
    And an error is logged

  Scenario: Handle corrupted image file
    Given a content URI pointing to a corrupted image
    When the image is loaded
    Then null is returned
    And the decoding error is handled gracefully
    And an error is logged

  Scenario: Load JPEG image
    Given a content URI pointing to a JPEG image
    When the image is loaded
    Then the Bitmap is decoded successfully
    And the Bitmap format is ARGB_8888 or RGB_565

  Scenario: Load PNG image with transparency
    Given a content URI pointing to a PNG image with alpha
    When the image is loaded
    Then the Bitmap is decoded successfully
    And the Bitmap format preserves alpha channel (ARGB_8888)

  Scenario: Load image with EXIF orientation
    Given a JPEG image with EXIF orientation metadata
    When the image is loaded
    Then the Bitmap is rotated according to EXIF orientation
    And the image displays correctly oriented

  Scenario: Replace existing texture
    Given a texture already exists for the background
    When a new texture is loaded
    Then the old texture is deleted
    And the new texture replaces it
    And no texture leak occurs

  Scenario: Load texture on GL thread
    Given a Bitmap is ready
    When loadTexture is called
    Then the texture operations occur on the GL thread
    And no GL errors occur from wrong-thread access

  Scenario: Handle out-of-memory during bitmap decode
    Given an extremely large image
    When decoding with insufficient memory
    Then OutOfMemoryError is caught
    And a fallback sample size is attempted
    And an error is logged

  Scenario: Calculate bitmap memory size
    Given a Bitmap of 1080x1920 pixels
    And ARGB_8888 format (4 bytes per pixel)
    When the memory size is calculated
    Then the size is 1080 * 1920 * 4 = 8,294,400 bytes (~8 MB)

  Scenario: Use RGB_565 format for non-transparent images
    Given a JPEG image (no alpha channel)
    When the Bitmap is decoded
    Then RGB_565 format can be used (2 bytes per pixel)
    And memory usage is halved vs ARGB_8888

  Scenario: Multiple texture lifecycle
    Given multiple textures are created
    When textures are bound and released
    Then each texture operates independently
    And no texture ID conflicts occur
    And all textures are properly cleaned up

  Scenario: Texture dimensions validation
    Given a Bitmap is loaded
    When the texture is created
    Then the width is a valid OpenGL dimension (> 0, <= GL_MAX_TEXTURE_SIZE)
    And the height is a valid OpenGL dimension (> 0, <= GL_MAX_TEXTURE_SIZE)

  Scenario: Load texture with cropping
    Given a Bitmap loaded from URI
    And a CropRect specifying a region
    When the Bitmap is cropped
    Then only the specified region is retained
    And the cropped Bitmap is uploaded to OpenGL
    And memory is conserved

  Scenario: Crop validation
    Given a CropRect with bounds
    When the crop is applied
    Then the crop bounds are validated against Bitmap size
    And invalid crops are rejected
    And an error is logged

  Scenario: Generate placeholder texture
    Given no background image is set
    When a placeholder texture is needed
    Then a 1x1 solid color texture is created
    And the texture is valid for rendering
    And minimal GPU memory is used

  Scenario: Texture manager state tracking
    Given TextureManager manages background texture
    When textures are loaded and released
    Then the current texture ID is tracked
    And hasTexture() reports correct state
    And getCurrentTextureId() returns valid ID or 0
