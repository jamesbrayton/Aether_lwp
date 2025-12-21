Feature: Layer Manager
  As a rendering system
  I want to manage multiple shader programs efficiently
  So that I can render multiple effect layers without recompiling shaders

  Background:
    Given a ShaderLoader is available
    And a compiled vertex shader exists

  Scenario: Cache compiled shader programs
    Given a LayerManager with empty cache
    When getOrCreateProgram is called with shader ID "snow"
    Then the snow shader is compiled successfully
    And the program handle is cached
    When getOrCreateProgram is called again with shader ID "snow"
    Then the cached program handle is returned
    And the shader is not recompiled

  Scenario: Return enabled layers sorted by order
    Given layers with the following configuration:
      | Shader ID | Order | Enabled |
      | bubbles   | 3     | true    |
      | snow      | 1     | true    |
      | rain      | 2     | false   |
      | dust      | 4     | true    |
    When getEnabledLayers is called
    Then layers are returned in the following order:
      | Shader ID | Order |
      | snow      | 1     |
      | bubbles   | 3     |
      | dust      | 4     |

  Scenario: Filter out disabled layers
    Given layers with the following configuration:
      | Shader ID | Order | Enabled |
      | snow      | 1     | true    |
      | rain      | 2     | false   |
      | dust      | 3     | true    |
    When getEnabledLayers is called
    Then the returned list contains 2 layers
    And the returned list contains "snow" with order 1
    And the returned list contains "dust" with order 3
    And the returned list does not contain "rain"

  Scenario: Update layers dynamically
    Given a LayerManager with layers [snow, rain]
    When updateLayers is called with new layers [bubbles, dust, test]
    And getEnabledLayers is called
    Then the returned list reflects the new layers

  Scenario: Handle shader compilation failure gracefully
    Given a LayerManager with empty cache
    When getOrCreateProgram is called with an invalid shader ID "nonexistent"
    Then the method returns 0 (invalid program handle)
    And an error is logged
    And the LayerManager continues to function normally

  Scenario: Release all cached programs
    Given a LayerManager with cached programs for [snow, rain, bubbles]
    When release is called
    Then all 3 shader programs are deleted via glDeleteProgram
    And the program cache is cleared
    And subsequent getOrCreateProgram calls will recompile shaders
