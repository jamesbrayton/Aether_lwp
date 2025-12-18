# OpenGL Testing Strategy for CI

## Problems with GLSurfaceView Approach

### 1. **Architectural Mismatch**
`GLSurfaceView` is designed for **UI rendering** with these assumptions:
- View is attached to a window
- View participates in Android's layout/measure/draw cycle
- Surface is created/destroyed based on window visibility
- GL thread lifecycle tied to Activity lifecycle

**In CI headless testing:**
- ❌ No window attachment
- ❌ No layout/measure cycle
- ❌ No Activity lifecycle
- ❌ Surface creation is unpredictable

### 2. **Resource Management Issues**
Our previous approach created ONE `GLSurfaceView` in `setUp()` and reused it:

```kotlin
@Before
fun setUp() {
    glSurfaceView = GLSurfaceView(context)  // Created once
}

// Each test calls runOnGLThread() which does:
private fun runOnGLThread(block: () -> Unit) {
    glSurfaceView.setRenderer(...)  // ← Replaces previous renderer!
    glSurfaceView.onResume()        // ← Called multiple times
    // ... test code ...
    glSurfaceView.onPause()         // ← But surface not destroyed
}
```

**Problems:**
- `setRenderer()` replaces the previous renderer without cleanup
- `onResume()/onPause()` called multiple times on same view
- Surface persists across tests → test interference
- GL resources leak between tests
- Unpredictable state accumulation

### 3. **Race Conditions**
```kotlin
var surfaceReady = false  // ← NOT @Volatile or Atomic!

// GL thread writes:
override fun onSurfaceCreated(...) {
    surfaceReady = true  // ← Write from GL thread
}

// Test thread reads:
while (!surfaceReady && waitTime < 5000) {  // ← Read from test thread
    Thread.sleep(100)
}
```

**Problem:** Non-volatile variable accessed from multiple threads can cause:
- Memory visibility issues
- Infinite loops (test thread never sees true)
- Flaky failures in CI

### 4. **Timing Dependencies**
```kotlin
glSurfaceView.onResume()
glSurfaceView.requestRender()
// Hope that onSurfaceCreated() fires within 5 seconds...
```

**Problems:**
- No guarantee when (or if) `onSurfaceCreated()` will be called
- Different timing on different API levels
- Slower in software rendering (CI emulators)
- 5-second timeout is arbitrary

### 5. **Thread Complexity**
Our approach used THREE threads:
1. **Test thread** - JUnit test execution
2. **GL thread** - `GLSurfaceView` rendering thread
3. **Coordination** - CountDownLatch, sleep loops, polling

**Problems:**
- Complex synchronization
- Hard to debug
- More points of failure
- Timeout tuning is guesswork

## Better Solution: Direct EGL with Pbuffer

### What is EGL?
EGL is the interface between OpenGL ES and the native window system. `GLSurfaceView` is a **wrapper** around EGL that handles:
- EGL display/config selection
- EGL context creation
- Surface creation (tied to window)
- GL thread management

For testing, we can use EGL directly and skip the window/threading complexity.

### What is a Pbuffer?
A **Pbuffer** (Pixel Buffer) is an **offscreen** OpenGL surface for rendering without a window. Perfect for testing!

### Architecture Comparison

#### Old Approach (GLSurfaceView):
```
Test Thread ←→ CountDownLatch ←→ GL Thread ←→ GLSurfaceView ←→ EGL ←→ OpenGL
    ↓             (sync)              ↓            (window)    ↓
  JUnit                           Renderer      SurfaceHolder
```

#### New Approach (Direct EGL):
```
Test Thread ←→ EGL ←→ OpenGL
    ↓          ↓
  JUnit     Pbuffer
```

**Benefits:**
- ✅ No separate GL thread
- ✅ No window/surface holder
- ✅ No lifecycle complexity
- ✅ Synchronous execution
- ✅ Deterministic behavior

### Implementation: GLTestContext

```kotlin
class GLTestContext {
    private var eglDisplay: EGLDisplay
    private var eglContext: EGLContext
    private var eglSurface: EGLSurface  // ← Pbuffer, not window surface
    
    init {
        // 1. Get EGL display
        // 2. Choose EGL config (ES 2.0, RGB8888, depth, etc.)
        // 3. Create EGL context
        // 4. Create Pbuffer surface (offscreen)
    }
    
    fun runOnGLThread(block: () -> Unit) {
        // Make context current on THIS thread (test thread)
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)
        
        // Execute test code synchronously
        block()
        
        // Release context
        EGL14.eglMakeCurrent(..., EGL_NO_CONTEXT)
    }
    
    fun destroy() {
        // Clean up EGL resources
    }
}
```

