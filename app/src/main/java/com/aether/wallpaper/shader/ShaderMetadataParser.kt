package com.aether.wallpaper.shader

import com.aether.wallpaper.model.ParameterDefinition
import com.aether.wallpaper.model.ParameterType
import com.aether.wallpaper.model.ShaderDescriptor

/**
 * Parses shader metadata from JavaDoc-style comments embedded in GLSL shader files.
 *
 * Metadata format uses JavaDoc-style comment blocks at the top of .frag files.
 * See test.frag in assets/shaders/ for a complete example.
 *
 * Required tags: @shader, @id, @version
 * Optional tags: @author, @source, @license, @description, @tags, @minOpenGL
 * Parameter format: @param name type default min=X max=Y step=Z name="Display" desc="Help"
 *
 * Supported parameter types: float, int, bool, color, vec2, vec3, vec4
 */
class ShaderMetadataParser {

    /**
     * Parses a shader source file and extracts metadata into a ShaderDescriptor.
     *
     * @param shaderSource The complete GLSL shader source code
     * @param filePath Path to the shader file (e.g., "shaders/snow.frag")
     * @return ShaderDescriptor containing all parsed metadata
     * @throws ShaderParseException if metadata is missing or malformed
     */
    fun parse(shaderSource: String, filePath: String): ShaderDescriptor {
        val metadataComment = extractMetadataComment(shaderSource)
            ?: throw ShaderParseException("No metadata comment found in $filePath")

        val lines = metadataComment.lines()

        // Parse required tags
        val name = parseRequiredTag(lines, "shader", filePath)
        val id = parseRequiredTag(lines, "id", filePath)
        val version = parseRequiredTag(lines, "version", filePath)

        // Parse optional tags
        val author = parseOptionalTag(lines, "author")
        val source = parseOptionalTag(lines, "source")
        val license = parseOptionalTag(lines, "license")
        val description = parseOptionalTag(lines, "description")
        val minOpenGL = parseOptionalTag(lines, "minOpenGL") ?: "2.0"

        // Parse tags list
        val tagsString = parseOptionalTag(lines, "tags")
        val tags = if (tagsString != null) {
            tagsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            emptyList()
        }

        // Parse parameters
        val parameters = mutableListOf<ParameterDefinition>()
        for (line in lines) {
            if (line.contains("@param")) {
                try {
                    val param = parseParameter(line)
                    if (param != null) {
                        parameters.add(param)
                    }
                } catch (e: Exception) {
                    throw ShaderParseException("Failed to parse parameter in $filePath: ${line.trim()}", e)
                }
            }
        }

        return ShaderDescriptor(
            id = id,
            name = name,
            version = version,
            author = author,
            source = source,
            license = license,
            description = description,
            tags = tags,
            fragmentShaderPath = filePath,
            parameters = parameters,
            minOpenGLVersion = minOpenGL
        )
    }

    /**
     * Extracts the JavaDoc-style metadata comment block from shader source.
     * Returns the content between /** and *\/ at the start of the file.
     */
    fun extractMetadataComment(shaderSource: String): String? {
        val regex = Regex("""/\*\*(.*?)\*/""", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(shaderSource) ?: return null
        return match.groupValues[1]
    }

    /**
     * Parses a single tag value from a line.
     * Example: " * @shader Falling Snow" with tag "shader" returns "Falling Snow"
     */
    fun parseTag(line: String, tagName: String): String? {
        val pattern = Regex("""@$tagName\s+(.+)""")
        val match = pattern.find(line.trim()) ?: return null
        return match.groupValues[1].trim()
    }

    /**
     * Parses a required tag, throwing an exception if not found.
     */
    private fun parseRequiredTag(lines: List<String>, tagName: String, filePath: String): String {
        for (line in lines) {
            val value = parseTag(line, tagName)
            if (value != null) return value
        }
        throw ShaderParseException("Missing required tag: @$tagName in $filePath")
    }

    /**
     * Parses an optional tag, returning null if not found.
     */
    private fun parseOptionalTag(lines: List<String>, tagName: String): String? {
        for (line in lines) {
            val value = parseTag(line, tagName)
            if (value != null) return value
        }
        return null
    }

    /**
     * Parses a @param line into a ParameterDefinition.
     *
     * Format: @param u_name type default min=X max=Y step=Z name="Display" desc="Description"
     * Required: u_name, type, default
     * Optional: min, max, step, name, desc
     */
    fun parseParameter(line: String): ParameterDefinition? {
        val paramPattern = Regex("""@param\s+(\S+)\s+(\S+)\s+(\S+)(.*)""")
        val match = paramPattern.find(line.trim()) ?: return null

        val paramId = match.groupValues[1]
        val typeString = match.groupValues[2]
        val defaultString = match.groupValues[3]
        val attributes = match.groupValues[4]

        // Parse type
        val type = when (typeString.lowercase()) {
            "float" -> ParameterType.FLOAT
            "int" -> ParameterType.INT
            "bool" -> ParameterType.BOOL
            "color" -> ParameterType.COLOR
            "vec2" -> ParameterType.VEC2
            "vec3" -> ParameterType.VEC3
            "vec4" -> ParameterType.VEC4
            else -> throw ShaderParseException("Invalid parameter type: $typeString")
        }

        // Parse default value
        val defaultValue: Any = when (type) {
            ParameterType.FLOAT -> defaultString.toFloatOrNull()
                ?: throw ShaderParseException("Invalid float default value: $defaultString")
            ParameterType.INT -> defaultString.toIntOrNull()
                ?: throw ShaderParseException("Invalid int default value: $defaultString")
            ParameterType.BOOL -> when (defaultString.lowercase()) {
                "true" -> true
                "false" -> false
                else -> throw ShaderParseException("Invalid bool default value: $defaultString (must be true/false)")
            }
            ParameterType.COLOR -> defaultString // Parse as string for now
            ParameterType.VEC2, ParameterType.VEC3, ParameterType.VEC4 -> defaultString // Parse as string for now
        }

        // Parse optional attributes
        val minValue = parseFloatAttribute(attributes, "min", type)
        val maxValue = parseFloatAttribute(attributes, "max", type)
        val stepValue = parseFloatAttribute(attributes, "step", type)
        val displayName = parseQuotedAttribute(attributes, "name") ?: paramId
        val description = parseQuotedAttribute(attributes, "desc") ?: ""

        return ParameterDefinition(
            id = paramId,
            name = displayName,
            type = type,
            defaultValue = defaultValue,
            minValue = minValue,
            maxValue = maxValue,
            step = stepValue,
            description = description
        )
    }

    /**
     * Parses a numeric attribute like min=0.0, max=5.0, step=0.1
     */
    private fun parseFloatAttribute(attributes: String, name: String, type: ParameterType): Any? {
        val pattern = Regex("""$name=([\d.]+)""")
        val match = pattern.find(attributes) ?: return null
        val valueString = match.groupValues[1]

        return when (type) {
            ParameterType.FLOAT -> valueString.toFloatOrNull()
            ParameterType.INT -> valueString.toIntOrNull()
            else -> null
        }
    }

    /**
     * Parses a quoted attribute like name="Display Name" or desc="Description text"
     */
    private fun parseQuotedAttribute(attributes: String, name: String): String? {
        val pattern = Regex("""$name="([^"]+)"""")
        val match = pattern.find(attributes) ?: return null
        return match.groupValues[1]
    }
}
