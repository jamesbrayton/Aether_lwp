---
tags: #phase1 #mvp #implementation_plan
created: 2025-12-16
updated: 2025-12-16
status: awaiting_approval
---

# Phase 1 (MVP) - Detailed Implementation Plan

## Overview
**Goal:** Create functional live wallpaper with 2 particle effects (snow, rain), background image support, and basic settings UI with dynamic shader discovery.

**Scope:** Foundation for extensibility - embedded shader metadata system, no gyroscope parallax, no multi-layer compositing yet.

**Duration Estimate:** 13-16 development days

---

## Component Breakdown (TDD Order)

### 1. Project Setup
**Gherkin:** `spec/project-setup.feature`
**Duration:** 1 day

**Tasks:**
- [ ] Create Android app module with `build.gradle.kts`
- [ ] Configure Kotlin 1.9.23, minSdk=26, targetSdk=34
- [ ] Add dependencies (AndroidX, Gson, Android-Image-Cropper, test frameworks)
- [ ] Create `AndroidManifest.xml` with wallpaper service declaration
- [ ] Create `/res/xml/wallpaper.xml` metadata
- [ ] Set up lint rules (Android code style, 2-space XML, 4-space Kotlin)
- [ ] Create `assets/shaders/` directory
- [ ] Verify: `./gradlew build` succeeds, APK generated

**Acceptance Criteria:**
```gherkin
Scenario: Project builds successfully
  Given a new Android project with Kotlin
  When I run ./gradlew build
  Then the build completes without errors
  And the debug APK is generated at app/build/outputs/apk/debug/
```

---

### 2. Shader Metadata Parser & Registry
**Gherkin:** `spec/shader-metadata.feature`
**Duration:** 2 days

**NEW: Core extensibility component for dynamic shader loading**

**Implementation:**
```kotlin
// app/src/main/java/com/aether/wallpaper/shader/ShaderMetadataParser.kt
class ShaderMetadataParser {
    fun parse(shaderSource: String): ShaderDescriptor
    
    private fun extractMetadataComment(source: String): String
    private fun parseTag(line: String, tagName: String): String?
    private fun parseParameter(line: String): ParameterDefinition
}

// app/src/main/java/com/aether/wallpaper/shader/ShaderDescriptor.kt
data class ShaderDescriptor(
    val id: String,
    val name: String,
    val version: String,
    val author: String? = null,
    val source: String? = null,
    val license: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val fragmentShaderPath: String,
    val parameters: List<ParameterDefinition>,
    val minOpenGLVersion: String = "2.0"
)

data class ParameterDefinition(
    val id: String,
    val name: String,
    val type: ParameterType,
    val defaultValue: Any,
    val minValue: Any? = null,
    val maxValue: Any? = null,
    val step: Any? = null,
    val description: String = ""
)

enum class ParameterType {
    FLOAT, INT, BOOL, COLOR, VEC2, VEC3, VEC4
}

// app/src/main/java/com/aether/wallpaper/shader/ShaderRegistry.kt
class ShaderRegistry(private val context: Context) {
    private val metadataParser = ShaderMetadataParser()
    private val descriptors = mutableMapOf<String, ShaderDescriptor>()
    
    fun discoverShaders(): List<ShaderDescriptor>
    fun getShaderById(id: String): ShaderDescriptor?
    fun getAllShaders(): List<ShaderDescriptor>
    
    private fun scanAssetsDirectory()
    private fun validateShader(descriptor: ShaderDescriptor): Boolean
}
```

**Metadata Format Example:**
```glsl
/**
 * @shader Test Effect
 * @id test
 * @version 1.0.0
 * @author Aether Team
 * @description Simple test shader
 * @param u_intensity float 1.0 min=0.0 max=2.0 step=0.1 name="Intensity"
 */
precision mediump float;
// ... shader code
```

**Tests:**
- [ ] Test: Parse metadata comment block from shader source
- [ ] Test: Extract @shader tag → returns display name
- [ ] Test: Extract @id tag → returns identifier
- [ ] Test: Parse @param line → returns ParameterDefinition
- [ ] Test: Missing required tag → throws ParseException
- [ ] Test: ShaderRegistry discovers shaders from assets/shaders/
- [ ] Test: Invalid metadata → logs warning, continues loading other shaders
- [ ] Test: Duplicate shader IDs → throws exception

