package com.aether.wallpaper.shader

/**
 * Exception thrown when shader metadata cannot be parsed.
 *
 * This can occur due to:
 * - Missing required metadata tags (@shader, @id, @version)
 * - Invalid tag syntax or values
 * - Malformed parameter definitions
 * - Invalid parameter types
 *
 * @param message Description of the parsing error
 * @param cause Optional underlying exception that caused the parse failure
 */
class ShaderParseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
