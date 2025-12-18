package com.aether.wallpaper.shader

/**
 * Exception thrown when shader compilation or linking fails.
 *
 * Contains the GLSL compiler/linker error log to help diagnose issues.
 */
class ShaderCompilationException(
    message: String,
    val errorLog: String,
    val shaderType: ShaderType? = null
) : Exception("$message\n\nGLSL Error Log:\n$errorLog") {

    enum class ShaderType {
        VERTEX,
        FRAGMENT,
        PROGRAM
    }

    companion object {
        /**
         * Create exception for vertex shader compilation failure.
         */
        fun vertexCompilationFailed(errorLog: String): ShaderCompilationException {
            return ShaderCompilationException(
                "Vertex shader compilation failed",
                errorLog,
                ShaderType.VERTEX
            )
        }

        /**
         * Create exception for fragment shader compilation failure.
         */
        fun fragmentCompilationFailed(errorLog: String): ShaderCompilationException {
            return ShaderCompilationException(
                "Fragment shader compilation failed",
                errorLog,
                ShaderType.FRAGMENT
            )
        }

        /**
         * Create exception for shader program linking failure.
         */
        fun linkingFailed(errorLog: String): ShaderCompilationException {
            return ShaderCompilationException(
                "Shader program linking failed",
                errorLog,
                ShaderType.PROGRAM
            )
        }
    }
}
