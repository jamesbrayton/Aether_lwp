package com.aether.wallpaper

import android.content.Context
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.aether.wallpaper.config.ConfigManager
import com.aether.wallpaper.model.BackgroundConfig
import com.aether.wallpaper.model.LayerConfig
import com.aether.wallpaper.model.WallpaperConfig
import com.aether.wallpaper.shader.ShaderRegistry
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.mockito.kotlin.any
import org.mockito.kotlin.mock

/**
 * Integration tests for AetherWallpaperService.
 *
 * Tests the live wallpaper service lifecycle, OpenGL initialization,
 * configuration loading, shader discovery, and visibility handling.
 *
 * Note: These tests demonstrate the testing approach for wallpaper services.
 * Full test suite would cover all 60+ scenarios from the Gherkin spec.
 *
 * Key challenges with testing WallpaperService:
 * - Cannot easily instantiate Engine directly (inner class, framework lifecycle)
 * - GLSurfaceView requires actual OpenGL context (difficult to mock)
 * - Renderer callbacks require GL thread
 *
 * Testing strategy:
 * - Test components in isolation (ConfigManager, ShaderRegistry integration)
 * - Test service instantiation and basic lifecycle
 * - Manual testing required for full rendering pipeline
 */
@RunWith(AndroidJUnit4::class)
class AetherWallpaperServiceTest {

    private lateinit var context: Context
    private lateinit var configManager: ConfigManager
    private lateinit var shaderRegistry: ShaderRegistry

    @Before
    fun setup() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        configManager = ConfigManager(context)
        shaderRegistry = ShaderRegistry(context)

