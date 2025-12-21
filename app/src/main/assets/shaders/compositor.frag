/**
 * @shader Layer Compositor
 * @id compositor
 * @version 1.0.0
 * @author Aether Team
 * @description Composites multiple layer textures with per-layer opacity
 * @tags utility, compositing, internal
 * @minOpenGL 2.0
 */

precision mediump float;

// Standard uniforms (required contract)
uniform sampler2D u_backgroundTexture;
uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_gyroOffset;
uniform float u_depthValue;

// Layer textures (up to 5 layers)
uniform sampler2D u_layer0;
uniform sampler2D u_layer1;
uniform sampler2D u_layer2;
uniform sampler2D u_layer3;
uniform sampler2D u_layer4;

// Per-layer opacity
uniform float u_opacity0;
uniform float u_opacity1;
uniform float u_opacity2;
uniform float u_opacity3;
uniform float u_opacity4;

// Active layer count
uniform int u_layerCount;

void main() {
    // Calculate UV coordinates
    vec2 uv = gl_FragCoord.xy / u_resolution;
    uv.y = 1.0 - uv.y; // Flip Y

    // Start with background
    vec4 finalColor = texture2D(u_backgroundTexture, uv);

    // Composite layers with alpha blending
    // Formula: finalColor = mix(finalColor, layerColor, layerColor.a * opacity)

    if (u_layerCount > 0) {
        vec4 layer0 = texture2D(u_layer0, uv);
        finalColor = mix(finalColor, layer0, layer0.a * u_opacity0);
    }

    if (u_layerCount > 1) {
        vec4 layer1 = texture2D(u_layer1, uv);
        finalColor = mix(finalColor, layer1, layer1.a * u_opacity1);
    }

    if (u_layerCount > 2) {
        vec4 layer2 = texture2D(u_layer2, uv);
        finalColor = mix(finalColor, layer2, layer2.a * u_opacity2);
    }

    if (u_layerCount > 3) {
        vec4 layer3 = texture2D(u_layer3, uv);
        finalColor = mix(finalColor, layer3, layer3.a * u_opacity3);
    }

    if (u_layerCount > 4) {
        vec4 layer4 = texture2D(u_layer4, uv);
        finalColor = mix(finalColor, layer4, layer4.a * u_opacity4);
    }

    gl_FragColor = finalColor;
}
