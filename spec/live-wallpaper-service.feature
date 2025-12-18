Feature: Live Wallpaper Service
  As an Android user
  I want the Aether wallpaper to run as a live wallpaper
  So that I can see animated particle effects on my home screen

  Background:
    Given the AetherWallpaperService is registered in AndroidManifest
    And wallpaper.xml metadata is configured
    And ConfigManager has a saved configuration

  # Service Registration and Metadata

  Scenario: Wallpaper service is registered in manifest
    Given the AndroidManifest.xml
    When the manifest is parsed
    Then a service with name "AetherWallpaperService" is declared
    And the service has permission "android.permission.BIND_WALLPAPER"
    And the service has action "android.service.wallpaper.WallpaperService"
    And the service has metadata pointing to wallpaper.xml

  Scenario: Wallpaper metadata is configured
    Given the wallpaper.xml file
    When the metadata is parsed
    Then it contains a description
    And it points to SettingsActivity
    And it has a thumbnail image

  # Service Lifecycle

  Scenario: Service starts when wallpaper is activated
    Given the user sets Aether as live wallpaper
    When the system calls onCreateEngine()
    Then an AetherEngine instance is created
    And the engine is returned to the system
    And no errors occur

  Scenario: Engine initializes OpenGL context
    Given an AetherEngine is created
    When onCreate() is called
    Then a GLSurfaceView is created
    And OpenGL ES 2.0 context is requested
    And the renderer is created
    And no errors occur

  Scenario: Engine loads configuration on startup
    Given a saved wallpaper configuration exists
    When the engine is created
    Then ConfigManager.loadConfig() is called
    And the configuration is loaded successfully
    And layers are extracted from configuration

  Scenario: Engine uses default config when none exists
    Given no saved configuration exists
    When the engine is created
    Then ConfigManager.getDefaultConfig() is used
    And default configuration is loaded
    And the engine initializes successfully

  # Shader Loading and Rendering

  Scenario: Engine loads shaders from configuration
    Given configuration with 2 layers (snow, rain)
    When the engine initializes rendering
    Then ShaderRegistry discovers available shaders
    And snow shader is loaded for layer 0
    And rain shader is loaded for layer 1
    And both shaders compile successfully

  Scenario: Engine sets shader parameters from configuration
    Given snow layer with custom parameters
      | parameter       | value |
      | u_particleCount | 150   |
      | u_speed         | 1.5   |
      | u_driftAmount   | 0.7   |
    When the renderer sets uniforms
    Then u_particleCount is set to 150
    And u_speed is set to 1.5
    And u_driftAmount is set to 0.7

  Scenario: Engine renders first layer
    Given configuration with snow layer
    When GLRenderer.onDrawFrame() is called
    Then the snow shader program is active
    And standard uniforms are set
    And custom parameters are set
    And the frame renders without errors

  Scenario: Engine renders multiple layers (future Phase 2)
    Given configuration with 2 layers
    When rendering a frame
    Then background is rendered first
    And layer 0 is rendered
    And layer 1 is rendered
    And layers composite correctly
    # Note: Multi-layer compositing deferred to Phase 2

  # Background Texture Loading

  Scenario: Engine loads background image from configuration
    Given configuration with background URI
    When the engine initializes
    Then TextureManager.loadTexture() is called with the URI
    And the texture is loaded successfully
    And the texture is bound for rendering

  Scenario: Engine uses placeholder when no background configured
    Given configuration with no background image
    When the engine initializes
    Then TextureManager.createPlaceholderTexture() is called
    And a solid color placeholder is created
    And rendering proceeds without errors

  Scenario: Engine handles invalid background URI
    Given configuration with invalid background URI
    When the engine attempts to load the texture
    Then TextureManager returns null or throws exception
    And the engine falls back to placeholder texture
    And rendering continues successfully

  # Rendering Loop

  Scenario: Engine renders continuously at 60fps
    Given the wallpaper is visible on home screen
    When the render loop runs
    Then frames are rendered at approximately 60fps
    And u_time uniform increases each frame
    And animation is smooth

  Scenario: Engine updates time uniform each frame
    Given rendering is active
    When frame 0 is rendered at t=0.0s
    Then u_time is set to 0.0
    When frame 1 is rendered at t=0.016s
    Then u_time is set to 0.016
    And time progresses monotonically

  Scenario: Engine sets resolution uniform correctly
    Given screen resolution is 1080x1920
    When the surface changes
    Then u_resolution is set to (1080.0, 1920.0)
    And the viewport is set to 1080x1920
    And rendering scales correctly

  # Visibility Handling

  Scenario: Engine pauses rendering when not visible
    Given the wallpaper is rendering
    When onVisibilityChanged(false) is called
    Then GLSurfaceView.onPause() is called
    And rendering stops
    And resources are preserved

  Scenario: Engine resumes rendering when visible
    Given the wallpaper is paused
    When onVisibilityChanged(true) is called
    Then GLSurfaceView.onResume() is called
    And rendering resumes
    And animation continues from previous state

  Scenario: Engine handles screen off event
    Given the wallpaper is rendering
    When the screen turns off
    Then onVisibilityChanged(false) is called
    And rendering is paused
    And battery consumption is reduced

  Scenario: Engine handles screen on event
    Given the wallpaper is paused
    When the screen turns on
    Then onVisibilityChanged(true) is called
    And rendering resumes
    And the wallpaper is displayed

  # Configuration Changes

  Scenario: Engine reloads on configuration change
    Given the wallpaper is running with old configuration
    When the user changes settings in SettingsActivity
    And the configuration is saved
    And the user applies the wallpaper again
    Then the engine is recreated
    And the new configuration is loaded
    And new parameters are applied

  Scenario: Engine handles surface size change (rotation)
    Given the wallpaper is rendering in portrait
    When the device rotates to landscape
    Then onSurfaceChanged() is called with new dimensions
    And u_resolution is updated
    And the viewport is resized
    And rendering adapts to new aspect ratio

  # Resource Management

  Scenario: Engine releases resources on destroy
    Given the wallpaper is running
    When onDestroy() is called
    Then GLSurfaceView is destroyed
    And OpenGL resources are released
    And textures are freed
    And no memory leaks occur

  Scenario: Engine releases textures properly
    Given a background texture is loaded
    When the engine is destroyed
    Then TextureManager.release() is called
    And the texture is deleted from GPU
    And memory is freed

  Scenario: Engine releases shader programs properly
    Given shader programs are compiled
    When the engine is destroyed
    Then shader programs are deleted
    And GPU memory is freed

  # Performance

  Scenario: Engine maintains 60fps with one layer
    Given configuration with snow layer (100 particles)
    When rendering continuously for 60 frames
    Then average frame time is <= 16.67ms
    And no frame drops occur
    And the wallpaper is smooth

  Scenario: Engine maintains 60fps with two layers
    Given configuration with snow and rain layers
    When rendering continuously for 60 frames
    Then average frame time is <= 16.67ms
    And frame rate remains at 60fps
    And animation is smooth

  Scenario: Engine uses GPU efficiently
    Given the wallpaper is rendering
    When performance is measured
    Then GPU usage is moderate
    And CPU usage is minimal (procedural shaders)
    And battery drain is acceptable

  # Error Handling

  Scenario: Engine handles missing shader gracefully
    Given configuration references shader "missing"
    When the engine attempts to load the shader
    Then ShaderRegistry returns null
    And the engine logs an error
    And skips the missing layer
    And other layers render successfully

  Scenario: Engine handles shader compilation error
    Given a shader file with invalid GLSL syntax
    When ShaderLoader attempts to compile
    Then a ShaderCompilationException is thrown
    And the error is logged with GLSL error message
    And the engine continues without the failed shader

  Scenario: Engine handles OpenGL errors gracefully
    Given rendering is active
    When an OpenGL error occurs
    Then the error is detected via glGetError()
    And the error is logged
    And rendering continues if possible
    And the service does not crash

  # Integration with Other Components

  Scenario: Engine integrates with ConfigManager
    Given ConfigManager has a saved configuration
    When the engine loads the config
    Then all layer configurations are loaded
    And parameter values are retrieved from params map
    And background configuration is loaded

  Scenario: Engine integrates with ShaderRegistry
    Given ShaderRegistry has discovered shaders
    When the engine needs to load a shader by ID
    Then ShaderRegistry.getShaderById() returns the descriptor
    And the descriptor contains shader metadata
    And the shader file path is available

  Scenario: Engine integrates with ShaderLoader
    Given a shader descriptor from ShaderRegistry
    When the engine compiles the shader
    Then ShaderLoader.createProgram() is called
    And the vertex and fragment shaders are compiled
    And the program is linked
    And a valid program ID is returned

  Scenario: Engine integrates with GLRenderer
    Given the engine has created a GLRenderer
    When frames are rendered
    Then GLRenderer.onSurfaceCreated() initializes OpenGL
    And GLRenderer.onSurfaceChanged() sets viewport
    And GLRenderer.onDrawFrame() renders each frame
    And standard uniforms are set automatically

  Scenario: Engine integrates with TextureManager
    Given a background image URI in configuration
    When the engine loads the background
    Then TextureManager.loadTexture() is called
    And bitmap is decoded with sampling
    And texture is uploaded to GPU
    And texture ID is returned for binding

  # User Experience

  Scenario: Wallpaper displays on home screen
    Given the user has set Aether as live wallpaper
    When the user navigates to the home screen
    Then the background image is displayed
    And particle effects are rendering
    And animation is smooth at 60fps
    And the wallpaper looks as configured in settings

  Scenario: Wallpaper persists across reboots
    Given the wallpaper is configured and active
    When the device is rebooted
    Then the wallpaper service starts automatically
    And the configuration is loaded from SharedPreferences
    And the wallpaper displays as before

  Scenario: Multiple home screens show same wallpaper
    Given the wallpaper is active
    When the user swipes between home screens
    Then the wallpaper renders consistently
    And animation continues smoothly
    And no visual glitches occur

  # Settings Integration

  Scenario: Settings button launches SettingsActivity
    Given the user is in the system wallpaper picker
    When the user selects Aether wallpaper
    And the user taps "Settings"
    Then SettingsActivity is launched
    And the current configuration is loaded
    And the user can modify settings

  Scenario: Configuration changes apply to running wallpaper
    Given the wallpaper is active with snow layer
    When the user opens settings
    And changes u_particleCount from 100 to 150
    And saves the configuration
    And applies the wallpaper again
    Then the wallpaper reloads
    And 150 particles are rendered
    And the change is visible on home screen

  # Disabled Layers

  Scenario: Engine skips disabled layers
    Given configuration with 2 layers
    And layer 0 is enabled
    And layer 1 is disabled
    When the engine renders frames
    Then only layer 0 is rendered
    And layer 1 is skipped
    And no errors occur

  Scenario: Engine renders only enabled layers
    Given configuration with 3 layers
    And only layer 1 is enabled
    When the engine renders
    Then only layer 1 shader is loaded and rendered
    And layers 0 and 2 are skipped

  # Future Phase 2 Features (Deferred)

  Scenario: Engine applies gyroscope parallax (Phase 2)
    Given gyroscope is enabled in global settings
    When the device tilts
    Then gyroscope data is read
    And u_gyroOffset uniform is updated
    And layers shift based on depth values
    And parallax effect is visible

  Scenario: Engine renders multi-layer compositing (Phase 2)
    Given configuration with 3 layers
    When rendering a frame
    Then each layer renders to a framebuffer
    And layers composite with alpha blending
    And per-layer opacity is applied
    And the final composite is displayed

  # Edge Cases

  Scenario: Engine handles zero layers
    Given configuration with no layers
    When the engine renders
    Then only the background is displayed
    And no effects are rendered
    And no errors occur

  Scenario: Engine handles maximum layers
    Given configuration with 5 layers
    When the engine renders
    Then all 5 layers are rendered
    And performance remains acceptable
    And frame rate is maintained

  Scenario: Engine handles very long runtime
    Given the wallpaper has been running for 24 hours
    When u_time exceeds 86400 seconds
    Then animation continues correctly
    And no float precision issues occur
    And rendering remains smooth
