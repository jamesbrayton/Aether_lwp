package com.aether.wallpaper

import android.opengl.GLSurfaceView
import android.view.SurfaceHolder
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface
import javax.microedition.khronos.opengles.GL10

/**
 * GL rendering thread for wallpaper service.
 *
 * This class manages the OpenGL ES rendering loop without using GLSurfaceView.
 * It creates and manages its own EGL context and rendering thread, using the
 * provided SurfaceHolder from the WallpaperService.Engine.
 *
 * This is the correct pattern for OpenGL wallpapers, as GLSurfaceView cannot
 * be properly integrated with WallpaperService's surface lifecycle.
 */
class GLWallpaperRenderer(
    private val holder: SurfaceHolder,
    private val renderer: GLSurfaceView.Renderer
) {
    private var glThread: GLThread? = null
    private var running = false

    fun start() {
        if (!running) {
            running = true
            glThread = GLThread()
            glThread?.start()
        }
    }

    fun stop() {
        if (running) {
            running = false
            glThread?.interrupt()
            try {
                glThread?.join()
            } catch (e: InterruptedException) {
                // Thread interrupted during join
            }
            glThread = null
        }
    }

    private inner class GLThread : Thread("GLWallpaperThread") {
        override fun run() {
            val egl = EGLContext.getEGL() as EGL10
            var eglDisplay: EGLDisplay? = null
            var eglSurface: EGLSurface? = null
            var eglContext: EGLContext? = null
            var gl: GL10

            try {
                // Initialize EGL
                eglDisplay = egl.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
                if (eglDisplay == EGL10.EGL_NO_DISPLAY) {
                    throw RuntimeException("eglGetDisplay failed")
                }

                val version = IntArray(2)
                if (!egl.eglInitialize(eglDisplay, version)) {
                    throw RuntimeException("eglInitialize failed")
                }

                // Configure EGL
                val configSpec = intArrayOf(
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_DEPTH_SIZE, 16,
                    EGL10.EGL_RENDERABLE_TYPE, 4, // EGL_OPENGL_ES2_BIT
                    EGL10.EGL_NONE
                )

                val configs = arrayOfNulls<EGLConfig>(1)
                val numConfigs = IntArray(1)
                if (!egl.eglChooseConfig(eglDisplay, configSpec, configs, 1, numConfigs)) {
                    throw RuntimeException("eglChooseConfig failed")
                }
                val eglConfig = configs[0]
                    ?: throw RuntimeException("eglChooseConfig returned null config")

                // Create EGL context
                val contextAttribs = intArrayOf(
                    0x3098, 2, // EGL_CONTEXT_CLIENT_VERSION = 2
                    EGL10.EGL_NONE
                )
                eglContext = egl.eglCreateContext(
                    eglDisplay,
                    eglConfig,
                    EGL10.EGL_NO_CONTEXT,
                    contextAttribs
                )
                if (eglContext == null || eglContext == EGL10.EGL_NO_CONTEXT) {
                    throw RuntimeException("eglCreateContext failed")
                }

                // Create EGL surface using wallpaper's surface holder
                eglSurface = egl.eglCreateWindowSurface(eglDisplay, eglConfig, holder, null)
                if (eglSurface == null || eglSurface == EGL10.EGL_NO_SURFACE) {
                    throw RuntimeException("eglCreateWindowSurface failed")
                }

                // Make context current
                if (!egl.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                    throw RuntimeException("eglMakeCurrent failed")
                }

                gl = eglContext.gl as GL10

                // Initialize renderer
                renderer.onSurfaceCreated(gl, eglConfig)

                // Get surface size
                val width = holder.surfaceFrame.width()
                val height = holder.surfaceFrame.height()
                renderer.onSurfaceChanged(gl, width, height)

                // Rendering loop
                while (running && !isInterrupted) {
                    // Draw frame
                    renderer.onDrawFrame(gl)

                    // Swap buffers
                    if (!egl.eglSwapBuffers(eglDisplay, eglSurface)) {
                        val error = egl.eglGetError()
                        if (error == 0x300E) { // EGL_CONTEXT_LOST
                            // Context lost, need to recreate
                            break
                        } else if (error != EGL10.EGL_SUCCESS) {
                            // Other error
                            break
                        }
                    }

                    // Cap at ~60fps
                    try {
                        sleep(16)
                    } catch (e: InterruptedException) {
                        break
                    }
                }
            } finally {
                // Clean up EGL
                if (eglDisplay != null) {
                    egl.eglMakeCurrent(
                        eglDisplay,
                        EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_SURFACE,
                        EGL10.EGL_NO_CONTEXT
                    )
                    if (eglSurface != null) {
                        egl.eglDestroySurface(eglDisplay, eglSurface)
                    }
                    if (eglContext != null) {
                        egl.eglDestroyContext(eglDisplay, eglContext)
                    }
                    egl.eglTerminate(eglDisplay)
                }
            }
        }
    }
}