**Deliverables:**
- [ ] `ShaderMetadataParser.kt` with regex-based parsing
- [ ] `ShaderDescriptor.kt` data models
- [ ] `ParameterDefinition.kt` and `ParameterType.kt`
- [ ] `ShaderRegistry.kt` with assets scanning
- [ ] `ShaderParseException.kt` custom exception
- [ ] `ShaderMetadataParserTest.kt` with 10+ test cases
- [ ] `ShaderRegistryTest.kt`

**Acceptance Criteria:**
```gherkin
Scenario: Parse shader metadata from GLSL comments
  Given a .frag file with embedded metadata comments
  When ShaderMetadataParser parses the file
  Then all @tags are extracted correctly
  And @param definitions are parsed into ParameterDefinition objects
  And a valid ShaderDescriptor is returned

Scenario: Discover shaders from assets
  Given 2 shader files in assets/shaders/
  When ShaderRegistry.discoverShaders() is called
  Then 2 ShaderDescriptor objects are returned
  And they can be retrieved by ID
```

---

### 3. Shader Loading System
**Gherkin:** `spec/shader-loader.feature`
**Duration:** 1-2 days

**Implementation:**
```kotlin
// app/src/main/java/com/aether/wallpaper/shader/ShaderLoader.kt
class ShaderLoader(private val context: Context) {
    fun loadShaderFromAssets(filename: String): String
    fun compileShader(source: String, type: Int): Int
    fun linkProgram(vertexId: Int, fragmentId: Int): Int
    fun createProgram(vertexFile: String, fragmentFile: String): Int
    
    // NEW: Strip metadata comments before compilation (optional)
    private fun stripMetadata(source: String): String
}
```

**Tests:**
- [ ] Test: Load valid vertex shader from assets → succeeds
- [ ] Test: Compile valid shader source → returns shader ID > 0
- [ ] Test: Shader with metadata comments compiles successfully (GLSL ignores comments)
- [ ] Test: Invalid shader syntax → throws ShaderCompilationException with GLSL log
- [ ] Test: Link vertex + fragment → returns program ID > 0
- [ ] Test: Missing shader file → throws FileNotFoundException

**Deliverables:**
- [ ] `ShaderLoader.kt` with error handling
- [ ] `ShaderCompilationException.kt` custom exception
- [ ] `assets/shaders/vertex_shader.vert` (fullscreen quad)
- [ ] `assets/shaders/test.frag` (simple test shader with metadata)
- [ ] `ShaderLoaderTest.kt` with 6+ test cases

**Acceptance Criteria:**
```gherkin
Scenario: Load and compile shader with embedded metadata
  Given a .frag file with metadata comments at the top
  When ShaderLoader compiles the shader
  Then the GLSL compiler ignores metadata comments
  And the shader compiles without errors
  And a valid shader program ID > 0 is returned
```

---

### 4. OpenGL ES Renderer
**Gherkin:** `spec/gl-renderer.feature`
**Duration:** 2 days

**Implementation:**
```kotlin
// app/src/main/java/com/aether/wallpaper/renderer/GLRenderer.kt
class GLRenderer(
    private val context: Context,
    private val config: WallpaperConfig
) : GLSurfaceView.Renderer {
    private var shaderProgram: Int = 0
    private val uniformLocations = mutableMapOf<String, Int>()
    private var startTime: Long = 0
    
    // NEW: Support for dynamic parameters
    private lateinit var currentShader: ShaderDescriptor
    private val parameterValues = mutableMapOf<String, Any>()
    
    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?)
    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int)
    override fun onDrawFrame(gl: GL10?)
    
    private fun initShaders()
    private fun setStandardUniforms()
    private fun setParameterUniforms()
    private fun renderFullscreenQuad()
}
```

**Tests:**
- [ ] Test: onSurfaceCreated initializes GL state
- [ ] Test: onSurfaceChanged sets viewport correctly
- [ ] Test: onDrawFrame renders without GL errors
- [ ] Test: Standard uniforms (u_time, u_resolution) set correctly
- [ ] Test: Parameter uniforms set from config
- [ ] Test: Renderer runs at target FPS (60fps)

**Deliverables:**
- [ ] `GLRenderer.kt` with fullscreen quad rendering
- [ ] `assets/shaders/vertex_shader.vert`:
  ```glsl
  attribute vec4 a_position;
  void main() {
      gl_Position = a_position;
  }
  ```
- [ ] Test placeholder shader with metadata and parameters
- [ ] `GLRendererTest.kt` (instrumentation test)

