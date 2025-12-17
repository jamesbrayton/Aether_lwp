---
tags: #active_work #infrastructure_complete
updated: 2025-12-17
phase: Ready for Phase 1 Implementation
---

# Active Context - Infrastructure Complete

## Current Status
**Branch:** `mvp`  
**Phase:** Infrastructure Complete - Ready to Begin Phase 1 Implementation  
**Infrastructure:** ✅ Complete (devcontainer, CI/CD, emulator, clean workflow)  
**Android Project:** Not yet created (next step)  

## Latest Development (2025-12-17)

### Clean Development Workflow Validated ✅

**Problem Solved:** Host system pollution vs clean development environment

**User Requirement:**
> "I don't want to change the Java install on my host machine. One of the reasons for using devcontainers is to not pollute my host system."

**Solution Implemented:**
- **Devcontainer:** Code editing only (VSCode/Claude Code + git)
- **GitHub Actions:** All builds happen in cloud (Ubuntu runners)
- **Mac:** Only Android Studio (emulator + ADB, no build tools)

**Validated Workflow:**
1. Edit code in devcontainer → commit → push
2. `gh workflow run build.yml --ref mvp` (trigger build from devcontainer)
3. `gh run download --name app-debug` (download APK artifact)
4. `adb -s emulator-5556 install -r app-debug.apk` (install on Mac)

**Result:** Zero host pollution, reproducible builds, end-to-end workflow confirmed ✅

### Infrastructure Components Complete

**Devcontainer:**
- ✅ JDK 21 (Eclipse Temurin)
- ✅ Gradle 8.7
- ✅ Android SDK 34
- ✅ Kotlin 1.9.23
- ✅ GitHub CLI (gh)
- ✅ Git configured

**CI/CD (GitHub Actions):**
- ✅ Workflow file: `.github/workflows/build.yml`
- ✅ Manual trigger: `workflow_dispatch`
- ✅ Auto-release: push to `main`
- ✅ PR testing: pull_request to `main`
- ✅ Debug APK artifact: `app-debug` (always built)
- ✅ Lint + unit tests
- ✅ ZeroVer (0ver) versioning

**Documentation:**
- ✅ BUILD.md (comprehensive build instructions)
- ✅ DEVELOPMENT_HANDOFF.md (IDE workflow guide)
- ✅ CI_CD.md (GitHub Actions setup)
- ✅ ARM_DEVELOPMENT.md (M-series Mac guide)
- ✅ RELEASE.md (ZeroVer strategy)
- ✅ QUICK_REFERENCE.md (push-button builds)
- ✅ CONTRIBUTING.md (contribution guide)

**Emulator:**
- ✅ ARM64 (arm64-v8a) system image, API 35
- ✅ Pixel emulator running successfully
- ✅ ADB connectivity confirmed
- ✅ APK installation validated

### Key Technical Insights

**ADB Architecture:**
- Linux ADB in devcontainer cannot manage Mac emulators
- Port 5037 conflict causes stuck commands
- **Solution:** Only use Mac's native ADB for emulator operations

**APK Signing:**
- Unsigned APKs won't install (INSTALL_PARSE_FAILED_NO_CERTIFICATES)
- Debug APKs auto-signed with debug keystore ✅
- GitHub Actions now produces debug APKs for all builds

**Clean Separation:**
- Code: Devcontainer (portable, reproducible)
- Build: GitHub Actions (cloud, consistent)
- Test: Mac emulator (native performance)

## Active Work: Phase 1 (MVP) - Ready to Begin

### Scope (Confirmed 2025-12-16)
1. **Language:** Kotlin (null safety, coroutines, modern syntax)
2. **Package Name:** `com.aether.wallpaper`
3. **Min SDK:** API 26 (Android 8.0) - 90%+ coverage
4. **Initial Effects:** 2 effects (Snow, Rain) - validate architecture
5. **Gyroscope Parallax:** Deferred to Phase 2
6. **Testing:** Both Robolectric + Instrumentation
7. **Image Cropping:** Android-Image-Cropper library
8. **Shader Metadata:** Embedded in GLSL (JavaDoc-style comments)
9. **Standard Uniforms:** Required for all shaders
10. **Build Strategy:** GitHub Actions (no local builds)

### Phase 1 Objectives
✅ Create Android project structure  
✅ **ShaderMetadataParser & ShaderRegistry** - Core extensibility  
✅ OpenGL ES 2.0 rendering pipeline  
✅ Shader loading system with error handling  
✅ 2 particle effects with **embedded metadata** (snow.frag, rain.frag)  
✅ **Dynamic Settings UI** - parameter controls auto-generated from metadata  
✅ Background image selection + cropping  
✅ Configuration persistence (SharedPreferences + JSON)  
✅ Live wallpaper service lifecycle  
✅ Comprehensive test suite (80%+ coverage)  

### Component Order (13-16 days)
1. **Project Setup** (1 day)
2. **ShaderMetadataParser & Registry** (2 days) - Core extensibility
3. **Shader Loading System** (2 days)
4. **OpenGL ES Renderer** (2 days)
5. **Configuration System** (1 day)
6. **Texture Manager** (1 day)
7. **Snow Shader** (1-2 days) - First effect with metadata
8. **Rain Shader** (1-2 days) - Second effect with metadata
9. **Settings Activity** (2 days) - Dynamic UI generation
10. **Image Cropping Integration** (1 day)
11. **Live Wallpaper Service** (2 days)

### Phase 2 (Deferred)
- Multi-layer framebuffer compositing
- 3 additional effects (bubbles, dust, smoke)
- Gyroscope parallax with depth-based offsets
- User shader imports (validation, compile test)
- Drag-and-drop layer reordering UI
- Performance optimization (FPS throttling, resolution scaling)

