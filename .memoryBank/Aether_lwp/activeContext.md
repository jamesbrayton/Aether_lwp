---
tags: #active_work #phase1_in_progress
updated: 2025-12-18
phase: Phase 1 Implementation - Component #6 Complete
---

# Active Context - Phase 1 Implementation (55% Complete)

## Current Status
**Branch:** `phase3`
**Phase:** Phase 1 Implementation - Core Systems Complete
**Progress:** 6/11 components complete (55%)
**Infrastructure:** ✅ Complete (devcontainer, CI/CD, emulator, clean workflow)  

## Latest Development (2025-12-18)

### Texture Manager Complete ✅

**Components Implemented:**
1. ✅ TextureManager with bitmap loading and OpenGL upload
2. ✅ Efficient sampling for large images (OOM prevention)
3. ✅ EXIF orientation support
4. ✅ Bitmap cropping integration
5. ✅ 35 instrumentation tests validating texture operations

**Key Features:**
- Load bitmaps from ContentResolver URIs
- Automatic sampling based on target resolution (memory efficient)
- Calculate sample sizes (powers of 2) for optimal GPU performance
- EXIF orientation correction (JPEG rotation/flip)
- Crop bitmaps to specified regions
- OpenGL texture creation with proper parameters
- Texture lifecycle management (create, bind, release)
- Placeholder texture generation (1x1 solid color)
- OOM recovery with fallback sample sizes (2, 4, 8, 16)

**Memory Optimization:**
- Sample size 2: reduces dimensions by 2x (25% memory)
- Sample size 4: reduces dimensions by 4x (6.25% memory)
- ARGB_8888: 4 bytes/pixel (~8MB for 1080x1920)
- RGB_565: 2 bytes/pixel (~4MB for 1080x1920, no alpha)

### CI/CD Workflow Optimized ✅

**Problem Solved:** Emulator testing reliability and cost optimization

**User Requirement:**
> "Main will only ever get changes by way of PR (branch protection). Releases should be push-button actions in GitHub UI, not every PR merge."

**Solution Implemented:**

**Workflow Behavior:**

| Event | Lint | Unit Tests | Debug APK | Instrumentation Tests | Release APK | GitHub Release |
|-------|------|-----------|-----------|----------------------|-------------|----------------|
| **Push to any branch** | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| **PR to main** | ✅ | ✅ | ✅ | ✅ (API 26, 30, 34) | ❌ | ❌ |
| **Manual: Run workflow** | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ |

**Key Features:**
- Debug builds on ANY branch push (uses `branches: - '**'` pattern)
- Instrumentation tests run ONLY on PRs (saves CI minutes)
- Instrumentation tests run on **Ubuntu with KVM** (free, fast, reliable)
- Releases are MANUAL ONLY via GitHub Actions UI
- No automatic releases (prevents accidental releases on PR merge)

**Emulator Configuration (Ubuntu + KVM):**
- **Runner**: `ubuntu-latest` (free vs macOS 10x cost)
- **Architecture**: `x86_64` with KVM hardware acceleration
- **API Levels**: 26, 30, 34 (matrix strategy)
- **Emulator options**: headless, software GPU, no audio/animations
- **Caching**: AVD and Gradle cached (3-8 minute savings per run)
- **Performance**: 5-10 minutes first run, 1-2 minutes cached

**Why Ubuntu + KVM?**
- ✅ Free (Linux runners have zero cost)
- ✅ Fast (KVM hardware acceleration)
- ✅ Reliable (industry standard for Android CI/CD)
- ✅ Compatible (x86_64 matches most Android devices)

## Active Work: Phase 1 (MVP)

### Completed Components (6/11)
1. ✅ **Project Setup** - Android project structure, Gradle build
2. ✅ **ShaderMetadataParser & Registry** - Metadata parsing, shader discovery (25+ tests)
3. ✅ **ShaderLoader** - GLSL compilation, linking, error handling (17 tests)
4. ✅ **GLRenderer** - OpenGL ES 2.0 renderer with 60fps loop (16 tests)
5. ✅ **Configuration System** - SharedPreferences + JSON persistence (24 tests)
6. ✅ **Texture Manager** - Bitmap loading and OpenGL upload (35 tests)

