Feature: Shader Loading and Compilation
  As the OpenGL renderer
  I want to load and compile GLSL shaders from assets
  So that I can create shader programs for rendering effects

  Background:
    Given the Android application context is available
    And the assets/shaders/ directory exists

  Scenario: Load valid vertex shader from assets
    Given a valid vertex shader file "vertex_shader.vert" exists in assets/shaders/
    When I load the shader from assets
    Then the shader source code is returned as a string
    And the source code contains "attribute vec4 a_position"

  Scenario: Load shader with embedded metadata comments
    Given a shader file "test.frag" with JavaDoc-style metadata comments exists
    When I load the shader from assets
    Then the shader source code includes the metadata comments
    And the source code is valid GLSL

  Scenario: Compile valid vertex shader
    Given valid vertex shader GLSL source code
    When I compile the shader as GL_VERTEX_SHADER
    Then a valid shader ID greater than 0 is returned
    And no OpenGL errors are reported

  Scenario: Compile valid fragment shader
    Given valid fragment shader GLSL source code
    When I compile the shader as GL_FRAGMENT_SHADER
    Then a valid shader ID greater than 0 is returned
    And no OpenGL errors are reported

  Scenario: Compile shader with metadata comments
    Given a fragment shader with embedded metadata comments
    When I compile the shader as GL_FRAGMENT_SHADER
    Then the GLSL compiler ignores the metadata comments
    And the shader compiles successfully
    And a valid shader ID greater than 0 is returned

  Scenario: Handle shader compilation error
    Given invalid GLSL source code with syntax errors
    When I attempt to compile the shader
    Then a ShaderCompilationException is thrown
    And the exception message contains the GLSL compilation error log
    And the exception indicates whether it was a vertex or fragment shader

  Scenario: Link vertex and fragment shaders into program
    Given a compiled vertex shader ID
    And a compiled fragment shader ID
    When I link them into a shader program
    Then a valid program ID greater than 0 is returned
    And no OpenGL errors are reported

  Scenario: Handle shader linking error
    Given incompatible vertex and fragment shaders
    When I attempt to link them into a program
    Then a ShaderCompilationException is thrown
    And the exception message contains the linking error log

  Scenario: Load missing shader file
    Given a shader file "missing.frag" does not exist in assets
    When I attempt to load the shader from assets
    Then an IOException is thrown
    And the exception message indicates the file was not found

  Scenario: Create complete shader program from asset files
    Given "vertex_shader.vert" exists in assets/shaders/
    And "test.frag" exists in assets/shaders/
    When I create a shader program from these files
    Then the vertex shader is loaded and compiled
    And the fragment shader is loaded and compiled
    And both shaders are linked into a program
    And a valid program ID is returned
    And I can use the program for rendering

  Scenario: Verify standard uniforms can be accessed
    Given a shader program created from vertex_shader.vert and test.frag
    When I query the location of uniform "u_time"
    Then a valid uniform location is returned
    When I query the location of uniform "u_resolution"
    Then a valid uniform location is returned
    When I query the location of uniform "u_backgroundTexture"
    Then a valid uniform location is returned
