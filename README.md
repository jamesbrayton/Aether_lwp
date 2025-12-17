# Aether Live Wallpaper

GPU-accelerated Android live wallpaper with customizable particle effects and background images.

## Project Status

**Branch:** `mvp`
**Phase:** Phase 1 Implementation - Project Setup
**Status:** Initial project structure created, build configuration pending resolution

## Current Setup

### What's Been Created

✅ **Project Structure:**
- Android app module with Kotlin support
- Gradle 8.7 build configuration
- Package structure: `com.aether.wallpaper`
- Test directories (unit tests, instrumentation tests)
- Assets directory for GLSL shaders

✅ **Configuration Files:**
- `build.gradle.kts` with dependencies (AndroidX, Gson, Coroutines, Test frameworks)
- `settings.gradle.kts` with repository configuration
- `AndroidManifest.xml` with wallpaper service declaration
- `proguard-rules.pro` for release builds

✅ **Resources:**
- Wallpaper metadata XML (`res/xml/wallpaper.xml`)
- String resources
- Color scheme and Material theme
- Placeholder app icons

✅ **Gherkin Specifications:**
- `spec/project-setup.feature` - Project setup acceptance criteria

✅ **Stub Classes:**
- `AetherWallpaperService.kt` - Live wallpaper service
- `SettingsActivity.kt` - Configuration UI

### Known Issues

#### Build Environment Limitation
The current Docker devcontainer runs on `aarch64` (ARM) architecture, which has compatibility issues with Android build tools (AAPT2).

**Error:** `Failed to start AAPT2 process`
**Cause:** Android build tools expect x86_64 architecture
**Impact:** Cannot complete `gradle build` in current environment

#### Dependency Resolution
The Android-Image-Cropper library from JitPack was temporarily commented out due to network/repository access issues during setup.

**TODO:** Re-enable once repository is accessible:
```kotlin
implementation("com.github.CanHub:Android-Image-Cropper:4.5.0")
```

### Next Steps

**To Resolve Build Issues:**
1. Test build on x86_64 machine or CI environment
2. Ensure Android SDK build tools are properly installed
3. Re-enable Android-Image-Cropper dependency

**To Continue Implementation:**
Once build is working, proceed with Phase 1 components in order:
1. ✅ Project Setup (current)
2. ShaderMetadataParser & ShaderRegistry
3. Shader Loading System
4. OpenGL ES Renderer
5. Configuration System
6. Texture Manager
7. Snow Shader Effect
8. Rain Shader Effect
9. Settings Activity UI
10. Image Cropping Integration
11. Live Wallpaper Service

## Development Environment

### Requirements
- JDK 21 (Eclipse Temurin)
- Gradle 8.7
- Android SDK 34
- Kotlin 1.9.23
- OpenGL ES 2.0 support

### Build Commands

```bash
# Build the project (once environment is fixed)
./gradlew build

# Run lint checks
./gradlew lint

# Run unit tests
./gradlew test

# Run Android instrumentation tests
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Assemble debug APK
./gradlew assembleDebug
```

## Architecture

See [CLAUDE.md](CLAUDE.md) for detailed architecture documentation.

### Key Design Decisions
- **Language:** Kotlin 1.9.23
- **Min SDK:** API 26 (Android 8.0)
- **Graphics:** OpenGL ES 2.0 + GLSL shaders
- **Shader Metadata:** Embedded in GLSL files (JavaDoc-style comments)
- **Config Persistence:** SharedPreferences + JSON (Gson)
- **Testing:** Robolectric (unit) + Instrumentation (integration)

### Shader Metadata System
Shaders will use embedded metadata for dynamic discovery and UI generation:

```glsl
/**
 * @shader Falling Snow
 * @id snow
 * @version 1.0.0
 * @author Aether Team
 * @param u_speed float 1.0 min=0.1 max=3.0 name="Fall Speed"
 */
precision mediump float;
// ... shader code
```

## Documentation

- **[CLAUDE.md](CLAUDE.md)** - Detailed architecture and development workflow
- **[docs/initial_requirements.md](docs/initial_requirements.md)** - Feature specifications
- **[AGENTS.md](AGENTS.md)** - TDD workflow and PR requirements
- **[.memoryBank/](memory:Bank/)** - Architectural decisions and progress log

## Contributing

This project follows Test-Driven Development (TDD):
1. Write Gherkin spec in `spec/`
2. Write failing test
3. Implement code to pass test
4. Refactor while keeping tests green
5. Commit with descriptive message

See [AGENTS.md](AGENTS.md) for full workflow details.

## License

[To be determined]

