# Animated Live Wallpaper - Requirements Document

## Project Overview

An Android live wallpaper application that combines user-selected background images with GPU-accelerated particle effects and parallax motion. The system uses GLSL shaders for rendering effects and supports multi-layer compositions with gyroscope-based 3D parallax.

## Core Features

### 1. Background Image Management

- **Image Selection**: Users can select any image from their device gallery
- **Cropping**: Interactive crop tool to frame the desired portion of the image
- **Storage**: Selected images are stored with persistent URI permissions or copied to app storage
- **Memory Management**: Efficient loading using BitmapFactory with appropriate sampling for device screen size

### 2. Particle Effect System (GLSL Shaders)

Modular shader-based particle effects rendered on the GPU for optimal performance.

#### Initial Effect Library

1. **Falling Snow**
   - Gentle downward motion with side-to-side drift
   - Constant particle size
   - Soft, white particles
   - Configurable: density, speed, drift amount

2. **Falling Rain**
   - Fast downward motion at steep angle
   - Elongated streaks/drops
   - Motion blur effect
   - Configurable: density, speed, angle, streak length

3. **Rising Bubbles**
   - Upward motion with wobble/sine wave pattern
   - Particles pop/disappear at top of screen
   - Varying sizes
   - Configurable: density, rise speed, wobble amount, size variation

4. **Floating Dust**
   - Very slow, random floating motion
   - Small particles
   - Gentle Brownian motion
   - Configurable: density, movement speed, particle size

5. **Rising Smoke**
   - Upward motion with expansion
   - Fades out over time (alpha reduction)
   - Particles grow larger as they rise
   - Swirling/turbulent motion
   - Configurable: density, rise speed, dissipation rate, turbulence

#### Shader Architecture

- Each effect is a separate fragment shader file (`.frag`)
- Shared vertex shader for fullscreen quad rendering
- Location: `/assets/shaders/`
  - `snow.frag`
  - `rain.frag`
  - `bubbles.frag`
  - `dust.frag`
  - `smoke.frag`
  - `vertex_shader.vert` (shared)

#### Shader Inputs (Uniforms)

Standard uniforms passed to all shaders:

- `sampler2D u_backgroundTexture` - The background image
- `float u_time` - Animation time in seconds
- `vec2 u_resolution` - Screen resolution
- `float u_particleCount` - Number of particles to render
- `float u_speed` - Effect speed multiplier
- Effect-specific parameters as needed

### 3. Layer System

Multi-pass rendering pipeline supporting multiple simultaneous effects with compositing.

#### Layer Structure

```json
{
  "background": {
    "uri": "content://...",
    "crop": { "x": 100, "y": 200, "width": 1080, "height": 1920 }
  },
  "layers": [
    {
      "type": "particle_effect",
      "shader": "smoke",
      "order": 1,
      "enabled": true,
      "opacity": 0.8,
      "depth": 0.3,
      "params": {
        "particleCount": 50,
        "speed": 1.2,
        "turbulence": 0.5
      }
    },
    {
      "type": "particle_effect",
      "shader": "snow",
      "order": 2,
      "enabled": true,
      "opacity": 1.0,
      "depth": 0.9,
      "params": {
        "particleCount": 100,
        "speed": 0.8
      }
    }
  ]
}
```

#### Rendering Pipeline

1. Render background image to framebuffer
2. For each active layer (in order):
   - Execute layer's shader to render to texture
   - Composite onto result using alpha blending
3. Output final composite to screen

#### Layer Properties

- **Order**: Rendering sequence (lower numbers render first, appear behind)
- **Enabled**: Toggle layer on/off without removing
- **Opacity**: Overall layer transparency (0.0 - 1.0)
- **Depth**: Parallax depth value (0.0 = far, 1.0 = near) - see section 4
- **Parameters**: Effect-specific configuration values

#### Layer Management

- Users can add/remove/reorder layers via Settings UI
- Maximum recommended layers: 3-5 (for performance)
- Drag-and-drop reordering interface
- Per-layer parameter adjustment
- Live preview of changes

### 4. 3D Parallax Effects

Gyroscope-driven multi-layer parallax for depth perception.

#### Parallax System

- Device tilt (gyroscope) drives layer offsets
- Each layer moves proportionally to its depth value
- Formula: `layerOffset = tiltAngle × depthValue × sensitivity`

#### Depth Values

- `0.0` - Background, minimal movement
- `0.5` - Midground, moderate movement
- `1.0` - Foreground, maximum movement

#### Parallax Layers

Two types of layers support parallax:

1. **Particle Effects** (already defined above)
   - Particles at different depths move at different rates
   - Creates volumetric depth perception

2. **Sticker/Image Layers** (future enhancement)
   - Transparent PNG images with alpha channel
   - Positioned at specific depths
   - User-uploaded or from built-in library

#### Gyroscope Integration

