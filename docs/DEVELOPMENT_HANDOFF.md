# Development Handoff: Claude Code/VSCode ↔ Android Studio

This guide explains the workflow for developing with Claude Code/VSCode and transitioning to Android Studio for building, testing, and debugging.

## Overview

**The Hybrid Development Approach:**
- **Claude Code / VSCode**: Code editing, TDD, Git operations, documentation
- **Android Studio**: Building, emulator testing, debugging, profiling
- **GitHub Actions**: Automated CI/CD, APK generation, release management

This approach leverages the strengths of each tool while working around the limitations of ARM-based development on M-series Macs.

---

## Phase 1: Development in Claude Code / VSCode

### What to Do in VSCode

✅ **Code Editing:**
- Write Kotlin source files
- Create GLSL shader files
- Edit configuration files (Gradle, XML, properties)
- Modify resources (layouts, strings, colors)

✅ **Test-Driven Development:**
- Write Gherkin specs in `spec/*.feature`
- Write unit tests in `app/src/test/`
- Write instrumentation tests in `app/src/androidTest/`
- Run tests via Gradle: `./gradlew test`

✅ **Git Operations:**
- Create feature branches
- Stage and commit changes
- Push to remote repository
- Create pull requests

✅ **Documentation:**
- Update README.md
- Maintain docs in `/docs` folder
- Write code comments and KDoc

✅ **Static Analysis:**
- Run lint checks: `./gradlew lint`
- Format code (if ktlint configured)

### What NOT to Do in VSCode (M-series Mac)

❌ **Don't build APKs** - AAPT2 issues on ARM Docker containers
❌ **Don't run emulator** - Use Android Studio's native emulator instead
❌ **Don't debug** - Android Studio has superior debugging tools

### VSCode Setup

**Recommended Extensions:**
```json
{
  "recommendations": [
    "mathiasfrohlich.kotlin",
    "fwcd.kotlin",
    "slevesque.shader",
    "eamodio.gitlens",
    "github.copilot",
    "github.vscode-pull-request-github"
  ]
}
```

**Workspace Settings (`.vscode/settings.json`):**
```json
{
  "files.exclude": {
    "**/build/": true,
    "**/.gradle/": true
  },
  "search.exclude": {
    "**/build/": true,
    "**/.gradle/": true,
    "**/node_modules/": true
  },
  "kotlin.languageServer.enabled": true
}
```

### Typical VSCode Workflow

1. **Start Feature Development:**
   ```bash
   git checkout -b feature/particle-color-picker
   ```

2. **Write Gherkin Spec:**
   ```bash
   # Create spec/particle-color-picker.feature
   code spec/particle-color-picker.feature
   ```

3. **Write Failing Tests:**
   ```bash
   code app/src/test/java/com/aether/wallpaper/ColorPickerTest.kt
   ```

4. **Run Tests (Verify Failure):**
   ```bash
   ./gradlew test --tests "ColorPickerTest"
   ```

5. **Implement Feature:**
   ```bash
   code app/src/main/java/com/aether/wallpaper/ui/ColorPicker.kt
   ```

6. **Run Tests (Verify Success):**
   ```bash
   ./gradlew test --tests "ColorPickerTest"
   ```

7. **Commit Changes:**
   ```bash
   git add .
   git commit -m "feat: add particle color picker UI"
   git push origin feature/particle-color-picker
   ```

8. **Proceed to Handoff** (see Phase 2)

---

## Phase 2: Handoff to Android Studio

### When to Switch to Android Studio

Switch from VSCode to Android Studio when you need to:
- ✅ **Build APKs** (debug or release)
- ✅ **Run on emulator** (fast ARM emulator on M-series)
- ✅ **Run on physical device** (USB debugging)
- ✅ **Debug runtime issues** (breakpoints, inspect variables)
- ✅ **Profile performance** (CPU, memory, GPU)
- ✅ **Test live wallpaper behavior** (requires emulator/device)
- ✅ **Inspect layouts** (Layout Inspector)

### Handoff Checklist

Before switching to Android Studio:

1. **Commit All Changes:**
   ```bash
   git status        # Ensure clean working directory
   git add .
   git commit -m "feat: complete color picker implementation"
   ```

2. **Push to Remote (Optional but Recommended):**
   ```bash
   git push origin feature/particle-color-picker
   ```

3. **Document Current State:**
   - Note which tests pass/fail
   - Document any known issues
   - Update README.md or branch description

### Opening Project in Android Studio

1. **Launch Android Studio**

2. **Open Project:**
   - File → Open
   - Navigate to `/workspace` (or wherever you cloned the repo)
   - Select the root folder containing `build.gradle.kts`
   - Click **Open**

3. **Wait for Gradle Sync:**
   - Android Studio will automatically sync Gradle
   - This may take 1-3 minutes on first open
   - Progress shown in bottom status bar

4. **Verify Setup:**
   - Bottom right: "Gradle sync finished successfully"
   - Project structure appears in left sidebar
   - No red errors in `build.gradle.kts`

### Building in Android Studio

