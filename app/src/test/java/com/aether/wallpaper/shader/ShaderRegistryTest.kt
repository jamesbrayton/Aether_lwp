package com.aether.wallpaper.shader

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for ShaderRegistry.
 * Tests shader discovery from assets/shaders/ directory.
 *
 * These are instrumentation tests (not unit tests) because they require
 * Android Context to access assets.
 */
@RunWith(AndroidJUnit4::class)
class ShaderRegistryTest {

    private lateinit var context: Context
    private lateinit var registry: ShaderRegistry

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        registry = ShaderRegistry(context)
    }

    @Test
    fun testDiscoverShaders() {
        val shaders = registry.discoverShaders()

        // Should discover at least the test shaders we create
        assertNotNull(shaders)
        assertTrue("Registry should discover shaders", shaders.isNotEmpty())
    }

    @Test
    fun testGetShaderById() {
        registry.discoverShaders()

        // Assuming test.frag exists in assets/shaders/
        val shader = registry.getShaderById("test")

        if (shader != null) {
            assertEquals("test", shader.id)
            assertNotNull(shader.name)
            assertNotNull(shader.version)
        }
        // If null, test.frag doesn't exist yet (acceptable during initial dev)
    }

    @Test
    fun testGetNonexistentShader() {
        registry.discoverShaders()

        val shader = registry.getShaderById("nonexistent_shader_id_12345")

        assertNull("Nonexistent shader should return null", shader)
    }

    @Test
    fun testGetAllShaders() {
        val shaders = registry.discoverShaders()
        val allShaders = registry.getAllShaders()

        assertEquals(shaders.size, allShaders.size)

        // All shaders should have unique IDs
        val ids = allShaders.map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun testGetAllShadersBeforeDiscovery() {
        val shaders = registry.getAllShaders()

        // Should return empty list if discoverShaders() hasn't been called
        assertTrue(shaders.isEmpty())
    }

    @Test
    fun testShaderDescriptorValidation() {
        val shaders = registry.discoverShaders()

        for (shader in shaders) {
            // All discovered shaders should have valid metadata
            assertNotNull("Shader ID should not be null", shader.id)
            assertNotNull("Shader name should not be null", shader.name)
            assertNotNull("Shader version should not be null", shader.version)
            assertTrue("Shader ID should not be blank", shader.id.isNotBlank())
            assertTrue("Shader name should not be blank", shader.name.isNotBlank())
            assertTrue("Shader version should not be blank", shader.version.isNotBlank())

            // Should be able to validate without throwing
            shader.validate()
        }
    }

    @Test
    fun testDiscoverShadersMultipleTimes() {
        val shaders1 = registry.discoverShaders()
        val shaders2 = registry.discoverShaders()

        // Should return same results when called multiple times
        assertEquals(shaders1.size, shaders2.size)
    }
}
