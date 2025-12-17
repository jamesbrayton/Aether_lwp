# Release and Versioning Strategy

This document outlines the release process, versioning strategy, and distribution workflow for Aether Live Wallpaper.

## Table of Contents
- [Versioning Scheme](#versioning-scheme)
- [Release Types](#release-types)
- [Release Workflow](#release-workflow)
- [GitHub Actions Integration](#github-actions-integration)
- [Distribution Channels](#distribution-channels)
- [Changelog Management](#changelog-management)

---

## Versioning Scheme

Aether follows **Semantic Versioning 2.0.0** (SemVer) with Android-specific adaptations.

### Format: `MAJOR.MINOR.PATCH[-PRERELEASE][+BUILD]`

**Examples:**
- `1.0.0` - First stable release
- `1.2.3` - Stable release with patches
- `2.0.0-alpha.1` - Alpha prerelease
- `2.0.0-beta.2` - Beta prerelease
- `2.0.0-rc.1` - Release candidate
- `1.2.3+20250117.abc1234` - Build metadata

### Version Components

#### `MAJOR` (Breaking Changes)

Increment when making **incompatible API changes** or **major architecture changes**.

**Examples:**
- `1.x.x` → `2.0.0`: Complete OpenGL renderer rewrite
- `2.x.x` → `3.0.0`: Move from GLSL to Vulkan
- `3.x.x` → `4.0.0`: Change configuration storage format (breaking compatibility)

#### `MINOR` (New Features)

Increment when adding **new functionality** in a **backward-compatible manner**.

**Examples:**
- `1.0.x` → `1.1.0`: Add new particle effect (bubbles)
- `1.1.x` → `1.2.0`: Add gyroscope parallax support
- `1.2.x` → `1.3.0`: Add custom shader import feature

#### `PATCH` (Bug Fixes)

Increment when making **backward-compatible bug fixes**.

**Examples:**
- `1.0.0` → `1.0.1`: Fix memory leak in texture loading
- `1.0.1` → `1.0.2`: Fix crash on specific devices
- `1.0.2` → `1.0.3`: Fix shader compilation error on Mali GPUs

#### `PRERELEASE` (Optional)

Used for pre-release versions: `alpha`, `beta`, `rc` (release candidate).

**Format:** `-alpha.N`, `-beta.N`, `-rc.N`

**Examples:**
- `2.0.0-alpha.1`: First alpha of version 2.0.0
- `2.0.0-beta.3`: Third beta of version 2.0.0
- `2.0.0-rc.1`: Release candidate for 2.0.0

**Progression:**
```
1.2.3 (current stable)
  ↓
2.0.0-alpha.1 (internal testing)
2.0.0-alpha.2 (internal testing)
  ↓
2.0.0-beta.1 (public testing)
2.0.0-beta.2 (public testing)
  ↓
2.0.0-rc.1 (release candidate)
2.0.0-rc.2 (final RC if issues found)
  ↓
2.0.0 (stable release)
```

#### `BUILD` (Optional)

Build metadata for CI/CD: `+YYYYMMDD.COMMIT_HASH`

**Example:** `1.2.3+20250117.abc1234`

**Usage:**
- Automatically appended by GitHub Actions
- Not used for version precedence (SemVer rule)
- Useful for debugging and traceability

---

## Android Version Codes

Android requires integer `versionCode` for app updates in Google Play Store.

### Version Code Scheme

**Format:** `XXYYZZZNN`

| Component | Position | Description | Example |
|-----------|----------|-------------|---------|
| `XX`      | 1-2      | MAJOR       | `01` = v1.x.x |
| `YY`      | 3-4      | MINOR       | `02` = v1.2.x |
| `ZZZ`     | 5-7      | PATCH       | `003` = v1.2.3 |
| `NN`      | 8-9      | BUILD       | `01` = first build |

**Examples:**
- `1.0.0` → `010000001` (version code: 10,000,001)
- `1.2.3` → `010200301`
- `2.0.0` → `020000001`
- `2.1.5` → `020100501`

**Maximum Version Code:** `2,147,483,647` (Android's `Integer.MAX_VALUE`)

### Mapping in `build.gradle.kts`

```kotlin
android {
    defaultConfig {
        versionName = "1.2.3"
        versionCode = 10200301
    }
}
```

**Automated Calculation (Optional):**
```kotlin
val majorVersion = 1
val minorVersion = 2
val patchVersion = 3
val buildNumber = 1

android {
    defaultConfig {
        versionName = "$majorVersion.$minorVersion.$patchVersion"
        versionCode = majorVersion * 10000000 + 
                       minorVersion * 100000 + 
                       patchVersion * 100 + 
                       buildNumber
    }
}
```

---

## Release Types

### 1. Development Builds

**Branch:** `mvp`, feature branches
**Versioning:** `0.x.y-alpha.N`
**Distribution:** GitHub Actions artifacts only
**Purpose:** Active development, internal testing

**Example:**
- Version: `0.5.0-alpha.12`
- Version Code: `5012`
- APK: `app-debug.apk`

**Trigger:**
- Every push to `mvp` branch
- Every pull request

**Artifacts:**
- APK uploaded to GitHub Actions artifacts (7-day retention)
- Not published to GitHub Packages
- Not tagged in Git

### 2. Alpha Releases

**Branch:** `mvp`, dedicated alpha branches
**Versioning:** `X.Y.Z-alpha.N`
**Distribution:** GitHub Packages
**Purpose:** Early testing, experimental features

**Example:**
- Version: `1.0.0-alpha.1`
- Version Code: `10000001`
- APK: `aether-1.0.0-alpha.1.apk`

**Trigger:**
- Manual: Push tag `v1.0.0-alpha.1`
- Automated: Merge to `release/alpha` branch

**Artifacts:**
- APK uploaded to GitHub Packages
- Tagged in Git
- Release notes on GitHub Releases

**Distribution:**
- Internal testers via GitHub
- Not on Google Play Store

### 3. Beta Releases

**Branch:** `release/beta` or `main`
**Versioning:** `X.Y.Z-beta.N`
**Distribution:** GitHub Packages + Google Play (Beta track)
**Purpose:** Public testing, feature complete

**Example:**
- Version: `1.0.0-beta.2`
- Version Code: `10000002`
- APK: `aether-1.0.0-beta.2.apk`

**Trigger:**
- Manual: Push tag `v1.0.0-beta.2`
- Automated: Merge to `release/beta` branch

**Artifacts:**
- Signed APK uploaded to GitHub Packages
- Tagged in Git
- Release notes on GitHub Releases
- Uploaded to Google Play Beta track

**Distribution:**
- Public testers via Google Play Beta
- Direct download from GitHub Releases

### 4. Release Candidates (RC)

**Branch:** `main`
**Versioning:** `X.Y.Z-rc.N`
**Distribution:** GitHub Packages + Google Play (Internal Testing track)
**Purpose:** Final testing before stable release

**Example:**
- Version: `1.0.0-rc.1`
- Version Code: `10000001`
- APK: `aether-1.0.0-rc.1.apk`

**Trigger:**
- Manual: Push tag `v1.0.0-rc.1`

**Artifacts:**
- Signed APK uploaded to GitHub Packages
- Tagged in Git
- Release notes on GitHub Releases
- Uploaded to Google Play Internal Testing track

**Acceptance Criteria:**
- All tests pass (unit + instrumentation)
- No critical bugs
- Performance meets targets (60 FPS on reference devices)
- Security review complete
- Documentation complete

### 5. Stable Releases

**Branch:** `main`
**Versioning:** `X.Y.Z`
**Distribution:** Google Play Store (Production track)
**Purpose:** Public release

**Example:**
- Version: `1.0.0`
- Version Code: `10000001`
- APK: `aether-1.0.0.apk`

**Trigger:**
- Manual: Push tag `v1.0.0`

**Artifacts:**
- Signed APK uploaded to GitHub Packages
- Tagged in Git
- Release notes on GitHub Releases
- Uploaded to Google Play Production track

**Acceptance Criteria:**
- RC tested for at least 7 days
- No critical or high-severity bugs
- Positive feedback from beta testers
- All documentation updated
- Marketing materials ready

---

## Release Workflow

### Step-by-Step Release Process

#### 1. Prepare Release Branch

```bash
# For beta or stable release, create release branch from main
git checkout main
git pull origin main
git checkout -b release/1.0.0

# Update version in build.gradle.kts
# Edit app/build.gradle.kts:
#   versionName = "1.0.0"
#   versionCode = 10000001

# Commit version bump
git add app/build.gradle.kts
git commit -m "chore: bump version to 1.0.0"
git push origin release/1.0.0
```

#### 2. Run Final Tests

```bash
# Local tests
./gradlew test
./gradlew connectedAndroidTest  # Requires emulator

# Or wait for GitHub Actions to run tests automatically
```

#### 3. Create Git Tag

```bash
# Create annotated tag
git tag -a v1.0.0 -m "Release version 1.0.0

Features:
- GPU-accelerated particle effects
- Customizable background images
- Gyroscope parallax support
- 5 built-in shader effects

Bug Fixes:
- Fixed memory leak in texture manager
- Fixed crash on Mali GPUs
"

# Push tag to GitHub
git push origin v1.0.0
```

#### 4. GitHub Actions Builds Release

Once tag is pushed, GitHub Actions automatically:
1. Checks out code at tag
2. Runs all tests
3. Builds signed release APK
4. Uploads APK to GitHub Packages
5. Creates GitHub Release with APK attached
6. Uploads APK to Google Play Store (if configured)

#### 5. Create GitHub Release

**Option 1: Automated (Recommended)**
- GitHub Actions creates release automatically from tag
- Release notes generated from tag message and commit history

**Option 2: Manual**
1. Go to GitHub repo → Releases → "Draft a new release"
2. Select tag: `v1.0.0`
3. Release title: `Aether Live Wallpaper v1.0.0`
4. Release notes:
   ```markdown
   ## 🎉 Features
   - GPU-accelerated particle effects
   - Customizable background images
   - Gyroscope parallax support
   - 5 built-in shader effects (snow, rain, bubbles, dust, smoke)

   ## 🐛 Bug Fixes
   - Fixed memory leak in texture manager
   - Fixed crash on Mali GPUs
   - Fixed shader compilation on Adreno 5xx

   ## 📦 Downloads
   - [aether-1.0.0.apk](link) (8.2 MB)

   ## 🔧 Minimum Requirements
   - Android 8.0 (API 26) or later
   - OpenGL ES 2.0 support
   ```
5. Attach APK: `aether-1.0.0.apk`
6. Click "Publish release"

#### 6. Merge Release Branch to Main

```bash
git checkout main
git merge release/1.0.0
git push origin main
```

#### 7. Update Development Version

```bash
# Immediately after release, bump to next development version
git checkout mvp
git merge main

# Update build.gradle.kts for next version
#   versionName = "1.1.0-alpha"
#   versionCode = 10100001

git add app/build.gradle.kts
git commit -m "chore: bump version to 1.1.0-alpha for development"
git push origin mvp
```

---

## GitHub Actions Integration

### Workflow File: `.github/workflows/build.yml`

See [CI_CD.md](CI_CD.md) for complete workflow configuration.

**Key Features:**
- Builds on every push/PR
- Runs all tests
- Uploads APK artifacts
- Creates GitHub Releases for tags
- Publishes to GitHub Packages

**Version Handling:**
```yaml
- name: Extract version from tag
  if: startsWith(github.ref, 'refs/tags/')
  run: |
    VERSION=${GITHUB_REF#refs/tags/v}
    echo "VERSION=$VERSION" >> $GITHUB_ENV
```

### Automatic Version Injection

**Option 1: Use Tag Version (Recommended)**

GitHub Actions reads version from Git tag and injects into build:

```yaml
- name: Build Release APK
  run: |
    ./gradlew assembleRelease \
      -PversionName=${{ env.VERSION }} \
      -PversionCode=${{ github.run_number }}
```

**Option 2: Use `gradle.properties`**

Store version in `gradle.properties`, read in `build.gradle.kts`:

```properties
# gradle.properties
VERSION_NAME=1.0.0
VERSION_CODE=10000001
```

```kotlin
// build.gradle.kts
android {
    defaultConfig {
        versionName = project.property("VERSION_NAME") as String
        versionCode = (project.property("VERSION_CODE") as String).toInt()
    }
}
```

---

## Distribution Channels

### 1. GitHub Actions Artifacts

**Purpose:** Development builds, CI artifacts
**Retention:** 7 days (GitHub default)
**Access:** Repository collaborators only

**Download:**
1. Go to repo → Actions tab
2. Select workflow run
3. Download artifact: `app-debug.apk`

**Use Cases:**
- Quick testing during development
- Sharing builds with team
- Debugging specific commits

### 2. GitHub Packages

**Purpose:** Versioned releases, long-term storage
**Retention:** Unlimited
**Access:** Repository collaborators + public (if configured)

**Maven Coordinates:**
```
group: com.aether.wallpaper
artifact: aether
version: 1.0.0
```

**Download:**
```bash
# Via GitHub CLI
gh release download v1.0.0 --pattern '*.apk'

# Via curl
curl -L -o aether-1.0.0.apk \
  https://github.com/OWNER/REPO/releases/download/v1.0.0/aether-1.0.0.apk
```

**Use Cases:**
- Public releases
- Beta testing
- Manual APK downloads

### 3. GitHub Releases

**Purpose:** Public release page with notes and downloads
**Retention:** Unlimited
**Access:** Public

**Features:**
- Release notes (Markdown)
- Attached APK files
- Change summaries
- Download statistics

**Use Cases:**
- Official release announcements
- User downloads (before Play Store)
- Release notes history

### 4. Google Play Store

**Purpose:** Primary distribution channel for end users
**Retention:** Unlimited
**Access:** Public (app listing)

**Tracks:**
- **Internal Testing:** Release candidates, limited audience
- **Closed Beta:** Invite-only beta testers
- **Open Beta:** Public opt-in beta testers
- **Production:** Public release

**Staged Rollout:**
```
Internal Testing (10 users)
  ↓ (24-48 hours)
Closed Beta (100 users)
  ↓ (1 week)
Open Beta (1000+ users)
  ↓ (1 week, monitor crash rate)
Production: 10% → 25% → 50% → 100%
```

**Use Cases:**
- Official app distribution
- Automatic updates
- Play Store discovery

---

## Changelog Management

### Maintaining CHANGELOG.md

Follow [Keep a Changelog](https://keepachangelog.com/) format.

**Structure:**
```markdown
# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- New particle effect: fireflies

### Changed
- Improved shader compilation performance

### Fixed
- Memory leak in texture manager

## [1.0.0] - 2025-01-17

### Added
- GPU-accelerated particle effects
- Customizable background images
- Gyroscope parallax support
- 5 built-in shader effects (snow, rain, bubbles, dust, smoke)

### Changed
- Migrated from Canvas to OpenGL ES rendering

### Fixed
- Crash on Mali GPUs
- Shader compilation errors on Adreno 5xx

## [0.5.0-alpha.12] - 2025-01-10

### Added
- Initial project structure
- Gherkin specs for Phase 1
```

### Automating Changelog Updates

**Option 1: Manual Updates**
- Developers update CHANGELOG.md with each PR
- Enforced by PR template checklist

**Option 2: Automated Generation**
- Use [conventional-changelog](https://github.com/conventional-changelog/conventional-changelog)
- Requires conventional commit messages
- Generates changelog from Git history

**Conventional Commits:**
```bash
feat: add particle color picker
fix: resolve memory leak in texture loading
docs: update BUILD.md with M-series Mac guide
chore: bump version to 1.1.0
```

**Generate Changelog:**
```bash
npx conventional-changelog-cli -p angular -i CHANGELOG.md -s
```

---

## Version Bump Checklist

Before creating a release:

- [ ] All tests pass (`./gradlew test connectedAndroidTest`)
- [ ] Update `versionName` in `app/build.gradle.kts`
- [ ] Update `versionCode` in `app/build.gradle.kts`
- [ ] Update `CHANGELOG.md` with release notes
- [ ] Update README.md if needed (version numbers, features)
- [ ] Commit version bump: `git commit -m "chore: bump version to X.Y.Z"`
- [ ] Create Git tag: `git tag -a vX.Y.Z -m "Release X.Y.Z"`
- [ ] Push tag: `git push origin vX.Y.Z`
- [ ] Verify GitHub Actions build succeeds
- [ ] Verify APK is uploaded to GitHub Packages
- [ ] Verify GitHub Release is created
- [ ] Test APK on physical device
- [ ] Merge release branch to `main`
- [ ] Bump `mvp` branch to next development version

---

## Rollback Strategy

If a release has critical bugs:

### Option 1: Hotfix Release

```bash
# Create hotfix branch from main
git checkout main
git checkout -b hotfix/1.0.1

# Fix the bug
# Update version: 1.0.0 → 1.0.1
# Update versionCode: 10000001 → 10000101

# Test, commit, tag
git commit -m "fix: critical bug in shader compilation"
git tag -a v1.0.1 -m "Hotfix release 1.0.1"
git push origin v1.0.1

# Merge back to main and mvp
git checkout main
git merge hotfix/1.0.1
git push origin main

git checkout mvp
git merge main
git push origin mvp
```

### Option 2: Rollback on Play Store

1. Go to Google Play Console
2. Releases → Production
3. Click "Create new release"
4. Select previous working version (e.g., 0.9.5)
5. Update release notes: "Rolled back to 0.9.5 due to critical bug in 1.0.0"
6. Click "Review release" → "Start rollout to Production"

**Note:** Users on 1.0.0 will **not** auto-downgrade. They must manually uninstall and reinstall.

---

## Frequently Asked Questions

### Q: How do I create an alpha release?

```bash
git tag -a v1.0.0-alpha.1 -m "Alpha release 1.0.0-alpha.1"
git push origin v1.0.0-alpha.1
```

GitHub Actions will build and publish automatically.

### Q: Can I skip version numbers?

**No.** Android's version code must always increase.

❌ **Bad:** `1.0.0` (code 10000001) → `1.0.2` (code 10000201) - Skip 1.0.1
✅ **Good:** `1.0.0` → `1.0.1` → `1.0.2` - Sequential

### Q: How do I version a hotfix?

Increment **PATCH** version:
- `1.0.0` → `1.0.1` (hotfix)
- `1.2.3` → `1.2.4` (hotfix)

### Q: When should I increment MAJOR version?

Only for **breaking changes**:
- API changes that break existing integrations
- Storage format changes that lose user data
- Minimum SDK version increase (e.g., 26 → 30)

### Q: How long should I keep old APKs in GitHub Packages?

- **Development builds:** 7 days (GitHub Actions artifacts)
- **Alpha/Beta:** Keep all versions
- **Stable releases:** Keep all versions (disk is cheap)

### Q: Can I publish to F-Droid or APKMirror?

**Yes!** F-Droid requires:
- Open source (✅ Aether is open source)
- No proprietary libraries (✅ Uses only FOSS dependencies)
- Reproducible builds (configure in `build.gradle.kts`)

APKMirror:
- Submit via their developer portal
- Requires signed APK

---

## Summary

**Versioning:**
- SemVer: `MAJOR.MINOR.PATCH[-PRERELEASE]`
- Android Version Code: `XXYYZZZNN`

**Release Types:**
- Development: `0.x.y-alpha.N` (artifacts only)
- Alpha: `X.Y.Z-alpha.N` (GitHub Packages)
- Beta: `X.Y.Z-beta.N` (GitHub + Play Beta)
- RC: `X.Y.Z-rc.N` (Internal Testing)
- Stable: `X.Y.Z` (Production)

**Workflow:**
1. Bump version in `build.gradle.kts`
2. Commit and push
3. Create Git tag: `vX.Y.Z`
4. GitHub Actions builds and publishes
5. Verify release on GitHub
6. Merge to `main`, bump `mvp` to next version

**Distribution:**
- GitHub Actions artifacts (7 days)
- GitHub Packages (unlimited)
- GitHub Releases (public)
- Google Play Store (production)

---

## Next Steps

- **CI/CD Configuration:** [CI_CD.md](CI_CD.md)
- **Building Locally:** [BUILD.md](BUILD.md)
- **Contributing:** [CONTRIBUTING.md](CONTRIBUTING.md)
- **Development Workflow:** [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md)