### Usage in Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class GLRendererTest {
    private lateinit var glContext: GLTestContext
    
    @Before
    fun setUp() {
        glContext = GLTestContext()  // ← Creates EGL context once
    }
    
    @After
    fun tearDown() {
        glContext.destroy()  // ← Properly cleans up
    }
    
    @Test
    fun testSomething() {
        glContext.runOnGLThread {
            // Your GL code here - runs synchronously on test thread
            // No CountDownLatch needed!
            // No timing issues!
            val renderer = GLRenderer(context)
            renderer.onSurfaceCreated(null, null)
            // Assertions work immediately
            assertEquals(GLES20.GL_NO_ERROR, GLES20.glGetError())
        }
    }
}
```

### Advantages

#### 1. **Reliability**
- ✅ EGL context creation is deterministic
- ✅ Pbuffer surfaces always work (no window needed)
- ✅ No timing dependencies
- ✅ No race conditions

#### 2. **Simplicity**
- ✅ Synchronous execution (no threads)
- ✅ No CountDownLatch/polling
- ✅ Easier to debug
- ✅ Straightforward code flow

#### 3. **Performance**
- ✅ Faster (no thread overhead)
- ✅ No sleep() calls
- ✅ Immediate execution
- ✅ Better CI resource usage

#### 4. **Isolation**
- ✅ Fresh context per test class
- ✅ No state leakage between tests
- ✅ Proper cleanup in @After
- ✅ Predictable behavior

#### 5. **Industry Standard**
- ✅ Recommended by Android docs
- ✅ Used by Google's own tests
- ✅ Used by major GL libraries (libGDX, etc.)
- ✅ Proven approach

## Migration Plan

### Phase 1: Create GLTestContext ✅
- [x] Implement `GLTestContext` class
- [x] Handle EGL initialization
- [x] Create Pbuffer surface
- [x] Implement `runOnGLThread()` and `destroy()`

### Phase 2: Update GLRendererTest (Example)
- [ ] Replace GLSurfaceView with GLTestContext
- [ ] Simplify runOnGLThread() helper
- [ ] Remove CountDownLatch/timing code
- [ ] Verify tests pass locally
- [ ] Verify tests pass in CI

### Phase 3: Update Remaining Tests
- [ ] ShaderLoaderTest
- [ ] TextureManagerTest
- [ ] Any future GL tests

### Phase 4: Remove Old Code
- [ ] Delete GLSurfaceView-based test helpers
- [ ] Update test documentation
- [ ] Update Memory Bank

## Testing the Fix

### Local Testing
```bash
# Run instrumentation tests locally
./gradlew connectedDebugAndroidTest

# Or specific test
./gradlew connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.aether.wallpaper.renderer.GLRendererTest
```

### CI Testing
Tests will run automatically on PR to main:
- Ubuntu runners with KVM
- API levels 26, 30, 34
- With emulator caching (fast)

## Expected Results

### Before (GLSurfaceView):
- ❌ "GL surface did not initialize in time"
- ❌ "GL thread did not complete in time"
- ❌ Flaky failures
- ❌ 5-10 minute test runs
- ❌ Requires timeouts/retries

### After (Direct EGL):
- ✅ Reliable initialization
- ✅ Fast execution
- ✅ Consistent pass/fail
- ✅ 1-2 minute test runs
- ✅ No timeouts needed

## References

- [Android EGL14 Documentation](https://developer.android.com/reference/android/opengl/EGL14)
- [Khronos EGL Specification](https://www.khronos.org/registry/EGL/)
- [Android Testing with OpenGL](https://source.android.com/docs/core/graphics/arch-egl-opengl)
- [libGDX Testing Approach](https://github.com/libgdx/libgdx/tree/master/tests/gdx-tests-android)

## Conclusion

Direct EGL with Pbuffer surfaces is the **correct** approach for headless OpenGL testing on Android. It's:
- More reliable
- Simpler to understand
- Faster to execute
- Industry standard
- Properly isolated

The GLSurfaceView approach was doomed from the start because we were fighting against its design. This new approach works **with** the platform instead of against it.
