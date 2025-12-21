Feature: FBO Manager
  As a rendering system
  I want to manage framebuffer objects efficiently
  So that I can render multiple layers to separate textures for compositing

  Background:
    Given an OpenGL ES 2.0 context is available
    And screen dimensions are 1080x1920

  Scenario: Create FBO with texture attachment
    Given an FBOManager instance
    When createFBO is called for layer "layer1"
    Then an FBO is generated with GL_FRAMEBUFFER_COMPLETE status
    And a texture is created with RGBA8 format
    And the texture size matches screen dimensions (1080x1920)
    And the texture has LINEAR filtering
    And the texture has CLAMP_TO_EDGE wrapping
    And the FBO info is stored in the internal map

  Scenario: Bind and unbind FBOs
    Given an FBOManager with FBO "layer1" created
    When bindFBO is called with "layer1"
    Then the FBO is bound as the active framebuffer
    And subsequent render calls draw to the FBO texture
    When unbindFBO is called
    Then the default framebuffer (0) is bound
    And subsequent render calls draw to the screen

  Scenario: Get texture ID from layer
    Given an FBOManager with FBOs created for "layer1", "layer2", "layer3"
    When getTexture is called with "layer2"
    Then the correct texture ID for layer2 is returned
    When getTexture is called with "nonexistent"
    Then 0 is returned (invalid texture)

  Scenario: Handle FBO creation failure
    Given an FBOManager instance
    And GL context is in an error state
    When createFBO is called for "bad_layer"
    Then createFBO returns null
    And an error is logged
    And the FBOManager continues to function normally

  Scenario: Resize recreates all FBOs
    Given an FBOManager with FBOs for 3 layers at 1080x1920
    And the screen rotates to landscape
    When resize is called with dimensions 1920x1080
    Then all existing FBOs are deleted
    And all textures are deleted
    And new FBOs are created with dimensions 1920x1080
    And new textures are created with dimensions 1920x1080
    And the same layer IDs are preserved

  Scenario: Release all resources
    Given an FBOManager with FBOs for "layer1", "layer2", "layer3"
    When release is called
    Then all 3 FBOs are deleted via glDeleteFramebuffers
    And all 3 textures are deleted via glDeleteTextures
    And the internal map is cleared
    And subsequent getFBO calls return 0

  Scenario: Multiple FBOs with unique texture IDs
    Given an FBOManager instance
    When createFBO is called for "layer1", "layer2", "layer3"
    Then 3 unique FBO IDs are generated
    And 3 unique texture IDs are generated
    And each layer ID maps to its own FBO and texture
    And texture IDs do not collide

  Scenario: FBO completeness check
    Given an FBOManager instance
    When createFBO is called for "layer1"
    Then glCheckFramebufferStatus returns GL_FRAMEBUFFER_COMPLETE
    And the FBO is ready for rendering
    When the FBO is incomplete due to missing attachment
    Then createFBO returns null
    And resources are cleaned up