- Sample rate: 30-60 Hz
- Low-pass filter to smooth jitter
- Calibration: Zero position when device is vertical
- Sensitivity adjustment in settings

#### Shader Implementation

```glsl
uniform vec2 u_gyroOffset;   // Device tilt offset
uniform float u_depthValue;  // Layer depth (0.0-1.0)

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec2 parallaxUV = uv + (u_gyroOffset * u_depthValue);
    // ... render with parallax offset
}
```

#### Optional Depth Effects

- **Depth fog**: Distant layers slightly faded
- **Focal blur**: Only one depth in focus
- **Motion blur**: On fast-moving foreground layers

### 5. Settings Activity

Regular Android application for configuration.

#### Main Features

- **Image Selection & Cropping**
  - Pick from gallery
  - Interactive crop tool with preview
  - Save cropped coordinates

- **Effect Selection**
  - Browse available effects
  - Visual previews/thumbnails of each effect
  - Add to active layers

- **Layer Management**
  - List view of active layers
  - Drag to reorder
  - Enable/disable toggles
  - Delete layers

- **Parameter Adjustment**
  - Per-layer sliders for parameters
  - Opacity control
  - Depth assignment
  - Speed/intensity controls

- **Live Preview**
  - Real-time preview of wallpaper
  - Gyroscope simulation (tilt device or slider)
  - Apply button to set as wallpaper

- **Global Settings**
  - Frame rate throttle (30/60 fps)
  - Battery saver mode (reduced effects)
  - Gyroscope sensitivity
  - Reset to defaults

#### Data Persistence

- Configuration saved to SharedPreferences as JSON
- Wallpaper service reads configuration on start
- Periodic refresh to detect changes (or broadcast receiver)

### 6. Live Wallpaper Service

Background service that continuously renders the wallpaper.

#### Core Responsibilities

- Extend `WallpaperService`
- Implement inner `Engine` class for rendering
- Create OpenGL ES 2.0 context
- Load shaders from assets
- Read configuration from SharedPreferences
- Render loop at configured frame rate
- Handle gyroscope sensor updates

#### Lifecycle

- Start: Load configuration, initialize OpenGL, load textures/shaders
- Run: Continuous render loop
- Stop: Release OpenGL resources, unregister sensors
- Settings change: Reload configuration and shaders

#### Performance Optimization

- Object pooling for particles
- Texture atlasing where appropriate
- Frame rate throttling option
- Resolution scaling for lower-end devices
- Limit active layer count

## Technical Architecture

### Technology Stack

- **Language**: Java or Kotlin
- **Graphics API**: OpenGL ES 2.0 (with GLSL shaders)
- **Platform**: Android (minimum SDK version TBD, recommend API 21+)
- **IDE**: Android Studio

### Component Architecture

```text
┌─────────────────────────────────────────┐
│         Settings Activity               │
│  (Image picker, cropper, layer config)  │
└────────────────┬────────────────────────┘
                 │
                 ▼
         SharedPreferences
           (JSON config)
                 │
                 ▼
┌─────────────────────────────────────────┐
│      Live Wallpaper Service             │
│  ┌─────────────────────────────────┐   │
│  │   OpenGL ES Renderer            │   │
│  │  - Shader loader                │   │
│  │  - Texture manager              │   │
│  │  - Multi-pass compositor        │   │
│  │  - Gyroscope handler            │   │
│  └─────────────────────────────────┘   │
└─────────────────────────────────────────┘
                 │
                 ▼
         /assets/shaders/
    (Modular GLSL shader files)
```

### File Structure

```text
/app/src/main/
├── java/com/yourapp/wallpaper/
│   ├── WallpaperService.java
│   ├── GLRenderer.java
│   ├── ShaderLoader.java
│   ├── LayerManager.java
│   ├── GyroscopeHandler.java
│   ├── SettingsActivity.java
│   ├── ImageCropActivity.java
│   └── LayerConfigAdapter.java
├── assets/shaders/
│   ├── vertex_shader.vert
│   ├── snow.frag
│   ├── rain.frag
│   ├── bubbles.frag
│   ├── dust.frag
│   └── smoke.frag
├── res/
│   ├── layout/
│   ├── drawable/
│   └── xml/
│       └── wallpaper.xml (wallpaper metadata)
└── AndroidManifest.xml
```

## Extensibility

### Adding New Particle Effects

To add a new effect, developers need to:

1. **Create new fragment shader** in `/assets/shaders/neweffect.frag`
   - Follow standard uniform naming conventions
   - Implement particle generation/animation logic
   - Composite with background texture

2. **Register effect in code**
   - Add to effect registry/enum
   - Provide default parameters
   - Create preview thumbnail

3. **Add to Settings UI**
   - Include in effect selection menu
   - Define parameter sliders if needed

4. **Documentation**
   - Comment shader code explaining algorithm
   - Document configurable parameters
   - Provide usage examples

### Shader Template

