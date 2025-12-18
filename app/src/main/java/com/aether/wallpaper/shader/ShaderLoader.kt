package com.aether.wallpaper.shader

import android.content.Context
import android.opengl.GLES20
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

/**
 * Loads and compiles GLSL shaders from assets.
 *
 * Handles shader compilation, linking, and error reporting.
 * All shaders are loaded from the assets/shaders/ directory.
 */
class ShaderLoader(private val context: Context) {

    /**
     * Load shader source code from assets/shaders/ directory.
     *
     * @param filename Name of the shader file (e.g., "vertex_shader.vert")
     * @return The shader source code as a string
     * @throws IOException if the file cannot be read
     */
    fun loadShaderFromAssets(filename: String): String {
        val path = "shaders/$filename"

        try {
            context.assets.open(path).use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    val stringBuilder = StringBuilder()
                    var line: String?

                    while (reader.readLine().also { line = it } != null) {
                        stringBuilder.append(line).append('\n')
                    }

                    return stringBuilder.toString()
                }
            }
        } catch (e: IOException) {
            throw IOException("Failed to load shader from assets: $path", e)
        }
    }

    /**
     * Compile a GLSL shader.
     *
     * @param source The GLSL source code
     * @param type The shader type (GLES20.GL_VERTEX_SHADER or GLES20.GL_FRAGMENT_SHADER)
     * @return The OpenGL shader ID
     * @throws ShaderCompilationException if compilation fails
     */
    fun compileShader(source: String, type: Int): Int {
        // Create shader object
        val shader = GLES20.glCreateShader(type)

        if (shader == 0) {
            throw ShaderCompilationException(
                "Failed to create shader object",
                "glCreateShader returned 0",
                getShaderType(type)
            )
        }

        // Set shader source and compile
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)

        // Check compilation status
        val compileStatus = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compileStatus, 0)

        if (compileStatus[0] == 0) {
            // Compilation failed - get error log
            val errorLog = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader) // Clean up failed shader

            throw when (type) {
                GLES20.GL_VERTEX_SHADER -> ShaderCompilationException.vertexCompilationFailed(errorLog)
                GLES20.GL_FRAGMENT_SHADER -> ShaderCompilationException.fragmentCompilationFailed(errorLog)
                else -> ShaderCompilationException(
                    "Shader compilation failed",
                    errorLog,
                    getShaderType(type)
                )
            }
        }

        return shader
    }

    /**
     * Link vertex and fragment shaders into a shader program.
     *
     * @param vertexShaderId The compiled vertex shader ID
     * @param fragmentShaderId The compiled fragment shader ID
     * @return The OpenGL program ID
     * @throws ShaderCompilationException if linking fails
     */
    fun linkProgram(vertexShaderId: Int, fragmentShaderId: Int): Int {
        // Create program object
        val program = GLES20.glCreateProgram()

        if (program == 0) {
            throw ShaderCompilationException(
                "Failed to create program object",
                "glCreateProgram returned 0",
                ShaderCompilationException.ShaderType.PROGRAM
            )
        }

        // Attach shaders
        GLES20.glAttachShader(program, vertexShaderId)
        GLES20.glAttachShader(program, fragmentShaderId)

        // Link program
        GLES20.glLinkProgram(program)

        // Check link status
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)

        if (linkStatus[0] == 0) {
            // Linking failed - get error log
            val errorLog = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program) // Clean up failed program

            throw ShaderCompilationException.linkingFailed(errorLog)
        }

        return program
    }

    /**
     * Create a complete shader program from asset files.
     *
     * Convenience method that loads, compiles, and links shaders in one call.
     *
     * @param vertexFile Vertex shader filename (e.g., "vertex_shader.vert")
     * @param fragmentFile Fragment shader filename (e.g., "snow.frag")
     * @return The OpenGL program ID
     * @throws IOException if shader files cannot be loaded
     * @throws ShaderCompilationException if compilation or linking fails
     */
    fun createProgram(vertexFile: String, fragmentFile: String): Int {
        // Load shader sources
        val vertexSource = loadShaderFromAssets(vertexFile)
        val fragmentSource = loadShaderFromAssets(fragmentFile)

        // Compile shaders
        val vertexShader = compileShader(vertexSource, GLES20.GL_VERTEX_SHADER)
        val fragmentShader = compileShader(fragmentSource, GLES20.GL_FRAGMENT_SHADER)

        // Link program
        val program = linkProgram(vertexShader, fragmentShader)

        // Clean up shader objects (they're now part of the program)
        GLES20.glDeleteShader(vertexShader)
        GLES20.glDeleteShader(fragmentShader)

        return program
    }

    /**
     * Convert OpenGL shader type constant to ShaderType enum.
     */
    private fun getShaderType(type: Int): ShaderCompilationException.ShaderType? {
        return when (type) {
            GLES20.GL_VERTEX_SHADER -> ShaderCompilationException.ShaderType.VERTEX
            GLES20.GL_FRAGMENT_SHADER -> ShaderCompilationException.ShaderType.FRAGMENT
            else -> null
        }
    }
}
