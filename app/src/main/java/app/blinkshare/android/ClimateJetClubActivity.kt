package app.blinkshare.android

import android.graphics.drawable.Drawable
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.content.ContextCompat
import app.blinkshare.android.adapters.AboutAdapter
import app.blinkshare.android.databinding.ActivityClimateJetClubBinding

class ClimateJetClubActivity : AppCompatActivity() {
    private lateinit var binding: ActivityClimateJetClubBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityClimateJetClubBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initAdapter()
    }

    private fun initAdapter(){
        binding.rv.adapter = AboutAdapter()
    }
}