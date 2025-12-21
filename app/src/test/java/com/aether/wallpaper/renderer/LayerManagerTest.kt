package com.aether.wallpaper.renderer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aether.wallpaper.model.LayerConfig
import com.aether.wallpaper.shader.ShaderLoader
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for LayerManager.
 *
 * Tests shader program caching, layer sorting, and filtering logic.
 * Uses Robolectric for Android framework support and Mockito for mocking ShaderLoader.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class LayerManagerTest {

    private lateinit var context: Context
    private lateinit var shaderLoader: ShaderLoader
    private lateinit var layerManager: LayerManager

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        shaderLoader = mock()
    }

    @Test
    fun testCacheCompiledShaderPrograms() {
        // Given: Empty cache
        val layers = listOf(
            LayerConfig(shaderId = "snow", order = 1, enabled = true)
        )
        layerManager = LayerManager(context, shaderLoader, layers)

        val vertexShaderId = 123
        val snowProgramHandle = 456

        // Mock shader compilation
        whenever(shaderLoader.loadShaderFromAssets("snow.frag")).thenReturn("snow shader source")
        whenever(shaderLoader.compileShader(any(), any())).thenReturn(789) // Fragment shader ID
        whenever(shaderLoader.linkProgram(vertexShaderId, 789)).thenReturn(snowProgramHandle)

        // When: First call to getOrCreateProgram
        val program1 = layerManager.getOrCreateProgram("snow", vertexShaderId)

        // Then: Shader is compiled and program is returned
        assertEquals(snowProgramHandle, program1)
        verify(shaderLoader, times(1)).loadShaderFromAssets("snow.frag")
        verify(shaderLoader, times(1)).compileShader(any(), any())
        verify(shaderLoader, times(1)).linkProgram(any(), any())

        // When: Second call with same shader ID
        val program2 = layerManager.getOrCreateProgram("snow", vertexShaderId)

        // Then: Cached program is returned, no recompilation
        assertEquals(snowProgramHandle, program2)
        verify(shaderLoader, times(1)).loadShaderFromAssets("snow.frag") // Still only 1 call
    }

    @Test
    fun testReturnEnabledLayersSortedByOrder() {
        // Given: Layers with various orders and enabled states
        val layers = listOf(
            LayerConfig(shaderId = "bubbles", order = 3, enabled = true),
            LayerConfig(shaderId = "snow", order = 1, enabled = true),
            LayerConfig(shaderId = "rain", order = 2, enabled = false),
            LayerConfig(shaderId = "dust", order = 4, enabled = true)
        )
        layerManager = LayerManager(context, shaderLoader, layers)

        // When: Get enabled layers
        val enabledLayers = layerManager.getEnabledLayers()

        // Then: Returns enabled layers sorted by order
        assertEquals(3, enabledLayers.size)
        assertEquals("snow", enabledLayers[0].shaderId)
        assertEquals(1, enabledLayers[0].order)
        assertEquals("bubbles", enabledLayers[1].shaderId)
        assertEquals(3, enabledLayers[1].order)
        assertEquals("dust", enabledLayers[2].shaderId)
        assertEquals(4, enabledLayers[2].order)
    }

    @Test
    fun testFilterOutDisabledLayers() {
        // Given: Mixed enabled/disabled layers
        val layers = listOf(
            LayerConfig(shaderId = "snow", order = 1, enabled = true),
            LayerConfig(shaderId = "rain", order = 2, enabled = false),
            LayerConfig(shaderId = "dust", order = 3, enabled = true)
        )
        layerManager = LayerManager(context, shaderLoader, layers)

        // When: Get enabled layers
        val enabledLayers = layerManager.getEnabledLayers()

        // Then: Only enabled layers are returned
        assertEquals(2, enabledLayers.size)
        assertTrue(enabledLayers.any { it.shaderId == "snow" })
        assertTrue(enabledLayers.any { it.shaderId == "dust" })
        assertFalse(enabledLayers.any { it.shaderId == "rain" })
    }

    @Test
    fun testUpdateLayersDynamically() {
        // Given: Initial layers
        val initialLayers = listOf(
            LayerConfig(shaderId = "snow", order = 1, enabled = true),
            LayerConfig(shaderId = "rain", order = 2, enabled = true)
        )
        layerManager = LayerManager(context, shaderLoader, initialLayers)

        // When: Update to new layers
        val newLayers = listOf(
            LayerConfig(shaderId = "bubbles", order = 1, enabled = true),
            LayerConfig(shaderId = "dust", order = 2, enabled = true),
            LayerConfig(shaderId = "test", order = 3, enabled = false)
        )
        layerManager.updateLayers(newLayers)

        // Then: Enabled layers reflect the new configuration
        val enabledLayers = layerManager.getEnabledLayers()
        assertEquals(2, enabledLayers.size)
        assertEquals("bubbles", enabledLayers[0].shaderId)
        assertEquals("dust", enabledLayers[1].shaderId)
    }

    @Test
    fun testHandleShaderCompilationFailure() {
        // Given: Layer manager
        val layers = listOf(
            LayerConfig(shaderId = "valid", order = 1, enabled = true)
        )
        layerManager = LayerManager(context, shaderLoader, layers)

        val vertexShaderId = 123

        // Mock shader compilation failure
        whenever(shaderLoader.loadShaderFromAssets("nonexistent.frag")).thenThrow(RuntimeException("Shader not found"))

        // When: Try to compile invalid shader
        val program = layerManager.getOrCreateProgram("nonexistent", vertexShaderId)

        // Then: Returns 0 and doesn't crash
        assertEquals(0, program)
    }

    @Test
    fun testReleaseAllCachedPrograms() {
        // Given: Layer manager with cached programs
        val layers = listOf(
            LayerConfig(shaderId = "snow", order = 1, enabled = true),
            LayerConfig(shaderId = "rain", order = 2, enabled = true)
        )
        layerManager = LayerManager(context, shaderLoader, layers)

        val vertexShaderId = 123

        // Mock successful compilation
        whenever(shaderLoader.loadShaderFromAssets(any())).thenReturn("shader source")
        whenever(shaderLoader.compileShader(any(), any())).thenReturn(789)
        whenever(shaderLoader.linkProgram(any(), any())).thenReturn(456, 457, 458)

        // Cache some programs
        layerManager.getOrCreateProgram("snow", vertexShaderId)
        layerManager.getOrCreateProgram("rain", vertexShaderId)

        // When: Release is called
        layerManager.release()

        // Then: Cache is cleared (verified by attempting to get program again)
        // Note: In real implementation, we'd verify GL calls, but this tests cache clearing
        layerManager.getOrCreateProgram("snow", vertexShaderId)
        // Should recompile since cache was cleared
        verify(shaderLoader, atLeast(2)).loadShaderFromAssets("snow.frag")
    }

    @Test
    fun testEmptyLayersList() {
        // Given: No layers
        val layers = emptyList<LayerConfig>()
        layerManager = LayerManager(context, shaderLoader, layers)

        // When: Get enabled layers
        val enabledLayers = layerManager.getEnabledLayers()

        // Then: Returns empty list
        assertTrue(enabledLayers.isEmpty())
    }

    @Test
    fun testAllLayersDisabled() {
        // Given: All layers disabled
        val layers = listOf(
            LayerConfig(shaderId = "snow", order = 1, enabled = false),
            LayerConfig(shaderId = "rain", order = 2, enabled = false)
        )
        layerManager = LayerManager(context, shaderLoader, layers)

        // When: Get enabled layers
        val enabledLayers = layerManager.getEnabledLayers()

        // Then: Returns empty list
        assertTrue(enabledLayers.isEmpty())
    }
}
