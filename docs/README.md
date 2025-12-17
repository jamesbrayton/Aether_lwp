# Aether Live Wallpaper Documentation

Welcome to the Aether Live Wallpaper documentation! This directory contains comprehensive guides for building, contributing, releasing, and developing the project.

## 📚 Documentation Index

### For Users
- **[README.md](../README.md)** - Project overview, current status, and quick start

### For Developers

#### Getting Started
- **[BUILD.md](BUILD.md)** - How to build Aether on different platforms (macOS, Windows, Linux)
  - Platform-specific instructions
  - M-series Mac guidance
  - Emulator setup
  - Troubleshooting build issues

- **[QUICK_REFERENCE.md](QUICK_REFERENCE.md)** - Cheat sheet for push-button builds ⭐ **NEW**
  - Trigger builds from any branch
  - Download APKs from releases
  - Common workflows
  - GitHub CLI commands
  - Bash aliases

- **[CONTRIBUTING.md](CONTRIBUTING.md)** - How to contribute to the project
  - Code of conduct
  - Development workflow (TDD)
  - Commit guidelines
  - Pull request process
  - Adding new shader effects

#### Development Workflow
- **[DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md)** - Working between IDEs ⭐ **NEW**
  - VSCode/Claude Code → Android Studio workflow
  - When to use each tool
  - Handoff checklist
  - Best practices for hybrid development
  - **Perfect for M-series Mac developers!**

#### Release and CI/CD
- **[RELEASE.md](RELEASE.md)** - Versioning and release strategy ⭐ **NEW**
  - Semantic versioning scheme
  - Android version codes
  - Release types (alpha, beta, RC, stable)
  - Step-by-step release process
  - Changelog management
  - Distribution channels

- **[CI_CD.md](CI_CD.md)** - GitHub Actions configuration ⭐ **NEW**
  - Workflow triggers
  - Build jobs
  - Secrets configuration
  - APK distribution via GitHub
  - Troubleshooting CI/CD

#### Requirements and Specifications
- **[initial_requirements.md](initial_requirements.md)** - Original project requirements
  - Feature specifications
  - Architecture decisions
  - Shader system design
  - Gherkin acceptance criteria

---

## 🚀 Quick Links by Task

### "I want to test my feature branch"
→ [QUICK_REFERENCE.md](QUICK_REFERENCE.md) - Push-button builds! ⭐ **NEW**

### "I want to build the app locally"
→ [BUILD.md](BUILD.md) - Choose your platform and follow instructions

### "I want to contribute code"
→ [CONTRIBUTING.md](CONTRIBUTING.md) - TDD workflow and guidelines

### "I'm on an M-series Mac and can't build locally"
→ [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md) - Use GitHub Actions for builds!

### "I want to make a release"
→ [RELEASE.md](RELEASE.md) - ZeroVer and release automation ⭐ **UPDATED**

### "I want to set up CI/CD"
→ [CI_CD.md](CI_CD.md) - GitHub Actions configuration

---

## 🎯 Recommended Reading Order

### New Contributors
1. [README.md](../README.md) - Understand the project
2. [initial_requirements.md](initial_requirements.md) - Understand the vision
3. [BUILD.md](BUILD.md) - Set up your build environment
4. [CONTRIBUTING.md](CONTRIBUTING.md) - Learn the TDD workflow

### M-series Mac Developers
1. [BUILD.md](BUILD.md) - M-series Mac section
2. [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md) - **Read this!**
3. [CI_CD.md](CI_CD.md) - Use GitHub Actions for automated builds

### Maintainers
1. [RELEASE.md](RELEASE.md) - Versioning strategy
2. [CI_CD.md](CI_CD.md) - Automate releases
3. [CONTRIBUTING.md](CONTRIBUTING.md) - Enforce quality standards

---

## 📱 Development on M-series Macs

**Key Challenge:** ARM architecture limitations with Android build tools.

**Solution:** Hybrid workflow leveraging multiple tools:

| Task | Tool | Why |
|------|------|-----|
| **Code editing** | VSCode/Claude Code | Fast, lightweight, great Git integration |
| **Testing (unit)** | VSCode terminal | `./gradlew test` works on ARM |
| **Building APKs** | Android Studio OR GitHub Actions | Native ARM support OR cloud x86 builds |
| **Debugging** | Android Studio | Superior debugging tools, native emulator |
| **CI/CD** | GitHub Actions | Automated builds, no local x86 needed |

