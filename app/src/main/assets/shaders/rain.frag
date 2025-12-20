/**
 * @shader Falling Rain
 * @id rain
 * @version 1.0.0
 * @author Aether Team
 * @source https://github.com/aetherteam/aether-lwp-shaders
 * @license MIT
 * @description Fast rain streaks with motion blur. Raindrops fall at a steep angle with elongated streaks, creating a dynamic rainy atmosphere.
 * @tags weather, rain, storm, intense
 * @minOpenGL 2.0
 *
 * @param u_particleCount float 100.0 min=50.0 max=150.0 step=5.0 name="Raindrop Count" desc="Number of rain streaks"
 * @param u_speed float 2.0 min=1.0 max=3.0 step=0.1 name="Fall Speed" desc="How fast rain falls"
 * @param u_angle float 70.0 min=60.0 max=80.0 step=1.0 name="Rain Angle" desc="Angle of rain streaks in degrees"
 * @param u_streakLength float 0.03 min=0.01 max=0.05 step=0.005 name="Streak Length" desc="Length of rain streaks"
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
uniform float u_angle;
uniform float u_streakLength;

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

    // Initialize rain color (additive blending)
    vec3 rainColor = vec3(0.0);

    // Convert angle from degrees to radians
    // 0 degrees = horizontal right, 90 degrees = straight down
    // Angle represents deviation from vertical (90 degrees = vertical)
    float angleRad = radians(u_angle);

    // Calculate rain direction vector
    // sin(angle) gives horizontal component (right)
    // -cos(angle) gives vertical component (down, negative because Y increases downward in UV space)
    vec2 rainDirection = vec2(sin(angleRad), -cos(angleRad));

    // Generate rain particles procedurally
    // Each iteration represents one rain streak
    for (float i = 0.0; i < 150.0; i++) {
        // Early exit if we've rendered enough particles
        if (i >= u_particleCount) {
            break;
        }

        // Generate pseudo-random initial position for this particle
        vec2 particleSeed = hash2D(i);

        // Fast diagonal motion
        // Speed is scaled up (0.3) for faster motion than snow
        // Particles move in the direction defined by rainDirection
        vec2 particlePos = particleSeed + rainDirection * u_time * u_speed * 0.3;

        // Wrap particle position using fract()
        // This creates seamless looping as particles exit screen bounds
        particlePos = fract(particlePos);

        // Calculate distance from current pixel to rain streak
        // Rain is rendered as a line segment, not a point

        // Vector from particle center to current pixel
        vec2 toPixel = uv - particlePos;

        // Calculate perpendicular distance to the rain streak line
        // This is the cross product of toPixel and rainDirection
        // abs() gives unsigned distance (both sides of line)
        float distToLine = abs(toPixel.x * rainDirection.y - toPixel.y * rainDirection.x);

        // Calculate distance along the rain streak direction
        // This is the dot product of toPixel and rainDirection
        // Positive values are ahead, negative values are behind
        float alongLine = dot(toPixel, rainDirection);

        // Determine if current pixel is within the streak bounds
        // alongLine must be between 0.0 and u_streakLength
        // This creates the elongated streak effect (motion blur)
        float isInStreak = step(0.0, alongLine) * step(alongLine, u_streakLength);

        // Create alpha channel based on distance to line
        // smoothstep creates soft edges for the streak
        // 0.001 = outer radius (transparent)
        // 0.0005 = inner radius (opaque)
        // Streaks are very thin for realistic rain appearance
        float alpha = smoothstep(0.001, 0.0005, distToLine) * isInStreak;

        // Accumulate rain color with slight blue tint
        // RGB(0.7, 0.8, 1.0) creates cool blue-white rain color
        // This enhances the rainy atmosphere and contrasts with warm backgrounds
        rainColor += vec3(0.7, 0.8, 1.0) * alpha;
    }

    // Composite: background + rain streaks using proper alpha blending
    // Limit rain intensity to prevent oversaturation
    float rainIntensity = min(length(rainColor), 0.5); // Cap at 0.5 to prevent white-out
    vec3 finalRainColor = normalize(rainColor + vec3(0.001)) * rainIntensity;

    // Alpha blend rain over background (not pure additive to prevent oversaturation)
    gl_FragColor = vec4(background.rgb * (1.0 - rainIntensity) + finalRainColor, background.a);
}
