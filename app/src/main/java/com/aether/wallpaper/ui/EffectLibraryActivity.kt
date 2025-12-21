package com.aether.wallpaper.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aether.wallpaper.R
import com.aether.wallpaper.shader.ShaderRegistry
import com.google.android.material.appbar.MaterialToolbar

class EffectLibraryActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_SHADER_ID = "SHADER_ID"
        const val REQUEST_CODE_EFFECT_LIBRARY = 1003
    }

    private lateinit var toolbar: MaterialToolbar
    private lateinit var effectRecyclerView: RecyclerView
    private lateinit var emptyStateContainer: LinearLayout
    private lateinit var shaderRegistry: ShaderRegistry
    private lateinit var effectAdapter: EffectLibraryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_effect_library)

        toolbar = findViewById(R.id.toolbar)
        effectRecyclerView = findViewById(R.id.effectLibraryRecyclerView)
        emptyStateContainer = findViewById(R.id.emptyStateContainer)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.setNavigationOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

        shaderRegistry = ShaderRegistry(this)

        effectRecyclerView.layoutManager = LinearLayoutManager(this)
        effectAdapter = EffectLibraryAdapter(onEffectSelected = { shaderId ->
            onEffectSelected(shaderId)
        })
        effectRecyclerView.adapter = effectAdapter

        val shaders = shaderRegistry.discoverShaders()
        if (shaders.isEmpty()) {
            effectRecyclerView.visibility = View.GONE
            emptyStateContainer.visibility = View.VISIBLE
        } else {
            effectRecyclerView.visibility = View.VISIBLE
            emptyStateContainer.visibility = View.GONE
            effectAdapter.submitList(shaders)
        }
    }

    private fun onEffectSelected(shaderId: String) {
        val resultIntent = Intent().apply {
            putExtra(EXTRA_SHADER_ID, shaderId)
        }
        setResult(RESULT_OK, resultIntent)
        finish()
    }
}
