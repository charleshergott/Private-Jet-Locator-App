package app.blinkshare.android.adapters

import android.content.Context
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import app.blinkshare.android.R
import app.blinkshare.android.databinding.LayoutAboutClimateJetclubBinding
import app.blinkshare.android.openYoutubeLink
import com.astritveliu.boom.Boom

class AboutAdapter() : RecyclerView.Adapter<AboutAdapter.ViewHolder>() {
    private lateinit var context: Context
    inner class ViewHolder(val binding: LayoutAboutClimateJetclubBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun onBind(position: Int) {
            when (position) {
                0 -> {
                    binding.iv.setImageResource(R.drawable.a1)
                }
                1 -> {
                    Boom(binding.iv)
                    binding.iv.setImageResource(R.drawable.a2)
                    //https://www.youtube.com/watch?v=UfPqQTkDv90
                    binding.iv.setOnClickListener {
                        context.openYoutubeLink("UfPqQTkDv90")
                    }

                }
                2 -> {
                    binding.iv.setImageResource(R.drawable.a3)
                }
                3 -> {
                    binding.iv.setImageResource(R.drawable.a4)
                }
                4 -> {
                    binding.iv.setImageResource(R.drawable.a5)
                }
                5 -> {
                    binding.iv.setImageResource(R.drawable.a6)
                }
                6 -> {
                    binding.iv.setImageResource(R.drawable.a7)
                }
                7 -> {
                    binding.iv.setImageResource(R.drawable.a8)
                }
                8 -> {
                    binding.iv.setImageResource(R.drawable.a9)
                }
                9 -> {
                    binding.iv.setImageResource(R.drawable.a10)
                }
                10 -> {
                    binding.iv.setImageResource(R.drawable.a11)
                }
                11 -> {
                    binding.iv.setImageResource(R.drawable.a12)
                }
                12 -> {
                    binding.iv.setImageResource(R.drawable.a13)
                }
                13 -> {
                    binding.iv.setImageResource(R.drawable.a14)
                }
                14 -> {
                    binding.iv.setImageResource(R.drawable.a15)
                }
                15 -> {
                    binding.iv.setImageResource(R.drawable.a16)
                }
                16 -> {
                    binding.iv.setImageResource(R.drawable.a17)
                }
                17 -> {
                    binding.iv.setImageResource(R.drawable.a18)
                }
                18 -> {
                    binding.iv.setImageResource(R.drawable.a19)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding: LayoutAboutClimateJetclubBinding = DataBindingUtil.inflate(
            LayoutInflater.from(parent.context), R.layout.layout_about_climate_jetclub,
            parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun getItemCount(): Int = 19
}