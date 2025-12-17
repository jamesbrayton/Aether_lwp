---
tags: #active_work #phase1_planning
updated: 2025-12-16
phase: Planning Complete
---

# Active Context - Phase 1 Planning Complete

## Current Status
**Branch:** `mvp`  
**Phase:** Design Complete - Awaiting User Approval to Begin Implementation  
**Infrastructure:** Complete (Docker devcontainer, SDK installed)  
**Android Project:** Not yet created  

## Latest Developments (2025-12-16 Evening)

### Critical Design Decision: Embedded Shader Metadata
User requirement: **"Make it easy to add new shaders"** - add .frag file, no code changes

**Solution: Embedded Metadata in GLSL Files**
- Metadata embedded in JavaDoc-style comments at top of .frag files
- Single source of truth (not `.frag` + `.json`)
- Compiler/linter safe (standard GLSL comments)
- Enables dynamic shader discovery and UI generation

**Key Design Decisions:**
1. **Format:** JavaDoc-style structured comments (Option A)
   - Familiar to developers, no external dependencies
   - Regex-based parsing (~100 lines)
   - Standard GLSL syntax (compilers ignore comments)
   
2. **Tags Added:**
   - `@source` - Source repository URL (GitHub, etc.)
   - `@license` - License identifier (MIT, Apache-2.0, etc.)
   - Critical for attribution and legal compliance
   
3. **No Thumbnails (YAGNI):**
   - Live preview better than static image
   - Reduces maintenance burden
   - Can auto-generate later if needed

4. **Standard Uniform Contract:**
   - ALL shaders MUST declare standard uniforms
   - No exceptions, no opt-out
   - Architectural consistency, future-proof for Phase 2 parallax
   - GPU optimizes away unused uniforms (zero cost)

## Active Work: Phase 1 (MVP) - Updated Plan

### Scope Decisions Made (2025-12-16)
1. **Language:** Kotlin (null safety, coroutines, modern syntax)
2. **Package Name:** `com.aether.wallpaper`
3. **Min SDK:** API 26 (Android 8.0) - 90%+ coverage
4. **Initial Effects:** 2 effects (Snow, Rain) - validate architecture
5. **Gyroscope Parallax:** Deferred to Phase 2
6. **Testing:** Both Robolectric + Instrumentation
7. **Image Cropping:** Android-Image-Cropper library
8. **Shader Metadata:** **Embedded in GLSL (NEW)**
9. **Standard Uniforms:** **Required for all shaders (NEW)**

### Phase 1 Objectives (Updated)
✅ Create Android project structure  
✅ **ShaderMetadataParser & ShaderRegistry (NEW)** - Core extensibility  
✅ OpenGL ES 2.0 rendering pipeline  
✅ Shader loading system with error handling  
✅ 2 particle effects with **embedded metadata** (snow.frag, rain.frag)  
✅ **Dynamic Settings UI** - parameter controls auto-generated from metadata  
✅ Background image selection + cropping  
✅ Configuration persistence (SharedPreferences + JSON)  
✅ Live wallpaper service lifecycle  
✅ Comprehensive test suite (80%+ coverage)  

### New Components Added to Phase 1
1. **ShaderMetadataParser.kt** - Parse JavaDoc-style comments from .frag files
2. **ShaderRegistry.kt** - Discover and catalog shaders from assets/
3. **ShaderDescriptor.kt** - Data model for parsed metadata
4. **ParameterDefinition.kt** - Generic parameter schema
5. **Dynamic UI Generation** - Generate controls from @param tags

### Phase 2 (Deferred)
- Multi-layer framebuffer compositing
- 3 additional effects (bubbles, dust, smoke)
- Gyroscope parallax with depth-based offsets
- User shader imports (Phase 2: validate uploaded .frag files)
- Drag-and-drop layer reordering UI
- Performance optimization (FPS throttling, resolution scaling)

### Phase 3+ (Future Vision)
- Shader marketplace/library
- Community shader submissions
- Auto-generated previews
- Shader version updates

## Immediate Next Steps (Pending User Approval)
1. Begin Phase 1 implementation
2. Create Android project structure with Gradle
3. Write first Gherkin spec: `spec/project-setup.feature`
4. Implement ShaderMetadataParser with comprehensive tests
5. Create snow.frag and rain.frag with full metadata

## Key Architectural Decisions (See architecture-decisions.md)
- **ADR-010:** Embedded Shader Metadata System (JavaDoc-style comments)
- **ADR-011:** Standard Uniform Contract (all shaders MUST declare standard uniforms)
- Procedural GPU particles (not CPU particles)
- SharedPreferences + JSON for config
- Framebuffer-based layer compositing (Phase 2)
- External cropping library (Android-Image-Cropper)

## Shader Metadata Example
```glsl
/**
 * @shader Falling Snow
 * @id snow
 * @version 1.0.0
 * @author Aether Team
 * @source https://github.com/...
 * @license MIT
 * @description Gentle falling snow with lateral drift
 * @tags winter, weather, particles
 * 
 * @param u_particleCount float 100.0 min=10.0 max=200.0 name="Particle Count"
 * @param u_speed float 1.0 min=0.1 max=3.0 name="Fall Speed"
 */
precision mediump float;
uniform sampler2D u_backgroundTexture;
uniform float u_time;
// ... shader code
```

## Developer Workflow: Adding New Shaders

**Phase 1 (bundled):**
1. Create `effect.frag` with metadata comments
2. Place in `assets/shaders/`
3. Rebuild app
4. **Effect automatically appears in Settings** ✅

**Phase 2+ (user imports):**
1. User creates `.frag` file
2. Import via Settings UI
3. Validation + compile test
4. Effect available immediately

**No code changes needed!**

## Known Constraints
- OpenGL ES 2.0 only (ES 3.0 deferred)
- Max 3-5 simultaneous layers (performance)
- Bitmap sampling required for large images (OOM prevention)
- Continuous rendering impacts battery (mitigate with FPS options)
- Metadata parser requires strict format adherence

## Open Questions / Risks
- **Shader complexity:** Will procedural snow/rain achieve desired visual quality?
- **Parser robustness:** Will regex-based parser handle edge cases well?
- **Performance:** Can we maintain 60fps with 2 layers + dynamic parameters?
- **GPU compatibility:** Need to test on Qualcomm and Mali GPUs early
- **Battery:** Need baseline battery metrics from reference wallpapers

## Success Criteria for Extensibility (NEW)
- Add shader in < 5 minutes
- 0 code changes needed
- UI adapts automatically to new parameters
- Settings shows shader name, description, author, license

---

**Key Innovation:** Metadata-driven architecture enables easy shader addition while maintaining architectural consistency. This positions the app for future user-contributed shaders and shader marketplace.