**Acceptance Criteria:**
```gherkin
Scenario: Initialize OpenGL context and render shader
  Given a GLSurfaceView with ES 2.0 context
  And a ShaderDescriptor with parameters
  When GLRenderer.onSurfaceCreated() is called
  Then OpenGL context is initialized without errors
  And shader program is compiled and linked
  And all standard uniforms are set
  And all parameter uniforms are set from config
```

---

### 5. Configuration System
**Gherkin:** `spec/configuration.feature`
**Duration:** 1-2 days

**Data Models:**
```kotlin
// app/src/main/java/com/aether/wallpaper/model/WallpaperConfig.kt
data class WallpaperConfig(
    val background: BackgroundConfig,
    val layers: List<LayerConfig> = emptyList(),
    val globalSettings: GlobalSettings = GlobalSettings()
)

data class BackgroundConfig(
    val uri: String,
    val crop: CropRect? = null
)

data class CropRect(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
)

data class LayerConfig(
    val shaderId: String,  // "snow", "rain" (matches @id in shader)
    val order: Int,
    val enabled: Boolean = true,
    val opacity: Float = 1.0f,
    val depth: Float = 0.5f,  // For Phase 2 parallax
    val params: Map<String, Any> = emptyMap()  // Parameter values
)

data class GlobalSettings(
    val targetFps: Int = 60,
    val gyroscopeEnabled: Boolean = false  // Phase 2
)
```

**Implementation:**
```kotlin
// app/src/main/java/com/aether/wallpaper/config/ConfigManager.kt
class ConfigManager(private val context: Context) {
    private val prefs: SharedPreferences
    private val gson: Gson
    
    fun saveConfig(config: WallpaperConfig)
    fun loadConfig(): WallpaperConfig
    fun getDefaultConfig(): WallpaperConfig
}
```

**Tests:**
- [ ] Test: Save config with dynamic parameters → JSON serialized correctly
- [ ] Test: Load saved config → deserializes to correct objects
- [ ] Test: Invalid JSON → fallback to default config
- [ ] Test: Missing fields → use defaults
- [ ] Test: Default config has valid structure

**Acceptance Criteria:**
```gherkin
Scenario: Save configuration with dynamic parameters
  Given a wallpaper configuration with 2 layers
  And layer 1 has shader "snow" with params {"u_speed": 1.5, "u_particleCount": 120}
  When the configuration is saved to SharedPreferences
  Then the configuration is serialized to valid JSON
  And parameter values are stored in params map
  And it can be retrieved on next app launch
```

---

### 6. Texture Manager
**Gherkin:** `spec/texture-manager.feature`
**Duration:** 1-2 days

**Implementation:**
```kotlin
// app/src/main/java/com/aether/wallpaper/renderer/TextureManager.kt
class TextureManager(private val context: Context) {
    fun loadBackgroundTexture(uri: Uri, cropRect: CropRect?): Int
    fun releaseTexture(textureId: Int)
    
    private fun loadBitmap(uri: Uri, targetWidth: Int, targetHeight: Int): Bitmap
    private fun uploadTextureToGL(bitmap: Bitmap): Int
}
```

**Tests:**
- [ ] Test: Load 4000x3000 image → downsampled to screen size
- [ ] Test: Apply crop rect → only cropped portion loaded
- [ ] Test: Texture uploaded to GL → valid texture ID returned
- [ ] Test: Memory usage < 20MB for large images
- [ ] Test: Release texture → GL texture deleted

**Deliverables:**
- [ ] `TextureManager.kt` with bitmap sampling
- [ ] Persistent URI permission handling
- [ ] Texture cleanup on release
- [ ] `TextureManagerTest.kt`

**Acceptance Criteria:**
```gherkin
Scenario: Load and downsample large image
  Given a 4000x3000 pixel background image
  When TextureManager loads the image for a 1080x1920 screen
  Then the image is downsampled using inSampleSize
  And memory usage is under 20MB
  And the texture is uploaded to GPU with valid texture ID
```

---

### 7. Snow Shader Effect (with Embedded Metadata)
**Gherkin:** `spec/snow-shader.feature`
**Duration:** 2 days

