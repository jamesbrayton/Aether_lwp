Feature: OpenGL ES Renderer
  As the live wallpaper engine
  I want to render fullscreen quad with GLSL shaders
  So that particle effects can be displayed smoothly at 60fps

  Background:
    Given an OpenGL ES 2.0 context is available
    And ShaderLoader is initialized
    And ShaderRegistry has discovered test shaders

  Scenario: Initialize OpenGL surface
    Given a GLRenderer is created
    When onSurfaceCreated is called
    Then the OpenGL state is initialized
    And no OpenGL errors are reported
    And shaders are loaded and compiled
    And a fullscreen quad vertex buffer is created

  Scenario: Handle surface size changes
    Given a GLRenderer with initialized surface
    When onSurfaceChanged is called with width=1080 and height=1920
    Then the viewport is set to 1080x1920
    And the u_resolution uniform is updated to vec2(1080, 1920)
    And no OpenGL errors are reported

  Scenario: Render frame with standard uniforms
    Given a GLRenderer with initialized surface
    And a shader program is active
    When onDrawFrame is called
    Then u_time uniform is set to elapsed time in seconds
    And u_resolution uniform is set to screen size
    And u_backgroundTexture uniform is bound to texture unit 0
    And u_gyroOffset uniform is set to vec2(0.0, 0.0)
    And u_depthValue uniform is set to 0.0
    And a fullscreen quad is rendered
    And no OpenGL errors are reported

  Scenario: Render fullscreen quad
    Given a GLRenderer with initialized surface
    When onDrawFrame is called
    Then two triangles are drawn forming a fullscreen quad
    And vertices cover the entire screen from (-1,-1) to (1,1)
    And fragment shader runs for every pixel on screen

  Scenario: Track frame timing
    Given a GLRenderer with initialized surface
    When onDrawFrame is called multiple times
    Then u_time increases with each frame
    And frame delta time is measured
    And rendering maintains 60fps target
    And frame time is less than 16.67ms on average

  Scenario: Load shader via ShaderLoader
    Given a GLRenderer is created
    When onSurfaceCreated is called
    Then ShaderLoader loads vertex_shader.vert
    And ShaderLoader loads test.frag
    And shaders are compiled into a program
    And the program is validated

  Scenario: Integrate with ShaderRegistry
    Given ShaderRegistry has discovered test shader
    When GLRenderer initializes with shader ID "test"
    Then the shader descriptor is retrieved from registry
    And the shader is loaded from assets
    And standard uniforms are available
    And custom parameters from metadata are accessible

  Scenario: Handle missing shader
    Given ShaderRegistry does not have shader ID "missing"
    When GLRenderer attempts to initialize with "missing"
    Then an exception is thrown
    And the error message indicates shader not found

  Scenario: Handle shader compilation failure
    Given an invalid shader source
    When GLRenderer attempts to compile the shader
    Then a ShaderCompilationException is thrown
    And the error log contains GLSL error details
    And rendering falls back to a safe state

  Scenario: Update uniforms on each frame
    Given a GLRenderer rendering at 60fps
    When 60 frames are rendered
    Then u_time progresses by approximately 1.0 second
    And each frame has unique time value
    And time never decreases between frames

  Scenario: Cleanup resources on surface destruction
    Given a GLRenderer with active resources
    When the surface is destroyed
    Then shader program is deleted
    And vertex buffer is deleted
    And texture resources are released
    And no memory leaks occur

  Scenario: Verify fullscreen quad vertices
    Given a GLRenderer with initialized vertex buffer
    When vertex data is queried
    Then vertices form two triangles:
      | Triangle | Vertex 1    | Vertex 2   | Vertex 3   |
      | 1        | (-1, -1, 0) | (1, -1, 0) | (-1, 1, 0) |
      | 2        | (-1, 1, 0)  | (1, -1, 0) | (1, 1, 0)  |

  Scenario: Set background texture uniform
    Given a GLRenderer with a background texture
    When onDrawFrame is called
    Then texture is bound to GL_TEXTURE0
    And u_backgroundTexture uniform is set to 0
    And texture sampling works correctly in shader

  Scenario: Maintain render state consistency
    Given a GLRenderer rendering continuously
    When 100 frames are rendered
    Then all frames use the same shader program
    And uniform values are consistent
    And no OpenGL state leaks occur
    And GL error state remains GL_NO_ERROR
