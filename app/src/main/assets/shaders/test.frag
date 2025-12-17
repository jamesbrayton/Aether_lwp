/**
 * @shader Test Effect
 * @id test
 * @version 1.0.0
 * @author Aether Team
 * @source https://github.com/jamesbrayton/Aether_lwp
 * @license MIT
 * @description A simple test shader for validating the metadata parsing system
 * @tags test, validation, simple
 * @minOpenGL 2.0
 *
 * @param u_intensity float 1.0 min=0.0 max=2.0 step=0.1 name="Intensity" desc="Effect intensity"
 * @param u_speed float 1.5 min=0.1 max=5.0 step=0.1 name="Speed" desc="Animation speed"
 */

precision mediump float;

// REQUIRED: Standard uniforms (all shaders must declare these)
uniform sampler2D u_backgroundTexture;
uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_gyroOffset;
uniform float u_depthValue;

// Effect-specific parameters
uniform float u_intensity;
uniform float u_speed;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;

    // Sample background
    vec4 background = texture2D(u_backgroundTexture, uv);

    // Simple animated gradient effect for testing
    float gradient = sin(uv.x * 10.0 + u_time * u_speed) * 0.5 + 0.5;
    vec3 testColor = vec3(gradient) * u_intensity;

    // Composite with background
    gl_FragColor = background + vec4(testColor, 1.0);
}