**Shader with Embedded Metadata:**
```glsl
/**
 * @shader Falling Snow
 * @id snow
 * @version 1.0.0
 * @author Aether Team
 * @source https://github.com/aetherteam/aether-lwp-shaders
 * @license MIT
 * @description Gentle falling snow with lateral drift. Particles fall downward with subtle side-to-side motion, creating a peaceful winter atmosphere.
 * @tags winter, weather, particles, gentle
 * @minOpenGL 2.0
 * 
 * @param u_particleCount float 100.0 min=10.0 max=200.0 step=1.0 name="Particle Count" desc="Number of visible snow particles"
 * @param u_speed float 1.0 min=0.1 max=3.0 step=0.1 name="Fall Speed" desc="How fast snow falls"
 * @param u_driftAmount float 0.5 min=0.0 max=1.0 step=0.05 name="Lateral Drift" desc="Amount of side-to-side wobble"
 */

precision mediump float;

// REQUIRED: Standard uniforms (all shaders must declare these)
uniform sampler2D u_backgroundTexture;
uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_gyroOffset;
uniform float u_depthValue;

// Effect-specific parameters (declared in @param tags above)
uniform float u_particleCount;
uniform float u_speed;
uniform float u_driftAmount;

// Hash function for pseudo-random particle positions
vec2 hash2D(float n) {
    return fract(sin(vec2(n, n + 1.0)) * vec2(43758.5453, 22578.1459));
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    
    // Sample background
    vec4 background = texture2D(u_backgroundTexture, uv);
    
    vec3 snowColor = vec3(0.0);
    
    // Generate particles
    for (float i = 0.0; i < u_particleCount; i++) {
        vec2 particleSeed = hash2D(i);
        
        // Vertical falling motion
        float fallOffset = mod(u_time * u_speed * 0.1, 1.0);
        float yPos = particleSeed.y - fallOffset;
        yPos = mod(yPos + 1.0, 1.0); // Wrap around
        
        // Lateral drift (sine wave)
        float xDrift = sin(u_time + i) * u_driftAmount * 0.05;
        float xPos = particleSeed.x + xDrift;
        
        vec2 particlePos = vec2(xPos, yPos);
        
        // Distance to particle
        float dist = distance(uv, particlePos);
        float particleSize = 0.003; // Small, fixed size
        
        // Soft circular particle
        float alpha = smoothstep(particleSize, particleSize * 0.5, dist);
        snowColor += vec3(1.0) * alpha;
    }
    
    // Composite
    gl_FragColor = background + vec4(snowColor, 1.0);
}
```

**Tests:**
- [ ] Integration test: Snow shader metadata parsed correctly
- [ ] Test: Snow shader compiles without errors
- [ ] Test: Metadata parameters match shader uniforms
- [ ] Visual test: Snow particles visible and falling
- [ ] Test: Particles wrap from bottom to top
- [ ] Test: Lateral drift responds to u_driftAmount parameter
- [ ] Performance test: 60fps with 100 particles on Pixel 4a

**Deliverables:**
- [ ] `assets/shaders/snow.frag` with complete metadata
- [ ] Integration test: ShaderRegistry discovers snow shader
- [ ] Visual validation test

**Acceptance Criteria:**
```gherkin
Scenario: Snow shader discovered and rendered
  Given snow.frag with embedded metadata in assets/shaders/
  When ShaderRegistry.discoverShaders() is called
  Then snow shader is discovered with id "snow"
  And 3 parameters are parsed (particleCount, speed, driftAmount)
  And default values are set correctly
  When shader is rendered with default parameters
  Then snow particles fall downward smoothly
  And particles exhibit lateral sine-wave drift
```

---

### 8. Rain Shader Effect (with Embedded Metadata)
**Gherkin:** `spec/rain-shader.feature`
**Duration:** 1-2 days

