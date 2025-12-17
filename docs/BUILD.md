# Building Aether Live Wallpaper

This guide covers building Aether on different platforms and environments.

## Table of Contents
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [Platform-Specific Instructions](#platform-specific-instructions)
- [Build Variants](#build-variants)
- [Troubleshooting](#troubleshooting)
- [Continuous Integration](#continuous-integration)

---

## Prerequisites

### Required Software
- **JDK 21** (Eclipse Temurin recommended)
  - Download: https://adoptium.net/
  - Verify: `java -version` should show version 21.x.x

- **Android Studio Hedgehog (2023.1.1) or later**
  - Download: https://developer.android.com/studio
  - Includes Android SDK, Gradle, and emulator

- **Git**
  - Download: https://git-scm.com/downloads

### Android SDK Requirements
- **Android SDK Platform 34** (Android 14)
- **Android SDK Build-Tools 34.0.0**
- **Android SDK Platform-Tools**
- **Android Emulator** (for testing)

These are automatically installed by Android Studio.

---

## Quick Start

### Clone the Repository
```bash
git checkout https://github.com/YOUR_USERNAME/Aether_lwp.git
cd Aether_lwp
```

### Build from Command Line
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK (unsigned)
./gradlew assembleRelease

# Run all tests
./gradlew test connectedAndroidTest

# Install to connected device
./gradlew installDebug
```

### Build in Android Studio
1. Open Android Studio
2. Select **File → Open**
3. Navigate to `Aether_lwp` folder
4. Click **Open**
5. Wait for Gradle sync to complete
6. Select **Build → Make Project** (or Ctrl/Cmd + F9)

---

## Platform-Specific Instructions

### macOS (Intel)

✅ **Fully Supported** - Standard Android development

```bash
# Install Homebrew (if not installed)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install JDK 21
brew install --cask temurin21

# Verify installation
java -version

# Clone and build
git clone https://github.com/YOUR_USERNAME/Aether_lwp.git
cd Aether_lwp
./gradlew assembleDebug
```

---

### macOS (Apple Silicon / M-series)

✅ **Fully Supported** - Android Gradle Plugin 8.x has native Apple Silicon support

#### Important Notes for M-series Macs:
1. **Android Studio runs natively on ARM** (no Rosetta needed)
2. **Gradle builds work directly** (no emulation required)
3. **Android Emulator runs natively** using Hypervisor.Framework
4. **Build performance is excellent** (often faster than Intel Macs)

#### Setup Instructions

```bash
# 1. Install Homebrew for ARM64
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. Install JDK 21 (ARM64 version)
brew install --cask temurin21

# 3. Verify Java is ARM64
java -version
# Should show "aarch64" in output

# 4. Download Android Studio (Apple Silicon version)
# Download from: https://developer.android.com/studio
# Look for "Mac (Apple Silicon)" download

# 5. Install Android Studio
# Drag to Applications folder and open
# Complete the setup wizard

# 6. Configure Android SDK
# Android Studio → Preferences → Appearance & Behavior → System Settings → Android SDK
# Ensure these are installed:
#   - Android SDK Platform 34
#   - Android SDK Build-Tools 34.0.0
#   - Android SDK Platform-Tools
#   - Android Emulator

# 7. Clone and Build
git clone https://github.com/YOUR_USERNAME/Aether_lwp.git
cd Aether_lwp

# Build using Gradle
./gradlew assembleDebug

# Or open in Android Studio and build from IDE
```

#### Emulator Setup (M-series Mac)

The Android Emulator on M-series Macs is **significantly faster** than on Intel Macs.

```bash
# Create ARM64 emulator (recommended for M-series)
# In Android Studio:
# Tools → Device Manager → Create Device

# Select a device definition (e.g., Pixel 6)
# Select System Image: API 34, arm64-v8a (not x86_64!)
# Name it: "Pixel_6_API_34_ARM"

# Launch emulator
# Device Manager → Play button next to your device
```

**Performance Tip:** ARM64 emulator images run **3-5x faster** on M-series Macs compared to x86 emulation.

#### Known Issues on M-series Mac

❌ **Docker-based Development:**
- The Docker devcontainer in this repo uses ARM64 Linux
- Android build tools in Docker may have AAPT2 issues
- **Workaround:** Use Android Studio directly, not Docker

✅ **Native Android Studio:** Works perfectly, no issues

#### Recommended Workflow for M-series Mac

**Best Approach:**
1. Use **Claude Code in VSCode** for code editing and TDD
2. Use **GitHub Actions** for automated builds (no local x86 needed!)
3. Use **Android Studio** for:
   - Building APKs (native ARM support)
   - Running on emulator/device
   - Debugging
   - Profiling

See [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md) for detailed workflow.

#### Leveraging GitHub Actions on M-series Mac

Since your M-series Mac is ARM-based and you may not have easy access to x86 hardware, you can leverage GitHub Actions for building:

**Workflow:**
1. **Local Development:** Edit code in VSCode/Claude Code
2. **Commit & Push:** Push to GitHub (to a feature branch)
3. **Automated Build:** GitHub Actions builds on x86 Ubuntu runners
4. **Download APK:** Retrieve built APK from GitHub Actions artifacts or Packages

**Benefits:**
- ✅ No local x86 machine needed
- ✅ Consistent build environment
- ✅ Automated testing
- ✅ APK artifacts for every commit
- ✅ Handles release signing securely

See [CI_CD.md](CI_CD.md) for complete setup instructions.

---

### Windows

✅ **Fully Supported**

```powershell
# 1. Install JDK 21
# Download from: https://adoptium.net/temurin/releases/?version=21
# Run installer, ensure JAVA_HOME is set

# 2. Verify Java installation
java -version

# 3. Install Android Studio
# Download from: https://developer.android.com/studio
# Run installer, complete setup wizard

# 4. Clone repository
git clone https://github.com/YOUR_USERNAME/Aether_lwp.git
cd Aether_lwp

# 5. Build
.\gradlew.bat assembleDebug
```

---

### Linux

✅ **Fully Supported**

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-21-jdk git

# Arch Linux
sudo pacman -S jdk21-openjdk git

# Fedora/RHEL
sudo dnf install java-21-openjdk-devel git

# Download Android Studio
# https://developer.android.com/studio
# Extract and run: ./android-studio/bin/studio.sh

# Clone and build
git clone https://github.com/YOUR_USERNAME/Aether_lwp.git
cd Aether_lwp
./gradlew assembleDebug
```

---

### CI/CD Environment (GitHub Actions)

✅ **Fully Automated** - See [CI_CD.md](CI_CD.md) for complete setup

GitHub Actions automatically builds the project on:
- Every push to `main` or `mvp` branches
- Every pull request
- Tagged releases

```yaml
# .github/workflows/build.yml (excerpt)
runs-on: ubuntu-latest
steps:
  - uses: actions/checkout@v4
  - uses: actions/setup-java@v4
    with:
      java-version: '21'
  - run: ./gradlew assembleDebug test
```

APKs are uploaded as GitHub Actions artifacts and GitHub Packages.

---

## Build Variants

### Debug Build
```bash
./gradlew assembleDebug
```

**Characteristics:**
- Includes debugging symbols
- Enables StrictMode
- LeakCanary memory leak detection
- No ProGuard obfuscation
- Larger APK size (~15-20MB)
- **Output:** `app/build/outputs/apk/debug/app-debug.apk`

### Release Build
```bash
./gradlew assembleRelease
```

**Characteristics:**
- ProGuard/R8 code shrinking and obfuscation
- No debugging tools
- Optimized for performance
- Smaller APK size (~8-10MB)
- Requires signing for distribution
- **Output:** `app/build/outputs/apk/release/app-release-unsigned.apk`

### Signing Release Build

Create `keystore.properties` in project root:
```properties
storeFile=/path/to/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

**⚠️ Never commit `keystore.properties` to git!**

Build signed release:
```bash
./gradlew assembleRelease
# Output: app/build/outputs/apk/release/app-release.apk (signed)
```

---

## Build Commands Reference

### Assembly
```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Build all variants
./gradlew assemble
```

### Installation
```bash
# Install debug to connected device
./gradlew installDebug

# Install release to connected device
./gradlew installRelease

# Uninstall from device
./gradlew uninstallDebug
```

### Testing
```bash
# Run unit tests (fast, no device needed)
./gradlew test

# Run instrumentation tests (requires device/emulator)
./gradlew connectedAndroidTest

# Run specific test class
./gradlew test --tests "ShaderMetadataParserTest"

# Generate test coverage report
./gradlew testDebugUnitTestCoverage
```

### Verification
```bash
# Run lint checks
./gradlew lint

# Run all checks (lint + tests)
./gradlew check

# Format code (if ktlint configured)
./gradlew ktlintFormat
```

### Cleaning
```bash
# Clean build artifacts
./gradlew clean

# Clean and rebuild
./gradlew clean assembleDebug
```

---

## Troubleshooting

### "AAPT2 process failed" on ARM/Apple Silicon

**Symptom:**
```
Failed to start AAPT2 process
```

**Cause:** Android build tools expect x86_64, but environment is ARM64

**Solutions:**

1. **Use Android Studio Directly** (Recommended for M-series Mac)
   - Android Studio has native ARM support
   - Build from IDE or terminal within Android Studio

2. **Use GitHub Actions for CI** (see [CI_CD.md](CI_CD.md))
   - Automated builds run on x86_64 Ubuntu runners
   - APKs available as artifacts

3. **Use Rosetta (Not Recommended)**
   ```bash
   arch -x86_64 ./gradlew assembleDebug
   ```
   - Slower performance
   - Not necessary on modern Android Studio

### "Could not resolve dependency"

**Symptom:**
```
Could not find com.github.CanHub:Android-Image-Cropper:4.5.0
```

**Cause:** JitPack repository not accessible or slow network

**Solution:**
```bash
# Clear Gradle cache
rm -rf ~/.gradle/caches

# Retry build
./gradlew assembleDebug --refresh-dependencies
```

### "Unsupported Java version"

**Symptom:**
```
Gradle requires Java 17 or later
```

**Solution:**
```bash
# Check Java version
java -version

# Install JDK 21
# See platform-specific instructions above

# Set JAVA_HOME explicitly
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk     # Linux
```

### "SDK location not found"

**Symptom:**
```
SDK location not found. Define location with sdk.dir in the local.properties file
```

**Solution:**
```bash
# Create local.properties
echo "sdk.dir=$ANDROID_HOME" > local.properties

# Or find SDK location
# macOS: ~/Library/Android/sdk
# Linux: ~/Android/Sdk
# Windows: C:\Users\USERNAME\AppData\Local\Android\Sdk
```

### Build is Slow

**Solutions:**
```bash
# Enable Gradle daemon (should be default)
echo "org.gradle.daemon=true" >> gradle.properties

# Increase Gradle heap
echo "org.gradle.jvmargs=-Xmx4g -XX:MaxMetaspaceSize=512m" >> gradle.properties

# Enable parallel builds
echo "org.gradle.parallel=true" >> gradle.properties

# Enable configuration cache (Gradle 8.1+)
echo "org.gradle.configuration-cache=true" >> gradle.properties
```

### Emulator Won't Start

**M-series Mac:**
```bash
# Ensure you're using ARM64 system images, not x86_64
# Device Manager → System Image → arm64-v8a (not x86_64!)
```

**Intel Mac/Linux/Windows:**
```bash
# Enable hardware acceleration
# Enable Intel HAXM (Intel Macs)
# Enable Hyper-V (Windows)
# Enable KVM (Linux)
```

---

## Performance Tips

### For M-series Mac Users

```bash
# 1. Use ARM64 emulator images (3-5x faster)
# In Device Manager, select arm64-v8a system images

# 2. Allocate more RAM to emulator
# Device Manager → Edit device → Show Advanced Settings
# Set RAM: 4096 MB (4 GB)

# 3. Enable Gradle Build Cache
echo "org.gradle.caching=true" >> gradle.properties

# 4. Use local Maven cache
echo "org.gradle.caching.debug=false" >> gradle.properties
```

### For All Platforms

```bash
# Gradle parallel execution
./gradlew assembleDebug --parallel --max-workers=4

# Offline mode (if dependencies cached)
./gradlew assembleDebug --offline

# Build without running tests
./gradlew assembleDebug -x test
```

---

## Continuous Integration

For automated builds via GitHub Actions, see [CI_CD.md](CI_CD.md).

**Summary:**
- Every push/PR triggers automated build
- APKs uploaded as artifacts
- Tagged releases create GitHub Releases
- No manual build needed for CI

---

## Next Steps

- **Development Workflow:** See [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md)
- **CI/CD Setup:** See [CI_CD.md](CI_CD.md)
- **Contributing:** See [CONTRIBUTING.md](CONTRIBUTING.md)
- **M-series Mac Guide:** See [ARM_DEVELOPMENT.md](ARM_DEVELOPMENT.md)

---

## Questions?

Open an issue if you encounter build problems not covered here.