### Next Component: Snow Shader Effect
**Duration:** 1-2 days
**Objectives:**
- Implement snow.frag shader with particle simulation
- Falling snow with lateral drift
- Configurable parameters (particle count, speed, drift)
- Depth-based parallax for gyroscope (Phase 2)
- Integration with GLRenderer and background texture

**Gherkin Spec:** `spec/snow-shader.feature` (to be written)

### Remaining Components (5/11)
7. ⏳ **Snow Shader** (1-2 days) - Next
8. **Rain Shader** (1-2 days)
9. **Settings Activity** (2 days)
10. **Image Cropping Integration** (1 day)
11. **Live Wallpaper Service** (2 days)

## Development Workflow (Updated)

### Iteration Cycle
```bash
# 1. Code (Devcontainer)
git checkout -b feature/new-component
# ... edit files in VSCode/Claude Code ...
git add .
git commit -m "feature: implement new component"
git push origin feature/new-component

# 2. Automatic Build (GitHub Actions)
# - Triggers automatically on push
# - Runs lint + unit tests
# - Builds debug APK
# - Uploads APK artifact (7 days)

# 3. Create PR
gh pr create --title "feat: new component" --base main

# 4. PR Build (GitHub Actions)
# - Runs lint + unit tests
# - Runs instrumentation tests (API 26, 30, 34)
# - Builds debug APK
# - Validates full test suite

# 5. After PR Merge
# - Main updated
# - No automatic release
# - Ready for next feature

# 6. Manual Release (When Ready)
# - GitHub UI: Actions → Run workflow
# - Select main branch
# - Check "Create GitHub Release"
# - Creates release APK + GitHub release with ZeroVer tag
```

### TDD Process (Per Component)
1. Write Gherkin spec in `spec/*.feature`
2. Write failing tests (unit or instrumentation)
3. Implement feature
4. Push to GitHub → automated build runs tests
5. Refactor while keeping tests green
6. Create PR → instrumentation tests run
7. Merge after approval

### Testing Strategy
- **Unit Tests:** Robolectric (fast, run on every push)
- **Instrumentation Tests:** Real OpenGL/Android (run on PRs only)
- **Manual Testing:** Download APK → install to emulator
- **Coverage Target:** 80%+

## Infrastructure Components

**Devcontainer:**
- ✅ JDK 21 (Eclipse Temurin)
- ✅ Gradle 8.7
- ✅ Android SDK 34
- ✅ Kotlin 1.9.23
- ✅ GitHub CLI (gh)
- ✅ Git configured

**CI/CD (GitHub Actions):**
- ✅ Workflow file: `.github/workflows/build.yml`
- ✅ Auto-build on any branch push: `branches: - '**'`
- ✅ Instrumentation tests on PRs only
- ✅ Manual releases via `workflow_dispatch`
- ✅ Debug APK artifact (always built)
- ✅ ZeroVer (0ver) versioning

**Documentation:**
- ✅ BUILD.md (build instructions)
- ✅ DEVELOPMENT_HANDOFF.md (IDE workflow)
- ⏳ CI_CD.md (needs update with new workflow)
- ✅ ARM_DEVELOPMENT.md (M-series Mac guide)
- ⏳ RELEASE.md (needs update with manual release process)
- ✅ QUICK_REFERENCE.md (push-button builds)
- ✅ CONTRIBUTING.md (contribution guide)

**Emulator:**
- ✅ ARM64 (arm64-v8a) system image, API 35
- ✅ Pixel emulator running
- ✅ ADB connectivity
- ✅ APK installation validated

## Shader Metadata System

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
- `uniform sampler2D u_backgroundTexture;` - User's background image
- `uniform float u_time;` - Animation time in seconds
- `uniform vec2 u_resolution;` - Screen resolution
- `uniform vec2 u_gyroOffset;` - Gyroscope offset (Phase 2)
- `uniform float u_depthValue;` - Layer depth (Phase 2)

**Rationale:** Consistency, GPU optimization, future-proof

## Developer Experience: Adding New Shaders

