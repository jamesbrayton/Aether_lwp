---
tags: #foundation #project_vision
created: 2025-12-16
---

# Aether_lwp - Project Brief

## Vision
Android live wallpaper application combining user-selected background images with GPU-accelerated particle effects and gyroscope-based 3D parallax motion using GLSL shaders.

## Core Value Propositions
1. **Visual Depth:** Multi-layer particle effects with true 3D parallax create immersive wallpapers
2. **Customization:** Users select backgrounds and compose multiple effects with fine-grained control
3. **Performance:** GPU-accelerated shaders deliver 60fps while maintaining battery efficiency
4. **Extensibility:** Modular shader architecture enables easy addition of new effects

## Technical Foundation
- **Platform:** Android API 26+ (Android 8.0+, 90%+ device coverage)
- **Language:** Kotlin 1.9.23
- **Graphics:** OpenGL ES 2.0 with GLSL fragment shaders
- **Architecture:** Clean separation - Settings Activity (UI) → SharedPreferences (config) → WallpaperService (rendering)

## Target Users
- Android users seeking dynamic, customizable wallpapers
- Users who value visual aesthetics and smooth animations
- Mid-range to high-end device owners (60fps target)

## Success Criteria
- ✅ 60fps on mid-range devices (30fps fallback configurable)
- ✅ Battery consumption comparable to standard live wallpapers
- ✅ Smooth gyroscope parallax without jitter or nausea
- ✅ Easy effect addition (< 30 min for new shader)
- ✅ Intuitive settings UI with real-time preview

## Out of Scope (V1)
- Sticker/image layers (transparent PNGs)
- Interactive effects (touch response)
- Time-based effects (day/night cycle)
- Weather integration
- Social sharing / preset marketplace
- OpenGL ES 3.0+ compute shaders
