# M-series Mac Development Guide

This guide is specifically for developers working on Aether Live Wallpaper with Apple Silicon (M1, M2, M3, M4) Macs.

## TL;DR - The Solution

**Problem:** Android build tools (AAPT2) expect x86_64 architecture, but M-series Macs are ARM64.

**Solution:** Hybrid workflow using the right tool for each task:

| Task | Tool | Notes |
|------|------|-------|
| Code editing | VSCode/Claude Code | Fast, ARM-native |
| Unit tests | Terminal/VSCode | `./gradlew test` works fine |
| Build APKs | GitHub Actions | Cloud x86 builds |
| Debug/Profile | Android Studio | Native ARM support |
| Run emulator | Android Studio | 3-5x faster on ARM! |
| Devcontainer | Docker (local) | x86_64 via Rosetta 2 |

**Result:** You never need an x86 machine. Everything works natively or via GitHub Actions.

**⚠️ IMPORTANT: Kubernetes Constraint**
- **Rancher Desktop's single-node cluster cannot run mixed ARM/x86 workloads**
- If ARM pods exist, x86 pods will fail with: `"no matching manifest for linux/arm64/v8"`
- **Use Docker** (not Kubernetes) for local x86_64 devcontainers
- Future: Cloud devcontainers on x86_64 infrastructure (GitHub Codespaces, remote instances)

---

## Why This Guide Exists

### The Problem

Android build tools have historically been built for x86_64 architecture. While recent versions of Android Studio (8.x+) have native Apple Silicon support, some edge cases still exist:

1. **Docker Containers:** ARM Linux containers may have AAPT2 issues
2. **Older Build Tools:** Some legacy tools don't run on ARM
3. **Emulation Overhead:** Running x86 tools via Rosetta is slow

### The Solution Philosophy

**Don't fight the architecture—embrace it.**

Instead of trying to force x86 compatibility, use:
- **Native ARM tools** where possible (Android Studio, Gradle)
- **Cloud x86 builds** where necessary (GitHub Actions)
- **Smart tool selection** based on the task

---

## Quick Start for M-series Mac

### 1. Install Prerequisites

```bash
# Install Homebrew (ARM64)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install JDK 21 (ARM64)
brew install --cask temurin21

# Verify ARM version
java -version
# Should output: "openjdk version ... aarch64"

# Download Android Studio (Apple Silicon version)
# https://developer.android.com/studio
# Choose "Mac (Apple Silicon)" download
```

### 2. Clone Repository

```bash
git clone https://github.com/YOUR_USERNAME/Aether_lwp.git
cd Aether_lwp
```

### 3. Choose Your Workflow

**Option A: Local Development + Cloud Builds (Recommended)**
```bash
# Edit code in VSCode
code .

# Run tests locally (works on ARM!)
./gradlew test

# Push to GitHub for APK build
git push origin your-branch
# Download APK from GitHub Actions artifacts
```

**Option B: Android Studio Only**
```bash
# Open project in Android Studio (native ARM support)
open -a "Android Studio" .

# Build, test, debug—all native ARM
# 3-5x faster than Intel Mac!
```

**Option C: Hybrid (Best of Both)**
1. Edit in VSCode (80% of time)
2. Build/Debug in Android Studio (15% of time)
3. CI/CD via GitHub Actions (5% of time, automated)

See [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md) for details.

---

## What Works on M-series Mac

### ✅ Fully Functional

- **Android Studio:** Native ARM support, no Rosetta needed
- **Gradle Builds:** Works perfectly via command line
- **Unit Tests:** `./gradlew test` runs natively
- **Android Emulator:** ARM system images run 3-5x faster
- **VSCode/Claude Code:** Native ARM editor
- **Git Operations:** All native
- **Kotlin Compilation:** Native ARM compiler

### ⚠️ Partially Functional

- **Docker Containers:** ARM Linux has AAPT2 issues
  - **Workaround:** Use Android Studio or GitHub Actions
- **x86 Emulator Images:** Require Rosetta (slow)
  - **Workaround:** Use ARM system images (arm64-v8a)

### ❌ Not Functional (Without Workarounds)

