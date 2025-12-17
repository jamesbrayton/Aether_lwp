package com.aether.wallpaper.model

/**
 * Complete descriptor for a shader effect, parsed from embedded metadata.
 *
 * Metadata is embedded in GLSL shader files using JavaDoc-style comments:
 * ```
 * /**
 *  * @shader Falling Snow
 *  * @id snow
 *  * @version 1.0.0
 *  * @author Aether Team
 *  * @source https://github.com/aetherteam/aether-lwp-shaders
 *  * @license MIT
 *  * @description Gentle falling snow with lateral drift
 *  * @tags winter, weather, particles
 *  * @minOpenGL 2.0
 *  *
 *  * @param u_speed float 1.0 min=0.1 max=3.0 step=0.1 name="Fall Speed"
 *  * (end of comment block)
 * ```
 *
 * @property id Unique identifier for the shader (e.g., "snow", "rain")
 * @property name Display name shown in UI (e.g., "Falling Snow")
 * @property version Semantic version string (e.g., "1.0.0")
 * @property author Creator name or organization (optional)
 * @property source Source repository URL (optional, for attribution)
 * @property license License identifier (e.g., "MIT", "Apache-2.0") (optional)
 * @property description Long-form description for UI/help (optional)
 * @property tags Searchable/filterable tags (e.g., ["winter", "weather"])
 * @property fragmentShaderPath Path to the .frag file in assets (e.g., "shaders/snow.frag")
 * @property parameters List of configurable parameters with UI metadata
 * @property minOpenGLVersion Minimum OpenGL ES version required (default: "2.0")
 */
data class ShaderDescriptor(
    val id: String,
    val name: String,
    val version: String,
    val author: String? = null,
    val source: String? = null,
    val license: String? = null,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val fragmentShaderPath: String,
    val parameters: List<ParameterDefinition> = emptyList(),
    val minOpenGLVersion: String = "2.0"
) {
    /**
     * Validates that this shader descriptor is complete and correct.
     * @throws IllegalArgumentException if validation fails
     */
    fun validate() {
        require(id.isNotBlank()) { "Shader id cannot be blank" }
        require(name.isNotBlank()) { "Shader name cannot be blank" }
        require(version.isNotBlank()) { "Shader version cannot be blank" }
        require(fragmentShaderPath.isNotBlank()) { "Shader fragmentShaderPath cannot be blank" }

        // Validate version format (basic semantic version check)
        val versionPattern = Regex("""^\d+\.\d+\.\d+.*$""")
        require(versionPattern.matches(version)) {
            "Shader version must follow semantic versioning (e.g., 1.0.0)"
        }

        // Validate OpenGL version format
        val glVersionPattern = Regex("""^\d+\.\d+$""")
        require(glVersionPattern.matches(minOpenGLVersion)) {
            "minOpenGLVersion must be in format X.Y (e.g., 2.0, 3.0)"
        }

        // Validate all parameters
        parameters.forEach { it.validate() }

        // Check for duplicate parameter IDs
        val paramIds = parameters.map { it.id }
        require(paramIds.size == paramIds.distinct().size) {
            "Duplicate parameter IDs found: ${paramIds.groupingBy { it }.eachCount().filter { it.value > 1 }.keys}"
        }
    }

    /**
     * Returns a display-friendly summary of the shader.
     */
    fun getSummary(): String {
        val authorText = author?.let { " by $it" } ?: ""
        val paramCount = parameters.size
        val paramText = when (paramCount) {
            0 -> "no parameters"
            1 -> "1 parameter"
            else -> "$paramCount parameters"
        }
        return "$name v$version$authorText ($paramText)"
    }
}