**Current Workflow (Phase 1):**
```bash
# 1. Create shader.frag with metadata
# 2. Place in assets/shaders/
git add app/src/main/assets/shaders/new-effect.frag
git commit -m "feature: add new effect shader"
git push origin feature/new-effect

# 3. Automatic build triggered
# - ShaderRegistry discovers shader
# - ShaderLoader compiles shader
# - Tests validate compilation
# - Debug APK available in artifacts

# Effect automatically available ✅
```

**Time: < 5 minutes**  
**Code changes: 0**

## Key Technical Decisions

### CI/CD Workflow (Updated 2025-12-18)
- **Pattern:** `branches: - '**'` matches all branches
- **PR-Only Tests:** Instrumentation tests run on PRs to save CI minutes
- **Manual Releases:** Via GitHub UI, prevents accidental releases
- **Branch Protection:** Assumes main requires PR, no direct pushes

### Shader Architecture (Established 2025-12-17)
- **Metadata:** Embedded in GLSL files (JavaDoc-style)
- **Discovery:** Runtime scanning of assets/shaders/
- **Compilation:** Validated with real OpenGL context
- **Standard Uniforms:** Required for all shaders (no exceptions)

### Build Architecture (Established 2025-12-17)
- **Code:** Devcontainer (portable environment)
- **Build:** GitHub Actions (cloud, reproducible)
- **Test:** Mac emulator (native performance)
- **Distribution:** APK artifacts (7 days) + GitHub releases (unlimited)

## Success Criteria

### Component #3 (ShaderLoader) ✅ COMPLETE
- ✅ Load shaders from assets
- ✅ Compile vertex and fragment shaders
- ✅ Metadata comments work with GLSL compiler
- ✅ Link shaders into programs
- ✅ Detailed error reporting
- ✅ 17 instrumentation tests passing

### CI/CD Workflow ✅ COMPLETE
- ✅ Debug builds on any branch
- ✅ No manual branch configuration
- ✅ Instrumentation tests on PRs
- ✅ Manual releases via GitHub UI
- ✅ No accidental releases

### Phase 1 Overall (In Progress)
- ✅ 6/11 components complete (55%)
- ✅ 117 tests passing (49 unit + 68 instrumentation)
- ✅ Zero-code shader addition validated
- ✅ 60fps rendering validated
- ✅ Configuration persistence validated
- ✅ Texture loading and memory optimization validated
- ⏳ Remaining: 5 components

## Known Constraints & Limitations
- OpenGL ES 2.0 only (ES 3.0 deferred)
- Max 3-5 simultaneous layers (performance)
- Bitmap sampling required for large images (OOM prevention)
- Continuous rendering impacts battery
- Metadata parser requires strict format
- Devcontainer cannot build Android apps → use GitHub Actions
- Linux ADB cannot manage Mac emulators → use Mac's native ADB
- Instrumentation tests require Linux KVM support (runs on ubuntu-latest)
- First PR build takes 5-10 minutes to create AVD cache
- Subsequent PR builds use cached AVD (1-2 minutes faster)

## Open Questions / Risks
- **Shader complexity:** Will procedural effects achieve desired quality?
- **Performance:** Can we maintain 60fps with 2 layers?
- **GPU compatibility:** Need early testing on Qualcomm/Mali
- **Battery:** Need baseline metrics from reference wallpapers
- **Release frequency:** How often should we create releases?

## Immediate Next Steps

**1. Implement Snow Shader Effect**
- Create Gherkin spec for snow particle simulation
- Implement snow.frag shader with falling particles
- Add lateral drift and configurable parameters
- Write integration tests with GLRenderer
- Test with real background textures

**2. Implement Rain Shader Effect**
- Create Gherkin spec for rain simulation
- Implement rain.frag shader with fast streaks
- Add motion blur and configurable parameters
- Write integration tests
- Validate performance with multiple layers

**3. Continue Phase 1 Implementation**
- Follow TDD workflow
- Commit frequently
- Run tests on every push
- Create PRs for review

---

**Key Innovation:**
- Embedded metadata → zero-code shader addition
- CI/CD optimization → PR-based workflow with manual releases
- Clean separation → code/build/test decoupled
- Type-safe configuration → immutable data classes with validation
- Memory optimization → automatic bitmap sampling with OOM recovery

**Status:** Phase 1 in progress, 55% complete, on track for MVP ✅