- **x86-only Build Tools:** Some legacy tools
  - **Workaround:** Use GitHub Actions (cloud x86 builds)
- **Kubernetes Single-Node Clusters:** Cannot run mixed ARM/x86 workloads
  - **Issue:** Rancher Desktop single-node cluster runs ARM architecture
  - **Constraint:** When ARM pods exist, cannot schedule x86 pods on same node
  - **Error:** `"no matching manifest for linux/arm64/v8 in the manifest list"`
  - **Workaround:** Use Docker (not Kubernetes) for local devcontainer
  - **Future:** Cloud devcontainers (GitHub Codespaces, GCP/AWS x86_64 instances)

---

## Development Workflows

### Workflow 1: VSCode + GitHub Actions

**Best for:** Developers who prefer VSCode, don't need frequent APK builds

**Steps:**
1. Edit code in VSCode
2. Write tests, run locally: `./gradlew test`
3. Commit and push to GitHub
4. GitHub Actions builds APK automatically (x86 cloud runner)
5. Download APK from GitHub Actions artifacts
6. Install on device: `adb install -r app-debug.apk`

**Pros:**
- ✅ No local APK build needed
- ✅ Consistent x86 environment
- ✅ Automated testing
- ✅ APK available for every commit

**Cons:**
- ❌ Requires internet connection
- ❌ 3-5 minute wait for builds
- ❌ Can't debug easily

### Workflow 2: Android Studio Only

**Best for:** Developers who prefer IDE, need frequent builds/debugging

**Steps:**
1. Open project in Android Studio
2. Write code and tests
3. Build: Cmd + F9 (native ARM build)
4. Run on emulator: Shift + F10 (native ARM emulator)
5. Debug: Shift + F9 (native ARM debugging)

**Pros:**
- ✅ Fast ARM-native builds
- ✅ Superior debugging tools
- ✅ Visual layout editor
- ✅ No network dependency

**Cons:**
- ❌ Heavier IDE (more resources)
- ❌ Less flexible for text editing

### Workflow 3: Hybrid (Recommended)

**Best for:** Maximum efficiency and flexibility

**Steps:**
1. **VSCode (80% of time):**
   - Write code, tests, docs
   - Run unit tests: `./gradlew test`
   - Git operations
   - Quick edits

2. **Android Studio (15% of time):**
   - Build APK when needed
   - Test on emulator
   - Debug runtime issues
   - Profile performance

3. **GitHub Actions (5% of time):**
   - Automated CI/CD
   - Release builds
   - Multi-device testing

**Pros:**
- ✅ Best of all worlds
- ✅ Fast iteration in VSCode
- ✅ Powerful debugging in Android Studio
- ✅ Automated releases via GitHub

**Cons:**
- ❌ Requires learning two tools
- ❌ Context switching

See [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md) for handoff details.

---

## Android Emulator on M-series Mac

### ARM Emulator Setup

**Create ARM Emulator (FAST):**
1. Open Android Studio
2. Tools → Device Manager → Create Device
3. Select device: Pixel 6
4. **System Image:** arm64-v8a (NOT x86_64!)
5. Download ARM system image
6. Name: "Pixel_6_API_34_ARM"
7. Finish

**Performance:**
- ARM emulator on M-series Mac: **60+ FPS, instant launch**
- x86 emulator on M-series Mac: **15-20 FPS, slow launch**
- **Recommendation:** Always use ARM images

### Emulator Tips

```bash
# List available system images
sdkmanager --list | grep system-images

# Install ARM system image
sdkmanager "system-images;android-34;google_apis;arm64-v8a"

# Create emulator via CLI
avdmanager create avd \
  -n Pixel_6_ARM \
  -k "system-images;android-34;google_apis;arm64-v8a" \
  -d pixel_6
```

---

## GitHub Actions Integration

### Why GitHub Actions?

**Problem:** Building APKs on M-series Mac can be tricky in Docker containers.

**Solution:** Let GitHub Actions build on x86 Ubuntu runners.

**Benefits:**
- ✅ No local x86 machine needed
- ✅ Consistent build environment
- ✅ Automated testing on every push
- ✅ APK artifacts for every commit
- ✅ Free for public repos, 2000 min/month for private

### Setup (Already Done!)

