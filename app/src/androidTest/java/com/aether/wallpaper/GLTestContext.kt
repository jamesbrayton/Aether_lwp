package com.aether.wallpaper

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import org.junit.Assert.fail

/**
 * Helper class for creating and managing an EGL context for headless OpenGL testing.
 *
 * This is the recommended approach for Android instrumentation tests that need OpenGL,
 * as it doesn't require a GLSurfaceView or window attachment.
 *
 * Usage:
 * ```
 * val glContext = GLTestContext()
 * glContext.runOnGLThread {
 *     // Your GL code here
 * }
 * glContext.destroy()
 * ```
 */
class GLTestContext {
    private var eglDisplay: EGLDisplay? = null
    private var eglContext: EGLContext? = null
    private var eglSurface: EGLSurface? = null
    private var initialized = false

    init {
        initialize()
    }

    /**
     * Initialize the EGL context with a Pbuffer surface (offscreen rendering).
     */
    private fun initialize() {
        // Get EGL display
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("Unable to get EGL14 display")
        }

        // Initialize EGL
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            throw RuntimeException("Unable to initialize EGL14: ${EGL14.eglGetError()}")
        }

        // Choose EGL config
        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_DEPTH_SIZE, 16,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE
        )

        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                eglDisplay, configAttribs, 0,
                configs, 0, configs.size,
                numConfigs, 0
            )
        ) {
            throw RuntimeException("Unable to find EGL config: ${EGL14.eglGetError()}")
        }

        val config = configs[0] ?: throw RuntimeException("No EGL config available")

        // Create EGL context
        val contextAttribs = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )

        eglContext = EGL14.eglCreateContext(
            eglDisplay, config,
            EGL14.EGL_NO_CONTEXT, contextAttribs, 0
        )

        if (eglContext == EGL14.EGL_NO_CONTEXT) {
            throw RuntimeException("Unable to create EGL context: ${EGL14.eglGetError()}")
        }

        // Create Pbuffer surface (1x1 is fine for testing, we don't render to screen)
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, 1080,
            EGL14.EGL_HEIGHT, 1920,
            EGL14.EGL_NONE
        )

        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, config, surfaceAttribs, 0)

        if (eglSurface == EGL14.EGL_NO_SURFACE) {
            throw RuntimeException("Unable to create Pbuffer surface: ${EGL14.eglGetError()}")
        }

        initialized = true
    }

    /**
     * Execute code with the EGL context current on this thread.
     * This is synchronous - the block executes on the calling thread.
     */
    fun runOnGLThread(block: () -> Unit) {
        if (!initialized) {
            throw IllegalStateException("GLTestContext not initialized")
        }

        // Make context current on this thread
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            throw RuntimeException("Unable to make EGL context current: ${EGL14.eglGetError()}")
        }

        try {
            // Execute test code
            block()

            // Check for GL errors
            val error = GLES20.glGetError()
            if (error != GLES20.GL_NO_ERROR) {
                fail("OpenGL error after test execution: $error")
            }
        } finally {
            // Release context from this thread
            EGL14.eglMakeCurrent(
                eglDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
        }
    }

    /**
     * Clean up EGL resources. Call this in @After.
     */
    fun destroy() {
        if (!initialized) return

        EGL14.eglMakeCurrent(
            eglDisplay,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_SURFACE,
            EGL14.EGL_NO_CONTEXT
        )

        eglSurface?.let {
            EGL14.eglDestroySurface(eglDisplay, it)
        }

        eglContext?.let {
            EGL14.eglDestroyContext(eglDisplay, it)
        }

        eglDisplay?.let {
            EGL14.eglTerminate(it)
        }

        eglDisplay = null
        eglContext = null
        eglSurface = null
        initialized = false
    }
}
