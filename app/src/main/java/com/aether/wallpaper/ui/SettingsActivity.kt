package com.aether.wallpaper.ui

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aether.wallpaper.R
import com.aether.wallpaper.config.ConfigManager
import com.aether.wallpaper.model.BackgroundConfig
import com.aether.wallpaper.model.CropRect
import com.aether.wallpaper.model.LayerConfig
import com.aether.wallpaper.model.ShaderDescriptor
import com.aether.wallpaper.model.WallpaperConfig
import com.aether.wallpaper.shader.ShaderRegistry
import com.google.android.material.appbar.MaterialToolbar

/**
 * Main settings activity for configuring the Aether live wallpaper.
 *
 * Features:
 * - Dynamic effect selector populated from ShaderRegistry
 * - Active layers list with parameter controls
 * - Background image selection and cropping
 * - Configuration persistence via ConfigManager
 * - Apply wallpaper to home screen
 *
 * UI is dynamically generated from shader metadata:
 * - Effect cards show @shader name, @description, @tags
 * - Parameter controls are generated from @param definitions
 * - No hardcoded shader names or parameters
 */
class SettingsActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var backgroundPreview: ImageView
    private lateinit var selectBackgroundButton: Button
    private lateinit var effectSelectorRecyclerView: RecyclerView
    private lateinit var layersRecyclerView: RecyclerView
    private lateinit var emptyLayersMessage: TextView
    private lateinit var applyWallpaperButton: Button

    private lateinit var shaderRegistry: ShaderRegistry
    private lateinit var configManager: ConfigManager
    private lateinit var effectAdapter: EffectSelectorAdapter
    private lateinit var layerAdapter: LayerAdapter

    private var currentConfig: WallpaperConfig? = null
    private val maxLayers = 5
    private var tempImageUri: Uri? = null // Temporary storage for image URI during crop flow

    companion object {
        private const val REQUEST_SELECT_IMAGE = 1001
        private const val REQUEST_CROP_IMAGE = 1002
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        initializeViews()
        initializeComponents()
        setupRecyclerViews()
        loadConfiguration()
        setupListeners()
    }

    private fun initializeViews() {
        toolbar = findViewById(R.id.toolbar)
        backgroundPreview = findViewById(R.id.backgroundPreview)
        selectBackgroundButton = findViewById(R.id.selectBackgroundButton)
        effectSelectorRecyclerView = findViewById(R.id.effectSelectorRecyclerView)
        layersRecyclerView = findViewById(R.id.layersRecyclerView)
        emptyLayersMessage = findViewById(R.id.emptyLayersMessage)
        applyWallpaperButton = findViewById(R.id.applyWallpaperButton)

        setSupportActionBar(toolbar)
    }

    private fun initializeComponents() {
        shaderRegistry = ShaderRegistry(this)
        configManager = ConfigManager(this)
    }

    private fun setupRecyclerViews() {
        // Effect selector (available shaders)
        effectSelectorRecyclerView.layoutManager = LinearLayoutManager(this)
        effectAdapter = EffectSelectorAdapter(shaderRegistry, onAddEffect = { shaderDescriptor: ShaderDescriptor ->
            onAddEffect(shaderDescriptor.id)
        })
        effectSelectorRecyclerView.adapter = effectAdapter

        // Active layers
        layersRecyclerView.layoutManager = LinearLayoutManager(this)
        layerAdapter = LayerAdapter(
            shaderRegistry = shaderRegistry,
            onLayerChanged = { saveConfiguration() },
            onDeleteLayer = { position -> onDeleteLayer(position) }
        )
        layersRecyclerView.adapter = layerAdapter

        // Discover shaders and populate effect selector
        val shaders = shaderRegistry.discoverShaders()
        effectAdapter.submitList(shaders)
    }

    private fun loadConfiguration() {
        currentConfig = configManager.loadConfig()

        // Load layers
        currentConfig?.let { config ->
            layerAdapter.submitList(config.layers.toMutableList())
            updateEmptyState()
            updateApplyButton()

            // Load background image if exists
            config.background?.let { background ->
                try {
                    val uri = Uri.parse(background.uri)
                    // Test if we can access the URI before setting it
                    contentResolver.openInputStream(uri)?.use {
                        // Stream opened successfully, we have permission
                        backgroundPreview.setImageURI(uri)
                    }
                } catch (e: SecurityException) {
                    // Permission denied - clear the background from config
                    android.util.Log.w("SettingsActivity", "Lost permission to background image URI", e)
                    val updatedConfig = config.copy(background = null)
                    configManager.saveConfig(updatedConfig)
                    currentConfig = updatedConfig
                    backgroundPreview.setImageDrawable(null)
                } catch (e: Exception) {
                    // Other error loading image
                    android.util.Log.e("SettingsActivity", "Error loading background image", e)
                    backgroundPreview.setImageDrawable(null)
                }
            }
        }
    }

    private fun saveConfiguration() {
        val layers = layerAdapter.getLayers()
        val config = currentConfig?.copy(layers = layers) ?: WallpaperConfig(
            background = null,
            layers = layers
        )

        configManager.saveConfig(config)
        currentConfig = config
        updateApplyButton()
    }

    private fun setupListeners() {
        selectBackgroundButton.setOnClickListener {
            selectBackgroundImage()
        }

        applyWallpaperButton.setOnClickListener {
            applyWallpaper()
        }
    }

    private fun onAddEffect(shaderId: String) {
        val currentLayers = layerAdapter.getLayers()

        if (currentLayers.size >= maxLayers) {
            AlertDialog.Builder(this)
                .setTitle("Maximum Layers Reached")
                .setMessage("You can add up to $maxLayers layers")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Get shader to retrieve default parameter values
        val shader = shaderRegistry.getShaderById(shaderId)
        if (shader == null) {
            AlertDialog.Builder(this)
                .setTitle("Error")
                .setMessage("Shader not found: $shaderId")
                .setPositiveButton("OK", null)
                .show()
            return
        }

        // Create default params map from shader metadata
        val defaultParams = mutableMapOf<String, Any>()
        shader.parameters.forEach { param ->
            defaultParams[param.id] = param.defaultValue
        }

        // Create new layer with default values
        val newLayer = LayerConfig(
            shaderId = shaderId,
            order = currentLayers.size,
            enabled = true,
            opacity = 1.0f,
            depth = 0.5f,
            params = defaultParams
        )

        layerAdapter.addLayer(newLayer)
        updateEmptyState()
        saveConfiguration()
    }

    private fun onDeleteLayer(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Layer")
            .setMessage("Are you sure you want to delete this layer?")
            .setPositiveButton("Delete") { _, _ ->
                layerAdapter.removeLayer(position)
                updateEmptyState()
                saveConfiguration()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateEmptyState() {
        val hasLayers = layerAdapter.itemCount > 0
        emptyLayersMessage.visibility = if (hasLayers) View.GONE else View.VISIBLE
    }

    private fun updateApplyButton() {
        val hasLayers = layerAdapter.itemCount > 0
        val hasBackground = currentConfig?.background != null
        // Enable apply button if user has either layers OR a background image
        applyWallpaperButton.isEnabled = hasLayers || hasBackground
    }

    private fun selectBackgroundImage() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, REQUEST_SELECT_IMAGE)
    }

    private fun applyWallpaper() {
        // Save configuration one final time
        saveConfiguration()

        // Launch system wallpaper chooser
        val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
        intent.putExtra(
            WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
            ComponentName(this, "com.aether.wallpaper.AetherWallpaperService")
        )
        startActivity(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_SELECT_IMAGE -> {
                if (resultCode == RESULT_OK && data != null) {
                    val imageUri: Uri? = data.data
                    imageUri?.let {
                        // Request persistent permission for this URI
                        try {
                            contentResolver.takePersistableUriPermission(
                                it,
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                            )
                        } catch (e: SecurityException) {
                            // Permission not granted - URI may not support persistent permissions
                            // Continue anyway as we may still have temporary access
                        }

                        // Launch crop activity
                        val cropIntent = Intent(this, ImageCropActivity::class.java)
                        cropIntent.putExtra(ImageCropActivity.EXTRA_IMAGE_URI, it.toString())
                        startActivityForResult(cropIntent, REQUEST_CROP_IMAGE)

                        // Store temporary URI for crop result
                        tempImageUri = it
                    }
                }
            }
            REQUEST_CROP_IMAGE -> {
                if (resultCode == RESULT_OK && data != null) {
                    // Extract crop coordinates
                    val cropX = data.getIntExtra(ImageCropActivity.EXTRA_CROP_X, 0)
                    val cropY = data.getIntExtra(ImageCropActivity.EXTRA_CROP_Y, 0)
                    val cropWidth = data.getIntExtra(ImageCropActivity.EXTRA_CROP_WIDTH, 0)
                    val cropHeight = data.getIntExtra(ImageCropActivity.EXTRA_CROP_HEIGHT, 0)

                    // Create CropRect from coordinates
                    val cropRect = CropRect(cropX, cropY, cropWidth, cropHeight)

                    // Save configuration with crop coordinates
                    tempImageUri?.let { uri ->
                        val updatedConfig = currentConfig?.copy(
                            background = BackgroundConfig(
                                uri = uri.toString(),
                                crop = cropRect
                            )
                        )
                        updatedConfig?.let { config ->
                            configManager.saveConfig(config)
                            currentConfig = config
                        }

                        // Display cropped image in preview
                        backgroundPreview.setImageURI(uri)
                    }

                    tempImageUri = null
                }
            }
        }
    }
}
