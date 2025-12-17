Feature: Shader Metadata Parsing
  As a developer
  I want to embed metadata in GLSL shader files
  So that shaders can be discovered and used without code changes

  Background:
    Given the shader metadata system uses JavaDoc-style comments
    And metadata is embedded at the top of .frag files
    And standard uniforms are required for all shaders

  Scenario: Parse shader with complete metadata
    Given a shader file with the following metadata:
      """
      /**
       * @shader Test Effect
       * @id test_effect
       * @version 1.0.0
       * @author Test Author
       * @source https://github.com/test/repo
       * @license MIT
       * @description A simple test shader for validation
       * @tags test, validation, simple
       * @minOpenGL 2.0
       *
       * @param u_intensity float 1.0 min=0.0 max=2.0 step=0.1 name="Intensity" desc="Effect intensity"
       * @param u_color color 1.0,1.0,1.0 name="Color" desc="Effect color"
       */
      """
    When the ShaderMetadataParser parses the file
    Then a ShaderDescriptor should be returned
    And the descriptor id should be "test_effect"
    And the descriptor name should be "Test Effect"
    And the descriptor version should be "1.0.0"
    And the descriptor author should be "Test Author"
    And the descriptor source should be "https://github.com/test/repo"
    And the descriptor license should be "MIT"
    And the descriptor description should be "A simple test shader for validation"
    And the descriptor should have 3 tags: "test", "validation", "simple"
    And the descriptor minOpenGLVersion should be "2.0"
    And the descriptor should have 2 parameters

  Scenario: Parse parameter definitions
    Given a shader with parameter metadata:
      """
      /**
       * @shader Param Test
       * @id param_test
       * @version 1.0.0
       *
       * @param u_speed float 1.5 min=0.0 max=5.0 step=0.1 name="Speed" desc="Movement speed"
       * @param u_count int 100 min=10 max=200 step=10 name="Count" desc="Particle count"
       * @param u_enabled bool true name="Enabled" desc="Enable effect"
       */
      """
    When the ShaderMetadataParser parses the parameters
    Then parameter 0 should have id "u_speed"
    And parameter 0 should have type FLOAT
    And parameter 0 should have defaultValue 1.5
    And parameter 0 should have minValue 0.0
    And parameter 0 should have maxValue 5.0
    And parameter 0 should have step 0.1
    And parameter 0 should have name "Speed"
    And parameter 0 should have description "Movement speed"
    And parameter 1 should have id "u_count"
    And parameter 1 should have type INT
    And parameter 2 should have id "u_enabled"
    And parameter 2 should have type BOOL

  Scenario: Parse shader with minimal required metadata
    Given a shader file with only required tags:
      """
      /**
       * @shader Minimal Effect
       * @id minimal
       * @version 1.0.0
       */
      """
    When the ShaderMetadataParser parses the file
    Then a ShaderDescriptor should be returned
    And the descriptor id should be "minimal"
    And the descriptor name should be "Minimal Effect"
    And the descriptor version should be "1.0.0"
    And the descriptor author should be null
    And the descriptor should have 0 parameters

  Scenario: Parse shader with missing required tag
    Given a shader file missing the @id tag:
      """
      /**
       * @shader Invalid Shader
       * @version 1.0.0
       */
      """
    When the ShaderMetadataParser attempts to parse the file
    Then a ShaderParseException should be thrown
    And the exception message should contain "Missing required tag: @id"

  Scenario: Parse shader with invalid parameter syntax
    Given a shader file with malformed parameter:
      """
      /**
       * @shader Bad Param
       * @id bad_param
       * @version 1.0.0
       *
       * @param u_speed INVALID_TYPE 1.0
       */
      """
    When the ShaderMetadataParser attempts to parse the file
    Then a ShaderParseException should be thrown
    And the exception message should contain "Invalid parameter type: INVALID_TYPE"

  Scenario: Shader discovery from assets directory
    Given the assets/shaders/ directory contains:
      | Filename        | Shader ID  | Shader Name    |
      | snow.frag       | snow       | Falling Snow   |
      | rain.frag       | rain       | Falling Rain   |
      | test.frag       | test       | Test Effect    |
    When ShaderRegistry.discoverShaders() is called
    Then 3 shaders should be discovered
    And the registry should contain shader with id "snow"
    And the registry should contain shader with id "rain"
    And the registry should contain shader with id "test"

  Scenario: Get shader by ID from registry
    Given the ShaderRegistry has discovered shaders
    And a shader with id "snow" exists
    When getShaderById("snow") is called
    Then the returned ShaderDescriptor should have name "Falling Snow"
    And the descriptor should not be null

  Scenario: Get shader by ID that doesn't exist
    Given the ShaderRegistry has discovered shaders
    When getShaderById("nonexistent") is called
    Then null should be returned

  Scenario: Invalid shader doesn't break discovery
    Given the assets/shaders/ directory contains:
      | Filename        | Valid      |
      | snow.frag       | true       |
      | invalid.frag    | false      |
      | rain.frag       | true       |
    When ShaderRegistry.discoverShaders() is called
    Then 2 shaders should be discovered
    And the registry should contain shader with id "snow"
    And the registry should contain shader with id "rain"
    And a warning should be logged for "invalid.frag"

  Scenario: Shader metadata includes standard uniforms
    Given a complete shader file with metadata and GLSL code
    When the shader source is compiled
    Then it should declare uniform sampler2D u_backgroundTexture
    And it should declare uniform float u_time
    And it should declare uniform vec2 u_resolution
    And it should declare uniform vec2 u_gyroOffset
    And it should declare uniform float u_depthValue
    And the GLSL compiler should not error on metadata comments

  Scenario: Registry returns all discovered shaders
    Given the ShaderRegistry has discovered 3 shaders
    When getAllShaders() is called
    Then a list of 3 ShaderDescriptor objects should be returned
    And each descriptor should have a unique id
    And each descriptor should have a non-empty name
