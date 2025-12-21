package com.aether.wallpaper.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aether.wallpaper.R
import com.aether.wallpaper.model.LayerConfig
import com.aether.wallpaper.model.ParameterDefinition
import com.aether.wallpaper.model.ParameterType
import com.aether.wallpaper.shader.ShaderRegistry
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * RecyclerView adapter for active wallpaper layers.
 *
 * Features:
 * - Displays shader name from ShaderRegistry
 * - Enable/disable toggle per layer
 * - Delete button with confirmation
 * - DYNAMIC parameter controls generated from @param metadata
 * - Saves configuration changes immediately
 *
 * Parameter UI Generation:
 * - Float parameters → Slider with min/max/step from @param
 * - Parameter labels use @param name attribute
 * - Descriptions shown as helper text
 * - No hardcoded parameters - fully metadata-driven
 */
class LayerAdapter(
    private val shaderRegistry: ShaderRegistry,
    private val onLayerChanged: () -> Unit,
    private val onDeleteLayer: (Int) -> Unit
) : RecyclerView.Adapter<LayerAdapter.LayerViewHolder>() {

    private var layers: MutableList<LayerConfig> = mutableListOf()

    fun submitList(newLayers: MutableList<LayerConfig>) {
        layers = newLayers
        notifyDataSetChanged()
    }

    fun addLayer(layer: LayerConfig) {
        layers.add(layer)
        notifyItemInserted(layers.size - 1)
        onLayerChanged()
    }

    fun removeLayer(position: Int) {
        if (position >= 0 && position < layers.size) {
            layers.removeAt(position)
            notifyItemRemoved(position)
            // Update order values for remaining layers
            layers.forEachIndexed { index, layer ->
                layers[index] = layer.copy(order = index)
            }
            notifyItemRangeChanged(position, layers.size - position)
            onLayerChanged()
        }
    }

    fun getLayers(): List<LayerConfig> = layers.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LayerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_layer, parent, false)
        return LayerViewHolder(view)
    }

    override fun onBindViewHolder(holder: LayerViewHolder, position: Int) {
        val layer = layers[position]
        holder.bind(layer, position)
    }

    override fun getItemCount(): Int = layers.size

    inner class LayerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layerName: TextView = itemView.findViewById(R.id.layerName)
        private val layerEnabledSwitch: SwitchMaterial = itemView.findViewById(R.id.layerEnabledSwitch)
        private val deleteLayerButton: ImageButton = itemView.findViewById(R.id.deleteLayerButton)
        private val parametersContainer: LinearLayout = itemView.findViewById(R.id.parametersContainer)

        fun bind(layer: LayerConfig, position: Int) {
            // Get shader metadata
            val shader = shaderRegistry.getShaderById(layer.shaderId)

            // Display shader name
            layerName.text = shader?.name ?: layer.shaderId

            // Enable/disable toggle
            layerEnabledSwitch.setOnCheckedChangeListener(null) // Remove old listener
            layerEnabledSwitch.isChecked = layer.enabled
            layerEnabledSwitch.setOnCheckedChangeListener { _, isChecked ->
                layers[position] = layer.copy(enabled = isChecked)
                onLayerChanged()
            }

            // Delete button
            deleteLayerButton.setOnClickListener {
                onDeleteLayer(position)
            }

            // Generate dynamic parameter controls
            generateParameterControls(layer, shader?.parameters ?: emptyList(), position)
        }

        /**
         * Dynamically generate UI controls for shader parameters.
         *
         * Reads @param metadata and creates appropriate controls:
         * - Float/Int → Slider with min/max/step
         * - Uses @param name for label
         * - Shows @param desc as helper text
         *
         * This is the key to zero-code shader addition - the UI
         * adapts automatically to any shader's parameters.
         */
        private fun generateParameterControls(
            layer: LayerConfig,
            parameters: List<ParameterDefinition>,
            position: Int
        ) {
            parametersContainer.removeAllViews()

            parameters.forEach { param ->
                when (param.type) {
                    ParameterType.FLOAT, ParameterType.INT -> {
                        val paramView = createParameterSlider(layer, param, position)
                        parametersContainer.addView(paramView)
                    }
                    else -> {
                        // TODO: Support other parameter types in future
                        // (bool, color, vec2, vec3, vec4)
                    }
                }
            }
        }

        private fun createParameterSlider(
            layer: LayerConfig,
            param: ParameterDefinition,
            position: Int
        ): View {
            val view = LayoutInflater.from(itemView.context)
                .inflate(R.layout.view_parameter_slider, parametersContainer, false)

            val parameterLabel: TextView = view.findViewById(R.id.parameterLabel)
            val parameterValue: TextView = view.findViewById(R.id.parameterValue)
            val parameterSlider: Slider = view.findViewById(R.id.parameterSlider)
            val parameterDescription: TextView = view.findViewById(R.id.parameterDescription)

            // Set label from @param name
            parameterLabel.text = param.name

            // Set description from @param desc
            parameterDescription.text = param.description

            // Get current value from layer config
            val currentValue = layer.params[param.id] as? Number ?: param.defaultValue as Number

            // Configure slider from @param metadata
            val minValue = (param.minValue as? Number)?.toFloat() ?: 0f
            val maxValue = (param.maxValue as? Number)?.toFloat() ?: 1f
            val stepSize = (param.step as? Number)?.toFloat() ?: 0.1f

            // Material Slider doesn't support negative valueFrom, so offset if needed
            val offset = if (minValue < 0f) -minValue else 0f
            parameterSlider.valueFrom = minValue + offset
            parameterSlider.valueTo = maxValue + offset
            parameterSlider.stepSize = stepSize
            parameterSlider.value = (currentValue.toFloat() + offset).coerceIn(minValue + offset, maxValue + offset)

            // Display current value (actual value, not offset)
            parameterValue.text = when (param.type) {
                ParameterType.INT -> currentValue.toInt().toString()
                else -> String.format("%.2f", currentValue.toFloat())
            }

            // Update on slider change
            parameterSlider.addOnChangeListener { _, value, _ ->
                // Convert from offset slider value back to actual parameter value
                val actualValue = value - offset

                val newValue: Any = when (param.type) {
                    ParameterType.INT -> actualValue.toInt()
                    else -> actualValue
                }

                // Update value label (show actual value, not offset)
                parameterValue.text = when (param.type) {
                    ParameterType.INT -> actualValue.toInt().toString()
                    else -> String.format("%.2f", actualValue)
                }

                // Update layer config
                val updatedParams = layer.params.toMutableMap()
                updatedParams[param.id] = newValue
                layers[position] = layer.copy(params = updatedParams)

                // Save configuration (debounced in SettingsActivity)
                onLayerChanged()
            }

            return view
        }
    }
}