**Shader with Embedded Metadata:**
```glsl
/**
 * @shader Falling Rain
 * @id rain
 * @version 1.0.0
 * @author Aether Team
 * @source https://github.com/aetherteam/aether-lwp-shaders
 * @license MIT
 * @description Fast rain streaks with motion blur. Raindrops fall at a steep angle with elongated streaks, creating a dynamic rainy atmosphere.
 * @tags weather, rain, storm, intense
 * @minOpenGL 2.0
 * 
 * @param u_particleCount float 100.0 min=50.0 max=150.0 step=5.0 name="Raindrop Count" desc="Number of rain streaks"
 * @param u_speed float 2.0 min=1.0 max=3.0 step=0.1 name="Fall Speed" desc="How fast rain falls"
 * @param u_angle float 70.0 min=60.0 max=80.0 step=1.0 name="Rain Angle" desc="Angle of rain streaks in degrees"
 * @param u_streakLength float 0.03 min=0.01 max=0.05 step=0.005 name="Streak Length" desc="Length of rain streaks"
 */

precision mediump float;

// REQUIRED: Standard uniforms
uniform sampler2D u_backgroundTexture;
uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_gyroOffset;
uniform float u_depthValue;

// Effect-specific parameters
uniform float u_particleCount;
uniform float u_speed;
uniform float u_angle;
uniform float u_streakLength;

vec2 hash2D(float n) {
    return fract(sin(vec2(n, n + 1.0)) * vec2(43758.5453, 22578.1459));
}

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;
    vec4 background = texture2D(u_backgroundTexture, uv);
    
    vec3 rainColor = vec3(0.0);
    
    float angleRad = radians(u_angle);
    vec2 rainDirection = vec2(sin(angleRad), -cos(angleRad));
    
    for (float i = 0.0; i < u_particleCount; i++) {
        vec2 particleSeed = hash2D(i);
        
        // Fast diagonal motion
        vec2 particlePos = particleSeed + rainDirection * u_time * u_speed * 0.3;
        particlePos = fract(particlePos); // Wrap around
        
        // Distance to streak line (not point)
        vec2 toPixel = uv - particlePos;
        float distToLine = abs(toPixel.x * rainDirection.y - toPixel.y * rainDirection.x);
        float alongLine = dot(toPixel, rainDirection);
        
        // Elongated streak with motion blur
        float isInStreak = step(0.0, alongLine) * step(alongLine, u_streakLength);
        float alpha = smoothstep(0.001, 0.0005, distToLine) * isInStreak;
        
        rainColor += vec3(0.7, 0.8, 1.0) * alpha; // Slight blue tint
    }
    
    gl_FragColor = background + vec4(rainColor, 1.0);
}
```

**Tests:**
- [ ] Test: Rain shader metadata parsed correctly
- [ ] Test: Rain shader compiles
- [ ] Test: 4 parameters discovered (particleCount, speed, angle, streakLength)
- [ ] Visual test: Rain streaks visible at angle
- [ ] Test: Streaks have motion blur effect
- [ ] Test: Angle parameter changes rain direction
- [ ] Performance test: 60fps with 100 rain particles

**Deliverables:**
- [ ] `assets/shaders/rain.frag` with complete metadata
- [ ] Integration test for metadata parsing
- [ ] Visual validation test

**Acceptance Criteria:**
```gherkin
Scenario: Rain shader with customizable parameters
  Given rain.frag with embedded metadata
  When shader is rendered with angle=70 and speed=2.0
  Then rain streaks fall at 70-degree angle
  And streaks have elongated shape with motion blur
  And speed is visibly faster than snow effect
  When angle parameter is changed to 80 degrees
  Then rain angle updates to steeper angle
```

---

### 9. Settings Activity UI (Dynamic UI Generation)
**Gherkin:** `spec/settings-activity.feature`
**Duration:** 3 days

**UPDATED: Uses ShaderRegistry for effect discovery, generates parameter controls from metadata**

**Layout Structure:**
```
SettingsActivity
├── ImageView (background preview)
├── Button "Select Background"
├── RecyclerView (effect selector - populated from ShaderRegistry)
│   └── EffectCard (name from @shader, description from @description, "Add" button)
├── RecyclerView (active layers)
│   └── LayerItem (name, enable toggle, DYNAMIC parameter controls, delete button)
└── Button "Apply Wallpaper"
```

**Implementation:**
```kotlin
// app/src/main/java/com/aether/wallpaper/ui/SettingsActivity.kt
class SettingsActivity : AppCompatActivity() {
    private lateinit var configManager: ConfigManager
    private lateinit var shaderRegistry: ShaderRegistry
    private lateinit var layerAdapter: LayerAdapter
    private lateinit var effectAdapter: EffectAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Discover available shaders
        shaderRegistry = ShaderRegistry(this)
        val availableShaders = shaderRegistry.discoverShaders()
        effectAdapter.submitList(availableShaders)
    }
    
    private fun selectBackgroundImage()
    private fun openCropActivity(imageUri: Uri)
    private fun addEffectLayer(shaderId: String)
    private fun removeLayer(position: Int)
    private fun showParameterControls(layerConfig: LayerConfig)
    private fun toggleLayerEnabled(position: Int)
    private fun applyWallpaper()
    
    // NEW: Dynamic UI generation from shader metadata
    private fun generateParameterControls(descriptor: ShaderDescriptor): View {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        
        descriptor.parameters.forEach { param ->
            val control = when (param.type) {
                ParameterType.FLOAT -> createFloatSlider(param)
                ParameterType.INT -> createIntSlider(param)
                ParameterType.BOOL -> createToggle(param)
                ParameterType.COLOR -> createColorPicker(param)
                else -> null
            }
            control?.let { layout.addView(it) }
        }
        
        return layout
    }
    
    private fun createFloatSlider(param: ParameterDefinition): View {
        // Inflate slider, set min/max/default from param, bind to config
    }
}
```