#### Build Debug APK

**Option 1: Via Build Menu**
1. Build → Build Bundle(s) / APK(s) → Build APK(s)
2. Wait for build to complete
3. Click "locate" link in notification
4. APK location: `app/build/outputs/apk/debug/app-debug.apk`

**Option 2: Via Terminal in Android Studio**
1. View → Tool Windows → Terminal
2. Run: `./gradlew assembleDebug`
3. APK location: `app/build/outputs/apk/debug/app-debug.apk`

#### Install to Device/Emulator

**Option 1: Run Configuration**
1. Select "app" in run configuration dropdown (top toolbar)
2. Select target device/emulator
3. Click green "Run" button (or Shift + F10)
4. Android Studio builds, installs, and launches the app

**Option 2: Terminal**
```bash
# Install to connected device
./gradlew installDebug

# Or drag APK to emulator window
```

### Testing in Android Studio

#### Run Unit Tests

1. **Via Test UI:**
   - Navigate to `app/src/test/` in Project view
   - Right-click on test class or package
   - Select "Run Tests in <name>"

2. **Via Terminal:**
   ```bash
   ./gradlew test
   ```

#### Run Instrumentation Tests (Requires Device/Emulator)

1. **Start Emulator:**
   - Tools → Device Manager
   - Click ▶ on your ARM64 emulator (e.g., Pixel 6 API 34 ARM)

2. **Run Tests:**
   - Navigate to `app/src/androidTest/`
   - Right-click test class
   - Select "Run <TestName>"

3. **Via Terminal:**
   ```bash
   ./gradlew connectedAndroidTest
   ```

### Debugging in Android Studio

#### Set Breakpoints

1. Open source file (e.g., `AetherWallpaperService.kt`)
2. Click in left gutter next to line number
3. Red dot appears (breakpoint set)

#### Debug on Emulator

1. **Start Emulator** (if not running)
2. **Select Debug Configuration:**
   - Top toolbar: Select "app"
3. **Click Debug Button** (green bug icon, or Shift + F9)
4. **Wait for App to Launch:**
   - App pauses at breakpoint
   - Variables, call stack, threads shown in Debug panel

#### Debug Panel Controls

- **Step Over** (F8): Execute current line, stay in current method
- **Step Into** (F7): Enter method call
- **Step Out** (Shift + F8): Complete current method, return to caller
- **Resume** (F9): Continue execution until next breakpoint
- **Evaluate Expression** (Alt + F8): Inspect variables, run code

### Profiling Performance

#### CPU Profiler

1. Run app on device/emulator
2. View → Tool Windows → Profiler
3. Click "+" → Select running app process
4. CPU section: Click "Record"
5. Interact with app (trigger the behavior to profile)
6. Click "Stop"
7. Analyze method traces, flame chart

#### Memory Profiler

1. Run app
2. Open Profiler
3. Memory section: Shows heap usage over time
4. Click "Capture heap dump" to inspect objects
5. Identify memory leaks, excessive allocations

#### GPU Profiler (OpenGL ES)

1. Run app
2. View → Tool Windows → Profiler
3. GPU section: Shows frame rendering time
4. Identify shader bottlenecks, overdraw

---

## Phase 3: Returning to VSCode

### Sync Changes Back to VSCode

If you made any changes in Android Studio (e.g., layout edits, quick fixes):

1. **In Android Studio: Commit Changes**
   - VCS → Commit (Cmd/Ctrl + K)
   - Review changes in diff view
   - Write commit message
   - Click "Commit and Push"

2. **In VSCode: Pull Changes**
   ```bash
   git pull origin feature/particle-color-picker
   ```

3. **Continue Development in VSCode**
   - Write more tests
   - Implement more features
   - Repeat cycle

### Recommended Workflow Cycle

```
┌─────────────────────────────────────────────────┐
│ 1. VSCode: Write Tests + Code                  │
│    ↓ (commit changes)                           │
│ 2. Android Studio: Build + Test on Emulator    │
│    ↓ (verify, debug, commit)                    │
│ 3. VSCode: Continue Next Feature               │
│    ↓ (repeat)                                   │
│ 4. Push to GitHub                               │
│    ↓                                            │
│ 5. GitHub Actions: Automated Build + Tests     │
│    ↓                                            │
│ 6. Create Pull Request                          │
└─────────────────────────────────────────────────┘
```

---

## Phase 4: GitHub Actions (Automated Builds)

For developers on M-series Macs without easy x86 access, GitHub Actions provides automated builds.

### When to Use GitHub Actions

- ✅ **Every commit/push** - Automated builds on x86 Ubuntu
- ✅ **Pull requests** - Verify builds pass before merge
- ✅ **Tagged releases** - Generate signed APKs for distribution
- ✅ **Nightly builds** - Automated testing on schedule

### GitHub Actions Workflow

1. **Push to GitHub:**
   ```bash
   git push origin feature/particle-color-picker
   ```

2. **GitHub Actions Triggers:**
   - Workflow: `.github/workflows/build.yml`
   - Runs on: `ubuntu-latest` (x86_64)
   - Builds debug APK
   - Runs all tests
   - Uploads APK as artifact

