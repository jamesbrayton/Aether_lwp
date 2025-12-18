# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Aether_lwp** is an Android live wallpaper application that combines user-selected background images with GPU-accelerated particle effects and gyroscope-based 3D parallax motion using GLSL shaders.

**Current Status**: The project is in early setup phase on the `mvp` branch. Development infrastructure is complete, but the Android project structure and source code have not yet been created.

## Development Environment

This project uses a Docker-based development environment configured in `.devcontainer/`.
The container runs as **x86_64** (`--platform=linux/amd64`) for Java/Android SDK compatibility.
On M-series Macs, Rosetta 2 handles ARM translation transparently.

### Environment Specifications
- **Container Architecture**: x86_64 (linux/amd64)
- **Java**: JDK 21 (Eclipse Temurin) at `/usr/lib/jvm/java-21`
- **Gradle**: 8.7
- **Kotlin**: 1.9.23
- **Android SDK**: API 34, build-tools 34.0.0
- **NDK**: 26.1.10909125
- **CMake**: 3.22.1
- **Graphics**: OpenGL ES 2.0 with GLSL shaders
- **Min Android API**: 21 (Android 5.0+)

### Build Commands (Once Android Project Created)

```bash
# Build the project
./gradlew build

# Run lint checks
./gradlew lint

# Run unit tests
./gradlew test

# Run Android instrumentation tests
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "com.aether.wallpaper.ShaderLoaderTest"

# Clean build
./gradlew clean

# Assemble debug APK
./gradlew assembleDebug

# Install to connected device
./gradlew installDebug
```

## Architecture

### Core Components (Planned)

The application follows a clean, modular architecture:

```
Settings Activity (Configuration UI)
    ↓ (persists config to JSON)
SharedPreferences
    ↓ (reads on startup)
Live Wallpaper Service
    └── OpenGL ES 2.0 Renderer
        ├── Shader Manager (loads GLSL from assets/shaders/)
        ├── Texture Manager (loads user images)
        ├── Multi-pass Compositor (blends layers)
        ├── Gyroscope Handler (sensor input)
        └── Render Loop (30/60 FPS configurable)
    ↓ (uses)
GLSL Shader Library (modular particle effects)
```

### Key Classes

Located in `app/src/main/java/com/aether/wallpaper/`:

- **WallpaperService.java**: Main live wallpaper service, extends Android WallpaperService
- **GLRenderer.java**: OpenGL ES 2.0 rendering engine, handles render loop and framebuffer management
- **ShaderLoader.java**: Loads and compiles GLSL shaders from assets, manages shader programs
- **LayerManager.java**: Manages multi-layer composition, ordering, and blending
- **GyroscopeHandler.java**: Processes gyroscope sensor data for parallax effects
- **SettingsActivity.java**: Main configuration UI with live preview
- **ImageCropActivity.java**: Interactive image cropping tool
- **LayerConfigAdapter.java**: RecyclerView adapter for layer management UI

### Shader System

GLSL shaders are stored in `app/src/main/assets/shaders/`:

- **vertex_shader.vert**: Shared vertex shader for fullscreen quad rendering
- **Effect Shaders** (fragment shaders):
  - `snow.frag` - Falling snow with lateral drift
  - `rain.frag` - Fast rain streaks with motion blur
  - `bubbles.frag` - Rising bubbles with wobble
  - `dust.frag` - Slow floating dust with Brownian motion
  - `smoke.frag` - Rising smoke with expansion and fade

#### Standard Shader Uniforms

All effect shaders receive these uniforms:
- `sampler2D u_backgroundTexture` - User's background image
- `float u_time` - Animation time in seconds
- `vec2 u_resolution` - Screen resolution
- `vec2 u_gyroOffset` - Gyroscope-based parallax offset
- `float u_depthValue` - Layer depth (0.0=far, 1.0=near)
- Effect-specific parameters (e.g., `u_particleCount`, `u_speed`)

#### Adding New Shader Effects

1. Create new `.frag` file in `assets/shaders/` using standard uniforms
2. Register effect in ShaderLoader effect registry
3. Add UI preview thumbnail and parameter controls in SettingsActivity
4. Document shader parameters and behavior

### Data Persistence

Configuration is stored in SharedPreferences as JSON:

```json
{
  "background": {
    "uri": "content://media/external/images/media/123",
    "crop": {"x": 100, "y": 200, "width": 1080, "height": 1920}
  },
  "layers": [
    {
      "type": "particle_effect",
      "shader": "smoke",
      "order": 1,
      "enabled": true,
      "opacity": 0.8,
      "depth": 0.3,
      "params": {"particleCount": 50, "speed": 1.2}
    }
  ]
}
```

### Multi-Layer Rendering

The system supports 3-5 simultaneous particle effect layers:
- Each layer renders to a texture via framebuffer
- Layers composite in order with alpha blending
- Per-layer opacity and depth control
- Parallax offset calculated per layer based on depth value

### Gyroscope Parallax

- Samples at 30-60 Hz with low-pass filter
- Formula: `layerOffset = tiltAngle × depthValue × sensitivity`
- Depth 0.0 = background (minimal movement)
- Depth 1.0 = foreground (maximum movement)

## Development Workflow (Required)

This project follows **Test-Driven Development (TDD)** with Gherkin specifications as mandated by AGENTS.md.

### For New Features:
1. Write Gherkin spec (`.feature` file) in `spec/` folder describing behavior (Given/When/Then)
2. Convert spec to failing unit/integration test
3. Run tests - verify failure
4. Implement production code to pass test
5. Refactor while keeping tests green
6. **Commit immediately** with descriptive message (e.g., `feature-x: add particle opacity control`)

