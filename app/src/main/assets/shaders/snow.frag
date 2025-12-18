/**
 * @shader Falling Snow
 * @id snow
 * @version 1.0.0
 * @author Aether Team
 * @source https://github.com/aetherteam/aether-lwp-shaders
 * @license MIT
 * @description Gentle falling snow with lateral drift. Particles fall downward with subtle side-to-side motion, creating a peaceful winter atmosphere.
 * @tags winter, weather, particles, gentle
 * @minOpenGL 2.0
 *
 * @param u_particleCount float 100.0 min=10.0 max=200.0 step=1.0 name="Particle Count" desc="Number of visible snow particles"
 * @param u_speed float 1.0 min=0.1 max=3.0 step=0.1 name="Fall Speed" desc="How fast snow falls"
 * @param u_driftAmount float 0.5 min=0.0 max=1.0 step=0.05 name="Lateral Drift" desc="Amount of side-to-side wobble"
 */

precision mediump float;

// REQUIRED: Standard uniforms (all shaders must declare these)
uniform sampler2D u_backgroundTexture;
uniform float u_time;
uniform vec2 u_resolution;
uniform vec2 u_gyroOffset;
uniform float u_depthValue;

// Effect-specific parameters (declared in @param tags above)
uniform float u_particleCount;
uniform float u_speed;
uniform float u_driftAmount;

// Hash function for pseudo-random particle positions
// Based on common hash functions used in shader programming
// Input: single float seed
// Output: 2D random vector in range [0.0, 1.0]
vec2 hash2D(float n) {
    // Use different prime numbers for X and Y to ensure independence
    return fract(sin(vec2(n, n + 1.0)) * vec2(43758.5453, 22578.1459));
}

void main() {
    // Calculate normalized UV coordinates (0.0 to 1.0)
    vec2 uv = gl_FragCoord.xy / u_resolution;

    // Sample background texture
    // Even if not visually prominent, always sample to comply with standard uniform contract
    vec4 background = texture2D(u_backgroundTexture, uv);

    // Initialize snow color (additive blending)
    vec3 snowColor = vec3(0.0);

    // Generate particles procedurally
    // Each iteration represents one particle
    // Note: GLSL loops require constant or uniform-based conditions
    for (float i = 0.0; i < 200.0; i++) {
        // Early exit if we've rendered enough particles
        if (i >= u_particleCount) {
            break;
        }

        // Generate pseudo-random initial position for this particle
        // Each particle ID (i) produces a unique random seed
        vec2 particleSeed = hash2D(i);

        // Vertical falling motion
        // Speed is scaled down (0.1) for gentle motion
        // mod() wraps particles from bottom (0.0) to top (1.0)
        float fallOffset = mod(u_time * u_speed * 0.1, 1.0);
        float yPos = particleSeed.y - fallOffset;
        yPos = mod(yPos + 1.0, 1.0); // Wrap around: when yPos < 0, it becomes 1.0

        // Lateral drift (side-to-side motion)
        // sin() creates oscillating motion
        // Different phase per particle (u_time + i) ensures variety
        // Scaled by u_driftAmount (0.0 = no drift, 1.0 = maximum drift)
        // 0.05 scales the drift to reasonable screen-space range
        float xDrift = sin(u_time + i) * u_driftAmount * 0.05;
        float xPos = particleSeed.x + xDrift;

        // Particle position in normalized screen space [0.0, 1.0]
        vec2 particlePos = vec2(xPos, yPos);

        // Calculate distance from current pixel to particle center
        float dist = distance(uv, particlePos);

        // Particle size in normalized coordinates
        // 0.003 is small and subtle (roughly 3-6 pixels on 1080p screen)
        float particleSize = 0.003;

        // Create soft circular particle with smooth edges
        // smoothstep creates gradual falloff from opaque center to transparent edge
        // particleSize * 0.5 = inner radius (fully opaque)
        // particleSize = outer radius (fully transparent)
        float alpha = smoothstep(particleSize, particleSize * 0.5, dist);

        // Accumulate snow color (white particles)
        // Additive blending: multiple overlapping particles create brighter areas
        snowColor += vec3(1.0) * alpha;
    }

    // Composite: background + snow particles
    // Snow is pure additive (no alpha channel needed, just RGB addition)
    gl_FragColor = background + vec4(snowColor, 1.0);
}
