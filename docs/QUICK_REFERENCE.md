# Quick Reference: Push-Button Builds

This is a cheat sheet for using the new push-button build system.

## TL;DR

```bash
# Test your feature branch (creates release)
gh workflow run build.yml --ref feature/your-branch

# Merge to main = automatic release
git push origin main
```

---

## Feature Branch Testing

### Via GitHub UI

1. **Go to Actions:** `https://github.com/YOUR_USERNAME/Aether_lwp/actions`
2. **Select workflow:** Click "Android Build and Release"
3. **Run workflow:**
   - Click "Run workflow" button (top right)
   - Select your branch from dropdown
   - Leave "Create GitHub Release?" checked
   - Click green "Run workflow" button
4. **Wait 3-5 minutes**
5. **Download APK:**
   - Go to Releases tab
   - Find release: `0.5.0-feature-your-branch+20251217.abc1234`
   - Download APK from Assets

### Via GitHub CLI

```bash
# Simple (uses current branch)
gh workflow run build.yml

# Specific branch
gh workflow run build.yml --ref feature/new-shader

# Without release (artifacts only)
gh workflow run build.yml --ref feature/test -f create_release=false

# Check status
gh run list --workflow=build.yml

# Watch logs
gh run watch

# Download artifacts
gh run download RUN_ID
```

### Via Browser Bookmark

**Bookmark this URL (replace YOUR_USERNAME/REPO):**
```
https://github.com/YOUR_USERNAME/Aether_lwp/actions/workflows/build.yml
```

One-click access to workflow trigger!

---

## Manual Releases Only

**Releases are intentional!** Merging to main does NOT create a release.

**To create a release:**
1. Go to Actions tab
2. Click "Android Build and Release"
3. Click "Run workflow"
4. Select branch (usually `main`)
5. Check ✅ "Create GitHub Release?"
6. Click "Run workflow"

**Via CLI:**
```bash
# Manual release from main
gh workflow run build.yml --ref main

# Manual release from feature branch
gh workflow run build.yml --ref feature/new-shader
```

**Result:**
```bash
# Release: 0.5.0+20251218.abc1234
# APK: aether-0.5.0+20251218.abc1234.apk

# Check releases
gh release list
gh release download latest --pattern '*.apk'
```

**Why manual?**
- Main branch is protected (all changes via PR)
- Prevents accidental releases
- Better control over what gets released

---

## Release Naming

### Feature Branch Release
```
0.5.0-feature-new-shader+20251217.abc1234
│   │   │                │        │
│   │   │                │        └─ Git commit (short)
│   │   │                └────────── Build date (YYYYMMDD)
│   │   └─────────────────────────── Branch name (sanitized)
│   └─────────────────────────────── Patch version
└─────────────────────────────────── Minor version (MAJOR always 0!)
```

### Main Branch Release
```
0.5.0+20251217.abc1234
│   │  │        │
│   │  │        └─ Git commit (short)
│   │  └────────── Build date (YYYYMMDD)
│   └───────────── Patch version
└───────────────── Minor version (ZeroVer!)
```

---

## Common Workflows

### 1. Test Feature Before PR

```bash
# On feature branch
git checkout feature/new-shader
git push origin feature/new-shader

# Trigger build
gh workflow run build.yml --ref feature/new-shader

# Wait, then download
gh release download 0.5.0-feature-new-shader+20251217.abc1234 --pattern '*.apk'

# Install and test
adb install -r aether-*.apk

# If good, create PR
gh pr create --title "feat: add new shader" --body "..."
```

### 2. Quick Test (No Release)

```bash
# Build without creating release
gh workflow run build.yml -f create_release=false

# Download from artifacts (not releases)
gh run list --workflow=build.yml
gh run download RUN_ID
```

### 3. Emergency Hotfix

