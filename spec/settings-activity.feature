Feature: Settings Activity UI
  As a wallpaper user
  I want a settings interface to configure my wallpaper
  So that I can customize background, effects, and parameters

  Background:
    Given the SettingsActivity is launched
    And ShaderRegistry has discovered available shaders
    And ConfigManager is initialized

  # Activity Launch and Initialization

  Scenario: Settings Activity launches successfully
    When the activity is created
    Then the activity is displayed
    And no errors occur
    And the toolbar is visible with title "Wallpaper Settings"

  Scenario: Effect selector is populated from ShaderRegistry
    Given ShaderRegistry has discovered 2 shaders (snow, rain)
    When the activity loads the effect list
    Then the effect selector displays 2 effect cards
    And effect card 1 shows "Falling Snow"
    And effect card 2 shows "Falling Rain"
    And each card has an "Add Effect" button

  Scenario: Effect cards display shader metadata
    Given ShaderRegistry has discovered snow shader
    When the effect card is rendered
    Then the card displays shader name from @shader tag
    And the card displays description from @description tag
    And the card displays tags as chips

  Scenario: Empty layer list shown initially
    Given no configuration exists
    When the activity loads
    Then the active layers list is empty
    And a message "No effects added yet" is shown
    And the "Apply Wallpaper" button is disabled

  # Adding Effects to Layers

  Scenario: Add snow effect to layers
    Given the effect selector displays available effects
    When user taps "Add Effect" on the snow card
    Then a new layer is added to the active layers list
    And the layer displays "Falling Snow"
    And the layer shows default parameter values
    And the layer is enabled by default
    And the "Apply Wallpaper" button is enabled

  Scenario: Add rain effect to layers
    Given the effect selector displays available effects
    When user taps "Add Effect" on the rain card
    Then a new layer is added to the active layers list
    And the layer displays "Falling Rain"
    And the layer shows 4 parameter controls (particleCount, speed, angle, streakLength)

  Scenario: Add multiple layers
    Given no layers exist
    When user adds snow effect
    And user adds rain effect
    Then 2 layers are displayed in the list
    And layer 1 is "Falling Snow" with order 0
    And layer 2 is "Falling Rain" with order 1
    And both layers are enabled

  Scenario: Layers are ordered by creation time
    Given user adds snow effect
    When user adds rain effect
    Then snow layer appears first
    And rain layer appears second
    And order is preserved in configuration

  # Layer Configuration - Parameter Controls

  Scenario: Layer shows dynamic parameter controls
    Given user has added snow effect
    When the layer details are expanded
    Then 3 parameter controls are displayed
    And control 1 is labeled "Particle Count" (from @param name)
    And control 1 is a slider with range 10.0-200.0
    And control 1 default value is 100.0
    And control 2 is labeled "Fall Speed"
    And control 3 is labeled "Lateral Drift"

  Scenario: Float parameter renders as slider
    Given snow layer with u_speed parameter (float type)
    When the layer details are shown
    Then a slider control is rendered
    And the slider min is 0.1 (from @param min)
    And the slider max is 3.0 (from @param max)
    And the slider step is 0.1 (from @param step)
    And the current value label shows "1.0"

  Scenario: Adjust parameter slider
    Given snow layer with u_speed = 1.0
    When user moves the slider to 1.5
    Then the slider value updates to 1.5
    And the value label shows "1.5"
    And the configuration is updated with u_speed = 1.5
    And the change is persisted to SharedPreferences

  Scenario: Parameter changes are saved immediately
    Given snow layer with default parameters
    When user changes u_particleCount from 100 to 150
    Then ConfigManager.saveConfig() is called
    And the new value is persisted
    And reloading the activity shows u_particleCount = 150

  Scenario: Rain shader shows angle parameter
    Given rain layer is added
    When layer details are shown
    Then "Rain Angle" slider is displayed
    And slider range is 60.0-80.0 degrees
    And default value is 70.0

  Scenario: Rain shader shows streak length parameter
    Given rain layer is added
    When layer details are shown
    Then "Streak Length" slider is displayed
    And slider range is 0.01-0.05
    And default value is 0.03

  # Layer Management - Enable/Disable

  Scenario: Toggle layer enabled state
    Given snow layer is enabled
    When user taps the layer enable toggle
    Then the layer is disabled
    And the configuration is updated with enabled = false
    And the layer appears visually disabled (grayed out)

  Scenario: Re-enable disabled layer
    Given snow layer is disabled
    When user taps the layer enable toggle
    Then the layer is enabled
    And the configuration is updated with enabled = true
    And the layer appears visually active

  Scenario: Disabled layers persist configuration
    Given snow layer with custom parameters
    When user disables the layer
    Then parameter values are preserved in configuration
    And re-enabling the layer restores the same parameters

  # Layer Management - Removal

  Scenario: Remove layer from list
    Given snow layer exists
    When user taps the delete button on the layer
    Then a confirmation dialog appears
    When user confirms deletion
    Then the layer is removed from the list
    And the configuration no longer contains the layer
    And ConfigManager.saveConfig() is called

  Scenario: Cancel layer deletion
    Given snow layer exists
    When user taps delete and then cancels
    Then the layer remains in the list
    And no configuration changes occur

  Scenario: Remove all layers
    Given 2 layers exist (snow and rain)
    When user removes both layers
    Then the active layers list is empty
    And "No effects added yet" message is shown
    And the "Apply Wallpaper" button is disabled

  # Layer Management - Reordering

  Scenario: Reorder layers via drag and drop
    Given 2 layers exist: snow (order 0), rain (order 1)
    When user drags rain layer above snow layer
    Then rain layer moves to order 0
    And snow layer moves to order 1
    And the configuration is updated with new order values
    And the visual order matches the configuration order

  Scenario: Reorder affects rendering order
    Given 3 layers exist with different orders
    When user reorders the layers
    Then the layer order in configuration reflects the new sequence
    And rendering will composite layers in the new order

  # Background Image Selection

  Scenario: Background preview shows placeholder initially
    Given no background image is configured
    When the activity loads
    Then the background preview shows a placeholder image
    And the placeholder text says "No background selected"

  Scenario: Select background image from gallery
    Given the background preview is displayed
    When user taps "Select Background" button
    Then the system image picker is launched
    When user selects an image from the gallery
    Then the image cropping activity opens
    When user confirms the crop
    Then the background preview shows the cropped image
    And the configuration is updated with image URI and crop rect

  Scenario: Background image persists across sessions
    Given user has selected a background image
    When the activity is closed and reopened
    Then the background preview shows the same image
    And the configuration contains the correct URI and crop rect

  Scenario: Replace existing background image
    Given a background image is already configured
    When user selects a new image
    Then the old image is replaced
    And the new image is saved to configuration
    And the preview updates to show the new image

  # Configuration Persistence

  Scenario: Configuration auto-saves on changes
    Given the activity is open
    When user adds a layer
    Or user changes a parameter
    Or user enables/disables a layer
    Or user removes a layer
    Or user reorders layers
    Then ConfigManager.saveConfig() is called automatically
    And changes are persisted to SharedPreferences

  Scenario: Configuration loads on activity start
    Given a saved configuration exists with 2 layers
    When the activity is launched
    Then the layers list is populated from configuration
    And layer 1 shows saved shader and parameters
    And layer 2 shows saved shader and parameters
    And background image is loaded from configuration

  Scenario: Default configuration used when none exists
    Given no saved configuration exists
    When the activity is launched
    Then ConfigManager.getDefaultConfig() is called
    And the default configuration is loaded
    And the activity displays empty state

  # Apply Wallpaper

  Scenario: Apply Wallpaper button enabled with layers
    Given no layers exist
    Then "Apply Wallpaper" button is disabled
    When user adds a layer
    Then "Apply Wallpaper" button is enabled

  Scenario: Apply Wallpaper launches wallpaper picker
    Given at least one layer exists
    When user taps "Apply Wallpaper"
    Then the system wallpaper chooser is launched
    And the user can set the wallpaper as home screen or lock screen
    And the configuration is already saved

  Scenario: Apply Wallpaper with no background image
    Given layers exist but no background image selected
    When user taps "Apply Wallpaper"
    Then the wallpaper uses a default solid color background
    And the effects render on top of the solid background
    And no error occurs

  # Validation and Error Handling

  Scenario: Invalid parameter value rejected
    Given a parameter with range 10.0-200.0
    When user attempts to set value to 300.0 (out of range)
    Then the value is clamped to 200.0
    And a warning message is shown
    And the configuration is not corrupted

  Scenario: Missing shader file handled gracefully
    Given ShaderRegistry discovers a shader with id "missing"
    But the shader file does not exist
    When user attempts to add the shader
    Then an error message is shown
    And no layer is added
    And the activity does not crash

  Scenario: Configuration load error handled
    Given ConfigManager.loadConfig() throws an exception
    When the activity is launched
    Then the default configuration is used as fallback
    And an error message is logged
    And the activity loads successfully

  # UI State Management

  Scenario: Activity state persists on rotation
    Given user has added layers and configured parameters
    When the device is rotated
    Then the layers list is preserved
    And parameter values remain the same
    And the background image is still displayed
    And no data is lost

  Scenario: Back button saves configuration
    Given user has made changes
    When user presses the back button
    Then the configuration is saved
    And the activity finishes
    And changes are persisted

  # Dynamic UI Generation Validation

  Scenario: Different shaders show different parameter counts
    Given snow shader has 3 parameters
    And rain shader has 4 parameters
    When user adds both effects
    Then snow layer shows 3 controls
    And rain layer shows 4 controls
    And each control matches the @param definition

  Scenario: Parameter names from metadata displayed
    Given a shader with @param name="Particle Count"
    When the parameter control is rendered
    Then the label displays "Particle Count"
    And not the uniform name "u_particleCount"

  Scenario: Parameter descriptions shown as hints
    Given a shader with @param desc="Number of visible snow particles"
    When the parameter control is rendered
    Then the description is shown as helper text or tooltip
    And the user can see what the parameter does

  Scenario: Parameter step value affects slider precision
    Given u_speed with step=0.1
    When user moves the slider
    Then values change in 0.1 increments
    And intermediate values are not selectable

  # Integration with Other Components

  Scenario: ShaderRegistry integration
    Given shaders exist in assets/shaders/
    When SettingsActivity calls ShaderRegistry.discoverShaders()
    Then all valid shaders are discovered
    And effect cards are generated for each shader
    And shader metadata is displayed correctly

  Scenario: ConfigManager integration
    Given layers are configured with parameters
    When ConfigManager.saveConfig() is called
    Then JSON is serialized with all layer data
    And parameters are stored in the params map
    And the configuration can be loaded by WallpaperService

  Scenario: TextureManager integration (future)
    Given a background image is selected
    When the wallpaper is applied
    Then TextureManager will load the image with saved crop rect
    And the texture is uploaded to OpenGL
    And the effects render on top of the background

  # Accessibility

  Scenario: UI is accessible
    Given the activity is displayed
    Then all interactive elements have content descriptions
    And sliders have min/max announcements
    And buttons have clear labels
    And the UI is navigable with TalkBack

  Scenario: High contrast mode supported
    Given the device is in high contrast mode
    When the activity is displayed
    Then text has sufficient contrast ratio
    And disabled elements are distinguishable
    And UI remains readable

  # Performance

  Scenario: Effect list loads quickly
    Given 10 shaders exist in assets
    When the activity is launched
    Then the effect list is populated within 1 second
    And the UI remains responsive
    And no ANR (Application Not Responding) occurs

  Scenario: Parameter changes are smooth
    Given a layer with 5 parameters
    When user adjusts sliders rapidly
    Then the UI updates smoothly without lag
    And configuration saves are debounced (not saved on every change)
    And the final value is saved when user stops interacting

  Scenario: Large layer lists scroll smoothly
    Given 10 layers are added
    When user scrolls the layers list
    Then scrolling is smooth at 60fps
    And RecyclerView efficiently recycles views
    And memory usage remains stable

  # Edge Cases

  Scenario: Empty assets folder handled
    Given no shader files exist in assets/shaders/
    When the activity is launched
    Then "No effects available" message is shown
    And the effect selector is empty
    And the activity does not crash

  Scenario: Corrupted shader metadata handled
    Given a shader file has invalid metadata
    When ShaderRegistry discovers shaders
    Then the invalid shader is skipped
    And a warning is logged
    And other valid shaders are loaded successfully

  Scenario: Maximum layers limit enforced
    Given 5 layers already exist
    When user attempts to add a 6th layer
    Then a message "Maximum 5 layers allowed" is shown
    And the 6th layer is not added
    And the UI prevents further additions

  Scenario: Duplicate shader instances allowed
    Given snow layer already exists
    When user adds snow effect again
    Then a second snow layer is created
    And both layers have independent parameters
    And both layers have unique IDs in configuration
