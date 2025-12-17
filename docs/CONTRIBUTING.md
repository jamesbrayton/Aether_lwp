# Contributing to Aether Live Wallpaper

Thank you for your interest in contributing to Aether! This document provides guidelines and workflows for contributing to the project.

## Table of Contents
- [Code of Conduct](#code-of-conduct)
- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Testing Requirements](#testing-requirements)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Adding New Shader Effects](#adding-new-shader-effects)

---

## Code of Conduct

- Be respectful and inclusive
- Provide constructive feedback
- Focus on what is best for the community
- Show empathy towards other contributors

---

## Getting Started

### Prerequisites
- JDK 21 (Eclipse Temurin recommended)
- Android Studio Hedgehog (2023.1.1) or later
- Git
- Understanding of Kotlin and OpenGL ES basics

### Setup
1. Fork the repository
2. Clone your fork:
   ```bash
   git clone https://github.com/YOUR_USERNAME/Aether_lwp.git
   cd Aether_lwp
   ```
3. Add upstream remote:
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/Aether_lwp.git
   ```
4. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```

See [BUILD.md](BUILD.md) for platform-specific build instructions.

**For M-series Mac Developers:** See [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md) for the recommended workflow between VSCode/Claude Code and Android Studio.

---

## Development Workflow

### Test-Driven Development (TDD)

This project **strictly follows TDD**. All contributions must adhere to this workflow:

#### For New Features:

1. **Write Gherkin Specification**
   ```bash
   # Create feature file in spec/
   touch spec/your-feature.feature
   ```

   Example Gherkin spec:
   ```gherkin
   Feature: Custom Particle Effect
     Scenario: User adds custom particle effect
       Given a custom .frag file with valid metadata
       When user imports the shader via Settings UI
       Then the shader is validated and compiled
       And the effect appears in the effects list
   ```

2. **Write Failing Test**
   ```kotlin
   @Test
   fun `import custom shader validates metadata`() {
       val shaderSource = """
           /**
            * @shader Test Effect
            * @id test
            */
           precision mediump float;
           // ...
       """

       val descriptor = parser.parse(shaderSource)

       assertEquals("test", descriptor.id)
       assertEquals("Test Effect", descriptor.name)
   }
   ```

3. **Run Tests - Verify Failure**
   ```bash
   ./gradlew test
   # Test should fail - expected behavior
   ```

4. **Implement Code to Pass Test**
   ```kotlin
   class ShaderMetadataParser {
       fun parse(source: String): ShaderDescriptor {
           // Implementation
       }
   }
   ```

5. **Run Tests - Verify Success**
   ```bash
   ./gradlew test
   # All tests should pass
   ```

6. **Refactor** (if needed)
   - Improve code quality while keeping tests green
   - Run tests after each refactoring step

7. **Commit Immediately**
   ```bash
   git add .
   git commit -m "feature-name: add shader validation with metadata parsing"
   ```

#### For Bug Fixes:

1. **Write Failing Test** that reproduces the bug
2. **Verify Test Fails** (confirms bug exists)
3. **Fix the Bug**
4. **Verify Test Passes**
5. **Refactor** if needed
6. **Commit**

#### For Refactoring:

1. **Ensure All Tests Pass** before starting
2. **Make Incremental Changes**
3. **Run Tests After Each Change**
4. **Keep Behavior Identical**
5. **Commit**

---

## Testing Requirements

### Test Coverage Targets
- **Unit Tests:** 80%+ coverage for business logic
- **Integration Tests:** All critical rendering paths
- **UI Tests:** All primary user workflows

### Test Types

#### Unit Tests (Robolectric + JUnit)
Location: `app/src/test/java/`

```kotlin
@RunWith(RobolectricTestRunner::class)
class ShaderMetadataParserTest {

    private lateinit var parser: ShaderMetadataParser

    @Before
    fun setup() {
        parser = ShaderMetadataParser()
    }

    @Test
    fun `parse extracts shader name from @shader tag`() {
        val source = """
            /**
             * @shader Falling Snow
             */
        """
        val descriptor = parser.parse(source)
        assertEquals("Falling Snow", descriptor.name)
    }
}
```

#### Integration Tests (Instrumentation)
Location: `app/src/androidTest/java/`

```kotlin
@RunWith(AndroidJUnit4::class)
class ShaderCompilationTest {

    @Test
    fun snowShaderCompilesSuccessfully() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val loader = ShaderLoader(context)

        // Should not throw exception
        val programId = loader.createProgram(
            "vertex_shader.vert",
            "snow.frag"
        )

        assertTrue(programId > 0)
    }
}
```

#### UI Tests (Espresso)
Location: `app/src/androidTest/java/`

```kotlin
@RunWith(AndroidJUnit4::class)
class SettingsActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(SettingsActivity::class.java)

    @Test
    fun addEffectButtonDisplaysEffectList() {
        onView(withId(R.id.add_effect_button))
            .perform(click())

        onView(withText("Falling Snow"))
            .check(matches(isDisplayed()))
    }
}
```

### Running Tests

```bash
# Run all unit tests
./gradlew test

# Run all instrumentation tests (requires connected device/emulator)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "ShaderMetadataParserTest"

# Run tests with coverage report
./gradlew testDebugUnitTestCoverage
```

---

## Commit Guidelines

### Commit Message Format

```
<type>: <short summary>

<detailed description>

<footer>
```

### Types
- **feature:** New feature implementation
- **fix:** Bug fix
- **refactor:** Code refactoring (no behavior change)
- **test:** Adding or updating tests
- **docs:** Documentation changes
- **build:** Build system or dependency changes
- **ci:** CI/CD configuration changes
- **perf:** Performance improvements

### Examples

**Good:**
```
shader-parser: add support for vec3 parameter types

Implement parsing for vec3 parameters in shader metadata to support
color parameters. Updated ParameterType enum and parser regex to handle
vec3(r, g, b) default values.

Fixes #42
```

**Bad:**
```
fix stuff
```

### Commit Footer

Include if applicable:
- `Fixes #123` - Links to GitHub issue
- `Breaking Change:` - Describes breaking changes
- `Co-Authored-By:` - For pair programming

---

## Pull Request Process

### Before Submitting

1. **Ensure All Tests Pass**
   ```bash
   ./gradlew test connectedAndroidTest lint
   ```

2. **Update Memory Bank**
   - Update `.memoryBank/Aether_lwp/progress.md` with changes
   - Add new architectural decisions if applicable
   - Update active context if changing current work

3. **Verify No Lint Errors**
   ```bash
   ./gradlew lint
   ```

4. **Update Documentation**
   - Update README if adding user-facing features
   - Update CLAUDE.md if changing architecture
   - Add/update KDoc comments for public APIs

5. **Rebase on Latest Main**
   ```bash
   git fetch upstream
   git rebase upstream/main
   ```

### PR Template

When creating a PR, include:

```markdown
## Summary
Brief description of changes

## Type of Change
- [ ] Bug fix
- [ ] New feature
- [ ] Breaking change
- [ ] Documentation update

## Changes Made
- Bullet list of specific changes
- Include file paths for major changes

## Testing
- [ ] All unit tests pass
- [ ] All integration tests pass
- [ ] Manual testing completed
- [ ] Test coverage maintained/improved

## Gherkin Specs
- [ ] Gherkin spec created/updated
- [ ] All scenarios pass

## Memory Bank Updates
- [ ] progress.md updated
- [ ] Architectural decisions documented (if applicable)
- [ ] activeContext.md updated (if applicable)

## Performance Impact
Describe any performance implications

## Breaking Changes
Describe any breaking changes (or "None")

## Screenshots/Videos
If UI changes, include visual evidence

## Checklist
- [ ] Code follows project style guidelines
- [ ] Self-review completed
- [ ] Comments added for complex logic
- [ ] Documentation updated
- [ ] No new warnings introduced
- [ ] Related issues linked
```

### PR Review Process

1. **Automated Checks**
   - GitHub Actions CI must pass
   - All tests must pass
   - Lint checks must pass
   - Coverage must meet thresholds

2. **Code Review**
   - At least one approval required
   - Address all review comments
   - Re-request review after changes

3. **Merge**
   - Squash merge for feature branches
   - Regular merge for release branches
   - Delete branch after merge

---

## Adding New Shader Effects

### Quick Start (Zero Code Changes)

1. **Create Shader File**
   ```glsl
   /**
    * @shader Rising Bubbles
    * @id bubbles
    * @version 1.0.0
    * @author Your Name
    * @source https://github.com/yourname/shader-repo
    * @license MIT
    * @description Bubbles rise with wobble motion and varying sizes
    * @tags bubbles, water, particles
    * @minOpenGL 2.0
    *
    * @param u_bubbleCount float 50.0 min=10.0 max=100.0 step=5.0 name="Bubble Count" desc="Number of bubbles"
    * @param u_riseSpeed float 1.0 min=0.5 max=2.0 step=0.1 name="Rise Speed" desc="How fast bubbles rise"
    * @param u_wobbleAmount float 0.3 min=0.0 max=1.0 step=0.05 name="Wobble" desc="Side-to-side motion amount"
    */

   precision mediump float;

   // REQUIRED: Standard uniforms (all shaders must declare)
   uniform sampler2D u_backgroundTexture;
   uniform float u_time;
   uniform vec2 u_resolution;
   uniform vec2 u_gyroOffset;
   uniform float u_depthValue;

   // Effect-specific parameters
   uniform float u_bubbleCount;
   uniform float u_riseSpeed;
   uniform float u_wobbleAmount;

   void main() {
       // Your shader implementation
   }
   ```

2. **Place in Assets**
   ```bash
   cp bubbles.frag app/src/main/assets/shaders/
   ```

3. **Rebuild App**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Verify in UI**
   - Open Aether Settings
   - Effect should appear automatically in "Available Effects"
   - Parameters should generate sliders automatically

### Shader Development Guidelines

#### Standard Uniform Contract
**All shaders MUST declare these uniforms:**

```glsl
uniform sampler2D u_backgroundTexture;  // User's background image
uniform float u_time;                   // Animation time (seconds)
uniform vec2 u_resolution;              // Screen resolution (pixels)
uniform vec2 u_gyroOffset;              // Gyroscope parallax offset
uniform float u_depthValue;             // Layer depth (0.0-1.0)
```

Even if unused, these must be declared for architectural consistency.

#### Metadata Tags Reference

| Tag | Required | Format | Example |
|-----|----------|--------|---------|
| `@shader` | ✅ Yes | String | `@shader Falling Snow` |
| `@id` | ✅ Yes | Identifier | `@id snow` |
| `@version` | ✅ Yes | Semver | `@version 1.0.0` |
| `@author` | ❌ No | String | `@author Your Name` |
| `@source` | ❌ No | URL | `@source https://github.com/...` |
| `@license` | ❌ No | SPDX | `@license MIT` |
| `@description` | ❌ No | String | `@description Gentle snow...` |
| `@tags` | ❌ No | CSV | `@tags winter, weather` |
| `@minOpenGL` | ❌ No | Version | `@minOpenGL 2.0` |
| `@param` | ❌ No | See below | Multiple allowed |

#### @param Format
```
@param <uniform_name> <type> <default> [min=<min>] [max=<max>] [step=<step>] [name="<display>"] [desc="<description>"]
```

**Supported types:** `float`, `int`, `bool`, `color`, `vec2`, `vec3`, `vec4`

**Example:**
```glsl
@param u_speed float 1.0 min=0.1 max=3.0 step=0.1 name="Speed" desc="Animation speed multiplier"
```

#### Performance Guidelines

- **Minimize texture lookups** (expensive on mobile GPUs)
- **Avoid expensive math** in tight loops (pow, sin/cos)
- **Use `mediump float`** when high precision not needed
- **Limit particle counts** to < 200 for smooth 60fps
- **Test on mid-range devices** (Pixel 4a, Galaxy A52 equivalent)

#### Testing Your Shader

1. **Visual Test:**
   - Install on device/emulator
   - Verify particles render correctly
   - Check animation is smooth (60fps)

2. **Parameter Test:**
   - Adjust each parameter in Settings
   - Verify slider ranges are appropriate
   - Confirm visual changes match expectations

3. **Performance Test:**
   ```bash
   # Check FPS with adb
   adb shell dumpsys gfxinfo com.aether.wallpaper
   ```

4. **Integration Test** (optional):
   ```kotlin
   @Test
   fun bubblesShaderCompilesSuccessfully() {
       val programId = shaderLoader.createProgram(
           "vertex_shader.vert",
           "bubbles.frag"
       )
       assertTrue(programId > 0)
   }
   ```

---

## Style Guidelines

### Kotlin Code Style

- **Indentation:** 4 spaces (no tabs)
- **Line Length:** 120 characters max
- **Naming:**
  - Classes: PascalCase (`ShaderMetadataParser`)
  - Functions: camelCase (`parseShaderSource`)
  - Constants: UPPER_SNAKE_CASE (`MAX_PARTICLE_COUNT`)
  - Private properties: prefix with underscore (`_cache`)

- **Documentation:**
  - All public APIs must have KDoc comments
  - Complex algorithms need explanation comments
  - Avoid obvious comments

**Good:**
```kotlin
/**
 * Parses GLSL shader source to extract embedded metadata.
 *
 * Metadata is expected in JavaDoc-style comments at the top of the file.
 * Supports tags: @shader, @id, @version, @param, etc.
 *
 * @param shaderSource Complete GLSL source code including metadata comments
 * @return Parsed shader descriptor with metadata and parameters
 * @throws ShaderParseException if required tags are missing or malformed
 */
fun parse(shaderSource: String): ShaderDescriptor {
    // Implementation
}
```

### XML Style

- **Indentation:** 2 spaces
- **Attribute Order:** id, layout, style, other
- **Resource Naming:** lowercase with underscores

---

## Questions or Issues?

- **Bug Reports:** Open an issue with reproduction steps
- **Feature Requests:** Open an issue with use case description
- **Questions:** Open a discussion or ask in issues
- **Documentation:** Submit PR with improvements

---

## License

By contributing, you agree that your contributions will be licensed under the same license as the project (see LICENSE file).