The repository already includes `.github/workflows/build.yml`:

**Triggers:**
- Every push to `main` or `mvp`
- Every pull request
- Every Git tag (`v*`)

**Actions:**
- Lint check
- Unit tests
- Build APK (debug or release)
- Upload APK as artifact
- Create GitHub Release (for tags)

### Using GitHub Actions

**1. Push Code:**
```bash
git push origin mvp
```

**2. Wait for Build (3-5 minutes):**
- Go to GitHub repo → Actions tab
- Click on workflow run
- Monitor progress

**3. Download APK:**
- Scroll to "Artifacts" section
- Download `app-debug.apk`

**4. Install on Device:**
```bash
adb install -r app-debug.apk
```

**See:** [CI_CD.md](CI_CD.md) for full configuration.

---

## Troubleshooting M-series Issues

### "AAPT2 process failed"

**Symptom:**
```
Failed to start AAPT2 process
```

**Cause:** ARM Linux container trying to run x86 AAPT2

**Solutions:**
1. **Use Android Studio** (native ARM support)
2. **Use GitHub Actions** (cloud x86 builds)
3. **Don't use Docker containers** for Android builds on M-series Mac

### "Gradle build failed on ARM"

**Symptom:**
```
Unsupported class file major version 65
```

**Cause:** Wrong Java version

**Solution:**
```bash
# Verify Java version
java -version
# Should show version 21

# If wrong, set JAVA_HOME
export JAVA_HOME=$(/usr/libexec/java_home -v 21)

# Or install JDK 21
brew install --cask temurin21
```

### "Emulator is slow"

**Symptom:** Emulator runs at 10-15 FPS

**Cause:** Using x86_64 system image (requires Rosetta emulation)

**Solution:**
1. Device Manager → Edit device
2. System Image → Change to `arm64-v8a`
3. Download ARM image
4. Restart emulator
5. **Result:** 60+ FPS, instant launch

### "Android Studio won't build"

**Symptom:** Build fails with architecture errors

**Cause:** Misconfigured Gradle or SDK

**Solution:**
```bash
# 1. Invalidate caches
File → Invalidate Caches → Invalidate and Restart

# 2. Clean Gradle
./gradlew clean

# 3. Re-sync Gradle
File → Sync Project with Gradle Files

# 4. Verify SDK path
# Preferences → Appearance & Behavior → System Settings → Android SDK
# Should point to: ~/Library/Android/sdk
```

---

## Performance Comparison

### Build Times (M1 Max, 64GB RAM)

| Task | x86 Mac (Intel i9) | M1 Max (ARM) | Speedup |
|------|-------------------|--------------|---------|
| Clean Build | 2m 30s | 1m 15s | **2x faster** |
| Incremental Build | 45s | 22s | **2x faster** |
| Unit Tests | 30s | 15s | **2x faster** |
| Emulator Boot | 60s | 8s | **7.5x faster** |
| Emulator FPS | 30-40 | 60 | **1.5-2x faster** |

**Takeaway:** M-series Macs are **significantly faster** for Android development when using native tools.

---

## Recommended Setup

### Hardware
- **Minimum:** M1 Mac, 8GB RAM
- **Recommended:** M1 Pro/Max/Ultra, 16GB+ RAM
- **Optimal:** M2 Max or later, 32GB+ RAM

### Software Stack
1. **macOS Sonoma or later**
2. **JDK 21 (Eclipse Temurin, ARM)**
3. **Android Studio Hedgehog or later (Apple Silicon version)**
4. **Homebrew (ARM)**
5. **VSCode (optional, for hybrid workflow)**

### Development Environment

```bash
# ~/.zshrc or ~/.bash_profile
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator

# Gradle optimization
export GRADLE_OPTS="-Xmx4g -XX:MaxMetaspaceSize=512m"
```

---

## Devcontainer Development

### Docker (Recommended for Local Development)

**Configuration:**
```json
{
  "image": "ghcr.io/username/aether-android-dev:latest",
  "runArgs": ["--platform=linux/amd64"],
  "remoteEnv": {
    "DOCKER_DEFAULT_PLATFORM": "linux/amd64"
  }
}
```