**Read more:** [DEVELOPMENT_HANDOFF.md](DEVELOPMENT_HANDOFF.md)

---

## 🏗️ Project Architecture

### Phase 1: MVP (Current)
- ✅ Project setup
- 🚧 Shader system foundation
- 🚧 Basic particle effects (snow, rain)
- 🚧 Background image management

### Phase 2: Core Features
- Layer system
- Gyroscope parallax
- Settings UI
- Performance optimization

### Phase 3: Polish
- Custom shader import
- Material Design 3
- Accessibility
- Play Store release

**See:** [initial_requirements.md](initial_requirements.md) for full roadmap.

---

## 🧪 Testing Strategy

**Test-Driven Development (TDD):** All features require tests before implementation.

**Test Types:**
1. **Gherkin Specs** (`spec/*.feature`) - Behavior specifications
2. **Unit Tests** (`app/src/test/`) - Fast, no device needed
3. **Instrumentation Tests** (`app/src/androidTest/`) - On emulator/device

**Example Workflow:**
```bash
# 1. Write Gherkin spec
code spec/shader-loading.feature

# 2. Write failing unit test
code app/src/test/java/com/aether/wallpaper/ShaderLoaderTest.kt

# 3. Run test (expect failure)
./gradlew test --tests "ShaderLoaderTest"

# 4. Implement feature
code app/src/main/java/com/aether/wallpaper/ShaderLoader.kt

# 5. Run test (expect success)
./gradlew test --tests "ShaderLoaderTest"
```

**Read more:** [CONTRIBUTING.md](CONTRIBUTING.md#test-driven-development-tdd)

---

## 🔄 Release Workflow

### Development → Release

```
Feature Branch (VSCode)
  ↓ (TDD, tests pass)
Pull Request → mvp
  ↓ (code review, GitHub Actions builds)
Merge to mvp
  ↓ (integration testing)
Release Branch (release/1.0.0)
  ↓ (bump version, final tests)
Git Tag (v1.0.0)
  ↓ (GitHub Actions builds signed APK)
GitHub Release
  ↓ (manual verification)
Google Play Store
```

**Read more:** [RELEASE.md](RELEASE.md#release-workflow)

---

## 🤝 Contributing

We welcome contributions! Please read:
1. [CONTRIBUTING.md](CONTRIBUTING.md) - Guidelines and workflow
2. [CODE_OF_CONDUCT.md](../CODE_OF_CONDUCT.md) - Community standards (if exists)

**Key principles:**
- ✅ **TDD is mandatory** - Tests before code
- ✅ **Gherkin specs** - Document behavior
- ✅ **Atomic commits** - Small, focused changes
- ✅ **Code review** - All PRs reviewed before merge

---

## 🐛 Troubleshooting

### Build Issues
→ [BUILD.md#troubleshooting](BUILD.md#troubleshooting)

### CI/CD Issues
→ [CI_CD.md#troubleshooting](CI_CD.md#troubleshooting)

### M-series Mac Issues
→ [DEVELOPMENT_HANDOFF.md#troubleshooting-handoff-issues](DEVELOPMENT_HANDOFF.md#troubleshooting-handoff-issues)

---

## 📞 Support

**GitHub Issues:** [Submit an issue](https://github.com/YOUR_USERNAME/Aether_lwp/issues)

**Discussions:** [GitHub Discussions](https://github.com/YOUR_USERNAME/Aether_lwp/discussions)

---

## 📝 Documentation Maintenance

**When to update:**
- New feature added → Update `initial_requirements.md` and `README.md`
- Build process changes → Update `BUILD.md`
- CI/CD changes → Update `CI_CD.md`
- Release process changes → Update `RELEASE.md`

**How to update:**
1. Make changes to Markdown files
2. Commit: `git commit -m "docs: update BUILD.md with new dependency"`
3. Push and create PR

**Documentation style:**
- Use clear headings (`##`, `###`)
- Include code examples with syntax highlighting
- Add troubleshooting sections
- Keep table of contents updated
- Use checkboxes for steps: `- [ ]`
- Link between docs liberally

---

## 📄 License

Aether Live Wallpaper is open source. See [../LICENSE](../LICENSE) for details.

---

**Last Updated:** December 17, 2025
**Project Status:** MVP Phase 1 (Active Development)
