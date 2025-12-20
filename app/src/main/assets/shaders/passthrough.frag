/**
 * @shader Passthrough
 * @id passthrough
 * @version 1.0.0
 * @author Aether Team
 * @source https://github.com/jamesbrayton/Aether_lwp
 * @license MIT
 * @description Simple passthrough shader that displays the background image without any effects. Used for background-only wallpapers.
 * @tags utility, background, simple
 * @minOpenGL 2.0
 */

precision mediump float;

// REQUIRED: Standard uniforms (all shaders must declare these)
uniform sampler2D u_backgroundTexture;
uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_gyroOffset;
uniform float u_depthValue;

void main() {
    vec2 uv = gl_FragCoord.xy / u_resolution;

    // Flip Y coordinate because OpenGL textures have (0,0) at bottom-left
    // but Android bitmaps have (0,0) at top-left
    uv.y = 1.0 - uv.y;

    // Simply output the background texture with gyroscope offset for parallax (future)
    vec2 offsetUV = uv + u_gyroOffset;
    gl_FragColor = texture2D(u_backgroundTexture, offsetUV);
}
