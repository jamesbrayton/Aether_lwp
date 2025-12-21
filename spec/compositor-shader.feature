Feature: Compositor Shader
  Scenario: Blend layers with opacity
    Given background and layer textures
    When compositor renders with u_layerCount layers
    Then output is correctly alpha-blended
    And per-layer opacity is applied