### Phase 3+ (Future Vision)
- Shader marketplace/library
- Community shader submissions
- Auto-generated previews
- Shader version updates

## Development Workflow (Final)

### Iteration Cycle
```bash
# 1. Code (Devcontainer)
# ... edit files in VSCode/Claude Code ...
git add .
git commit -m "feature: implement shader parser"
git push origin mvp

# 2. Build (GitHub Actions)
gh workflow run build.yml --ref mvp
gh run list --workflow=build.yml --limit 3

# 3. Download (Devcontainer)
gh run download --name app-debug

# 4. Test (Mac Terminal)
adb -s emulator-5556 install -r app-debug.apk
adb shell am start -n com.aether.wallpaper/.MainActivity
```

### TDD Process (Per Component)
1. Write Gherkin spec in `spec/*.feature`
2. Write failing unit test
3. Run tests: `git push` → GitHub Actions runs tests
4. Implement feature
5. Run tests: `git push` → GitHub Actions validates
6. Refactor
7. Commit + push

### Testing Strategy
- **Unit Tests:** Robolectric (run in GitHub Actions)
- **Integration Tests:** Espresso (run in GitHub Actions with emulator)
- **Manual Testing:** Download APK → install to Mac emulator
- **Coverage Target:** 80%+

## Shader Metadata System (Core Architecture)

### Format: Embedded JavaDoc-style Comments
```glsl
/**
 * @shader Falling Snow
 * @id snow
 * @version 1.0.0
 * @author Aether Team
 * @source https://github.com/aetherteam/aether-lwp-shaders
 * @license MIT
 * @description Gentle falling snow with lateral drift
 * @tags winter, weather, particles
 * @minOpenGL 2.0
 * 
 * @param u_particleCount float 100.0 min=10.0 max=200.0 name="Particle Count"
 * @param u_speed float 1.0 min=0.1 max=3.0 name="Fall Speed"
 */

precision mediump float;

// REQUIRED: Standard uniforms (all shaders must declare)
uniform sampler2D u_backgroundTexture;
uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_gyroOffset;
uniform float u_depthValue;

// Effect-specific parameters
uniform float u_particleCount;
uniform float u_speed;

void main() {
    // ... shader code
}
```

### Standard Uniform Contract (ADR-011)
**ALL shaders MUST declare these uniforms:**
- `uniform sampler2D u_backgroundTexture;` - User's selected background image
- `uniform float u_time;` - Elapsed time in seconds
- `uniform vec2 u_resolution;` - Screen resolution (width, height)
- `uniform vec2 u_gyroOffset;` - Gyroscope offset (Phase 2, zero in Phase 1)
- `uniform float u_depthValue;` - Layer depth (Phase 2, zero in Phase 1)

**Rationale:**
- Architectural consistency
- GPU optimizes away unused uniforms (zero cost)
- Future-proof for Phase 2 parallax

## Developer Experience: Adding New Shaders

**Phase 1 (bundled shaders):**
```bash
# 1. Create effect.frag with metadata comments
# 2. Place in assets/shaders/
# 3. Push to GitHub
git add app/src/main/assets/shaders/bubbles.frag
git commit -m "feature: add bubbles shader"
git push origin mvp

# 4. Trigger build
gh workflow run build.yml --ref mvp

# 5. Download and test
gh run download --name app-debug
adb -s emulator-5556 install -r app-debug.apk

# Effect automatically appears in Settings UI ✅
```

**No code changes needed!**

## Known Constraints & Limitations
- OpenGL ES 2.0 only (ES 3.0 deferred)
- Max 3-5 simultaneous layers (performance)
- Bitmap sampling required for large images (OOM prevention)
- Continuous rendering impacts battery (mitigate with FPS options)
- Metadata parser requires strict format adherence
- Devcontainer cannot build Android apps (AAPT2 issue) → use GitHub Actions
- Linux ADB cannot manage Mac emulators (port conflict) → use Mac's native ADB

## Open Questions / Risks
- **Shader complexity:** Will procedural snow/rain achieve desired visual quality?
- **Parser robustness:** Will regex-based parser handle edge cases well?
- **Performance:** Can we maintain 60fps with 2 layers + dynamic parameters?
- **GPU compatibility:** Need to test on Qualcomm and Mali GPUs early
- **Battery:** Need baseline battery metrics from reference wallpapers

## Success Criteria for Extensibility
- ✅ Add shader in < 5 minutes
- ✅ 0 code changes needed
- ✅ 0 host system pollution
- ✅ UI adapts automatically to new parameters
- ✅ Settings shows shader name, description, author, license
- ✅ Reproducible builds in CI/CD

## Immediate Next Steps

**1. Create Android Project Structure**
```bash
# In devcontainer
mkdir -p app/src/main/java/com/aether/wallpaper
mkdir -p app/src/main/assets/shaders
mkdir -p app/src/test/java/com/aether/wallpaper
mkdir -p app/src/androidTest/java/com/aether/wallpaper
```

**2. Write First Gherkin Spec**
```gherkin
# spec/project-setup.feature
Feature: Android Project Setup
  Scenario: Gradle build succeeds
    Given the Android project structure exists
    When I run ./gradlew assembleDebug
    Then the build should succeed
    And app-debug.apk should be created
```

**3. Begin TDD: ShaderMetadataParser**
- Write failing tests for JavaDoc parsing
- Implement regex-based parser
- Validate all @tags extracted correctly

---

**Key Innovation:** Clean separation of code (devcontainer), build (CI/CD), and test (emulator) enables zero host pollution while maintaining full Android development capabilities.

**Status:** Infrastructure complete, ready to begin Phase 1 implementation ✅