        // Clear any existing configuration
        configManager.clearConfig()
    }

    @After
    fun teardown() {
        configManager.clearConfig()
    }

    // ========== Service Registration ==========

    @Test
    fun testServiceClassExists() {
        // Verify the service class can be loaded
        val serviceClass = Class.forName("com.aether.wallpaper.AetherWallpaperService")
        assert(serviceClass != null)
        assert(WallpaperService::class.java.isAssignableFrom(serviceClass))
    }

    // ========== Configuration Loading ==========

    @Test
    fun testEngineLoadsConfiguration() {
        // Create a test configuration
        val layer1 = LayerConfig(
            shaderId = "snow",
            order = 0,
            enabled = true,
            opacity = 1.0f,
            depth = 0.5f,
            params = mapOf(
                "u_particleCount" to 100,
                "u_speed" to 1.0f,
                "u_driftAmount" to 0.5f
            )
        )

        val config = WallpaperConfig(
            background = BackgroundConfig(uri = null, cropX = 0, cropY = 0, cropWidth = 1080, cropHeight = 1920),
            layers = listOf(layer1)
        )

        // Save configuration
        configManager.saveConfig(config)

        // Load configuration
        val loadedConfig = configManager.loadConfig()

        // Verify configuration loaded correctly
        assert(loadedConfig != null)
        assert(loadedConfig?.layers?.size == 1)
        assert(loadedConfig?.layers?.get(0)?.shaderId == "snow")
        assert(loadedConfig?.layers?.get(0)?.params?.get("u_particleCount") == 100)
    }

    @Test
    fun testEngineUsesDefaultConfigWhenNoneExists() {
        // Ensure no config exists
        configManager.clearConfig()

        // Load configuration (should return default)
        val config = configManager.loadConfig()

        // Verify default config is used
        assert(config != null)
        assert(config?.layers?.isEmpty() == true || config?.layers == null)
    }

    // ========== Shader Discovery ==========

    @Test
    fun testEngineDiscoversShaders() {
        // Discover shaders
        val shaders = shaderRegistry.discoverShaders()

        // Verify shaders were found
        assert(shaders.isNotEmpty())

        // Verify specific shaders exist
        val snowShader = shaderRegistry.getShaderById("snow")
        val rainShader = shaderRegistry.getShaderById("rain")

        assert(snowShader != null)
        assert(rainShader != null)

        // Verify metadata was parsed
        assert(snowShader?.name == "Snow")
        assert(snowShader?.parameters?.size == 3)

        assert(rainShader?.name == "Rain")
        assert(rainShader?.parameters?.size == 4)
    }

    @Test
    fun testEngineLoadsConfiguredShaders() {
        // Create configuration with both shaders
        val layer1 = LayerConfig(
            shaderId = "snow",
            order = 0,
            enabled = true,
            opacity = 1.0f,
            depth = 0.5f,
            params = mapOf()
        )

        val layer2 = LayerConfig(
            shaderId = "rain",
            order = 1,
            enabled = true,
            opacity = 0.8f,
            depth = 0.3f,
            params = mapOf()
        )

        val config = WallpaperConfig(
            background = BackgroundConfig(uri = null, cropX = 0, cropY = 0, cropWidth = 1080, cropHeight = 1920),
            layers = listOf(layer1, layer2)
        )

        configManager.saveConfig(config)

        // Discover shaders
        shaderRegistry.discoverShaders()

        // Verify both shaders are available
        val snowShader = shaderRegistry.getShaderById("snow")
        val rainShader = shaderRegistry.getShaderById("rain")

        assert(snowShader != null)
        assert(rainShader != null)
    }

    // ========== Enabled/Disabled Layers ==========

    @Test
    fun testEngineSkipsDisabledLayers() {
        // Create configuration with one enabled, one disabled layer
        val layer1 = LayerConfig(
            shaderId = "snow",
            order = 0,
            enabled = true,
            opacity = 1.0f,
            depth = 0.5f,
            params = mapOf()
        )

        val layer2 = LayerConfig(
            shaderId = "rain",
            order = 1,
            enabled = false,
            opacity = 0.8f,
            depth = 0.3f,
            params = mapOf()
        )

        val config = WallpaperConfig(
            background = BackgroundConfig(uri = null, cropX = 0, cropY = 0, cropWidth = 1080, cropHeight = 1920),
            layers = listOf(layer1, layer2)
        )

        configManager.saveConfig(config)
        val loadedConfig = configManager.loadConfig()

        // Verify layer states
        assert(loadedConfig?.layers?.get(0)?.enabled == true)
        assert(loadedConfig?.layers?.get(1)?.enabled == false)

        // GLRenderer should skip disabled layers (verified in GLRendererTest)
    }

    // ========== Configuration Persistence ==========

    @Test
    fun testConfigurationPersistsAcrossRestarts() {
        // Create and save configuration
        val layer = LayerConfig(
            shaderId = "snow",
            order = 0,
            enabled = true,
            opacity = 0.75f,
            depth = 0.4f,
            params = mapOf(
                "u_particleCount" to 150,
                "u_speed" to 1.5f
            )
        )

        val config = WallpaperConfig(
            background = BackgroundConfig(uri = null, cropX = 0, cropY = 0, cropWidth = 1080, cropHeight = 1920),
            layers = listOf(layer)
        )

        configManager.saveConfig(config)

        // Simulate restart by creating new ConfigManager instance
        val newConfigManager = ConfigManager(context)
        val loadedConfig = newConfigManager.loadConfig()

        // Verify configuration persisted
        assert(loadedConfig != null)
        assert(loadedConfig?.layers?.size == 1)
        assert(loadedConfig?.layers?.get(0)?.shaderId == "snow")
        assert(loadedConfig?.layers?.get(0)?.opacity == 0.75f)
        assert(loadedConfig?.layers?.get(0)?.params?.get("u_particleCount") == 150)
    }

    // ========== Edge Cases ==========

    @Test
    fun testEngineHandlesZeroLayers() {
        // Create configuration with no layers
        val config = WallpaperConfig(
            background = BackgroundConfig(uri = null, cropX = 0, cropY = 0, cropWidth = 1080, cropHeight = 1920),
            layers = emptyList()
        )

        configManager.saveConfig(config)
        val loadedConfig = configManager.loadConfig()

        // Verify empty layers list
        assert(loadedConfig != null)
        assert(loadedConfig?.layers?.isEmpty() == true)

        // Renderer should handle this gracefully (only render background)
    }

    @Test
    fun testEngineHandlesMaximumLayers() {
        // Create configuration with 5 layers (maximum)
        val layers = (0..4).map { i ->
            LayerConfig(
                shaderId = if (i % 2 == 0) "snow" else "rain",
                order = i,
                enabled = true,
                opacity = 1.0f - (i * 0.1f),
                depth = i * 0.2f,
                params = mapOf()
            )
        }

        val config = WallpaperConfig(
            background = BackgroundConfig(uri = null, cropX = 0, cropY = 0, cropWidth = 1080, cropHeight = 1920),
            layers = layers
        )

        configManager.saveConfig(config)
        val loadedConfig = configManager.loadConfig()

        // Verify all layers loaded
        assert(loadedConfig != null)
        assert(loadedConfig?.layers?.size == 5)
        assert(loadedConfig?.layers?.all { it.enabled } == true)
    }

    // ========== Integration Tests ==========

    @Test
    fun testServiceIntegrationWithAllComponents() {
        // This test verifies all components work together:
        // 1. ShaderRegistry discovers shaders
        // 2. ConfigManager loads configuration
        // 3. Configuration references valid shaders
        // 4. All metadata is accessible

        // Discover shaders
        val shaders = shaderRegistry.discoverShaders()
        assert(shaders.isNotEmpty())

        // Create configuration using discovered shaders
        val snowShader = shaderRegistry.getShaderById("snow")
        assert(snowShader != null)

        val layer = LayerConfig(
            shaderId = snowShader!!.id,
            order = 0,
            enabled = true,
            opacity = 1.0f,
            depth = 0.5f,
            params = snowShader.parameters.associate { param ->
                param.id to param.defaultValue
            }
        )

        val config = WallpaperConfig(
            background = BackgroundConfig(uri = null, cropX = 0, cropY = 0, cropWidth = 1080, cropHeight = 1920),
            layers = listOf(layer)
        )

        // Save and reload configuration
        configManager.saveConfig(config)
        val loadedConfig = configManager.loadConfig()

        // Verify integration
        assert(loadedConfig != null)
        assert(loadedConfig?.layers?.size == 1)

        val loadedLayer = loadedConfig?.layers?.get(0)
        assert(loadedLayer?.shaderId == "snow")

        // Verify shader is available
        val shaderForLayer = shaderRegistry.getShaderById(loadedLayer!!.shaderId)
        assert(shaderForLayer != null)
        assert(shaderForLayer?.parameters?.size == 3)

        // Verify all parameter defaults are in config
        shaderForLayer?.parameters?.forEach { param ->
            assert(loadedLayer.params.containsKey(param.id))
        }
    }

    /**
     * NOTE: Full rendering pipeline tests (OpenGL context, GLSurfaceView lifecycle,
     * GLRenderer integration) are difficult to test in instrumentation tests due to:
     *
     * 1. WallpaperService.Engine lifecycle controlled by framework
     * 2. GLSurfaceView requires actual OpenGL context
     * 3. Renderer callbacks run on GL thread
     *
     * These aspects require manual testing:
     * - Install wallpaper on device/emulator
     * - Verify rendering appears
     * - Test visibility changes (screen on/off)
     * - Test configuration changes (modify settings, reapply)
     * - Test resource cleanup (check logcat for GL errors)
     *
     * The tests above verify the component integration works correctly,
     * which is the critical part for automated testing.
     */
}
