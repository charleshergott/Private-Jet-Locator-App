package app.blinkshare.android

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import app.blinkshare.android.adapters.AircraftAdapter
import app.blinkshare.android.databinding.ActivityAircraftBinding
import app.blinkshare.android.di.NetworkResult
import app.blinkshare.android.model.AircraftResult
import app.blinkshare.android.viewModels.AircraftViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class AircraftActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAircraftBinding
    private val list: ArrayList<AircraftResult> = ArrayList()
    val aircraftViewModel: AircraftViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAircraftBinding.inflate(layoutInflater)
        setContentView(binding.root)
        initAdapter()
        fetchData()
    }

    private fun initAdapter() {
        binding.rec.adapter = AircraftAdapter(list, object: AircraftAdapter.OnItemClickListener{
            override fun onItemClick(position: Int) {
                list[position].isSelected = !list[position].isSelected
                binding.rec.adapter?.notifyItemChanged(position)
            }

        })
    }

    private fun fetchData() {
//        chatViewModel.fetchDogResponse()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                aircraftViewModel.response.collect() { response ->

                    when (response) {
                        is NetworkResult.Success -> {
                            // bind data to the view
                            hideShowLoading(false)
                            Log.e("MainActivity", "NetworkResult.Success")
                            response.data?.results?.let {
                                list.addAll(it)
                                binding.rec.adapter?.notifyDataSetChanged()
                            }
                        }
                        is NetworkResult.Error -> {
                            hideShowLoading(false)
                            // show error message
                            Log.e("MainActivity", "NetworkResult.Error")
                            Toast.makeText(applicationContext,response.message.toString(),Toast.LENGTH_LONG).show()
                        }
                        is NetworkResult.Loading -> {
                            // show a progress bar
                            hideShowLoading(true)
                            Log.e("MainActivity", "NetworkResult.Loading")
                        }
                    }
                }
            }
        }
    }

    private fun hideShowLoading(show: Boolean) {
        if (show) {
            binding.rlProgressLoading.visibility = View.VISIBLE
            binding.animationView.visibility = View.VISIBLE
        } else {
            binding.rlProgressLoading.visibility = View.GONE
            binding.animationView.visibility = View.GONE
        }
    }
}