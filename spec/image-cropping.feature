Feature: Image Cropping Integration
  As a user
  I want to crop background images to fit my screen
  So that I can customize how the wallpaper looks

  Background:
    Given the SettingsActivity is launched
    And ImageCropActivity is implemented
    And TextureManager supports crop coordinates

  # Activity Launch and Integration

  Scenario: User selects background image
    Given no background image is selected
    When the user taps "Select Background" button
    Then the image picker is launched
    When the user selects an image
    Then ImageCropActivity is launched with the selected image
    And the full image is displayed

  Scenario: Crop activity displays selected image
    Given the user selected an image from gallery
    When ImageCropActivity is launched
    Then the full image is loaded and displayed
    And the image is scaled to fit the screen
    And no crop overlay is shown initially

  Scenario: User can preview full image before cropping
    Given ImageCropActivity is displaying an image
    When the activity first loads
    Then the user sees the full uncropped image
    And EXIF orientation is applied correctly
    And aspect ratio is preserved

  # Crop Overlay UI

  Scenario: User enables crop mode
    Given ImageCropActivity is displaying an image
    When the user taps "Crop" button
    Then a crop overlay appears on the image
    And the overlay is centered on the screen
    And the overlay has resize handles at corners
    And the overlay has drag handles to move it

  Scenario: Crop overlay starts at screen aspect ratio
    Given the device screen is 1080x1920 (9:16 portrait)
    When the crop overlay is enabled
    Then the overlay aspect ratio is 9:16
    And the overlay is as large as possible within the image
    And the overlay is centered

  Scenario: Crop overlay adapts to landscape screens
    Given the device screen is 1920x1080 (16:9 landscape)
    When the crop overlay is enabled
    Then the overlay aspect ratio is 16:9
    And the overlay fits within the image bounds

  # Interactive Crop Manipulation

  Scenario: User drags crop region to reposition
    Given the crop overlay is enabled
    When the user drags the overlay center
    Then the overlay moves with the touch
    And the overlay stays within image bounds
    And the overlay does not resize

  Scenario: User resizes crop region with corner handles
    Given the crop overlay is enabled
    When the user drags a corner handle
    Then the overlay resizes
    And the aspect ratio is maintained at 9:16
    And the opposite corner stays fixed
    And the overlay stays within image bounds

  Scenario: User resizes crop region with edge handles
    Given the crop overlay is enabled
    When the user drags an edge handle
    Then the overlay resizes along that edge
    And the aspect ratio is maintained
    And the opposite edge stays fixed

  Scenario: Crop region cannot exceed image bounds
    Given the crop overlay is 500x888 pixels
    When the user tries to drag it outside the image
    Then the overlay stops at the image boundary
    And the overlay remains fully within the image
    And no white space or background is visible

  Scenario: Crop region cannot be smaller than minimum size
    Given the crop overlay is enabled
    When the user tries to resize it very small
    Then the overlay stops at minimum size (200x356 pixels)
    And further shrinking is prevented
    And the overlay remains usable

  # Zoom and Pan

  Scenario: User pinch-zooms the image
    Given the crop overlay is enabled
    When the user performs a pinch-zoom gesture
    Then the image scales up or down
    And the crop overlay stays fixed on screen
    And the image can be panned after zooming
    And the crop region adapts to the new view

  Scenario: User pans zoomed image
    Given the image is zoomed in 2x
    When the user drags with two fingers
    Then the image pans behind the crop overlay
    And the crop overlay position remains fixed
    And the user can see different parts of the image

  Scenario: Zoom limits prevent excessive scaling
    Given the image is at default zoom
    When the user zooms in repeatedly
    Then zoom stops at 4x maximum
    When the user zooms out repeatedly
    Then zoom stops at 1x minimum (full image visible)

  # Crop Confirmation and Cancellation

  Scenario: User confirms crop selection
    Given the crop overlay is positioned at x=100, y=200
    And the crop overlay is 540x960 pixels
    When the user taps "Done" button
    Then ImageCropActivity finishes
    And returns RESULT_OK
    And returns crop coordinates (100, 200, 540, 960)
    And SettingsActivity receives the result

  Scenario: User cancels crop selection
    Given the crop overlay is enabled
    When the user taps "Cancel" button
    Then ImageCropActivity finishes
    And returns RESULT_CANCELED
    And SettingsActivity receives the cancellation
    And the background image is not changed

  Scenario: User backs out of crop activity
    Given the crop overlay is enabled
    When the user presses the back button
    Then ImageCropActivity finishes
    And returns RESULT_CANCELED
    And no crop is applied

  # SettingsActivity Integration

  Scenario: SettingsActivity launches crop after image selection
    Given SettingsActivity is active
    When the user selects an image from the picker
    Then SettingsActivity receives the image URI
    And SettingsActivity launches ImageCropActivity
    And passes the image URI as intent extra
    And waits for crop result

  Scenario: SettingsActivity receives crop result
    Given ImageCropActivity returns crop coordinates
    When SettingsActivity.onActivityResult() is called
    Then SettingsActivity extracts crop coordinates
    And creates BackgroundConfig with URI and CropRect
    And saves the configuration via ConfigManager
    And updates the background preview ImageView
    And enables the "Apply Wallpaper" button

  Scenario: SettingsActivity handles crop cancellation
    Given ImageCropActivity returns RESULT_CANCELED
    When SettingsActivity.onActivityResult() is called
    Then SettingsActivity does not save any configuration
    And the previous background (if any) is preserved
    And the user can select a different image

  Scenario: SettingsActivity displays cropped preview
    Given a background with crop coordinates is saved
    When SettingsActivity loads the configuration
    Then the preview ImageView shows the cropped region
    And the preview matches what will appear in the wallpaper
    And the full image URI is preserved in config

  # TextureManager Integration

  Scenario: TextureManager loads full image when no crop specified
    Given BackgroundConfig has uri but crop is null
    When TextureManager.loadTexture() is called
    Then the full image is loaded
    And EXIF orientation is applied
    And the image is sampled to fit GPU texture limits
    And the texture is uploaded to OpenGL

  Scenario: TextureManager applies crop coordinates
    Given BackgroundConfig has crop (x=100, y=200, width=540, height=960)
    When TextureManager.loadTexture() is called
    Then the full image is loaded first
    Then the bitmap is cropped to (100, 200, 540, 960)
    Then EXIF orientation is applied to the crop
    Then the cropped bitmap is sampled if needed
    Then the cropped texture is uploaded to OpenGL

  Scenario: TextureManager handles invalid crop coordinates
    Given BackgroundConfig has crop (x=-10, y=-10, width=10000, height=10000)
    When TextureManager.loadTexture() is called
    Then invalid coordinates are clamped to image bounds
    And the maximum valid crop region is used
    And no crash occurs
    And a texture is uploaded successfully

  Scenario: TextureManager handles crop larger than image
    Given the image is 1000x1000 pixels
    And BackgroundConfig has crop (x=0, y=0, width=2000, height=2000)
    When TextureManager.loadTexture() is called
    Then crop is clamped to (x=0, y=0, width=1000, height=1000)
    And the full image is used
    And no error occurs

  # Configuration Persistence

  Scenario: Crop coordinates persist across app restarts
    Given the user cropped an image to (150, 300, 600, 1067)
    When the configuration is saved
    And the app is restarted
    And SettingsActivity loads the configuration
    Then the crop coordinates are loaded correctly
    And the preview shows the cropped region
    And the wallpaper renders with the crop applied

  Scenario: Crop coordinates are included in JSON
    Given BackgroundConfig with crop (100, 200, 540, 960)
    When ConfigManager serializes to JSON
    Then the JSON contains "crop" object
    And the JSON contains "x": 100
    And the JSON contains "y": 200
    And the JSON contains "width": 540
    And the JSON contains "height": 960

  Scenario: Missing crop field in JSON defaults to null
    Given a JSON configuration without "crop" field
    When ConfigManager deserializes the JSON
    Then BackgroundConfig.crop is null
    And TextureManager loads the full image
    And no error occurs

  # Wallpaper Rendering with Crop

  Scenario: Wallpaper renders cropped background
    Given configuration with crop (200, 400, 1080, 1920)
    When the live wallpaper service starts
    And GLRenderer loads the background texture
    Then TextureManager applies the crop
    And the cropped region fills the screen
    And no distortion occurs
    And the crop matches the preview

  Scenario: Wallpaper updates when crop changes
    Given the wallpaper is active with a cropped background
    When the user opens settings
    And selects a new crop region
    And applies the wallpaper
    Then the wallpaper service reloads
    And the new crop region is displayed
    And the old texture is released

  # Image Formats and Edge Cases

  Scenario: Crop works with portrait images
    Given a portrait image (1080x1920)
    When the user crops a 540x960 region
    Then the crop is applied correctly
    And the aspect ratio is preserved
    And the wallpaper displays without distortion

  Scenario: Crop works with landscape images
    Given a landscape image (1920x1080)
    When the user crops a 1080x1920 region (portrait)
    Then the crop is applied correctly
    And the wallpaper is portrait despite source being landscape
    And no rotation issues occur

  Scenario: Crop works with square images
    Given a square image (2000x2000)
    When the user crops a 1080x1920 region
    Then the crop is applied correctly
    And the user can position it anywhere in the square

  Scenario: Crop works with very large images
    Given a 4000x6000 pixel image
    When the user selects a crop region
    Then the image is sampled before cropping
    And the crop is applied to the sampled bitmap
    And no OutOfMemoryError occurs
    And the quality is acceptable

  Scenario: Crop works with small images
    Given a 540x960 pixel image (exact screen size)
    When the user tries to crop
    Then the overlay fills the entire image
    And the user can still crop a smaller region
    Or the user can use the full image

  Scenario: Crop handles EXIF rotation
    Given an image with EXIF rotation 90°
    When ImageCropActivity displays the image
    Then the image is rotated correctly
    When the user crops the rotated image
    Then the crop coordinates are in the rotated space
    And TextureManager applies rotation before cropping
    And the final wallpaper orientation is correct

  # Error Handling

  Scenario: ImageCropActivity handles missing image URI
    Given ImageCropActivity is launched
    And no image URI is provided in the intent
    When onCreate() executes
    Then an error is logged
    And the activity finishes immediately
    And RESULT_CANCELED is returned

  Scenario: ImageCropActivity handles invalid image URI
    Given ImageCropActivity is launched with URI "content://invalid/123"
    When the activity tries to load the image
    Then a FileNotFoundException is caught
    And an error message is shown to the user
    And the activity finishes
    And RESULT_CANCELED is returned

  Scenario: ImageCropActivity handles corrupted image
    Given an image file is corrupted
    When ImageCropActivity tries to decode it
    Then BitmapFactory returns null
    And an error message is shown
    And the activity finishes gracefully

  Scenario: TextureManager handles crop after image rotation
    Given an image with EXIF rotation
    And crop coordinates in the original orientation
    When TextureManager loads the image
    Then EXIF rotation is applied first
    Then crop coordinates are adjusted for the rotation
    And the cropped texture is correct

  # Performance

  Scenario: Crop activity loads large images efficiently
    Given a 12MP image (4000x3000)
    When ImageCropActivity loads the image
    Then the image is sampled to fit the screen
    And memory usage is under 50MB
    And the activity loads in under 2 seconds
    And the UI is responsive

  Scenario: Crop overlay responds smoothly to touch
    Given the crop overlay is enabled
    When the user drags or resizes the overlay
    Then the UI updates at 60fps
    And there is no lag or stuttering
    And touch feedback is immediate

  Scenario: Crop confirmation is fast
    Given the user taps "Done"
    When ImageCropActivity prepares the result
    Then the crop coordinates are calculated instantly
    And the activity finishes in under 100ms
    And no additional bitmap processing occurs

  # Accessibility

  Scenario: Crop overlay is visible against all images
    Given any background image (light or dark)
    When the crop overlay is displayed
    Then the overlay border is clearly visible
    And corner handles are easy to see
    And the overlay has sufficient contrast

  Scenario: Crop handles are large enough to tap
    Given the crop overlay is displayed
    When the user tries to tap a corner handle
    Then the touch target is at least 48x48dp
    And the handle is easy to grab
    And accidental taps are minimized

  # User Experience

  Scenario: Crop activity provides visual feedback
    Given the crop overlay is enabled
    When the user drags a handle
    Then the overlay border highlights
    And the dimensions are shown in real-time
    And the user knows what area will be cropped

  Scenario: Crop activity has clear action buttons
    Given ImageCropActivity is displayed
    Then a "Done" button is visible
    And a "Cancel" button is visible
    And a "Crop" button toggles the overlay
    And button labels are clear

  Scenario: User can reset crop to full image
    Given the user has positioned a crop region
    When the user taps "Reset" button
    Then the crop overlay is removed
    And the full image is used
    And the user can start over

  # Integration with Existing Components

  Scenario: Crop integrates with ConfigManager
    Given a BackgroundConfig with crop coordinates
    When ConfigManager.saveConfig() is called
    Then the crop is serialized to JSON
    When ConfigManager.loadConfig() is called
    Then the crop is deserialized correctly
    And CropRect object is reconstructed

  Scenario: Crop integrates with GLRenderer
    Given GLRenderer is rendering a frame
    When the background texture has crop applied
    Then the texture UV coordinates are 0.0-1.0
    And the texture fills the screen
    And aspect ratio is correct

  Scenario: Crop integrates with WallpaperService
    Given the live wallpaper is active
    When the background has crop coordinates
    Then TextureManager applies the crop on startup
    And the wallpaper displays correctly
    And no recropping occurs on each frame
