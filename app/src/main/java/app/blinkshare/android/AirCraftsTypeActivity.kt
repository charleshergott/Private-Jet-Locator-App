package app.blinkshare.android

import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import app.blinkshare.android.adapters.AirCraftTypeAdapter
import app.blinkshare.android.databinding.ActivityAirCraftsTypeBinding
import app.blinkshare.android.model.Aircraft
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference

class AirCraftsTypeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAirCraftsTypeBinding

    private lateinit var db: FirebaseFirestore
    private lateinit var storageRef: StorageReference

    var mList = mutableListOf<Aircraft>()


    private val mAdapter: AirCraftTypeAdapter by lazy {
        AirCraftTypeAdapter(this, object : AirCraftTypeAdapter.AirCraftTypeInterface{
            override fun onAirCraftItemClick(image: Uri,typeName:String) {
//                println(image)
                val sIntent = Intent()
                sIntent.putExtra("image", image)
                sIntent.putExtra("typeName",typeName)
                sIntent.putExtra("fromAirCraft", true)
                setResult(RESULT_OK, sIntent)
                this@AirCraftsTypeActivity.finish()
            }

        })
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAirCraftsTypeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.ivBack.setOnClickListener {
            onBackPressed()
        }

        db = FirebaseFirestore.getInstance()
        storageRef = FirebaseStorage.getInstance().reference

        getAirCraftTypeFromDb()
    }


    private fun getAirCraftTypeFromDb() {
        db.collection("AircraftsPictures").get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    mAdapter.aircraftTypeList = mList
                    setAdapter()
                    hideShowLoading(false)
                    for (document in it.result) {
                        val id = document.id
                        var name = ""

                        if (document.contains("name")) {
                            name = document.get("name").toString()
                        }
                        val aircraft = Aircraft(id, name, null)
                        mList.add(aircraft)
                        storageRef.child("Aircraft Pictures/$id.jpg").downloadUrl.addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                val downloadUri = task.result
                                aircraft.path = downloadUri
                                binding.imagesRecycler.adapter?.notifyDataSetChanged()
                            }
                        }
                    }

                }
            }.addOnFailureListener {
                hideShowLoading(false)
            }
    }

    private fun setAdapter() {
        binding.imagesRecycler.let {
            it.layoutManager = LinearLayoutManager(this)
            it.adapter = mAdapter
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