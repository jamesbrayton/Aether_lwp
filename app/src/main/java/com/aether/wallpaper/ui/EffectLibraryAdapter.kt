package com.aether.wallpaper.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aether.wallpaper.R
import com.aether.wallpaper.model.ShaderDescriptor
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

/**
 * RecyclerView adapter for displaying available shader effects in the Effect Library.
 *
 * Displays effect cards with shader metadata from ShaderRegistry:
 * - Shader name from @shader tag
 * - Description from @description tag
 * - Tags as chips from @tags
 * - "Select Effect" button to choose effect for wallpaper
 *
 * Zero hardcoded shader names - all metadata-driven.
 */
class EffectLibraryAdapter(
    private val onEffectSelected: (String) -> Unit
) : RecyclerView.Adapter<EffectLibraryAdapter.EffectViewHolder>() {

    private var shaders: List<ShaderDescriptor> = emptyList()

    fun submitList(newShaders: List<ShaderDescriptor>) {
        shaders = newShaders
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EffectViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_effect_card, parent, false)
        return EffectViewHolder(view)
    }

    override fun onBindViewHolder(holder: EffectViewHolder, position: Int) {
        val shader = shaders[position]
        holder.bind(shader)
    }

    override fun getItemCount(): Int = shaders.size

    inner class EffectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val shaderName: TextView = itemView.findViewById(R.id.shaderName)
        private val shaderDescription: TextView = itemView.findViewById(R.id.shaderDescription)
        private val tagsChipGroup: ChipGroup = itemView.findViewById(R.id.tagsChipGroup)
        private val addEffectButton: Button = itemView.findViewById(R.id.addEffectButton)

        fun bind(shader: ShaderDescriptor) {
            // Display shader name from @shader tag
            shaderName.text = shader.name

            // Display description from @description tag
            shaderDescription.text = shader.description

            // Display tags as chips from @tags
            tagsChipGroup.removeAllViews()
            shader.tags.forEach { tag: String ->
                val chip = Chip(itemView.context)
                chip.text = tag
                chip.isClickable = false
                chip.isCheckable = false
                chip.setTextColor(0xFFFF9800.toInt()) // Light orange color
                tagsChipGroup.addView(chip)
            }

            // Change button text to "Select Effect" for library context
            addEffectButton.text = itemView.context.getString(R.string.select_effect)
            addEffectButton.setOnClickListener {
                onEffectSelected(shader.id)
            }
        }
    }
}
