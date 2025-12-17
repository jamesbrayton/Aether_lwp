Feature: Android Project Setup
  As a developer
  I want a properly configured Android project
  So that I can build the Aether live wallpaper application

  Background:
    Given a workspace with devcontainer environment
    And JDK 21, Gradle 8.7, and Android SDK 34 are installed

  Scenario: Project builds successfully
    Given a new Android project with Kotlin
    And minSdk is set to 26
    And targetSdk is set to 34
    When I run ./gradlew build
    Then the build completes without errors
    And the debug APK is generated at app/build/outputs/apk/debug/app-debug.apk

  Scenario: Project structure is correct
    Given the Android project exists
    Then the directory app/src/main/java/com/aether/wallpaper exists
    And the directory app/src/main/res exists
    And the directory app/src/main/assets/shaders exists
    And the directory app/src/test exists
    And the directory app/src/androidTest exists

  Scenario: Dependencies are configured
    Given the build.gradle.kts file exists
    Then it includes AndroidX core-ktx dependency
    And it includes Gson dependency
    And it includes Android-Image-Cropper dependency
    And it includes JUnit test dependency
    And it includes Robolectric test dependency
    And it includes Espresso test dependency

  Scenario: AndroidManifest declares wallpaper service
    Given the AndroidManifest.xml file exists
    Then it declares AetherWallpaperService
    And the service has BIND_WALLPAPER permission
    And the service references wallpaper metadata XML

  Scenario: Wallpaper metadata is configured
    Given the res/xml/wallpaper.xml file exists
    Then it specifies a wallpaper description
    And it references SettingsActivity
    And it includes a thumbnail drawable reference

  Scenario: Lint configuration is applied
    Given the build.gradle.kts file exists
    When I run ./gradlew lint
    Then lint checks complete
    And there are no critical errors
