package com.aether.wallpaper.config

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aether.wallpaper.model.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for ConfigManager using Robolectric.
 */
@RunWith(RobolectricTestRunner::class)
class ConfigManagerTest {

    private lateinit var context: Context
    private lateinit var configManager: ConfigManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        configManager = ConfigManager(context)
        configManager.clearConfig() // Start with clean state
    }

    @After
    fun tearDown() {
        configManager.clearConfig() // Clean up after tests
    }

    @Test
    fun testGetDefaultConfig() {
        val defaultConfig = configManager.getDefaultConfig()

        assertNotNull("Default config should not be null", defaultConfig)
        assertNull("Default config should have no background", defaultConfig.background)
        assertTrue("Default config should have no layers", defaultConfig.layers.isEmpty())
        assertNotNull("Default config should have global settings", defaultConfig.globalSettings)
        assertEquals("Default FPS should be 60", 60, defaultConfig.globalSettings.targetFps)
        assertFalse("Default gyroscope should be disabled", defaultConfig.globalSettings.gyroscopeEnabled)
    }

    @Test
    fun testSaveAndLoadConfig() {
        val config = WallpaperConfig(
            background = BackgroundConfig(
                uri = "content://media/external/images/media/123",
                crop = CropRect(100, 200, 1080, 1920)
            ),
            layers = listOf(
                LayerConfig(
                    shaderId = "snow",
                    order = 1,
                    enabled = true,
                    opacity = 0.8f,
                    depth = 0.3f,
                    params = mapOf("u_speed" to 1.5, "u_particleCount" to 100.0)
                )
            )
        )

        // Save
        val saveResult = configManager.saveConfig(config)
        assertTrue("Config should save successfully", saveResult)

        // Load
        val loadedConfig = configManager.loadConfig()

        assertNotNull("Loaded config should not be null", loadedConfig)
        assertEquals("Background URI should match", config.background?.uri, loadedConfig.background?.uri)
        assertEquals("Crop X should match", config.background?.crop?.x, loadedConfig.background?.crop?.x)
        assertEquals("Crop Y should match", config.background?.crop?.y, loadedConfig.background?.crop?.y)
        assertEquals("Layer count should match", config.layers.size, loadedConfig.layers.size)
        assertEquals("Layer shader should match", config.layers[0].shaderId, loadedConfig.layers[0].shaderId)
        assertEquals("Layer opacity should match", config.layers[0].opacity, loadedConfig.layers[0].opacity, 0.001f)
    }

    @Test
    fun testLoadConfigWithNoSavedData() {
        // Don't save anything, just load
        val config = configManager.loadConfig()

        assertNotNull("Config should not be null", config)
        assertNull("Config should have no background", config.background)
        assertTrue("Config should have no layers", config.layers.isEmpty())
    }

    @Test
    fun testSaveConfigWithMultipleLayers() {
        val config = WallpaperConfig(
            layers = listOf(
                LayerConfig(shaderId = "snow", order = 1, enabled = true, opacity = 0.8f, depth = 0.3f),
                LayerConfig(shaderId = "rain", order = 2, enabled = true, opacity = 0.9f, depth = 0.5f),
                LayerConfig(shaderId = "smoke", order = 3, enabled = false, opacity = 0.7f, depth = 0.7f)
            )
        )

        configManager.saveConfig(config)
        val loadedConfig = configManager.loadConfig()

        assertEquals("Should load 3 layers", 3, loadedConfig.layers.size)
        assertEquals("First layer shader should be snow", "snow", loadedConfig.layers[0].shaderId)
        assertEquals("Second layer shader should be rain", "rain", loadedConfig.layers[1].shaderId)
        assertEquals("Third layer shader should be smoke", "smoke", loadedConfig.layers[2].shaderId)
        assertEquals("First layer order should be 1", 1, loadedConfig.layers[0].order)
        assertEquals("Second layer order should be 2", 2, loadedConfig.layers[1].order)
        assertEquals("Third layer order should be 3", 3, loadedConfig.layers[2].order)
        assertTrue("First layer should be enabled", loadedConfig.layers[0].enabled)
        assertTrue("Second layer should be enabled", loadedConfig.layers[1].enabled)
        assertFalse("Third layer should be disabled", loadedConfig.layers[2].enabled)
    }

    @Test
    fun testSaveConfigWithDynamicParameters() {
        val params = mapOf(
            "u_particleCount" to 150.0,
            "u_speed" to 2.0,
            "u_driftAmount" to 0.75
        )

        val config = WallpaperConfig(
            layers = listOf(
                LayerConfig(
                    shaderId = "snow",
                    order = 1,
                    params = params
                )
            )
        )

        configManager.saveConfig(config)
        val loadedConfig = configManager.loadConfig()

        val loadedParams = loadedConfig.layers[0].params
        assertEquals("Param count should match", 3, loadedParams.size)
        assertTrue("Should have u_particleCount", loadedParams.containsKey("u_particleCount"))
        assertTrue("Should have u_speed", loadedParams.containsKey("u_speed"))
        assertTrue("Should have u_driftAmount", loadedParams.containsKey("u_driftAmount"))

        // Note: Gson deserializes numbers as Double
        assertEquals("u_particleCount should match", 150.0, loadedParams["u_particleCount"])
        assertEquals("u_speed should match", 2.0, loadedParams["u_speed"])
        assertEquals("u_driftAmount should match", 0.75, loadedParams["u_driftAmount"])
    }

    @Test
    fun testUpdateExistingConfiguration() {
        val config1 = WallpaperConfig(
            layers = listOf(LayerConfig(shaderId = "snow", order = 1))
        )

        configManager.saveConfig(config1)
        val loaded1 = configManager.loadConfig()
        assertEquals("First config should have snow layer", "snow", loaded1.layers[0].shaderId)

        // Update with new config
        val config2 = WallpaperConfig(
            layers = listOf(LayerConfig(shaderId = "rain", order = 1))
        )

        configManager.saveConfig(config2)
        val loaded2 = configManager.loadConfig()
        assertEquals("Updated config should have rain layer", "rain", loaded2.layers[0].shaderId)
    }

    @Test
    fun testSaveConfigWithNoBackground() {
        val config = WallpaperConfig(
            background = null,
            layers = listOf(LayerConfig(shaderId = "rain", order = 1))
        )

        val saveResult = configManager.saveConfig(config)
        assertTrue("Should save successfully", saveResult)

        val loadedConfig = configManager.loadConfig()
        assertNull("Background should be null", loadedConfig.background)
        assertEquals("Should have 1 layer", 1, loadedConfig.layers.size)
    }

    @Test
    fun testSaveConfigWithNoLayers() {
        val config = WallpaperConfig(
            background = BackgroundConfig(uri = "content://test", crop = null),
            layers = emptyList()
        )

        val saveResult = configManager.saveConfig(config)
        assertTrue("Should save successfully", saveResult)

        val loadedConfig = configManager.loadConfig()
        assertNotNull("Background should not be null", loadedConfig.background)
        assertEquals("URI should match", "content://test", loadedConfig.background?.uri)
        assertTrue("Layers should be empty", loadedConfig.layers.isEmpty())
    }

    @Test
    fun testSaveConfigWithGlobalSettings() {
        val config = WallpaperConfig(
            globalSettings = GlobalSettings(
                targetFps = 30,
                gyroscopeEnabled = true
            )
        )

        configManager.saveConfig(config)
        val loadedConfig = configManager.loadConfig()

        assertEquals("Target FPS should be 30", 30, loadedConfig.globalSettings.targetFps)
        assertTrue("Gyroscope should be enabled", loadedConfig.globalSettings.gyroscopeEnabled)
    }

    @Test
    fun testHasConfig() {
        assertFalse("Should not have config initially", configManager.hasConfig())

        configManager.saveConfig(WallpaperConfig())
        assertTrue("Should have config after save", configManager.hasConfig())

        configManager.clearConfig()
        assertFalse("Should not have config after clear", configManager.hasConfig())
    }

    @Test
    fun testClearConfig() {
        val config = WallpaperConfig(
            layers = listOf(LayerConfig(shaderId = "snow", order = 1))
        )

        configManager.saveConfig(config)
        assertTrue("Should have config", configManager.hasConfig())

        configManager.clearConfig()
        assertFalse("Should not have config after clear", configManager.hasConfig())

        val loadedConfig = configManager.loadConfig()
        assertTrue("Should return default config", loadedConfig.layers.isEmpty())
    }

    @Test
    fun testConfigValidation() {
        val validConfig = WallpaperConfig(
            layers = listOf(
                LayerConfig(shaderId = "snow", order = 1, opacity = 0.8f, depth = 0.5f)
            )
        )

        assertTrue("Valid config should validate", validConfig.validate())
    }

    @Test
    fun testInvalidConfigNotSaved() {
        val invalidConfig = WallpaperConfig(
            layers = listOf(
                LayerConfig(shaderId = "snow", order = 1, opacity = 1.5f) // Invalid opacity > 1.0
            )
        )

        val saveResult = configManager.saveConfig(invalidConfig)
        assertFalse("Invalid config should not save", saveResult)
    }

    @Test
    fun testLayerValidation() {
        // Valid layer
        val validLayer = LayerConfig(
            shaderId = "snow",
            order = 1,
            opacity = 0.8f,
            depth = 0.5f
        )
        assertTrue("Valid layer should validate", validLayer.validate())

        // Invalid opacity (negative)
        val invalidOpacityNeg = validLayer.copy(opacity = -0.1f)
        assertFalse("Negative opacity should not validate", invalidOpacityNeg.validate())

        // Invalid opacity (> 1.0)
        val invalidOpacityHigh = validLayer.copy(opacity = 1.5f)
        assertFalse("Opacity > 1.0 should not validate", invalidOpacityHigh.validate())

        // Invalid depth (negative)
        val invalidDepthNeg = validLayer.copy(depth = -0.1f)
        assertFalse("Negative depth should not validate", invalidDepthNeg.validate())

        // Invalid depth (> 1.0)
        val invalidDepthHigh = validLayer.copy(depth = 1.5f)
        assertFalse("Depth > 1.0 should not validate", invalidDepthHigh.validate())

        // Invalid negative order
        val invalidOrder = validLayer.copy(order = -1)
        assertFalse("Negative order should not validate", invalidOrder.validate())

        // Invalid empty shader ID
        val invalidShaderId = validLayer.copy(shaderId = "")
        assertFalse("Empty shader ID should not validate", invalidShaderId.validate())
    }

    @Test
    fun testCropRectValidation() {
        val validCrop = CropRect(x = 100, y = 200, width = 1080, height = 1920)
        assertTrue("Valid crop should validate", validCrop.validate())

        val invalidX = validCrop.copy(x = -1)
        assertFalse("Negative X should not validate", invalidX.validate())

        val invalidY = validCrop.copy(y = -1)
        assertFalse("Negative Y should not validate", invalidY.validate())

        val invalidWidth = validCrop.copy(width = 0)
        assertFalse("Zero width should not validate", invalidWidth.validate())

        val invalidHeight = validCrop.copy(height = -1)
        assertFalse("Negative height should not validate", invalidHeight.validate())
    }

    @Test
    fun testGlobalSettingsValidation() {
        val validSettings = GlobalSettings(targetFps = 60, gyroscopeEnabled = false)
        assertTrue("Valid settings should validate", validSettings.validate())

        val invalidFpsZero = validSettings.copy(targetFps = 0)
        assertFalse("Zero FPS should not validate", invalidFpsZero.validate())

        val invalidFpsNeg = validSettings.copy(targetFps = -1)
        assertFalse("Negative FPS should not validate", invalidFpsNeg.validate())

        val invalidFpsHigh = validSettings.copy(targetFps = 200)
        assertFalse("FPS > 120 should not validate", invalidFpsHigh.validate())

        val validFps30 = validSettings.copy(targetFps = 30)
        assertTrue("30 FPS should validate", validFps30.validate())

        val validFps120 = validSettings.copy(targetFps = 120)
        assertTrue("120 FPS should validate", validFps120.validate())
    }

    @Test
    fun testConfigImmutability() {
        val original = WallpaperConfig(
            layers = listOf(LayerConfig(shaderId = "snow", order = 1))
        )

        val modified = original.copy(
            layers = listOf(LayerConfig(shaderId = "rain", order = 1))
        )

        assertEquals("Original should still have snow", "snow", original.layers[0].shaderId)
        assertEquals("Modified should have rain", "rain", modified.layers[0].shaderId)
    }

    @Test
    fun testConfigEquality() {
        val config1 = WallpaperConfig(
            layers = listOf(LayerConfig(shaderId = "snow", order = 1))
        )

        val config2 = WallpaperConfig(
            layers = listOf(LayerConfig(shaderId = "snow", order = 1))
        )

        val config3 = WallpaperConfig(
            layers = listOf(LayerConfig(shaderId = "rain", order = 1))
        )

        assertEquals("Identical configs should be equal", config1, config2)
        assertNotEquals("Different configs should not be equal", config1, config3)
        assertEquals("Identical configs should have same hashCode", config1.hashCode(), config2.hashCode())
    }
}