```bash
# Create hotfix branch
git checkout -b hotfix/critical-crash main

# Fix bug, commit
git add .
git commit -m "fix: resolve critical crash on Mali GPUs"

# Test with push-button build
gh workflow run build.yml --ref hotfix/critical-crash

# Download, test, verify fix
gh release download latest --pattern '*hotfix*'
adb install -r aether-*.apk

# If fixed, merge to main (auto-release)
gh pr create --title "fix: critical crash" --body "..."
# After approval:
gh pr merge --squash
```

### 4. Bump Version

```bash
# When ready for new minor version
git checkout main
git pull

# Edit app/build.gradle.kts
# Change:
#   versionName = "0.5.3" → "0.6.0"
#   versionCode = 500301 → 600001

git add app/build.gradle.kts
git commit -m "chore: bump version to 0.6.0"
git push origin main

# This creates release: 0.6.0+20251217.xyz
```

---

## Version Bumping (ZeroVer)

### When to Bump MINOR (0.x.0)

- ✅ New features added
- ✅ Breaking changes
- ✅ Major refactoring
- ✅ New shader effects
- ✅ Architecture changes

**Example:** `0.5.3` → `0.6.0`

### When to Bump PATCH (0.x.y)

- ✅ Bug fixes
- ✅ Performance improvements (no API changes)
- ✅ Documentation updates
- ✅ Minor tweaks

**Example:** `0.5.3` → `0.5.4`

### Never Bump MAJOR

**MAJOR stays at 0 forever!** That's the ZeroVer way. 🎉

---

## Troubleshooting

### "Workflow run failed"

**Check logs:**
```bash
gh run list --workflow=build.yml
gh run view RUN_ID
```

**Common issues:**
- Tests failed → Fix tests locally first
- Lint errors → Run `./gradlew lint` locally
- Gradle sync issues → Check `build.gradle.kts`

### "Release not created"

**Verify trigger:**
- Was it a PR build? (PRs don't create releases)
- Did you uncheck "Create GitHub Release?"?
- Check workflow logs for errors

### "Can't find APK"

**Check both places:**
```bash
# Releases (if create_release=true)
gh release list
gh release view RELEASE_TAG

# Artifacts (always available)
gh run list
gh run view RUN_ID
gh run download RUN_ID
```

### "Permission denied"

**Ensure GitHub CLI authenticated:**
```bash
gh auth status
gh auth login
```

---

## GitHub CLI Setup

**Install:**
```bash
# macOS
brew install gh

# Linux
sudo apt install gh

# Windows
winget install GitHub.cli
```

**Authenticate:**
```bash
gh auth login
# Select: GitHub.com
# Select: HTTPS
# Authenticate in browser
```

**Verify:**
```bash
gh auth status
gh repo view
```

---

## Aliases (Optional)

Add to `~/.bashrc` or `~/.zshrc`:

```bash
# Push-button build current branch
alias apk-build='gh workflow run build.yml'

# Push-button build specific branch
apk-build-branch() {
  gh workflow run build.yml --ref "$1"
}

# Download latest release APK
alias apk-download='gh release download latest --pattern "*.apk"'

# Install latest APK
alias apk-install='gh release download latest --pattern "*.apk" && adb install -r aether-*.apk'

# Watch workflow
alias apk-watch='gh run watch'

# List releases
alias apk-releases='gh release list'
```

**Usage:**
```bash
# Build current branch
apk-build

# Build specific branch
apk-build-branch feature/new-shader

# Download and install latest
apk-install
```

---

## Summary

**Push-button builds:**
- ✅ Test before PR (no main pollution)
- ✅ Easy APK access (GitHub Releases)
- ✅ Automatic tagging (version+date+commit)
- ✅ Prerelease marking (clear distinction)

**Main auto-releases:**
- ✅ Every merge is a release (continuous delivery)
- ✅ No manual steps (fully automated)
- ✅ Historical versions (all in Releases tab)
- ✅ Latest APK always available

**ZeroVer:**
- ✅ No 1.0.0 pressure (perpetual beta)
- ✅ Frequent releases (no artificial milestones)
- ✅ Honest about status (always improving)

---

**Questions?** Open an issue or discussion on GitHub!