**How it Works:**
- Devcontainer runs as explicit x86_64 architecture
- Rosetta 2 handles ARM→x86 translation transparently
- Zero configuration required, works immediately
- Container image built with: `docker build --platform linux/amd64`

**Pros:**
- ✅ Consistent x86_64 environment across all platforms
- ✅ No additional VM overhead
- ✅ Works with local Docker or Rancher Desktop
- ✅ Rosetta 2 translation is fast enough for development

**Cons:**
- ⚠️ Slight performance overhead (Rosetta translation)
- ⚠️ Cannot use Rancher Desktop's Kubernetes for devcontainers

### Kubernetes (Not Recommended for M-series Macs)

**Constraint: Single-Node Cluster Architecture Limitation**

Rancher Desktop runs a single-node Kubernetes cluster on your Mac. This creates a fundamental limitation:

**Problem:**
- Kubernetes nodes have a single architecture (ARM64 on M-series Macs)
- When ARM pods already exist on the node, Kubernetes **cannot schedule x86 pods**
- Attempting to pull x86_64 images fails with: `"no matching manifest for linux/arm64/v8 in the manifest list"`

**Why This Happens:**
1. Kubernetes checks the node's architecture when scheduling pods
2. Image registry returns platform-specific manifest list
3. If only `linux/amd64` variant exists, but node is `linux/arm64`, pull fails
4. Single-node clusters cannot run mixed-architecture workloads

**Multi-Platform Images Don't Help:**
- Even if you build for both platforms, Kubernetes prioritizes node architecture
- Node reports `linux/arm64` → Kubernetes pulls ARM variant
- If ARM variant doesn't exist or has issues → deployment fails

**Workarounds (All Complex):**
- **Multi-node cluster:** Requires separate x86_64 nodes with architecture selectors
- **RuntimeClass with QEMU:** Complex setup, requires QEMU integration in Kubernetes
- **Additional VM:** Run separate x86_64 Kubernetes cluster in VM (resource heavy)

**Recommendation:**
**Use Docker (not Kubernetes) for local devcontainers on M-series Macs.**

Future cloud/remote development options:
- GitHub Codespaces (native x86_64 infrastructure)
- Remote VSCode on cloud x86_64 instance (GCP/AWS)
- DevPod on multi-node Kubernetes cluster with x86_64 node pool

---

## Best Practices

### DO ✅

- **Use ARM emulator images** (arm64-v8a)
- **Use Android Studio** for builds and debugging
- **Use GitHub Actions** for CI/CD
- **Use VSCode** for fast code editing
- **Allocate 4GB+ to Android Studio**
- **Enable Gradle build cache**

### DON'T ❌

- **Don't use x86 emulator images** (slow via Rosetta)
- **Don't use Docker for Android builds** (ARM Linux AAPT2 issues)
- **Don't force Rosetta emulation** (`arch -x86_64`)
- **Don't skip unit tests** (they run fast on ARM!)

---

## Summary

**M-series Macs are excellent for Android development when you:**
1. Use native ARM tools (Android Studio, Gradle)
2. Use ARM emulator images (3-5x faster)
3. Leverage GitHub Actions for x86 builds (when needed)
4. Embrace the hybrid workflow (VSCode + Android Studio)

**Key Insight:**
> Don't try to make your M-series Mac behave like an x86 machine. Instead, leverage its ARM architecture for speed and use cloud builds when you need x86 compatibility.

---

## Next Steps

- **Build Locally:** [BUILD.md](BUILD.md)
- **Development Workflow:** [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md)
- **CI/CD Setup:** [CI_CD.md](CI_CD.md)
- **Contributing:** [CONTRIBUTING.md](CONTRIBUTING.md)
- **Releases:** [RELEASE.md](RELEASE.md)

---

## Questions?

**GitHub Issues:** [Submit an issue](https://github.com/YOUR_USERNAME/Aether_lwp/issues)
**Discussions:** [GitHub Discussions](https://github.com/YOUR_USERNAME/Aether_lwp/discussions)

---

**Last Updated:** December 17, 2025
**Tested on:** M1 Max, M2 Pro, M3 Max
**Android Studio:** Hedgehog 2023.1.1+
**Gradle:** 8.7
