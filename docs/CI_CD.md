# CI/CD Configuration Guide

This document explains the Continuous Integration and Continuous Deployment (CI/CD) pipeline for Aether Live Wallpaper using GitHub Actions.

## Table of Contents
- [Overview](#overview)
- [Workflow Triggers](#workflow-triggers)
- [Build Jobs](#build-jobs)
- [Secrets Configuration](#secrets-configuration)
- [APK Distribution](#apk-distribution)
- [Release Process](#release-process)
- [Troubleshooting](#troubleshooting)

---

## Overview

Aether uses **GitHub Actions** for automated building, testing, and releasing. This is especially beneficial for developers on M-series Macs who don't have easy access to x86 build environments.

**Workflow File:** `.github/workflows/build.yml`

**Key Features:**
- ✅ Automated builds on every push/PR
- ✅ Unit and instrumentation tests
- ✅ Lint checks
- ✅ APK artifacts for every build
- ✅ Automated releases for Git tags
- ✅ GitHub Packages publishing
- ✅ Signed APK generation

---

## Workflow Triggers

### 1. Pull Request Builds

**Trigger:** Any PR to `main` branch

**Actions:**
- Lint check
- Unit tests
- Build debug APK
- Upload APK as artifact (7-day retention)
- **No release created**

**Purpose:** Verify PR builds successfully before merge

**Example:**
```bash
# Create PR
git checkout -b feature/new-shader
# ... make changes ...
git push origin feature/new-shader
# Open PR on GitHub
# GitHub Actions automatically builds and tests
```

### 2. Feature Branch Push-Button Builds (NEW! 🎉)

**Trigger:** Manual workflow dispatch from GitHub UI or CLI

**Actions:**
- Lint check
- Unit tests
- Build debug APK
- Upload APK as artifact (7-day retention)
- **Create GitHub Release** (prerelease, with branch name)

**Purpose:** Test feature branches before creating PR

**How to trigger:**

**Via GitHub UI:**
1. Go to repo → Actions tab
2. Select "Android Build and Release" workflow
3. Click "Run workflow" dropdown
4. Select your branch (e.g., `feature/new-shader`)
5. Check "Create GitHub Release?" (default: yes)
6. Click "Run workflow"
7. Wait 3-5 minutes
8. Download APK from Release or Artifacts

**Via GitHub CLI:**
```bash
# From your feature branch
gh workflow run build.yml --ref feature/new-shader

# Or with specific inputs
gh workflow run build.yml \
  --ref feature/new-shader \
  -f create_release=true
```

**Result:**
- Release created: `0.5.0-feature-new-shader+20251217.abc1234`
- APK: `aether-0.5.0-feature-new-shader+20251217.abc1234.apk`
- Marked as "prerelease" (not production)

### 3. Main Branch Auto-Release (NEW! 🎉)

**Trigger:** Push to `main` branch (e.g., merged PR)

**Actions:**
- Lint check
- Unit tests
- Build release APK
- Upload APK as artifact (90-day retention)
- **Automatically create GitHub Release**

**Purpose:** Production releases, automatic deployment

**Example:**
```bash
# Merge PR to main
git checkout main
git merge feature/new-shader
git push origin main
# GitHub Actions automatically builds and creates release!
```

**Result:**
- Release created: `0.5.0+20251217.abc1234`
- APK: `aether-0.5.0+20251217.abc1234.apk`
- Marked as "latest release" (production)

### 4. Manual Builds Without Release

**Trigger:** Manual workflow dispatch with "Create GitHub Release?" = false

**Actions:**
- Lint check
- Unit tests
- Build debug APK
- Upload APK as artifact only (no release)

**Purpose:** Quick test builds, internal testing

**How to trigger:**
```bash
gh workflow run build.yml \
  --ref feature/new-shader \
  -f create_release=false
```

**Result:**
- APK available in Actions artifacts
- No GitHub Release created
- Artifact expires in 7 days

### Summary Table

| Trigger | Branch | Builds APK? | Creates Release? | APK Retention |
|---------|--------|-------------|------------------|---------------|
| Pull Request | any → main | ✅ Debug | ❌ No | 7 days |
| Manual (with release) | any | ✅ Debug | ✅ Yes (prerelease) | 7 days + Release |
| Manual (no release) | any | ✅ Debug | ❌ No | 7 days |
| Push to main | main | ✅ Release | ✅ Yes (latest) | 90 days + Release |

### 5. Instrumentation Tests (Optional)

**Trigger:** Pull requests and pushes to `main`

**Actions:**
- Run on Android emulator (API 26, 30, 34)
- Upload test results

**Purpose:** Test on real Android environment

**Note:** Disabled by default (runs on macOS runner, slower). Enable by uncommenting in workflow.

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
   - For tags, uses tag version (overrides `build.gradle.kts`)

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

6. **Build APK**
   - PR/mvp: Debug APK
   - main: Release APK (unsigned)
   - Tags: Handled by `release` job

7. **Upload artifacts**
   - APK file
   - Test results
   - Lint reports

### Job 2: `release`

**Runs on:** `ubuntu-latest`
**Condition:** Only for Git tags (`v*`)
**Requires:** `build` job to succeed

**Steps:**

1. **Extract version from tag**
   - Tag `v1.2.3` → version `1.2.3`

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
   - `app-release.apk` → `aether-1.2.3.apk`

5. **Generate changelog**
   - Extracts from `CHANGELOG.md` (if exists)
   - Falls back to Git commit history

6. **Create GitHub Release**
   - Attaches APK
   - Includes changelog
   - Marks as prerelease if version contains `-` (e.g., `-alpha`, `-beta`)

7. **Publish to GitHub Packages** (if signed)
   ```bash
   ./gradlew publish
   ```

8. **Clean up keystore**
   - Deletes `keystore.jks` file

### Job 3: `instrumentation-tests` (Optional)

**Runs on:** `macos-latest` (faster emulator)
**Condition:** Pull requests or pushes to `main`

**Steps:**

1. **Run on emulator**
   - Uses `android-emulator-runner` action
   - Tests on API levels 26, 30, 34
   - Runs `./gradlew connectedAndroidTest`

2. **Upload results**
   - Test results for each API level

---

## Secrets Configuration

To enable signed releases and GitHub Packages publishing, configure these secrets:

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

**2. `KEYSTORE_PASSWORD`**

Password for the keystore file.

**Add to GitHub:**
1. New repository secret
2. Name: `KEYSTORE_PASSWORD`
3. Value: `your_keystore_password`

**3. `KEY_ALIAS`**

Alias of the key in the keystore.

**Add to GitHub:**
1. New repository secret
2. Name: `KEY_ALIAS`
3. Value: `aether` (or whatever you used in keytool)

**4. `KEY_PASSWORD`**

Password for the key (may be same as keystore password).

**Add to GitHub:**
1. New repository secret
2. Name: `KEY_PASSWORD`
3. Value: `your_key_password`

### Optional Secrets

**5. `GITHUB_TOKEN`**

Automatically provided by GitHub Actions. No configuration needed.

Used for:
- Creating releases
- Publishing to GitHub Packages
- Commenting on PRs

---

## APK Distribution

### 1. GitHub Actions Artifacts

**For:** Development builds (PR, mvp branch)

**Retention:**
- Debug APKs: 7 days
- Release APKs (unsigned): 30 days

**Download:**
1. Go to repo → Actions tab
2. Click on workflow run (e.g., "Android Build and Release")
3. Scroll to "Artifacts" section
4. Download `app-debug.apk` or `app-release-unsigned.apk`

**Install:**
```bash
adb install -r app-debug.apk
```

### 2. GitHub Releases

**For:** Official releases (Git tags)

**Retention:** Unlimited

**Download:**
1. Go to repo → Releases tab
2. Click on release (e.g., "v1.0.0")
3. Download `aether-1.0.0.apk` from Assets section

**Or via CLI:**
```bash
gh release download v1.0.0 --pattern '*.apk'
```

**Or via curl:**
```bash
curl -L -o aether-1.0.0.apk \
  https://github.com/OWNER/REPO/releases/download/v1.0.0/aether-1.0.0.apk
```

### 3. GitHub Packages

**For:** Versioned releases (if publishing enabled)

**Maven coordinates:**
```
group: com.aether.wallpaper
artifact: aether
version: 1.0.0
```

**Download via Gradle (for libraries):**
```kotlin
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/OWNER/REPO")
        credentials {
            username = System.getenv("GITHUB_ACTOR")
            password = System.getenv("GITHUB_TOKEN")
        }
    }
}

dependencies {
    implementation("com.aether.wallpaper:aether:1.0.0")
}
```

---

## Release Process

### Automated Release Workflow

**Step 1: Prepare Release**

Update version in `app/build.gradle.kts`:
```kotlin
versionName = "1.0.0"
versionCode = 10000001
```

Commit:
```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to 1.0.0"
git push origin main
```

**Step 2: Create Git Tag**

```bash
git tag -a v1.0.0 -m "Release 1.0.0

Features:
- GPU-accelerated particle effects
- Customizable backgrounds
- 5 built-in shaders

Bug Fixes:
- Fixed memory leak in texture manager
"

git push origin v1.0.0
```

**Step 3: Wait for GitHub Actions**

GitHub Actions automatically:
1. ✅ Checks out code at tag
2. ✅ Runs lint and tests
3. ✅ Builds signed APK (if keystore configured)
4. ✅ Creates GitHub Release
5. ✅ Uploads APK to release
6. ✅ Publishes to GitHub Packages

**Step 4: Verify Release**

1. Go to repo → Actions → Check workflow status
2. Go to repo → Releases → Verify release created
3. Download APK and test on device

**Step 5: Announce Release**

- Update README.md with new version
- Post on social media / forums
- Submit to F-Droid (if applicable)

### Manual Release (Fallback)

If GitHub Actions fails or you need to release manually:

**Build locally:**
```bash
./gradlew assembleRelease
```

**Sign APK manually:**
```bash
jarsigner -verbose \
  -sigalg SHA256withRSA \
  -digestalg SHA-256 \
  -keystore aether.jks \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  aether
```

**Zipalign:**
```bash
zipalign -v 4 \
  app/build/outputs/apk/release/app-release-unsigned.apk \
  aether-1.0.0.apk
```

**Create release manually:**
1. Go to repo → Releases → "Draft a new release"
2. Tag: `v1.0.0`
3. Title: `Aether Live Wallpaper v1.0.0`
4. Upload `aether-1.0.0.apk`
5. Publish

---

## Troubleshooting

### Build Fails: "Gradle sync failed"

**Check:**
- JDK version (must be 21)
- Gradle version (should be 8.7 via wrapper)
- Dependencies in `build.gradle.kts`

**Solution:**
```bash
# Local test
./gradlew clean build --refresh-dependencies
```

### Tests Fail on CI but Pass Locally

**Cause:** Different environment (locale, timezone, etc.)

**Solution:**
```yaml
# Add to workflow
env:
  TZ: UTC
  LANG: en_US.UTF-8
```

### Signed APK Not Generated

**Check:**
- Secrets configured correctly
- Keystore base64 encoded properly
- Passwords match keystore/key

**Debug:**
```yaml
# Add to workflow (temporarily)
- name: Debug keystore
  run: |
    echo "Keystore exists: $(test -f keystore.jks && echo yes || echo no)"
    keytool -list -keystore keystore.jks -storepass "$KEYSTORE_PASSWORD"
```

### Release Not Created

**Check:**
- Tag pushed to GitHub
- Workflow triggered (Actions tab)
- `GITHUB_TOKEN` has write permissions

**Verify permissions:**
```yaml
# In workflow file
jobs:
  release:
    permissions:
      contents: write
      packages: write
```

### Instrumentation Tests Fail

**Common issues:**
- Emulator timeout
- Missing permissions in manifest
- Network issues (if tests require internet)

**Solution:**
```yaml
# Increase timeout
with:
  emulator-options: -no-window -gpu swiftshader_indirect -noaudio -no-boot-anim
  disable-animations: true
```

### APK Artifact Not Found

**Check:**
- Build job succeeded
- Artifact name matches (case-sensitive)
- Retention period (7/30 days)

**Solution:**
```bash
# List artifacts via GitHub CLI
gh run list
gh run view RUN_ID
```

---

## Performance Optimization

### Speed Up Builds

**1. Cache Gradle dependencies**
```yaml
- uses: actions/setup-java@v4
  with:
    cache: 'gradle'  # Already configured
```

**2. Skip unnecessary jobs**
```yaml
# Don't run instrumentation tests on every PR
if: github.event_name == 'pull_request' && contains(github.event.pull_request.labels.*.name, 'test-on-emulator')
```

**3. Parallelize tests**
```kotlin
// In build.gradle.kts
tasks.withType<Test> {
    maxParallelForks = Runtime.getRuntime().availableProcessors()
}
```

**4. Use Gradle Build Cache**
```properties
# gradle.properties
org.gradle.caching=true
org.gradle.parallel=true
org.gradle.configureondemand=true
```

### Reduce Workflow Runtime

**Current runtimes:**
- Build job: ~3-5 minutes
- Release job: ~5-7 minutes
- Instrumentation tests: ~15-20 minutes (per API level)

**Optimization:**
- ✅ Gradle cache enabled
- ✅ Dependencies cached
- ✅ Parallel builds enabled
- ⚠️ Instrumentation tests optional (slow)

---

## Cost Considerations

### GitHub Actions Minutes

**Free tier (Public repos):** Unlimited
**Free tier (Private repos):** 2,000 minutes/month

**Typical usage:**
- PR build: ~5 minutes
- Release build: ~7 minutes
- Instrumentation tests: ~20 minutes × 3 API levels = 60 minutes

**Monthly estimate (private repo):**
- 50 PRs × 5 min = 250 min
- 20 releases × 7 min = 140 min
- 10 instrumentation test runs × 60 min = 600 min
- **Total: ~1,000 minutes/month** (within free tier)

**For public repos:** No cost, unlimited minutes.

---

## Security Best Practices

### Keystore Security

**✅ DO:**
- Store keystore in secrets (base64 encoded)
- Use strong passwords (16+ characters)
- Rotate keystore passwords annually
- Limit secret access to admins only

**❌ DON'T:**
- Commit keystore to Git (`.gitignore` it!)
- Share keystore passwords in plain text
- Use same password for keystore and key
- Store passwords in code or logs

### Secret Rotation

**When to rotate:**
- Annually (proactive)
- After team member leaves
- If keystore/password compromised

**How to rotate:**
```bash
# Generate new keystore
keytool -genkey -v -keystore aether-new.jks ...

# Update secrets on GitHub
# Update workflow to use new keystore

# Revoke old keystore
```

### Access Control

**Limit who can:**
- Trigger workflows manually
- Access secrets
- Create releases
- Publish packages

**Configure in:** Repo Settings → Actions → General

---

## Monitoring and Notifications

### Build Status Badge

Add to `README.md`:
```markdown
[![Android Build](https://github.com/OWNER/REPO/actions/workflows/build.yml/badge.svg)](https://github.com/OWNER/REPO/actions/workflows/build.yml)
```

### Email Notifications

**Default:** GitHub sends email on workflow failure

**Customize:**
- GitHub Settings → Notifications → Actions
- Enable/disable per workflow

### Slack/Discord Notifications

**Add to workflow:**
```yaml
- name: Notify Slack
  if: failure()
  uses: slackapi/slack-github-action@v1
  with:
    webhook-url: ${{ secrets.SLACK_WEBHOOK_URL }}
    payload: |
      {
        "text": "Build failed: ${{ github.repository }}@${{ github.sha }}"
      }
```

---

## Advanced Configuration

### Matrix Builds

Test multiple configurations:
```yaml
strategy:
  matrix:
    api-level: [26, 30, 34]
    architecture: [x86_64, arm64-v8a]
```

### Scheduled Builds

Run nightly builds:
```yaml
on:
  schedule:
    - cron: '0 2 * * *'  # 2 AM UTC daily
```

### Manual Triggers

Allow manual workflow runs:
```yaml
on:
  workflow_dispatch:
    inputs:
      version:
        description: 'Version to build'
        required: true
```

---

## Summary

**GitHub Actions provides:**
- ✅ Automated builds on every commit
- ✅ Testing on multiple Android versions
- ✅ Signed APK generation
- ✅ Automatic releases
- ✅ No local x86 machine needed (perfect for M-series Mac!)

**Key workflows:**
1. **Push to PR/mvp:** Build debug APK, run tests
2. **Push tag:** Build signed APK, create release
3. **Manual:** Download APK from Actions artifacts

**Setup steps:**
1. Add secrets to GitHub (keystore, passwords)
2. Push tag to trigger release
3. Download APK from Releases tab

---

## Next Steps

- **Build Locally:** [BUILD.md](BUILD.md)
- **Release Process:** [RELEASE.md](RELEASE.md)
- **Development Workflow:** [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md)
- **Contributing:** [CONTRIBUTING.md](CONTRIBUTING.md)
