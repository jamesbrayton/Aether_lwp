---
tags: #architecture #design_decisions #rationale
created: 2025-12-16
updated: 2025-12-18
---

# Architectural Decision Records (ADR)

## ADR-001: Kotlin as Primary Language

**Status:** Accepted  
**Date:** 2025-12-16  
**Context:** Need to choose between Java and Kotlin for Android development.

**Decision:** Use Kotlin 1.9.23 as the primary language.

**Rationale:**
- **Null Safety:** Eliminates most NullPointerExceptions at compile time
- **Coroutines:** Better async/await for background operations (texture loading, config I/O)
- **Conciseness:** ~40% less boilerplate than Java
- **Modern Features:** Extension functions, data classes, sealed classes
- **Interop:** Full Java library compatibility, can adapt OpenGL examples easily
- **Team Preference:** Kotlin is standard for modern Android development

**Consequences:**
- ✅ Safer, more maintainable code
- ✅ Better async handling for I/O operations
- ⚠️ Fewer OpenGL ES tutorials in Kotlin (mitigated: adapt Java examples)
- ⚠️ Slightly longer compile times than Java

---

## ADR-002: Minimum SDK API 26 (Android 8.0)

**Status:** Accepted  
**Date:** 2025-12-16  
**Context:** Need to balance device coverage with modern API access.

**Decision:** Set minSdkVersion = 26 (Android 8.0 Oreo)

**Rationale:**
- **Device Coverage:** 90%+ of active Android devices (Google Play statistics)
- **Modern APIs:** NotificationChannel, background limits, better permissions
- **Java 8 Features:** Can use lambdas, streams freely without desugaring overhead
- **Scoped Storage Prep:** Easier to implement when migrating to API 29+ requirements
- **Lifecycle Handling:** Improved component lifecycle management
- **Reduces Complexity:** Fewer compatibility workarounds needed

**Consequences:**
- ✅ Cleaner code with modern Android APIs
- ✅ Fewer edge cases for background service behavior
- ⚠️ Excludes ~10% of older devices
- ✅ Still compatible with most target users (mid-range devices from 2017+)

**Alternative Considered:** API 21 (Android 5.0)
- **Rejected:** Adds complexity for background handling, permissions, notifications
- Marginal device coverage gain (~5-7%) not worth maintenance burden

---

## ADR-003: Procedural GPU Particles (Not CPU-based)

**Status:** Accepted  
**Date:** 2025-12-16  
**Context:** Particle systems can be implemented on CPU (with vertex buffers) or GPU (procedurally generated in shaders).

**Decision:** Use procedural GLSL shaders for all particle effects.

**Rationale:**
- **Performance:** No CPU-GPU data transfer per frame, no vertex buffer updates
- **Scalability:** Can render 100+ particles without CPU overhead
- **Battery Efficiency:** GPU is already active for wallpaper rendering
- **Simplicity:** No particle lifecycle management, no object pooling needed
- **Shader Modularity:** Each effect is self-contained .frag file

**Implementation Approach:**
```glsl
// Pseudo-random particle generation from pixel coordinates + time
vec2 particlePos = hash2D(particleID) + vec2(0.0, u_time * speed);
particlePos = mod(particlePos, 1.0); // Wrap around screen
float alpha = particleAlpha(particlePos, particleID);
```