3. **Download Built APK:**
   - Go to GitHub repo → Actions tab
   - Click on your workflow run
   - Scroll to "Artifacts" section
   - Download `app-debug.apk`

4. **Install APK on Device:**
   ```bash
   adb install -r app-debug.apk
   ```

See [CI_CD.md](CI_CD.md) for complete GitHub Actions setup.

---

## Best Practices for Handoff

### 1. Keep Commits Atomic

✅ **Good:**
```bash
git commit -m "feat: add color picker UI component"
git commit -m "test: add unit tests for color picker"
git commit -m "fix: handle null color values"
```

❌ **Bad:**
```bash
git commit -m "wip: lots of changes, not sure what works"
```

### 2. Document Breaking Points

If switching from VSCode to Android Studio mid-feature:

```kotlin
// TODO(Android Studio): Test this on emulator with live wallpaper
// Current status: Unit tests pass, but not tested on device yet
fun renderParticles(particles: List<Particle>) {
    // ...
}
```

### 3. Run Tests Before Handoff

```bash
# Before switching to Android Studio:
./gradlew test --warning-mode all

# If tests pass, commit:
git add .
git commit -m "feat: implement particle renderer (tests pass)"
```

### 4. Use Branches Effectively

```bash
# Feature branch (VSCode + Android Studio)
git checkout -b feature/particle-color-picker

# After feature complete and tested:
git push origin feature/particle-color-picker
# Open pull request
```

### 5. Leverage Android Studio's Strengths

**Use Android Studio for:**
- Layout editing (visual drag-and-drop)
- XML resource editing (auto-complete, validation)
- Refactoring (rename, extract method)
- Quick fixes (Alt + Enter)
- Code generation (getters, constructors)

**Return to VSCode for:**
- Bulk text editing
- Multi-cursor editing
- Git operations
- Markdown documentation
- Cross-file refactoring

---

## Troubleshooting Handoff Issues

### "Gradle sync failed" in Android Studio

**Cause:** Gradle cache or dependencies issue

**Solution:**
1. File → Invalidate Caches → Invalidate and Restart
2. Terminal: `./gradlew clean`
3. Terminal: `./gradlew build --refresh-dependencies`

### "Git conflicts" when pulling

**Cause:** Made changes in both VSCode and Android Studio

**Solution:**
```bash
git status        # Check conflicted files
git diff          # Review conflicts
# Edit files to resolve conflicts
git add .
git commit -m "merge: resolve conflicts from Android Studio edits"
```

### "Changes not visible" after switching IDEs

**Cause:** IDE didn't reload files

**Solution:**
- **VSCode:** Cmd/Ctrl + Shift + P → "Reload Window"
- **Android Studio:** File → Sync Project with Gradle Files

### "Build works in Android Studio but not VSCode terminal"

**Cause:** Different Java/Gradle versions

**Solution:**
```bash
# In VSCode terminal:
java -version       # Should show Java 21
./gradlew --version # Should show Gradle 8.7

# If different, set JAVA_HOME:
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
```

---

## IDE-Specific Shortcuts

### VSCode Shortcuts (macOS)

| Action | Shortcut |
|--------|----------|
| Command Palette | Cmd + Shift + P |
| Quick Open File | Cmd + P |
| Find in Files | Cmd + Shift + F |
| Go to Definition | F12 |
| Rename Symbol | F2 |
| Format Document | Shift + Alt + F |
| Toggle Terminal | Ctrl + ` |
| Git Commit | Cmd + Enter (in Source Control) |

### Android Studio Shortcuts (macOS)

| Action | Shortcut |
|--------|----------|
| Search Everywhere | Double Shift |
| Find Class | Cmd + O |
| Find File | Cmd + Shift + O |
| Find in Path | Cmd + Shift + F |
| Refactor This | Ctrl + T |
| Quick Fix | Alt + Enter |
| Run | Shift + F10 |
| Debug | Shift + F9 |
| Build APK | Cmd + F9 |
| Sync Gradle | Cmd + Shift + O |

---

## Summary

**Optimal Workflow for M-series Mac:**

1. **VSCode/Claude Code:**
   - 80% of development time
   - Write tests, code, docs
   - Git operations
   - Fast iteration

2. **Android Studio:**
   - 15% of development time
   - Build APKs
   - Test on emulator
   - Debug runtime issues
   - Profile performance

3. **GitHub Actions:**
   - 5% of development time
   - Automated CI/CD
   - No manual builds needed for CI
   - APK artifacts for every commit

**Key Principle:**
> Use the right tool for the right job. Don't fight the M-series Mac's ARM architecture—embrace the hybrid approach.

---

## Next Steps

- **CI/CD Setup:** [CI_CD.md](CI_CD.md)
- **Building:** [BUILD.md](BUILD.md)
- **Contributing:** [CONTRIBUTING.md](CONTRIBUTING.md)
- **Releases:** [RELEASE.md](RELEASE.md)
