package com.aether.wallpaper.model

/**
 * Supported parameter types for shader uniforms.
 * These types map to GLSL uniform types and UI control types.
 */
enum class ParameterType {
    /** Single floating-point value (GLSL: float) */
    FLOAT,

    /** Single integer value (GLSL: int) */
    INT,

    /** Boolean value (GLSL: bool) */
    BOOL,

    /** RGB color value (GLSL: vec3) */
    COLOR,

    /** 2D vector (GLSL: vec2) */
    VEC2,

    /** 3D vector (GLSL: vec3) */
    VEC3,

    /** 4D vector (GLSL: vec4) */
    VEC4
}
