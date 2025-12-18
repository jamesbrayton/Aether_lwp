# CI/CD Configuration Guide

This document explains the Continuous Integration and Continuous Deployment (CI/CD) pipeline for Aether Live Wallpaper using GitHub Actions.

## Table of Contents
- [Overview](#overview)
- [Workflow Triggers](#workflow-triggers)
- [Build Jobs](#build-jobs)
- [Release Process](#release-process)
- [APK Distribution](#apk-distribution)
- [Troubleshooting](#troubleshooting)

---

## Overview

Aether uses **GitHub Actions** for automated building, testing, and releasing. This is especially beneficial for developers on M-series Macs who don't have easy access to x86 build environments.

**Workflow File:** `.github/workflows/build.yml`

**Key Features:**
- ✅ Automated builds on every branch push
- ✅ Unit and instrumentation tests
- ✅ Lint checks
- ✅ Debug APK artifacts for every build
- ✅ Manual releases via GitHub UI
- ✅ PR-based development workflow
- ✅ Branch protection compatible

**Design Philosophy:**
- **Main branch is protected** - All changes via PR
- **Releases are manual** - Intentional, controlled releases
- **Test on PR** - Full test suite before merge
- **Build everywhere** - Debug APK on every branch push

---

## Workflow Triggers

### Summary Table

| Trigger | Lint | Unit Tests | Debug APK | Instrumentation Tests | Release APK | GitHub Release |
|---------|------|-----------|-----------|----------------------|-------------|----------------|
| **Push to any branch** | ✅ | ✅ | ✅ | ❌ | ❌ | ❌ |
| **PR to main** | ✅ | ✅ | ✅ | ✅ (API 26, 30, 34) | ❌ | ❌ |
| **Manual: Run workflow** | ✅ | ✅ | ✅ | ❌ | ✅ | ✅ |

### 1. Feature Branch Builds (Automatic)

**Trigger:** Push to any branch

**Actions:**
- Lint check
- Unit tests (Robolectric)
- Build debug APK
- Upload APK as artifact (7-day retention)
- **No instrumentation tests** (saves CI minutes)
- **No release created**

**Purpose:** Fast feedback during development

**Example:**
```bash
# Work on feature branch
git checkout -b feature/new-shader
# ... make changes ...
git add .
git commit -m "feature: add new shader effect"
git push origin feature/new-shader

# GitHub Actions automatically:
# - Runs lint
# - Runs unit tests
# - Builds debug APK
# - Uploads to artifacts
```

**Download APK:**
1. Go to repo → Actions tab
2. Click on workflow run
3. Scroll to "Artifacts" section
4. Download `app-debug.apk`

**Install:**
```bash
adb install -r app-debug.apk
```

### 2. Pull Request Builds (Automatic)

**Trigger:** PR to `main` branch

**Actions:**
- Lint check
- Unit tests (Robolectric)
- **Instrumentation tests** (API 26, 30, 34 via emulator)
- Build debug APK
- Upload APK as artifact (7-day retention)
- **No release created**

**Purpose:** Validate code before merge with full test suite

**Example:**
```bash
# Create PR
gh pr create \
  --title "feat: add new shader effect" \
  --body "Implements new shader as specified in #123"

# GitHub Actions automatically:
# - Runs all unit tests
# - Runs instrumentation tests on 3 Android API levels
# - Validates OpenGL shader compilation
# - Builds debug APK
```

**Test Results:**
- Unit test results uploaded as artifacts
- Instrumentation test results uploaded per API level
- PR checks show pass/fail status

**Merge Requirements:**
- ✅ All tests must pass
- ✅ Lint must pass
- ✅ PR approved by maintainer

### 3. Manual Releases (Push-Button)

**Trigger:** Manual workflow dispatch via GitHub UI or CLI

**Actions:**
- Lint check
- Unit tests
- Build release APK (signed if keystore configured)
- Create GitHub Release with ZeroVer tag
- Upload APK to release
- Generate changelog from commits

**Purpose:** Controlled, intentional public releases

**How to trigger via GitHub UI:**

1. Go to repo → **Actions** tab
2. Select **"Android Build and Release"** workflow
3. Click **"Run workflow"** dropdown
4. Select branch (usually `main`)
5. Check ✅ **"Create GitHub Release?"** (default: true)
6. Click **"Run workflow"** button
7. Wait 5-7 minutes for build to complete
8. Go to **Releases** tab to see new release

**How to trigger via GitHub CLI:**
```bash
# From your branch (usually main)
gh workflow run build.yml --ref main

# Or with specific inputs
gh workflow run build.yml \
  --ref main \
  -f create_release=true
```

**Result:**
- Release created with ZeroVer tag (e.g., `0.1.0-alpha+20251218.abc1234`)
- APK uploaded as release asset
- Automated changelog from Git commits
- Marked as prerelease if version contains `-` (e.g., `-alpha`, `-beta`)

**Why manual?**
- Main branch is protected (all changes via PR)
- Releases should be intentional decisions, not automatic
- Allows testing and validation before public release
- Prevents accidental releases on every PR merge

---

## Build Jobs

### Job 1: `build`

**Runs on:** `ubuntu-latest` (x86_64)

**Steps:**

1. **Checkout code**
   ```yaml
   - uses: actions/checkout@v4
     with:
       fetch-depth: 0  # Full git history for versioning
   ```

2. **Set up JDK 21**
   ```yaml
   - uses: actions/setup-java@v4
     with:
       java-version: '21'
       distribution: 'temurin'
       cache: 'gradle'  # Cache dependencies for faster builds
   ```

3. **Extract version info**
   - Reads `versionName` and `versionCode` from `build.gradle.kts`
   - Adds build metadata: `+YYYYMMDD.COMMITHASH`
   - Creates release tag if manual workflow dispatch

4. **Run lint**
   ```bash
   ./gradlew lint
   ```
   - Continues on error (doesn't fail build on warnings)
   - Results uploaded as artifact

5. **Run unit tests**
   ```bash
   ./gradlew test
   ```
   - Fails build if tests fail
   - Results uploaded as artifact

6. **Build debug APK**
   ```bash
   ./gradlew assembleDebug
   ```
   - **Always built** (every trigger)
   - Auto-signed with debug keystore
   - Uploaded as artifact (7-day retention)

7. **Upload artifacts**
   - Debug APK file
   - Test results
   - Lint reports

### Job 2: `release`

**Runs on:** `ubuntu-latest`
**Condition:** Only for manual workflow dispatch with `create_release=true`
**Requires:** `build` job to succeed

**Steps:**

1. **Extract version info**
   - Reads version from `build.gradle.kts`
   - Generates release tag: `VERSION+YYYYMMDD.COMMITHASH`
   - For feature branches: `VERSION-BRANCHNAME+YYYYMMDD.COMMITHASH`

2. **Decode keystore** (if configured)
   - Reads `KEYSTORE_BASE64` secret
   - Decodes to `keystore.jks` file

3. **Build signed release APK**
   ```bash
   ./gradlew assembleRelease \
     -Pandroid.injected.signing.store.file=keystore.jks \
     -Pandroid.injected.signing.store.password=$KEYSTORE_PASSWORD \
     -Pandroid.injected.signing.key.alias=$KEY_ALIAS \
     -Pandroid.injected.signing.key.password=$KEY_PASSWORD
   ```

4. **Rename APK**
   - `app-release.apk` → `aether-VERSION+BUILD.apk`

5. **Generate changelog**
   - Extracts from `CHANGELOG.md` (if exists)
   - Falls back to Git commit history
   - Includes build metadata and commit info

6. **Create GitHub Release**
   - Attaches APK
   - Includes changelog
   - Marks as prerelease if version contains `-` (e.g., `-alpha`, `-beta`)
   - Tags with full version: `0.1.0-alpha+20251218.abc1234`

7. **Publish to GitHub Packages** (if signed)
   ```bash
   ./gradlew publish
   ```

8. **Clean up keystore**
   - Deletes `keystore.jks` file for security

### Job 3: `instrumentation-tests`

**Runs on:** `macos-latest` (faster emulator)
**Condition:** Only for pull requests to `main`

**Purpose:** Validate on real Android environment before merge

**Steps:**

1. **Run on emulator**
   - Uses `android-emulator-runner` action
   - Tests on API levels 26, 30, 34 (matrix strategy)
   - Runs `./gradlew connectedAndroidTest`

2. **Upload results**
   - Test results for each API level
   - Uploaded as artifacts for debugging

**Why PR-only?**
- Instrumentation tests are slow (~20 min per API level)
- Expensive in CI minutes
- Only need to run before merge, not on every commit
- Feature branches get fast feedback (unit tests only)

---

## Release Process

### Workflow for Creating a Release

**Prerequisites:**
- Code merged to `main` via PR
- All tests passing
- Version updated in `build.gradle.kts` (if needed)

**Step 1: Prepare Release (Optional)**

If you want to bump version before release:

```bash
# On main branch
git checkout main
git pull origin main

# Edit app/build.gradle.kts:
#   versionName = "0.2.0"  # Bump minor for new features
#   versionCode = 200001   # Calculate: 2 * 100000 + 0 * 100 + 1

git add app/build.gradle.kts
git commit -m "chore: bump version to 0.2.0 for release"
git push origin main
```

**Step 2: Trigger Release Build**

**Via GitHub UI:**
1. Go to **Actions** tab
2. Click **"Android Build and Release"**
3. Click **"Run workflow"** dropdown
4. Select branch: `main`
5. Keep ✅ **"Create GitHub Release?"** checked
6. Click **"Run workflow"**

**Via GitHub CLI:**
```bash
gh workflow run build.yml --ref main -f create_release=true
```

**Step 3: Wait for Build**

GitHub Actions will:
- ✅ Run lint and tests
- ✅ Build signed APK (if keystore configured)
- ✅ Create GitHub Release with tag: `0.2.0+20251218.abc1234`
- ✅ Upload APK to release
- ✅ Generate changelog

Build time: ~5-7 minutes

**Step 4: Verify Release**

1. Go to repo → **Releases** tab
2. Verify new release is created
3. Download APK and test on device:
   ```bash
   gh release download latest --pattern '*.apk'
   adb install -r aether-*.apk
   ```

**Step 5: Announce Release**

- Update README.md if needed
- Post on social media / forums
- Submit to F-Droid (if applicable)

### Alpha/Beta Releases

For pre-releases:

```bash
# Update version in build.gradle.kts
versionName = "0.2.0-beta.1"
versionCode = 200001

# Commit and push
git add app/build.gradle.kts
git commit -m "chore: version 0.2.0-beta.1"
git push origin main

# Trigger release (same as above)
# GitHub Actions will mark as prerelease due to "-beta"
```

---

## APK Distribution

### 1. GitHub Actions Artifacts

**For:** Development builds (feature branches, PRs)

**Retention:**
- Debug APKs: 7 days

**Download:**
1. Go to repo → Actions tab
2. Click on workflow run
3. Scroll to "Artifacts" section
4. Download `app-debug` artifact

**Install:**
```bash
# Extract from zip
unzip app-debug.zip

# Install to device
adb install -r app-debug.apk
```

### 2. GitHub Releases

**For:** Official releases (manual triggers)

**Retention:** Unlimited

**Download:**
1. Go to repo → **Releases** tab
2. Click on release (e.g., `0.2.0+20251218.abc1234`)
3. Download `aether-*.apk` from Assets section

**Or via CLI:**
```bash
# Latest release
gh release download latest --pattern '*.apk'

# Specific release
gh release download 0.2.0+20251218.abc1234 --pattern '*.apk'
```

**Or via curl:**
```bash
curl -L -o aether.apk \
  https://github.com/OWNER/REPO/releases/download/TAG/aether-TAG.apk
```

### 3. Google Play Store (Future)

**For:** Public distribution

**Tracks:**
- **Internal Testing:** Release candidates
- **Closed Beta:** Invite-only testers
- **Open Beta:** Public opt-in testers
- **Production:** Public release

**Process:**
1. Build signed APK via GitHub Actions
2. Download APK from GitHub Release
3. Upload to Google Play Console manually
4. Submit for review

---

## Secrets Configuration

To enable signed releases, configure these secrets in GitHub:

### Required Secrets

**1. `KEYSTORE_BASE64`**

Base64-encoded Android keystore file.

**Create Keystore:**
```bash
# Generate keystore
keytool -genkey -v \
  -keystore aether.jks \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias aether

# Encode to base64
base64 -i aether.jks | pbcopy  # macOS
base64 -w 0 aether.jks         # Linux
```

**Add to GitHub:**
1. Go to repo → Settings → Secrets and variables → Actions
2. Click "New repository secret"
3. Name: `KEYSTORE_BASE64`
4. Value: (paste base64 string)
5. Click "Add secret"

**2. `KEYSTORE_PASSWORD`** - Password for keystore file
**3. `KEY_ALIAS`** - Alias of the key (e.g., `aether`)
**4. `KEY_PASSWORD`** - Password for the key

---

## Troubleshooting

### Build Fails: "Gradle sync failed"

**Check:**
- JDK version (must be 21)
- Gradle version (8.7 via wrapper)
- Dependencies in `build.gradle.kts`

**Solution:**
```bash
# Local test
./gradlew clean build --refresh-dependencies
```

### Tests Fail on CI but Pass Locally

**Cause:** Different environment

**Solution:**
Check test logs in GitHub Actions artifacts

### Signed APK Not Generated

**Check:**
- Secrets configured correctly
- Keystore base64 encoded properly
- Passwords match keystore/key

### Release Not Created

**Check:**
- Workflow dispatch triggered with `create_release=true`
- Build job succeeded
- No errors in release job logs

### Instrumentation Tests Not Running

**Expected:** Only run on pull requests to `main`

**To run on feature branch:**
Create a PR to main

---

## Performance Optimization

### Speed Up Builds

**1. Cache Gradle dependencies**
Already configured in workflow:
```yaml
- uses: actions/setup-java@v4
  with:
    cache: 'gradle'
```

**2. Skip instrumentation tests on feature branches**
Already configured - only run on PRs

**3. Parallelize tests**
```kotlin
// In build.gradle.kts
tasks.withType<Test> {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}
```

### Current Runtimes

- **Feature branch build:** ~3-5 minutes
- **PR build (with instrumentation):** ~25-30 minutes
- **Release build:** ~5-7 minutes

---

## Summary

**GitHub Actions provides:**
- ✅ Automated builds on every branch push
- ✅ Full test suite on pull requests
- ✅ Manual, controlled releases
- ✅ No accidental releases
- ✅ Perfect for PR-based workflow
- ✅ No local x86 machine needed (perfect for M-series Mac!)

**Key Workflows:**
1. **Push to feature branch:** Lint + unit tests + debug APK
2. **Create PR to main:** Full test suite including instrumentation tests
3. **Manual release:** GitHub UI → Run workflow → Creates signed APK + GitHub release

**Setup Steps:**
1. Add secrets to GitHub (keystore, passwords)
2. Work on feature branches
3. Create PRs for review
4. Manually trigger releases when ready

---

## Next Steps

- **Building Locally:** [BUILD.md](BUILD.md)
- **Release Process:** [RELEASE.md](RELEASE.md)
- **Development Workflow:** [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md)
- **Contributing:** [CONTRIBUTING.md](CONTRIBUTING.md)
