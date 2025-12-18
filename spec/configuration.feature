Feature: Configuration Management
  As the wallpaper application
  I want to persist wallpaper configuration to SharedPreferences
  So that user settings are saved between app sessions

  Background:
    Given ConfigManager is initialized
    And SharedPreferences is available

  Scenario: Save wallpaper configuration
    Given a wallpaper configuration with:
      | background_uri | content://media/external/images/media/123 |
      | crop_x         | 100                                        |
      | crop_y         | 200                                        |
      | crop_width     | 1080                                       |
      | crop_height    | 1920                                       |
    And a layer with shader "snow"
    And layer parameters:
      | u_particleCount | 100  |
      | u_speed         | 1.5  |
    When the configuration is saved
    Then the configuration is stored in SharedPreferences as JSON
    And the JSON contains the background URI
    And the JSON contains the crop rectangle
    And the JSON contains the layer configuration
    And the JSON contains the layer parameters

  Scenario: Load saved configuration
    Given a configuration was previously saved
    When the configuration is loaded
    Then the loaded config matches the saved config
    And the background URI is correct
    And the crop rectangle is correct
    And all layers are restored
    And all layer parameters are restored

  Scenario: Load configuration with no saved data
    Given no configuration has been saved
    When the configuration is loaded
    Then a default configuration is returned
    And the default has no background image
    And the default has no layers

  Scenario: Save configuration with multiple layers
    Given a configuration with 3 layers:
      | shader | order | enabled | opacity | depth |
      | snow   | 1     | true    | 0.8     | 0.3   |
      | rain   | 2     | true    | 0.9     | 0.5   |
      | smoke  | 3     | false   | 0.7     | 0.7   |
    When the configuration is saved
    Then all 3 layers are stored in JSON
    And layer order is preserved
    And layer enabled states are preserved
    And layer opacity values are preserved
    And layer depth values are preserved

  Scenario: Load configuration with multiple layers
    Given a configuration with 3 layers was saved
    When the configuration is loaded
    Then 3 layers are restored
    And layers are in the correct order
    And each layer has the correct shader ID
    And each layer has the correct parameters

  Scenario: Save configuration with dynamic parameters
    Given a layer with shader "snow"
    And layer parameters:
      | u_particleCount | 150    |
      | u_speed         | 2.0    |
      | u_driftAmount   | 0.75   |
    When the configuration is saved
    Then the parameters are stored as a map in JSON
    And parameter types are preserved
    And parameter values are accurate

  Scenario: Load configuration with dynamic parameters
    Given a configuration with dynamic parameters was saved
    When the configuration is loaded
    Then all parameters are restored
    And parameter types match the saved types
    And parameter values match the saved values

  Scenario: Update existing configuration
    Given a configuration was previously saved
    When the configuration is modified
    And the modified configuration is saved
    Then the new configuration overwrites the old one
    And loading returns the updated configuration

  Scenario: Save configuration with no background
    Given a configuration with no background image
    And 1 layer with shader "rain"
    When the configuration is saved
    Then the configuration is stored successfully
    And the background section is null or empty
    And the layer is stored correctly

  Scenario: Save configuration with no layers
    Given a configuration with a background image
    And no layers
    When the configuration is saved
    Then the configuration is stored successfully
    And the background is stored correctly
    And the layers array is empty

  Scenario: Configuration JSON serialization
    Given a complete wallpaper configuration
    When the configuration is serialized to JSON
    Then the JSON is valid
    And the JSON can be deserialized back to a config object
    And the deserialized config equals the original config

  Scenario: Handle invalid JSON during load
    Given invalid JSON is stored in SharedPreferences
    When the configuration is loaded
    Then a default configuration is returned
    And no exception is thrown
    And the invalid data is logged

  Scenario: Save global settings
    Given a configuration with global settings:
      | targetFps         | 60    |
      | gyroscopeEnabled  | false |
    When the configuration is saved
    Then global settings are stored in JSON
    And targetFps is saved as 60
    And gyroscopeEnabled is saved as false

  Scenario: Load global settings
    Given a configuration with custom global settings was saved
    When the configuration is loaded
    Then global settings are restored
    And targetFps matches the saved value
    And gyroscopeEnabled matches the saved value

  Scenario: Get default configuration
    When getDefaultConfig is called
    Then a valid configuration is returned
    And the configuration has no background
    And the configuration has no layers
    And the configuration has default global settings

  Scenario: Configuration equals and hashCode
    Given two identical configurations
    Then they should be equal
    And they should have the same hashCode
    Given two different configurations
    Then they should not be equal

  Scenario: Layer configuration validation
    Given a layer with valid parameters
    Then the layer validates successfully
    Given a layer with negative opacity
    Then the layer validation fails
    Given a layer with opacity > 1.0
    Then the layer validation fails
    Given a layer with negative depth
    Then the layer validation fails
    Given a layer with depth > 1.0
    Then the layer validation fails

  Scenario: Background configuration with crop
    Given a background with URI and crop rectangle
    When the background is serialized to JSON
    Then the URI is stored as a string
    And the crop rectangle is stored as an object
    And all crop values (x, y, width, height) are preserved

  Scenario: Configuration immutability
    Given a wallpaper configuration
    When a field is modified via copy
    Then the original configuration is unchanged
    And the copied configuration has the new value