**Consequences:**
- ✅ Excellent performance (60fps with many particles)
- ✅ Simple architecture (no particle managers)
- ⚠️ Less flexible than CPU particles (harder to do physics interactions)
- ⚠️ Harder to debug (can't inspect individual particle state)
- ✅ Aligns with project goal of GPU-accelerated effects

**Alternative Considered:** CPU particles with VBOs
- **Rejected:** CPU overhead for updating positions, more complex lifecycle, worse battery

---

## ADR-004: SharedPreferences + JSON for Configuration

**Status:** Accepted  
**Date:** 2025-12-16  
**Context:** Need to persist wallpaper configuration (background, layers, parameters).

**Decision:** Use SharedPreferences with JSON serialization (Gson).

**Configuration Schema:**
```json
{
  "background": {
    "uri": "content://...",
    "crop": {"x": 100, "y": 200, "width": 1080, "height": 1920}
  },
  "layers": [
    {
      "shader": "snow",
      "order": 1,
      "enabled": true,
      "opacity": 0.8,
      "depth": 0.5,
      "params": {"particleCount": 100, "speed": 1.0}
    }
  ]
}
```

**Rationale:**
- **Simplicity:** No DB schema, migrations, or ORM overhead
- **Human-Readable:** Easy to inspect for debugging
- **Versioning:** Can add schema version field for future migrations
- **Small Data:** Config is < 5KB, fits SharedPreferences well
- **Fast Access:** In-memory cache, no query overhead
- **Type Safety:** Gson maps to Kotlin data classes

**Consequences:**
- ✅ Simple implementation, less code to maintain
- ✅ Easy to debug (can inspect SharedPreferences XML)
- ✅ Fast reads (critical for WallpaperService startup)
- ⚠️ Not suitable for complex queries (not needed here)
- ⚠️ No transactions (acceptable, single-writer pattern)

**Alternative Considered:** Room database
- **Rejected:** Overkill for simple key-value config, adds boilerplate and dependencies

---

## ADR-005: Framebuffer Multi-Layer Compositing (Phase 2)

**Status:** Accepted for Phase 2  
**Date:** 2025-12-16  
**Context:** Multiple particle effects must composite with correct layer ordering and opacity.

**Decision:** Render each layer to a separate framebuffer (FBO), then composite with alpha blending.

**Rendering Pipeline:**
```
1. Render background to FBO_background
2. Render layer 1 (smoke) to FBO_layer1
3. Render layer 2 (snow) to FBO_layer2
4. Composite: background + (layer1 * opacity1) + (layer2 * opacity2)
5. Output final composite to screen
```

**Rationale:**
- **True Layering:** Each effect renders independently, then blends
- **Per-Layer Opacity:** Can control alpha of entire layer (not just particles)
- **Depth Control:** Can apply parallax offset per layer
- **Flexibility:** Can reorder layers without changing shader code
- **Standard Technique:** Well-documented OpenGL pattern

**Consequences:**
- ✅ Clean separation of effects
- ✅ Per-layer opacity/depth control
- ⚠️ More GPU memory (3-5 textures at screen resolution)
- ⚠️ Slight performance cost (multiple render passes)
- ✅ Acceptable for 3-5 layers on modern GPUs

**Performance Mitigation:**
- Limit to 3-5 layers (enforce in UI)
- Use RGB565 or RGB888 textures (not RGBA if opaque layers)
- Release FBOs when layer disabled

**Alternative Considered:** Single-pass additive blending
- **Rejected:** No per-layer opacity control, no true layer ordering

---

## ADR-006: External Image Cropping Library

**Status:** Accepted  
**Date:** 2025-12-16  
**Context:** Users need to crop background images after selection.

**Decision:** Use Android-Image-Cropper library (com.github.CanHub:Android-Image-Cropper)

**Rationale:**
- **Battle-Tested:** 10k+ stars on GitHub, actively maintained
- **Feature-Complete:** Gesture-based crop, aspect ratio lock, rotation
- **Time Savings:** ~1-2 days development time vs custom implementation
- **Quality:** Professional UI/UX, accessibility support
- **Dependency Size:** ~100KB (acceptable cost)

**Integration:**
```kotlin
val cropIntent = CropImage.activity(imageUri)
    .setAspectRatio(9, 16) // Match typical phone screen
    .getIntent(this)
startActivityForResult(cropIntent, CROP_REQUEST_CODE)
```

**Consequences:**
- ✅ Professional crop experience
- ✅ Fast implementation
- ⚠️ External dependency (mitigated: well-maintained)
- ⚠️ Small APK size increase (~100KB)
- ✅ Can replace with custom implementation later if needed

**Alternative Considered:** Custom crop activity
- **Rejected:** Significant dev time, harder to get gestures right, not core differentiator

---

## ADR-007: Comprehensive Testing Strategy

**Status:** Accepted  
**Date:** 2025-12-16  
**Context:** Need reliable testing across business logic, rendering, and UI.

**Decision:** Multi-layer testing approach:
1. **Unit Tests (Robolectric + JUnit):** Business logic, config, shader loading
2. **Integration Tests (Instrumentation):** OpenGL rendering, shader compilation
3. **UI Tests (Espresso):** Settings activity workflows
4. **Performance Tests:** FPS measurement, memory profiling

**Rationale:**
- **Fast Feedback:** Robolectric unit tests run in < 5s on CI
- **Real Rendering:** Instrumentation tests validate actual OpenGL behavior
- **User Workflows:** Espresso ensures UI interactions work end-to-end
- **Performance Validation:** Catch regressions in FPS or memory early

**Coverage Targets:**
- Unit tests: 80%+ of business logic
- Integration tests: All critical rendering paths
- UI tests: All primary user workflows
- Performance tests: Baseline metrics on reference device

**Consequences:**
- ✅ High confidence in changes
- ✅ Fast local testing (unit tests)
- ⚠️ Longer CI pipeline (~5-10 min with instrumentation tests)
- ✅ Catches regressions early (OpenGL errors, config bugs)

**Test Organization:**
```
app/src/test/            # Robolectric unit tests
app/src/androidTest/     # Instrumentation + Espresso tests
spec/                    # Gherkin feature files
```

---

## ADR-008: Incremental Effect Implementation (Phase 1: 2 effects)

**Status:** Accepted  
**Date:** 2025-12-16  
**Context:** Initial requirements specify 5 particle effects (snow, rain, bubbles, dust, smoke).

**Decision:** Implement only Snow and Rain in Phase 1, defer Bubbles/Dust/Smoke to Phase 2.

**Rationale:**
- **Validate Architecture:** Ensure shader system, renderer, and config work before scaling
- **Risk Reduction:** Catch architectural issues early with smaller scope
- **Contrast Effects:** Snow (slow, downward) vs Rain (fast, angled) provides good variety
- **Faster MVP:** Get usable wallpaper faster, gather user feedback earlier
- **TDD Efficiency:** Easier to refactor with fewer effects in codebase

**Phase 1 Effects:**
1. **Snow:** Slow falling particles, lateral drift, gentle
2. **Rain:** Fast streaks, steep angle, motion blur

**Phase 2 Effects:**
3. **Bubbles:** Rising with wobble, varying sizes
4. **Dust:** Brownian motion, very slow
5. **Smoke:** Rising, expansion, fade-out

**Consequences:**
- ✅ Faster Phase 1 delivery (focus on quality over quantity)
- ✅ Easier testing and debugging
- ✅ Validates multi-layer architecture with 2 effects
- ⚠️ Users see limited effect variety initially
- ✅ Can gather feedback and adjust remaining effects based on learnings

**Exit Criteria for Phase 1:**
- Snow and Rain shaders render smoothly at 60fps
- Layer system supports 2+ simultaneous effects
- Settings UI allows adding/removing/configuring effects
- All tests pass with 80%+ coverage

---

## ADR-009: Defer Gyroscope Parallax to Phase 2

**Status:** Accepted  
**Date:** 2025-12-16  
**Context:** Gyroscope parallax is a key feature but adds sensor handling complexity.

**Decision:** Implement gyroscope parallax in Phase 2, not Phase 1 MVP.

**Rationale:**
- **Focus on Foundation:** Phase 1 should nail rendering pipeline and shader system
- **Complexity:** Gyroscope requires sensor management, filtering, calibration
- **Independent Feature:** Parallax can be added without changing shader architecture
- **Testability:** Easier to test static rendering first, then add motion
- **Risk Management:** Sensor jitter and nausea risks handled separately

**Phase 1:** Static rendering (no parallax)
- Shaders receive `u_gyroOffset = vec2(0.0, 0.0)`
- Background and layers render without depth offset

**Phase 2:** Add gyroscope parallax
- `GyroscopeHandler` class with low-pass filter
- Per-layer depth values (0.0 = far, 1.0 = near)
- Shader updates: `parallaxUV = uv + (u_gyroOffset * u_depthValue)`
- Settings UI: depth sliders, sensitivity control

**Consequences:**
- ✅ Cleaner Phase 1 implementation (no sensor code)
- ✅ Can validate shader quality without parallax motion
- ⚠️ Phase 1 wallpaper lacks key differentiator (acceptable for MVP)
- ✅ Parallax can be refined based on Phase 1 feedback

**Implementation Note:**
All Phase 1 shaders should include `u_gyroOffset` and `u_depthValue` uniforms (set to 0.0) so Phase 2 is a simple enhancement, not refactor.

---

## ADR-010: Embedded Shader Metadata System

**Status:** Accepted  
**Date:** 2025-12-16  
**Context:** Need extensible shader system that supports easy addition of effects without code changes. Future phases require user-imported shaders and shader marketplace.

**Decision:** Embed shader metadata in GLSL files using JavaDoc-style structured comments. Each shader is a single `.frag` file containing both GLSL code and metadata.

**Metadata Format:**
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
 * @param u_particleCount float 100.0 min=10.0 max=200.0 step=1.0 name="Particle Count" desc="Number of visible particles"
 * @param u_speed float 1.0 min=0.1 max=3.0 step=0.1 name="Fall Speed" desc="How fast snow falls"
 */

precision mediump float;
// ... GLSL code
```

**Rationale:**

**1. Single Source of Truth:**
- One file per shader (not `.frag` + `.json`)
- Metadata cannot get out of sync with code
- Easier maintenance (update one file, not two)
- Simpler for user imports: "Pick a .frag file" - done

**2. Compiler/Linter Safe:**
- Block comments (`/** */`) are standard GLSL syntax (GLSL ES spec section 3.1)
- All GLSL compilers strip comments during preprocessing
- No custom compile logic needed
- Works with all GLSL linters and editors

**3. Industry Standard Pattern:**
- ShaderToy uses comment metadata
- Unity shaders use `Properties` blocks
- Godot uses comment annotations
- Familiar to shader developers

**4. Extensibility Path:**
- **Phase 1:** Scan `assets/shaders/*.frag`, parse metadata, generate UI
- **Phase 2:** User imports - validate single `.frag` file
- **Phase 3:** Marketplace - download single file with metadata embedded

**5. Dynamic UI Generation:**
- `@param` tags define parameter controls automatically
- No hardcoded shader names or parameter lists
- Adding shader = drop `.frag` file in assets, rebuild (Phase 1)
- Adding shader = import file via UI (Phase 2+)

**Tag Specification:**

| Tag | Required | Format | Example |
|-----|----------|--------|---------|
| `@shader` | Yes | String | `@shader Falling Snow` |
| `@id` | Yes | Identifier | `@id snow` |
| `@version` | Yes | Semver | `@version 1.0.0` |
| `@author` | No | String | `@author Aether Team` |
| `@source` | No | URL | `@source https://github.com/...` |
| `@license` | No | SPDX ID | `@license MIT` |
| `@description` | No | String | `@description Gentle...` |
| `@tags` | No | CSV | `@tags winter, weather` |
| `@minOpenGL` | No | Version | `@minOpenGL 2.0` |
| `@param` | No | See below | `@param u_speed float 1.0 ...` |

**@param Format:**
```
@param <uniform_name> <type> <default> [min=<min>] [max=<max>] [step=<step>] [name="<display>"] [desc="<description>"]
```

Supported types: `float`, `int`, `bool`, `color` (vec3), `vec2`, `vec3`, `vec4`

**Implementation Components:**

1. **ShaderMetadataParser.kt:** Parse JavaDoc comments, extract metadata
2. **ShaderRegistry.kt:** Scan assets/shaders/, catalog discovered shaders
3. **ShaderDescriptor.kt:** Data model for parsed metadata
4. **Dynamic UI:** Generate parameter controls from `@param` definitions

**Validation:**
- **Phase 1:** Basic validation (required tags present, parse succeeds), log warnings
- **Phase 2:** Strict validation for user imports (compile test, uniform verification)

**Consequences:**
- ✅ No two-file sync issues (metadata embedded in shader)
- ✅ Easy shader addition: add `.frag` file, no code changes
- ✅ Zero external dependencies (regex-based parsing)
- ✅ Compiler/linter safe (standard GLSL comments)
- ✅ Enables user imports and marketplace (Phase 2+)
- ✅ Familiar pattern for shader developers
- ⚠️ Custom parser needed (simple regex-based, ~100 lines)

**Alternative Considered: Separate JSON metadata files**
- **Rejected:** Two-file sync problem, maintenance burden, more error-prone

**Alternative Considered: YAML front-matter**
- **Rejected:** Requires YAML parser library (~300KB dependency)

**Alternative Considered: GLSL `#pragma` directives**
- **Rejected:** Less familiar than JavaDoc-style, harder to read/write

---

## ADR-011: Standard Uniform Contract for All Shaders

**Status:** Accepted  
**Date:** 2025-12-16  
**Context:** Need consistent interface between renderer and shaders. Should all shaders support the same base uniforms, or allow opt-out?

**Decision:** ALL shaders MUST declare and support the standard uniforms. No exceptions, no opt-out.

**Standard Uniforms (Required):**
```glsl
uniform sampler2D u_backgroundTexture;  // User's background image
uniform float u_time;                   // Animation time in seconds
uniform vec2 u_resolution;              // Screen resolution in pixels
uniform vec2 u_gyroOffset;              // Gyroscope parallax offset (Phase 2)
uniform float u_depthValue;             // Layer depth for parallax (Phase 2)
```

**Rationale:**

**1. Architectural Consistency:**
- All shaders have identical interface contract
- GLRenderer uses single code path for all shaders
- No per-shader branching or special cases
- Simpler, more maintainable renderer

**2. GPU Optimization:**
- Unused uniforms are optimized away by GLSL compiler
- Zero performance cost for unused uniforms
- No need to conditionally set uniforms

**3. Business Logic:**
- These uniforms represent core wallpaper features:
  - `u_backgroundTexture`: User customization (core feature)
  - `u_time`: Animation (core requirement)
  - `u_resolution`: Screen adaptation (essential)
  - `u_gyroOffset`, `u_depthValue`: Parallax (Phase 2 feature)

**4. Future-Proof:**
- When gyroscope added in Phase 2, all shaders "just work"
- No shader rewrites needed for new features
- Consistent behavior across all effects

**5. Developer Experience:**
- Shader authors copy standard template
- No guessing which uniforms to declare
- Clear contract documented in shader template

**Enforcement:**
- **Phase 1:** Log warning if shader missing standard uniform (trust built-ins)
- **Phase 2:** Strict validation for user imports - reject if missing uniforms

**Shader Template:**
```glsl
/**
 * @shader Effect Name
 * @id effect_id
 * ... metadata
 */

precision mediump float;

// REQUIRED: Standard uniforms (DO NOT REMOVE)
uniform sampler2D u_backgroundTexture;
uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_gyroOffset;
uniform float u_depthValue;

// Optional: Effect-specific parameters
uniform float u_customParam;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    
    // Always sample background (even if not used visually)
    vec4 background = texture2D(u_backgroundTexture, uv);
    
    // Generate effect
    vec3 effect = vec3(0.0);
    // ... effect logic
    
    // Composite
    gl_FragColor = background + vec4(effect, 1.0);
}
```

**Consequences:**
- ✅ Simple, consistent renderer implementation
- ✅ All shaders work with all features (background, parallax)
- ✅ Zero performance cost (GPU optimizes unused uniforms)
- ✅ Clear contract for shader developers
- ⚠️ Shaders must declare uniforms even if unused (acceptable - boilerplate is minimal)

**This is non-negotiable - standard uniforms are the shader contract.**

---

## ADR-012: Docker for Development, Not Kubernetes

**Status:** Accepted  
**Date:** 2025-12-18  
**Context:** Experimented with Kubernetes-based devcontainers (via DevPod) to enable cloud-based development. Encountered platform architecture constraints.

**Decision:** Use Docker-based devcontainers locally. Defer remote/cloud development to future exploration.

**Problem Discovered:**
- **Rancher Desktop** runs a single-node Kubernetes cluster
- Kubernetes nodes have a single architecture (ARM64 on M-series Macs)
- When ARM workloads already exist on a node, Kubernetes **cannot provision x86/amd64 workloads**
- Mixed architecture workloads require multi-node clusters with architecture-specific node selectors
- Single-node clusters (like Rancher Desktop) cannot run both ARM and x86 containers simultaneously

**Root Cause:**
- Kubernetes pod scheduling considers node architecture
- Image manifest list contains platform-specific variants
- When pulling `ghcr.io/username/image:latest`, Kubernetes checks manifest list
- If only `linux/amd64` variant exists, but node is `linux/arm64`, pull fails
- Error: `"no matching manifest for linux/arm64/v8 in the manifest list"`

**Attempted Solutions (Failed):**
1. **Build multi-platform image** - Would work if node could run both architectures
2. **Force platform via `--platform` flag** - Only works for Docker, not Kubernetes pod specs
3. **RuntimeClass with emulation** - Requires multi-node cluster or QEMU integration
4. **DevPod platform flag** - DevPod respects host architecture, not custom platform specs

**Why This Matters:**
- Android SDK tools (AAPT2, build-tools) are compiled for x86_64
- Devcontainer must run as x86_64 for Android builds to work
- Rancher Desktop is convenient for local Kubernetes development (already installed for other projects)
- Running devcontainer in Kubernetes would enable future cloud/remote development workflows

**Decision Rationale:**
1. **Local Development:** Docker-based devcontainer works perfectly
   - `docker run --platform linux/amd64` forces x86_64 container
   - Rosetta 2 handles emulation transparently on M-series Macs
   - Zero configuration required, works immediately

2. **Cloud Development (Future):** Requires different approach
   - Multi-node Kubernetes cluster with x86_64 node pool
   - GitHub Codespaces (native x86_64 infrastructure)
   - Cloud workstations (GCP, AWS) with x86_64 instances
   - Remote VSCode server on x86_64 host

3. **Not Worth VM Overhead:** Running additional VM for multi-node Kubernetes is overkill
   - Local Docker already works
   - Adds complexity (VM management, resource allocation)
   - No tangible benefit for local development

**Implementation:**
- ✅ Use Docker-based devcontainer for local development
- ✅ Keep existing Rancher Desktop for other Kubernetes projects
- ✅ Document Kubernetes constraint in Memory Bank and ARM_DEVELOPMENT.md
- ✅ Explore cloud-based devcontainers in future (Phase 3+)

**Devcontainer Configuration:**
```json
{
  "image": "ghcr.io/username/aether-android-dev:latest",
  "runArgs": ["--platform=linux/amd64"],
  "remoteEnv": {
    "DOCKER_DEFAULT_PLATFORM": "linux/amd64"
  }
}
```

**Dockerfile:**
```dockerfile
FROM --platform=linux/amd64 ubuntu:noble
# ... rest of build
```

**Build Command:**
```bash
DOCKER_DEFAULT_PLATFORM=linux/amd64 docker build --platform linux/amd64 -t image:latest .
```

**Consequences:**
- ✅ Local Docker devcontainer works perfectly (x86_64 + Rosetta 2)
- ✅ No additional VM overhead on development machine
- ✅ Can still use Rancher Desktop for ARM Kubernetes workloads (other projects)
- ✅ Clean separation: Docker for dev, GitHub Actions for CI/CD
- ⚠️ Remote/cloud development deferred (acceptable - not blocking Phase 1)
- ✅ Architecture constraint documented for future reference

**Future Exploration (Phase 3+):**
- GitHub Codespaces (native x86_64, no configuration needed)
- Remote development on cloud x86_64 instances (GCP/AWS)
- Multi-node Kubernetes cluster with architecture node selectors (if needed)

**Key Learning:**
Kubernetes single-node clusters cannot run mixed-architecture workloads. This is a fundamental Kubernetes scheduling constraint, not a DevPod or container runtime issue. For local development with architecture constraints, Docker is simpler and more flexible than Kubernetes.

---

## Summary of Key Architectural Choices

| Component | Decision | Rationale |
|-----------|----------|-----------|
| **Language** | Kotlin | Null safety, coroutines, modern syntax |
| **Min SDK** | API 26 | 90%+ coverage, modern APIs, less complexity |
| **Particles** | GPU Procedural | Performance, battery, simplicity |
| **Config** | SharedPreferences + JSON | Simple, fast, human-readable |
| **Layering** | Framebuffer Compositing | True opacity/depth control |
| **Cropping** | External Library | Battle-tested, time savings |
| **Testing** | Robolectric + Instrumentation | Fast feedback + real rendering |
| **Phase 1 Scope** | 2 effects, no parallax | Validate architecture, faster MVP |
| **Shader Metadata** | Embedded in GLSL (JavaDoc-style) | Single source of truth, extensible |
| **Shader Contract** | Standard uniforms required | Consistency, simplicity, future-proof |
| **Development** | Docker (not Kubernetes) | Local flexibility, no VM overhead |

---

## Open Architectural Questions

### Q1: Shader Complexity vs Performance
- **Question:** Can procedural shaders achieve desired visual quality for complex effects (smoke turbulence)?
- **Risk:** May need hybrid approach (GPU + CPU) for advanced effects
- **Mitigation:** Start simple, measure performance, optimize incrementally

### Q2: Battery Impact
- **Question:** How does continuous 60fps rendering compare to reference wallpapers?
- **Plan:** Benchmark against top live wallpapers (Muzei, KLWP)
- **Mitigation:** Frame rate throttling (30fps option), battery saver mode

### Q3: Multi-Layer Performance
- **Question:** Can mid-range devices handle 3-5 FBO layers at 60fps?
- **Plan:** Test on Pixel 4a, Galaxy A52 equivalents
- **Mitigation:** Resolution scaling, layer limit enforcement

### Q4: GPU Compatibility
- **Question:** Will shaders work consistently on Qualcomm, Mali, PowerVR GPUs?
- **Plan:** Test on diverse devices early, use standard GLSL ES 2.0 features
- **Mitigation:** Shader fallbacks, quality presets per GPU vendor