**Tests (Espresso):**
- [ ] Test: SettingsActivity discovers shaders from registry
- [ ] Test: Effect list populated with shader names and descriptions
- [ ] Test: Tap "Select Background" → image picker opens
- [ ] Test: Tap "Add Effect" on Snow → snow layer added with default parameters
- [ ] Test: Layer detail view shows dynamically generated parameter controls
- [ ] Test: Adjust parameter slider → config updated with new value
- [ ] Test: Toggle enable → layer enabled state changes
- [ ] Test: Delete layer → removed from list
- [ ] Test: Tap "Apply Wallpaper" → wallpaper chooser opens

**Deliverables:**
- [ ] `activity_settings.xml` layout
- [ ] `item_effect_card.xml` layout (displays @shader name, @description)
- [ ] `item_layer.xml` layout (with dynamic parameter container)
- [ ] `item_parameter_slider.xml` layout (for float/int parameters)
- [ ] `item_parameter_toggle.xml` layout (for bool parameters)
- [ ] `SettingsActivity.kt` with dynamic UI generation
- [ ] `EffectAdapter.kt` (RecyclerView adapter for discovered shaders)
- [ ] `LayerAdapter.kt` (RecyclerView adapter for active layers)
- [ ] `SettingsActivityTest.kt` (Espresso)

**Acceptance Criteria:**
```gherkin
Scenario: Settings UI populated from shader metadata
  Given ShaderRegistry has discovered 2 shaders (snow, rain)
  When SettingsActivity opens
  Then effect selector shows 2 cards
  And each card displays shader name from @shader tag
  And each card displays description from @description tag

Scenario: Dynamic parameter controls generated
  Given user adds snow layer
  When user opens layer details
  Then 3 parameter controls are displayed (particleCount, speed, driftAmount)
  And each control shows name from @param name attribute
  And each slider has min/max from @param attributes
  When user adjusts "Fall Speed" slider to 1.5
  Then config.layers[0].params["u_speed"] is updated to 1.5
```

---

### 10. Image Cropping Integration
**Gherkin:** `spec/image-cropping.feature`
**Duration:** 1 day

**Implementation:**
```kotlin
// In SettingsActivity.kt
private fun openCropActivity(imageUri: Uri) {
    val cropIntent = CropImage.activity(imageUri)
        .setAspectRatio(9, 16) // Typical phone aspect
        .setGuidelines(CropImageView.Guidelines.ON)
        .getIntent(this)
    startActivityForResult(cropIntent, CROP_REQUEST_CODE)
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    super.onActivityResult(requestCode, resultCode, data)
    
    if (requestCode == CROP_REQUEST_CODE && resultCode == RESULT_OK) {
        val result = CropImage.getActivityResult(data)
        val croppedUri = result.uri
        val cropRect = result.cropRect
        
        // Save to config
        val config = configManager.loadConfig()
        val updatedConfig = config.copy(
            background = BackgroundConfig(croppedUri.toString(), cropRect)
        )
        configManager.saveConfig(updatedConfig)
    }
}
```

**Tests:**
- [ ] Test: Crop activity returns valid URI
- [ ] Test: Crop rect saved to config
- [ ] Test: Cropped image displayed in preview
- [ ] Integration test: TextureManager loads cropped portion

**Acceptance Criteria:**
```gherkin
Scenario: Crop background image
  Given user selected an image from gallery
  When the crop tool opens
  And user adjusts crop rectangle
  And user confirms crop
  Then crop coordinates are saved to configuration
  And only the cropped portion is used as background texture
```

---

### 11. Live Wallpaper Service
**Gherkin:** `spec/wallpaper-service.feature`
**Duration:** 2 days