### For Bug Fixes:
1. Write failing test reproducing the bug
2. Run tests - verify failure
3. Fix the bug
4. Refactor while keeping tests green
5. **Commit immediately**

### For Refactoring:
1. Ensure current tests pass
2. Make incremental changes, update tests if needed
3. Maintain behavior
4. **Commit immediately**

### PR Requirements:
- All tests must pass
- No lint/static-analysis errors
- Memory Bank entries updated (see below)
- PR description must include: summary, new features/fixes, performance improvements, refactoring, new dependencies, config changes, documentation updates, known issues

## Memory Bank Integration (CRITICAL)

This project uses the **memorybank MCP server** (`.memoryBank/` directory) to persist architectural decisions and context between Claude Code sessions.

### Before EVERY Task:
1. **Query Memory Bank** for relevant prior decisions, design patterns, known limitations
2. **Read in hierarchical order**: `projectBrief.md` → context files → `activeContext.md` → `progress.md`
3. Make informed decisions based on historical context

### After Changes:
1. **Update Memory Bank** with new decisions, features, or patterns
2. **Update in reverse order**: `progress.md` → `activeContext.md` → context files → `projectBrief.md`
3. Use YAML frontmatter tags: `#foundation`, `#active_work`, `#status_tracking`, `#feature`, `#bug`

### Memory Bank Files:
- **projectBrief.md**: Core project vision and foundation (rarely changes)
- **activeContext.md**: Current work focus (regenerate after merges)
- **progress.md**: Chronological implementation log
- **Custom files**: Feature specs, bug reports, API docs, integration guides

### Memory Bank Merge Conflicts:
- `projectBrief.md`: Requires human review if conflicts
- `activeContext.md`: Regenerate from merged context files
- `progress.md`: Merge both timelines chronologically
- Context files: Merge semantically, keep both perspectives unless contradictory

## Context7 MCP Server

Use the **context7 MCP server** when:
- Generating code requiring library/API documentation
- Looking up Android API references
- Researching OpenGL ES or GLSL documentation
- Checking Kotlin/Java syntax and best practices

Do NOT manually search for documentation - use Context7 automatically.

## Coding Standards

- **Design Patterns**: SOLID principles, prefer composition over inheritance
- **Naming**: Meaningful variable/function names, follow Android conventions
- **Documentation**: Public APIs must have KDoc/Javadoc comments, complex logic needs justification
- **Formatting**: Follow Android code style (2-space indentation for XML, 4-space for Kotlin/Java)
- **Error Handling**: Validate at system boundaries (user input, external APIs), trust internal code

### What NOT to Do:
- Do not over-engineer or add features beyond requirements
- Do not add unnecessary abstractions for one-time operations
- Do not add comments to code you didn't change
- Do not implement error handling for impossible scenarios
- Do not use backwards-compatibility hacks (delete unused code completely)

## Performance Considerations

### Target Performance:
- 60 FPS on mid-range devices (configurable 30 FPS fallback)
- Moderate battery consumption (continuous GPU usage)
- Efficient memory usage with bitmap sampling and texture compression

### Optimization Strategies:
- Minimize texture lookups in shaders
- Avoid expensive shader operations (pow, sin/cos in tight loops)
- Use lower precision where acceptable (mediump float)
- Limit active layers to 3-5
- Object pooling for particles
- Frame rate throttling option
- Resolution scaling on weaker devices

### Testing Performance:
- Frame rate measurement across devices
- Battery consumption analysis
- Memory usage monitoring
- Thermal testing for extended use

## Testing Requirements

### Test Categories:
- **Unit Tests**: Business logic, configuration parsing, shader parameter validation
- **Integration Tests**: Shader compilation, layer compositing, sensor data processing
- **Performance Tests**: Frame rate benchmarks, memory usage, battery impact
- **Compatibility Tests**: Multiple Android versions, screen sizes, GPU vendors (Qualcomm, Mali)

### Test Frameworks (To Be Configured):
- JUnit for unit tests
- Robolectric for Android unit tests without emulator
- Espresso for UI instrumentation tests

## Common Issues and Limitations

### Device Compatibility:
- Requires OpenGL ES 2.0 (universal on Android 5.0+)
- Older devices may need reduced effects or Canvas 2D fallback
- GPU-specific shader quirks (test on Qualcomm and Mali)

### Performance:
- Too many layers (>5) can cause frame drops
- Very large background images need bitmap sampling
- Continuous rendering impacts battery life

### Permissions:
- Storage access for image selection (scoped storage on API 30+)
- Sensor access for gyroscope parallax

## Future Enhancements (Out of Scope for V1)

- Sticker/Image layers with transparent PNGs
- Interactive effects (touch response)
- Time-based effects (day/night cycle)
- Weather integration
- OpenGL ES 3.0+ compute shaders
- Community shader library

## Important Files

- **docs/initial_requirements.md**: Comprehensive feature specifications and technical requirements
- **AGENTS.md**: Developer workflow, TDD process, Memory Bank usage, PR requirements
- **.devcontainer/**: Docker development environment configuration
- **.vscode/mcp.json**: MCP server configuration (Context7, Memory Bank)

## Project Status

**Branch**: `mvp`
**Phase**: Infrastructure setup complete, awaiting Android project creation
**Next Steps**: Create Android project structure, set up Gradle build, implement core WallpaperService