```glsl
// neweffect.frag
precision mediump float;

// Standard uniforms
uniform sampler2D u_backgroundTexture;
uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_gyroOffset;
uniform float u_depthValue;

// Effect-specific parameters
uniform float u_parameterOne;
uniform float u_parameterTwo;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    
    // Apply parallax
    vec2 parallaxUV = uv + (u_gyroOffset * u_depthValue);
    
    // Sample background
    vec4 background = texture2D(u_backgroundTexture, parallaxUV);
    
    // TODO: Generate particles and effects
    vec3 particles = vec3(0.0);
    // ... your effect logic here ...
    
    // Composite
    gl_FragColor = background + vec4(particles, 1.0);
}
```

## Performance Considerations

### Target Performance

- **Frame Rate**: 60 fps preferred, 30 fps acceptable
- **Battery Impact**: Moderate (continuous GPU usage)
- **Memory**: Efficient bitmap loading, texture compression

### Optimization Strategies

1. **Shader Optimization**
   - Minimize texture lookups
   - Avoid expensive operations (pow, sin/cos in tight loops)
   - Use lower precision where acceptable

2. **Multi-Pass Management**
   - Limit active layers (3-5 max recommended)
   - Render lower resolution on weaker devices
   - Batch similar effects when possible

3. **Frame Rate Control**
   - User-configurable (60/30 fps)
   - Battery saver mode (reduce particle count)
   - Sleep/lock screen pause

4. **Resource Management**
   - Texture pooling
   - Shader caching
   - Release resources when wallpaper inactive

### Device Compatibility

- **Minimum**: OpenGL ES 2.0 (nearly universal on modern Android)
- **Recommended**: Android 5.0+ (API 21)
- **Fallback**: Reduce effects or use Canvas 2D on very old devices

## User Experience Requirements

### Settings UI/UX

- Intuitive layer management (drag-and-drop)
- Real-time preview of changes
- Clear visual feedback
- Preset configurations for beginners
- Advanced mode for power users

### Accessibility

- "Reduce motion" option (disable parallax)
- High contrast mode compatibility
- Large touch targets for controls
- Screen reader support for settings

### Performance Feedback

- FPS indicator (debug mode)
- Battery usage estimate
- Device capability detection
- Warnings for intensive configurations

## Future Enhancements (Out of Scope for V1)

### Sticker/Image Layer System

- User-uploaded transparent PNGs
- Built-in sticker library (categorized)
- Auto background removal (ML Kit)
- Sticker marketplace/sharing

### Advanced Effects

- Per-pixel depth maps (depth-aware parallax)
- Interactive effects (touch response)
- Time-based effects (day/night cycle)
- Weather integration (real weather → matching effects)

### Social Features

- Share wallpaper configurations
- Community shader library
- Preset themes/packs

### Advanced Rendering

- OpenGL ES 3.0+ features
- Compute shaders for complex simulations
- Post-processing effects (bloom, blur)

## Testing Requirements

### Functional Testing

- Image selection and cropping
- Shader compilation success
- Layer ordering and compositing
- Gyroscope calibration
- Settings persistence
- Wallpaper service lifecycle

### Performance Testing

- Frame rate measurement across devices
- Battery consumption analysis
- Memory usage monitoring
- Thermal testing (extended use)

### Compatibility Testing

- Multiple Android versions
- Various screen sizes/resolutions
- Different GPU vendors (Qualcomm, Mali, etc.)
- Orientation changes

### Edge Cases

- Very large images
- Many simultaneous layers
- Rapid device tilting
- Low storage scenarios
- Background process limits

## Success Metrics

- Smooth 60 fps on mid-range devices (or configurable 30 fps)
- Battery consumption comparable to other live wallpapers
- Positive user feedback on visual quality
- Easy shader addition process (< 30 minutes for new effect)
- Stable performance over extended periods

## Risks and Mitigations

### Risk: Battery Drain

- **Mitigation**: Frame rate options, battery saver mode, auto-reduce when battery low

### Risk: Performance on Older Devices

- **Mitigation**: Device capability detection, automatic quality reduction, Canvas 2D fallback

### Risk: Memory Issues with Large Images

- **Mitigation**: Bitmap sampling, texture compression, size warnings

### Risk: Shader Complexity

- **Mitigation**: Shader templates, documentation, example library

## Documentation Deliverables

1. **User Guide**
   - How to set up wallpaper
   - Effect descriptions
   - Layer management tutorial
   - Troubleshooting

2. **Developer Guide**
   - Architecture overview
   - How to add new shaders
   - Shader uniform reference
   - Performance best practices

3. **API Reference**
   - Shader interface specification
   - Configuration JSON schema
   - Service lifecycle hooks

---

## Summary

This project combines modern GPU-accelerated rendering with intuitive user controls to create a highly customizable live wallpaper experience. The modular shader architecture enables easy extension while the multi-layer system with parallax support provides engaging visual depth. Performance optimization ensures smooth operation across a range of devices while maintaining acceptable battery consumption.