**UPDATED: Loads shader from registry, applies parameter values**

**Implementation:**
```kotlin
// app/src/main/java/com/aether/wallpaper/AetherWallpaperService.kt
class AetherWallpaperService : WallpaperService() {
    
    override fun onCreateEngine(): Engine {
        return AetherEngine()
    }
    
    inner class AetherEngine : Engine() {
        private var glSurfaceView: WallpaperGLSurfaceView? = null
        private var renderer: GLRenderer? = null
        private lateinit var shaderRegistry: ShaderRegistry
        
        override fun onCreate(surfaceHolder: SurfaceHolder) {
            super.onCreate(surfaceHolder)
            
            glSurfaceView = WallpaperGLSurfaceView(this@AetherWallpaperService)
            glSurfaceView?.setEGLContextClientVersion(2)
            
            // Load configuration
            val configManager = ConfigManager(this@AetherWallpaperService)
            val config = configManager.loadConfig()
            
            // Discover shaders
            shaderRegistry = ShaderRegistry(this@AetherWallpaperService)
            shaderRegistry.discoverShaders()
            
            renderer = GLRenderer(this@AetherWallpaperService, config, shaderRegistry)
            glSurfaceView?.setRenderer(renderer)
        }
        
        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)
            if (visible) {
                glSurfaceView?.onResume()
            } else {
                glSurfaceView?.onPause()
            }
        }
        
        override fun onDestroy() {
            super.onDestroy()
            glSurfaceView?.onDestroy()
        }
    }
}

class WallpaperGLSurfaceView(context: Context) : GLSurfaceView(context) {
    fun onDestroy() {
        // Release GL resources
    }
}
```

**AndroidManifest.xml:**
```xml
<service
    android:name=".AetherWallpaperService"
    android:label="@string/wallpaper_name"
    android:permission="android.permission.BIND_WALLPAPER">
    <intent-filter>
        <action android:name="android.service.wallpaper.WallpaperService" />
    </intent-filter>
    <meta-data
        android:name="android.service.wallpaper"
        android:resource="@xml/wallpaper" />
</service>
```

**res/xml/wallpaper.xml:**
```xml
<?xml version="1.0" encoding="utf-8"?>
<wallpaper
    xmlns:android="http://schemas.android.com/apk/res/android"
    android:description="@string/wallpaper_description"
    android:settingsActivity="com.aether.wallpaper.ui.SettingsActivity"
    android:thumbnail="@drawable/wallpaper_thumbnail" />
```

**Tests:**
- [ ] Test: Service starts without crash
- [ ] Test: ShaderRegistry discovers shaders on service start
- [ ] Test: GL context created successfully
- [ ] Test: Configuration loaded and parameters applied
- [ ] Test: Wallpaper renders on home screen
- [ ] Integration test: Parameter changes reload correctly

**Acceptance Criteria:**
```gherkin
Scenario: Wallpaper renders with custom parameters
  Given wallpaper is set as active live wallpaper
  And config has snow layer with u_speed=1.5
  When user navigates to home screen
  Then wallpaper renders continuously at 60fps
  And snow falls at speed 1.5 (faster than default)
  And background image is displayed correctly
```

---

## Phase 1 Exit Criteria

### Functional Requirements
- [ ] Android project builds without errors (`./gradlew build`)
- [ ] All lint checks pass with no errors
- [ ] ShaderRegistry discovers shaders from assets/shaders/
- [ ] Shader metadata parsed correctly from JavaDoc-style comments
- [ ] Wallpaper can be set from system wallpaper picker
- [ ] Background image can be selected and cropped
- [ ] Snow effect renders smoothly (60fps) with customizable parameters
- [ ] Rain effect renders smoothly (60fps) with customizable parameters
- [ ] Settings UI dynamically generates parameter controls
- [ ] Multiple layers can be added and configured
- [ ] Layer opacity controls work correctly
- [ ] Parameter changes update wallpaper in real-time
- [ ] Configuration persists across app restarts
- [ ] Wallpaper reloads when config changes

### Quality Requirements
- [ ] Unit test coverage ≥ 80% (Robolectric)
- [ ] All integration tests pass (instrumentation)
- [ ] All UI tests pass (Espresso)
- [ ] No memory leaks detected (LeakCanary)
- [ ] Frame rate ≥ 60fps on Pixel 4a equivalent
- [ ] Memory usage < 100MB
- [ ] APK size < 10MB

