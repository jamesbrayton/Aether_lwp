# Aether Live Wallpaper

GPU-accelerated Android live wallpaper with customizable particle effects and background images.

## Project Status

**Branch:** `phase3`
**Phase:** Phase 1 Implementation - 27% Complete
**Status:** Shader loading system complete, OpenGL renderer next

**Progress:**
- ✅ Project Setup (Component #1)
- ✅ ShaderMetadataParser & Registry (Component #2) - 25 tests passing
- ✅ ShaderLoader (Component #3) - 17 instrumentation tests passing
- ⏳ GLRenderer (Component #4) - Next up

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

### CI/CD Workflow

All builds happen in **GitHub Actions** (zero host pollution):

| Event | Lint | Unit Tests | Debug APK | Instrumentation Tests | Release |
|-------|------|-----------|-----------|----------------------|---------|
| Push to any branch | ✅ | ✅ | ✅ | ❌ | ❌ |
| PR to main | ✅ | ✅ | ✅ | ✅ | ❌ |
| Manual: Run workflow | ✅ | ✅ | ✅ | ❌ | ✅ |

**Update (2025-12-18):** Devcontainer now explicitly runs as **x86_64** (`--platform=linux/amd64`).
Rosetta 2 handles ARM translation on M-series Macs. Build issues are resolved.

**Previous Error:** `Failed to start AAPT2 process`
**Previous Cause:** Android build tools expect x86_64 architecture
**Resolution:** Explicit platform specification in Dockerfile and devcontainer.json

**Creating a Release:**
1. Go to Actions tab → "Android Build and Release"
2. Click "Run workflow" → Select `main` branch
3. Check "Create GitHub Release?" → Click "Run workflow"
4. Wait 5-7 minutes → Check Releases tab

**See [docs/CI_CD.md](docs/CI_CD.md) for complete details.**

### Next Components

Plan Phase 2

## Development Environment

### Requirements
- JDK 21 (Eclipse Temurin)
- Gradle 8.7
- Android SDK 34
- Kotlin 1.9.23
- OpenGL ES 2.0 support

### Build Commands

**All builds happen in GitHub Actions** (see [docs/BUILD.md](docs/BUILD.md) for details).

**Development Workflow:**
```bash
# 1. Work on feature branch (in devcontainer)
git checkout -b feature/new-component
# ... make changes ...
git commit -m "feature: add new component"
git push origin feature/new-component

# 2. GitHub Actions automatically:
#    - Runs lint + unit tests
#    - Builds debug APK
#    - Uploads APK artifact (7 days)

# 3. Create PR
gh pr create --title "feat: new component" --base main

# 4. GitHub Actions on PR:
#    - Runs full test suite (includes instrumentation tests)
#    - Validates before merge

# 5. After merge to main:
#    - Nothing automatic happens
#    - Ready for next feature or manual release
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

