Feature: Snow Shader Effect
  As a wallpaper user
  I want a falling snow particle effect
  So that I can have a peaceful winter atmosphere on my home screen

  Background:
    Given the ShaderRegistry is initialized
    And the OpenGL ES 2.0 context is available

  # Shader Discovery and Metadata

  Scenario: Snow shader is discovered by ShaderRegistry
    Given snow.frag exists in assets/shaders/
    When ShaderRegistry.discoverShaders() is called
    Then a ShaderDescriptor with id "snow" is returned
    And the shader name is "Falling Snow"
    And the shader version is "1.0.0"

  Scenario: Snow shader metadata contains required tags
    Given snow.frag with embedded metadata
    When ShaderMetadataParser parses the file
    Then the @shader tag is "Falling Snow"
    And the @id tag is "snow"
    And the @version tag is "1.0.0"
    And the @author tag is "Aether Team"
    And the @license tag is "MIT"
    And the @description tag contains "falling snow"
    And the @tags include "winter" and "weather"

  Scenario: Snow shader declares all standard uniforms
    Given snow.frag source code
    When the shader is parsed for uniform declarations
    Then it declares "uniform sampler2D u_backgroundTexture"
    And it declares "uniform float u_time"
    And it declares "uniform vec2 u_resolution"
    And it declares "uniform vec2 u_gyroOffset"
    And it declares "uniform float u_depthValue"

  Scenario: Snow shader metadata defines custom parameters
    Given snow.frag with embedded metadata
    When ShaderMetadataParser extracts parameters
    Then 3 parameters are defined
    And parameter "u_particleCount" has type float, default 100.0, min 10.0, max 200.0
    And parameter "u_speed" has type float, default 1.0, min 0.1, max 3.0
    And parameter "u_driftAmount" has type float, default 0.5, min 0.0, max 1.0

  Scenario: Snow shader parameter metadata includes display names
    Given snow.frag parameter definitions
    When parameter metadata is parsed
    Then "u_particleCount" has display name "Particle Count"
    And "u_speed" has display name "Fall Speed"
    And "u_driftAmount" has display name "Lateral Drift"

  Scenario: Snow shader parameter metadata includes descriptions
    Given snow.frag parameter definitions
    When parameter metadata is parsed
    Then "u_particleCount" has description "Number of visible snow particles"
    And "u_speed" has description "How fast snow falls"
    And "u_driftAmount" has description "Amount of side-to-side wobble"

  # Shader Compilation

  Scenario: Snow shader compiles without errors
    Given snow.frag loaded from assets
    And vertex_shader.vert loaded from assets
    When ShaderLoader compiles both shaders
    Then no compilation errors occur
    And a valid shader program ID > 0 is returned

  Scenario: Snow shader metadata comments are ignored by GLSL compiler
    Given snow.frag with JavaDoc-style metadata comments
    When the GLSL compiler processes the shader
    Then the metadata comments are stripped during preprocessing
    And the shader compiles successfully
    And no syntax errors related to comments occur

  Scenario: Snow shader program can be linked and activated
    Given a compiled snow shader program
    When the program is linked
    And glUseProgram() is called with the program ID
    Then no OpenGL errors occur
    And the program is active

  # Uniform Locations

  Scenario: Snow shader standard uniforms are accessible
    Given a compiled and linked snow shader program
    When glGetUniformLocation() is called for standard uniforms
    Then "u_backgroundTexture" location is >= 0
    And "u_time" location is >= 0
    And "u_resolution" location is >= 0
    And "u_gyroOffset" location is >= 0
    And "u_depthValue" location is >= 0

  Scenario: Snow shader custom parameter uniforms are accessible
    Given a compiled and linked snow shader program
    When glGetUniformLocation() is called for custom parameters
    Then "u_particleCount" location is >= 0
    And "u_speed" location is >= 0
    And "u_driftAmount" location is >= 0

  # Rendering Behavior

  Scenario: Snow shader renders without OpenGL errors
    Given a GLRenderer configured with snow shader
    And a background texture loaded
    When GLRenderer.onDrawFrame() is called
    And standard uniforms are set
    And custom parameters are set to defaults
    Then no OpenGL errors occur
    And the frame renders successfully

  Scenario: Snow particles fall downward over time
    Given snow shader rendering with default parameters
    When u_time = 0.0
    Then particles are at initial Y positions
    When u_time = 1.0
    Then particles have moved downward
    And particle Y positions have decreased

  Scenario: Snow particles wrap from bottom to top
    Given snow shader rendering
    And a particle has fallen below the screen (Y < 0.0)
    When the shader calculates the next frame
    Then the particle reappears at the top of the screen (Y wraps to 1.0)
    And animation continues seamlessly

  Scenario: Snow particles exhibit lateral drift
    Given snow shader with u_driftAmount = 0.5
    When particles are animated over time
    Then particle X positions oscillate side-to-side
    And drift follows a sine wave pattern
    And drift amplitude is proportional to u_driftAmount

  Scenario: Snow particles have no lateral drift when u_driftAmount = 0.0
    Given snow shader with u_driftAmount = 0.0
    When particles are animated
    Then particle X positions remain constant
    And no lateral movement occurs

  # Parameter Behavior

  Scenario: Snow particle count affects number of visible particles
    Given snow shader with u_particleCount = 50
    When the shader renders
    Then approximately 50 particles are visible
    When u_particleCount is changed to 150
    Then approximately 150 particles are visible

  Scenario: Snow fall speed controls animation rate
    Given snow shader with u_speed = 1.0
    When u_time increases by 1.0 second
    Then particles fall a baseline distance
    Given u_speed = 2.0
    When u_time increases by 1.0 second
    Then particles fall twice the baseline distance

  Scenario: Snow drift amount controls lateral movement amplitude
    Given snow shader with u_driftAmount = 0.0
    Then particle X offset is 0.0
    Given u_driftAmount = 0.5
    Then particle X offset is moderate
    Given u_driftAmount = 1.0
    Then particle X offset is maximum

  # Performance

  Scenario: Snow shader maintains 60fps with default parameters
    Given snow shader with u_particleCount = 100
    When rendering continuously for 100 frames
    Then average frame time is <= 16.67ms
    And no frames drop below 60fps

  Scenario: Snow shader handles maximum particle count
    Given snow shader with u_particleCount = 200
    When rendering continuously for 60 frames
    Then average frame time is <= 16.67ms
    And rendering remains smooth

  # Integration with Background

  Scenario: Snow shader composites with background texture
    Given a background texture loaded
    And snow shader rendering
    When the shader samples u_backgroundTexture
    Then the background is visible
    And snow particles are rendered on top
    And alpha blending is correct

  Scenario: Snow shader works with placeholder background
    Given no background image selected
    And a 1x1 placeholder texture is loaded
    When snow shader renders
    Then no OpenGL errors occur
    And snow particles are visible against solid background

  # Integration with GLRenderer

  Scenario: Snow shader integrates with GLRenderer
    Given GLRenderer initialized with snow shader
    When GLRenderer.onSurfaceCreated() is called
    Then the snow shader is loaded and compiled
    And all uniforms are set correctly
    When GLRenderer.onDrawFrame() is called
    Then the snow effect renders
    And u_time is updated each frame

  Scenario: Snow shader receives correct resolution uniform
    Given GLRenderer with viewport 1080x1920
    When u_resolution uniform is set
    Then u_resolution.x = 1080.0
    And u_resolution.y = 1920.0
    And particles scale correctly to screen size

  # Edge Cases

  Scenario: Snow shader handles zero particles
    Given snow shader with u_particleCount = 0.0
    When the shader renders
    Then no particles are visible
    And no OpenGL errors occur

  Scenario: Snow shader handles minimum speed
    Given snow shader with u_speed = 0.1
    When the shader renders
    Then particles fall very slowly
    And animation is smooth

  Scenario: Snow shader handles maximum speed
    Given snow shader with u_speed = 3.0
    When the shader renders
    Then particles fall very quickly
    And animation is smooth

  Scenario: Snow shader handles time overflow
    Given snow shader rendering continuously
    When u_time exceeds 10000.0 seconds
    Then the shader continues to render correctly
    And no precision issues occur

  # Visual Quality

  Scenario: Snow particles have soft edges
    Given snow shader rendering
    When a particle is drawn
    Then the particle has alpha blending
    And edges are smoothed with smoothstep
    And particles appear soft and gentle

  Scenario: Snow particles are small and subtle
    Given snow shader with default parameters
    When particles are rendered
    Then particle size is approximately 0.003 normalized units
    And particles are not overly prominent
    And the effect is gentle and peaceful