### Extensibility Validation
- [ ] Adding new shader = add .frag file with metadata, rebuild
- [ ] No code changes needed for new shader
- [ ] UI automatically shows new shader in effect list
- [ ] Parameter controls auto-generated from @param tags

### Documentation Requirements
- [ ] All Gherkin specs written and passing
- [ ] Public APIs have KDoc comments
- [ ] Shader metadata format documented
- [ ] Complex shader logic documented with comments
- [ ] README updated with shader development guide
- [ ] Memory Bank updated with implementation notes

---

## Dependencies for Phase 1

### Gradle Configuration
```kotlin
// build.gradle.kts (app module)
plugins {
    id("com.android.application")
    kotlin("android")
}

android {
    namespace = "com.aether.wallpaper"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "com.aether.wallpaper"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0-alpha"
        
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Core Android
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Configuration
    implementation("com.google.code.gson:gson:2.10.1")
    
    // Image Cropping
    implementation("com.github.CanHub:Android-Image-Cropper:4.5.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    
    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("androidx.test:core-ktx:1.5.0")
    
    // Instrumentation Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    
    // Debug Tools
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.12")
}
```

---

## Developer Workflow: Adding New Shaders

**Phase 1 Workflow (bundled shaders):**
1. Create `new_effect.frag` in `assets/shaders/`
2. Add metadata comment block at top:
   ```glsl
   /**
    * @shader Effect Name
    * @id effect_id
    * @version 1.0.0
    * @param u_param float 1.0 min=0.0 max=2.0 name="Param"
    */
   ```
3. Implement GLSL shader with standard uniforms
4. Rebuild app (`./gradlew build`)
5. Effect appears in Settings automatically

**No code changes needed!** ✅

**Phase 2+ Workflow (user imports):**
1. User creates `.frag` file with metadata
2. Opens Settings → "Import Custom Shader"
3. Selects `.frag` file
4. App validates & compiles
5. Effect appears in list immediately

---

## Risk Mitigation Strategies

### Risk: Shader Metadata Parsing Errors
- **Mitigation:** Comprehensive parser tests, graceful error handling
- **Fallback:** Log error, skip invalid shader, continue loading others

### Risk: Shader Visual Quality
- **Mitigation:** Start with simple particle algorithms, iterate based on visual feedback
- **Fallback:** Can switch to texture-based particles if procedural approach insufficient

### Risk: Performance on Mid-Range Devices
- **Mitigation:** Profile early on Pixel 4a, add FPS counter in debug builds
- **Fallback:** Resolution scaling, particle count reduction, 30fps mode

### Risk: Large Images Cause OOM
- **Mitigation:** Aggressive bitmap sampling (inSampleSize), max texture size limit
- **Fallback:** Warn user if image too large, suggest lower resolution

### Risk: Dynamic UI Generation Complexity
- **Mitigation:** Simple parameter types first (float, int, bool), extensive UI tests
- **Fallback:** Start with sliders only, add color picker in Phase 2

---

## Success Metrics for Phase 1

### Performance
- 60fps sustained on Pixel 4a (Snapdragon 730G equivalent)
- Frame time < 16.67ms (95th percentile)
- Memory usage < 100MB
- APK size < 10MB

### Quality
- 0 crashes in 1-hour stress test
- 0 memory leaks detected
- 80%+ unit test coverage
- 100% of acceptance criteria passing

### Extensibility (NEW)
- Add shader in < 5 minutes (create .frag, rebuild)
- 0 code changes needed for new shader
- UI automatically adapts to new parameters

### User Experience
- Background image displays within 2 seconds of wallpaper activation
- Settings UI responds to input within 100ms
- Effect list populated instantly from shader discovery
- Parameter controls generated dynamically
- Wallpaper applies from settings within 3 seconds

---

## Next Steps After Phase 1 Completion

1. **User Feedback Collection:** Deploy to small test group, validate extensibility
2. **Performance Benchmarking:** Battery consumption vs reference wallpapers
3. **Shader Community:** Create shader template repo, accept community contributions
4. **Phase 2 Planning:** Multi-layer compositing, user shader imports, gyroscope
5. **Additional Effects:** Bubbles, dust, smoke shader development
6. **Phase 3 Exploration:** Shader marketplace/library design

---

**Status:** Awaiting user approval to begin implementation
**Key Innovation:** Embedded metadata system enables zero-code shader additions
