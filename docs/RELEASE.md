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

Aether follows **[ZeroVer (0ver)](https://0ver.org/)** - "Your software's 0ver".

> "Software that _actually_ never reaches 1.0.0. Embrace the perpetual beta!"

### The ZeroVer Manifesto 🎉

**Why we'll never reach 1.0.0:**
- ✨ Software is never "done"—it's always evolving
- 🚀 Perpetual improvement > artificial milestones
- 🎨 Creative projects are journeys, not destinations
- 😄 Takes pressure off "perfect" releases
- 🔄 Embraces continuous delivery philosophy

**What ZeroVer means for you:**
- We're **always** shipping new features
- Updates are **frequent** and **iterative**
- Breaking changes? Sure! We're still in `0.x.y`
- **Stability** matters more than version numbers
- **Quality** over artificial versioning constraints

**Our commitment:**
> "We'll reach 1.0.0 when particle effects achieve sentience and start filing bug reports about humans."

Until then, enjoy the ride at 0.x.y! 🎢

### Format: `0.MINOR.PATCH[-PRERELEASE][+BUILD]`

**Examples:**
- `0.1.0` - Initial release (we're getting started!)
- `0.5.3` - Still not ready (but getting there!)
- `0.99.42` - Almost ready (just kidding, never ready)
- `0.2.1-alpha.3` - Alpha prerelease
- `0.10.0-beta.2` - Beta prerelease
- `0.5.3+20250117.abc1234` - Build metadata

### The ZeroVer Philosophy

**Why ZeroVer?**
- ✅ **Honest:** We're always improving, never "done"
- ✅ **Flexible:** No pressure to reach mythical 1.0
- ✅ **Humble:** Software is perpetually evolving
- ✅ **Humorous:** Takes the pressure off version anxiety

**ZeroVer Rules:**
1. **MAJOR always stays at 0** (we're never "done"!)
2. **MINOR increments** for new features or breaking changes
3. **PATCH increments** for bug fixes
4. **PRERELEASE tags** for alpha/beta/rc versions

### Version Components

#### `0.` (The Zero)

**Never changes.** We embrace perpetual development.

**Philosophy:**
> "Version 1.0.0 is a lie. Software is never finished. Embrace the 0."

#### `MINOR` (Features & Breaking Changes)

Increment when adding **new functionality** or making **breaking changes**.

**Examples:**
- `0.1.x` → `0.2.0`: Add new particle effect (bubbles)
- `0.5.x` → `0.6.0`: Add gyroscope parallax support
- `0.10.x` → `0.11.0`: Complete OpenGL renderer rewrite (yes, even breaking changes!)
- `0.20.x` → `0.21.0`: Change configuration storage format

**No limits!** We can go to `0.99.0`, `0.150.0`, `0.999.0`... forever!

#### `PATCH` (Bug Fixes)

Increment when making **bug fixes** without adding features.

**Examples:**
- `0.5.0` → `0.5.1`: Fix memory leak in texture loading
- `0.5.1` → `0.5.2`: Fix crash on specific devices
- `0.5.2` → `0.5.3`: Fix shader compilation error on Mali GPUs

#### `PRERELEASE` (Optional)

Used for pre-release versions: `alpha`, `beta`, `rc` (release candidate).

**Format:** `-alpha.N`, `-beta.N`, `-rc.N`

**Examples:**
- `0.5.0-alpha.1`: First alpha of version 0.5.0
- `0.5.0-beta.3`: Third beta of version 0.5.0
- `0.5.0-rc.1`: Release candidate for 0.5.0

**Progression:**
```
0.4.5 (current stable... ish)
  ↓
0.5.0-alpha.1 (internal testing)
0.5.0-alpha.2 (internal testing)
  ↓
0.5.0-beta.1 (public testing)
0.5.0-beta.2 (public testing)
  ↓
0.5.0-rc.1 (release candidate)
0.5.0-rc.2 (final RC if issues found)
  ↓
0.5.0 ("stable" release... still 0ver!)
```

#### `BUILD` (Optional)

Build metadata for CI/CD: `+YYYYMMDD.COMMIT_HASH`

**Example:** `0.5.3+20250117.abc1234`

**Usage:**
- Automatically appended by GitHub Actions
- Not used for version precedence (0ver rule)
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
- `0.1.0` → `000100001` (version code: 100,001)
- `0.5.3` → `000500301`
- `0.10.0` → `001000001`
- `0.99.42` → `009904201`

**Maximum Version Code:** `2,147,483,647` (Android's `Integer.MAX_VALUE`)

### Mapping in `build.gradle.kts`

```kotlin
android {
    defaultConfig {
        versionName = "0.5.3"
        versionCode = 500301
    }
}
```

**Automated Calculation (ZeroVer):**
```kotlin
// MAJOR is always 0, so we skip it!
val minorVersion = 5
val patchVersion = 3
val buildNumber = 1

android {
    defaultConfig {
        versionName = "0.$minorVersion.$patchVersion"
        versionCode = minorVersion * 100000 + 
                       patchVersion * 100 + 
                       buildNumber
    }
}
```

---

## Release Types

### 1. Feature Branch Builds (Push-Button)

**Branch:** Any feature branch
**Versioning:** `0.x.y-BRANCHNAME+YYYYMMDD.HASH`
**Distribution:** GitHub Releases (prerelease) + Artifacts
**Purpose:** Testing before PR

**Example:**
- Version: `0.5.0-feature-new-shader+20251217.abc1234`
- Version Code: `500001`
- APK: `aether-0.5.0-feature-new-shader+20251217.abc1234.apk`

**Trigger:**
- Manual: GitHub Actions UI "Run workflow" button
- Manual: `gh workflow run build.yml --ref feature/new-shader`

**Artifacts:**
- APK in GitHub Actions artifacts (7 days)
- APK in GitHub Releases (prerelease, unlimited)
- Automatically tagged with build metadata

### 2. Pull Request Builds

**Branch:** Any \u2192 main
**Versioning:** Current version in `build.gradle.kts`
**Distribution:** GitHub Actions artifacts only
**Purpose:** Verify PR builds successfully

**Example:**
- APK: `app-debug.apk`

**Trigger:**
- Automatic on PR creation/update

**Artifacts:**
- APK in GitHub Actions artifacts (7 days)
- No release created

### 3. Main Branch Auto-Releases

**Branch:** `main`
**Versioning:** `0.x.y+YYYYMMDD.HASH`
**Distribution:** GitHub Releases (latest) + Artifacts
**Purpose:** Production releases

**Example:**
- Version: `0.5.3+20251217.abc1234`
- Version Code: `500301`
- APK: `aether-0.5.3+20251217.abc1234.apk`

**Trigger:**
- Automatic on push to `main` (e.g., merged PR)

**Artifacts:**
- APK in GitHub Actions artifacts (90 days)
- APK in GitHub Releases (latest release, unlimited)
- Automatically tagged with build metadata

**Distribution:**
- GitHub Releases (public download)
- Can be uploaded to Google Play Store manually

### 4. Alpha/Beta/RC Releases (Manual)

**Branch:** `main`
**Versioning:** `0.x.y-alpha.N`, `0.x.y-beta.N`, `0.x.y-rc.N`
**Distribution:** GitHub Releases + Google Play (testing tracks)
**Purpose:** Staged testing before production

**Example:**
- Version: `0.6.0-beta.2`
- Version Code: `600002`
- APK: `aether-0.6.0-beta.2.apk`

**Trigger:**
- Manual workflow dispatch from `main` branch
- Update version in `build.gradle.kts` to include `-beta.2`
- Run workflow

**Artifacts:**
- Signed APK (if keystore configured)
- GitHub Release (marked as prerelease)
- Can be uploaded to Google Play testing tracks

**Acceptance Criteria:**
- All tests pass (unit + instrumentation)
- No critical bugs
- Performance acceptable (60 FPS on reference devices)

**Note:** With ZeroVer, we embrace perpetual beta status. The difference between \"beta\" and \"stable\" is mostly semantic. We're always improving!

---

## Release Workflow

### Step-by-Step Release Process

#### 1. Prepare Release (Optional Version Bump)

**For feature branch push-button build:**
```bash
# No preparation needed! Just click "Run workflow" in GitHub Actions
# Or use CLI:
gh workflow run build.yml --ref feature/your-feature
```

**For main branch release (after PR merge):**
```bash
# If you want to bump version before merge:
git checkout feature/your-feature

# Update version in build.gradle.kts (ZeroVer style)
# Edit app/build.gradle.kts:
#   versionName = "0.6.0"  # Bump minor for new features
#   versionCode = 600001   # Calculate: 6 * 100000 + 0 * 100 + 1

# Commit version bump
git add app/build.gradle.kts
git commit -m "chore: bump version to 0.6.0 for new shader effects"
git push origin feature/your-feature

# Then merge PR to main, which auto-creates release
```

#### 2. Test Your Feature Branch

**Push-button build for testing:**
```bash
# On your feature branch
git checkout feature/new-shader

# Ensure tests pass locally (optional but recommended)
./gradlew test

# Trigger push-button build via GitHub UI:
# 1. Go to GitHub → Actions → "Android Build and Release"
# 2. Click "Run workflow"
# 3. Select branch: feature/new-shader
# 4. Check "Create GitHub Release?" (default: yes)
# 5. Click "Run workflow"

# Or via CLI:
gh workflow run build.yml --ref feature/new-shader

# Wait 3-5 minutes, then download APK from:
# - Releases tab (prerelease)
# - Actions artifacts
```

#### 3. Install and Test APK

```bash
# Download APK from GitHub Release
gh release download 0.5.0-feature-new-shader+20251217.abc1234 \
  --pattern '*.apk'

# Install on device
adb install -r aether-*.apk

# Test the feature!
```

#### 4. Create Pull Request

```bash
# If everything works, create PR to main
gh pr create \
  --title "feat: add new shader effect" \
  --body "Implements new shader effect as specified in #123"
```

#### 5. Merge to Main (Auto-Release!)

```bash
# After PR approval, merge to main
# GitHub automatically:
# 1. Runs tests
# 2. Builds release APK
# 3. Creates GitHub Release with tag: 0.5.0+20251217.abc1234
# 4. Uploads APK to release

# No manual steps needed!
```

#### 6. Verify Release

```bash
# Check GitHub Releases tab
# Download and test production APK
gh release download latest --pattern '*.apk'
adb install -r aether-*.apk
```

#### 7. Bump Version (When Ready)

```bash
# When enough features accumulated, bump minor version
git checkout main
git pull

# Edit app/build.gradle.kts:
#   versionName = "0.6.0"  # Was 0.5.x, now 0.6.0
#   versionCode = 600001

git add app/build.gradle.kts
git commit -m "chore: bump version to 0.6.0"
git push origin main

# This creates a new release: 0.6.0+20251217.xyz
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
