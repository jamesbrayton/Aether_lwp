Feature: Rain Shader Effect
  As a wallpaper user
  I want a fast-moving rain particle effect
  So that I can have a dynamic rainy atmosphere on my home screen

  Background:
    Given the ShaderRegistry is initialized
    And the OpenGL ES 2.0 context is available

  # Shader Discovery and Metadata

  Scenario: Rain shader is discovered by ShaderRegistry
    Given rain.frag exists in assets/shaders/
    When ShaderRegistry.discoverShaders() is called
    Then a ShaderDescriptor with id "rain" is returned
    And the shader name is "Falling Rain"
    And the shader version is "1.0.0"

  Scenario: Rain shader metadata contains required tags
    Given rain.frag with embedded metadata
    When ShaderMetadataParser parses the file
    Then the @shader tag is "Falling Rain"
    And the @id tag is "rain"
    And the @version tag is "1.0.0"
    And the @author tag is "Aether Team"
    And the @license tag is "MIT"
    And the @description tag contains "rain"
    And the @tags include "weather" and "storm"

  Scenario: Rain shader declares all standard uniforms
    Given rain.frag source code
    When the shader is parsed for uniform declarations
    Then it declares "uniform sampler2D u_backgroundTexture"
    And it declares "uniform float u_time"
    And it declares "uniform vec2 u_resolution"
    And it declares "uniform vec2 u_gyroOffset"
    And it declares "uniform float u_depthValue"

  Scenario: Rain shader metadata defines custom parameters
    Given rain.frag with embedded metadata
    When ShaderMetadataParser extracts parameters
    Then 4 parameters are defined
    And parameter "u_particleCount" has type float, default 100.0, min 50.0, max 150.0
    And parameter "u_speed" has type float, default 2.0, min 1.0, max 3.0
    And parameter "u_angle" has type float, default 70.0, min 60.0, max 80.0
    And parameter "u_streakLength" has type float, default 0.03, min 0.01, max 0.05

  Scenario: Rain shader parameter metadata includes display names
    Given rain.frag parameter definitions
    When parameter metadata is parsed
    Then "u_particleCount" has display name "Raindrop Count"
    And "u_speed" has display name "Fall Speed"
    And "u_angle" has display name "Rain Angle"
    And "u_streakLength" has display name "Streak Length"

  Scenario: Rain shader parameter metadata includes descriptions
    Given rain.frag parameter definitions
    When parameter metadata is parsed
    Then "u_particleCount" has description containing "rain streaks"
    And "u_speed" has description containing "fast"
    And "u_angle" has description containing "angle"
    And "u_streakLength" has description containing "length"

  # Shader Compilation

  Scenario: Rain shader compiles without errors
    Given rain.frag loaded from assets
    And vertex_shader.vert loaded from assets
    When ShaderLoader compiles both shaders
    Then no compilation errors occur
    And a valid shader program ID > 0 is returned

  Scenario: Rain shader metadata comments are ignored by GLSL compiler
    Given rain.frag with JavaDoc-style metadata comments
    When the GLSL compiler processes the shader
    Then the metadata comments are stripped during preprocessing
    And the shader compiles successfully
    And no syntax errors related to comments occur

  Scenario: Rain shader program can be linked and activated
    Given a compiled rain shader program
    When the program is linked
    And glUseProgram() is called with the program ID
    Then no OpenGL errors occur
    And the program is active

  # Uniform Locations

  Scenario: Rain shader standard uniforms are accessible
    Given a compiled and linked rain shader program
    When glGetUniformLocation() is called for standard uniforms
    Then "u_backgroundTexture" location is >= 0
    And "u_time" location is >= 0
    And "u_resolution" location is >= 0
    And "u_gyroOffset" location is >= 0
    And "u_depthValue" location is >= 0

  Scenario: Rain shader custom parameter uniforms are accessible
    Given a compiled and linked rain shader program
    When glGetUniformLocation() is called for custom parameters
    Then "u_particleCount" location is >= 0
    And "u_speed" location is >= 0
    And "u_angle" location is >= 0
    And "u_streakLength" location is >= 0

  # Rendering Behavior

  Scenario: Rain shader renders without OpenGL errors
    Given a GLRenderer configured with rain shader
    And a background texture loaded
    When GLRenderer.onDrawFrame() is called
    And standard uniforms are set
    And custom parameters are set to defaults
    Then no OpenGL errors occur
    And the frame renders successfully

  Scenario: Rain particles fall diagonally over time
    Given rain shader rendering with default angle 70 degrees
    When u_time = 0.0
    Then particles are at initial positions
    When u_time = 1.0
    Then particles have moved diagonally downward
    And movement is both downward and sideways

  Scenario: Rain particles fall faster than snow
    Given rain shader with u_speed = 2.0
    And snow shader with u_speed = 1.0
    When both render for 1 second
    Then rain particles travel twice the distance of snow particles
    And rain effect appears more dynamic

  Scenario: Rain particles have elongated streak appearance
    Given rain shader rendering
    When a rain particle is drawn
    Then the particle has elongated shape
    And the streak follows the rain direction
    And the streak length is controlled by u_streakLength parameter

  Scenario: Rain particles wrap around screen
    Given rain shader rendering
    And a particle has fallen off the screen (beyond screen bounds)
    When the shader calculates the next frame
    Then the particle reappears at the starting edge
    And animation continues seamlessly

  Scenario: Rain angle affects particle direction
    Given rain shader with u_angle = 70.0
    When particles are animated
    Then particles fall at 70-degree angle from vertical
    When u_angle is changed to 80.0
    Then particles fall at steeper 80-degree angle
    And the angle change is visually apparent

  # Parameter Behavior

  Scenario: Rain particle count affects number of visible streaks
    Given rain shader with u_particleCount = 50
    When the shader renders
    Then approximately 50 rain streaks are visible
    When u_particleCount is changed to 150
    Then approximately 150 rain streaks are visible

  Scenario: Rain fall speed controls animation rate
    Given rain shader with u_speed = 1.0
    When u_time increases by 1.0 second
    Then particles travel a baseline distance
    Given u_speed = 2.0
    When u_time increases by 1.0 second
    Then particles travel twice the baseline distance
    Given u_speed = 3.0
    Then particles travel three times the baseline distance

  Scenario: Rain angle controls diagonal direction
    Given rain shader with u_angle = 60.0
    Then particles fall at shallow 60-degree angle (more horizontal)
    Given u_angle = 70.0
    Then particles fall at moderate 70-degree angle
    Given u_angle = 80.0
    Then particles fall at steep 80-degree angle (more vertical)

  Scenario: Streak length controls motion blur effect
    Given rain shader with u_streakLength = 0.01
    Then rain streaks are short (minimal blur)
    Given u_streakLength = 0.03
    Then rain streaks are medium length
    Given u_streakLength = 0.05
    Then rain streaks are long (pronounced blur)

  # Performance

  Scenario: Rain shader maintains 60fps with default parameters
    Given rain shader with u_particleCount = 100
    When rendering continuously for 100 frames
    Then average frame time is <= 16.67ms
    And no frames drop below 60fps

  Scenario: Rain shader handles maximum particle count
    Given rain shader with u_particleCount = 150
    When rendering continuously for 60 frames
    Then average frame time is <= 16.67ms
    And rendering remains smooth

  Scenario: Rain shader with fast speed maintains performance
    Given rain shader with u_speed = 3.0
    And u_particleCount = 150
    When rendering continuously for 60 frames
    Then average frame time is <= 16.67ms
    And no performance degradation occurs

  # Integration with Background

  Scenario: Rain shader composites with background texture
    Given a background texture loaded
    And rain shader rendering
    When the shader samples u_backgroundTexture
    Then the background is visible
    And rain streaks are rendered on top
    And alpha blending is correct

  Scenario: Rain shader works with placeholder background
    Given no background image selected
    And a 1x1 placeholder texture is loaded
    When rain shader renders
    Then no OpenGL errors occur
    And rain streaks are visible against solid background

  # Integration with GLRenderer

  Scenario: Rain shader integrates with GLRenderer
    Given GLRenderer initialized with rain shader
    When GLRenderer.onSurfaceCreated() is called
    Then the rain shader is loaded and compiled
    And all uniforms are set correctly
    When GLRenderer.onDrawFrame() is called
    Then the rain effect renders
    And u_time is updated each frame

  Scenario: Rain shader receives correct resolution uniform
    Given GLRenderer with viewport 1080x1920
    When u_resolution uniform is set
    Then u_resolution.x = 1080.0
    And u_resolution.y = 1920.0
    And rain streaks scale correctly to screen size

  # Visual Quality

  Scenario: Rain streaks have motion blur appearance
    Given rain shader rendering
    When a rain streak is drawn
    Then the streak has elongated shape
    And the streak follows the angle direction
    And edges are smooth (not pixelated)

  Scenario: Rain has blue-white tint
    Given rain shader rendering with default parameters
    When rain color is sampled
    Then rain particles have slight blue tint
    And color approximates vec3(0.7, 0.8, 1.0)
    And the tint enhances rainy atmosphere

  Scenario: Rain streaks are thin and fast
    Given rain shader with default parameters
    When rain is rendered
    Then streaks are thin (narrow width)
    And streaks move quickly across screen
    And the effect appears dynamic and intense

  # Edge Cases

  Scenario: Rain shader handles zero particles
    Given rain shader with u_particleCount = 0.0
    When the shader renders
    Then no rain streaks are visible
    And no OpenGL errors occur

  Scenario: Rain shader handles minimum speed
    Given rain shader with u_speed = 1.0
    When the shader renders
    Then rain falls at slower pace
    And animation is smooth

  Scenario: Rain shader handles maximum speed
    Given rain shader with u_speed = 3.0
    When the shader renders
    Then rain falls very quickly
    And animation is smooth
    And no visual artifacts occur

  Scenario: Rain shader handles minimum angle
    Given rain shader with u_angle = 60.0
    When the shader renders
    Then rain falls at shallow angle
    And particles have more horizontal movement

  Scenario: Rain shader handles maximum angle
    Given rain shader with u_angle = 80.0
    When the shader renders
    Then rain falls at steep angle
    And particles are nearly vertical

  Scenario: Rain shader handles minimum streak length
    Given rain shader with u_streakLength = 0.01
    When the shader renders
    Then rain streaks are very short
    And motion blur is minimal

  Scenario: Rain shader handles maximum streak length
    Given rain shader with u_streakLength = 0.05
    When the shader renders
    Then rain streaks are long
    And motion blur is pronounced

  Scenario: Rain shader handles time overflow
    Given rain shader rendering continuously
    When u_time exceeds 10000.0 seconds
    Then the shader continues to render correctly
    And no precision issues occur

  # Contrast with Snow

  Scenario: Rain is visually distinct from snow
    Given rain shader and snow shader
    When both are rendered side-by-side
    Then rain moves significantly faster than snow
    And rain has diagonal motion vs snow's vertical motion
    And rain has streaks vs snow's circular particles
    And rain has blue tint vs snow's white color
    And the effects are clearly different
