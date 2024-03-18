package app.blinkshare.android.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import app.blinkshare.android.R
import app.blinkshare.android.databinding.AircraftTypeRecyclerLayoutBinding
import app.blinkshare.android.model.Aircraft
import com.bumptech.glide.Glide


class AirCraftTypeAdapter(
    private var context: Context,
    private var listener: AirCraftTypeInterface

) : RecyclerView.Adapter<AirCraftTypeAdapter.AirCraftTypeViewHolder>() {

    var aircraftTypeList = mutableListOf<Aircraft>()

    inner class AirCraftTypeViewHolder constructor(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var binding = AircraftTypeRecyclerLayoutBinding.bind(itemView)

        var imageView = binding.imgPhoto
        var name = binding.name

    }

    private val mInflater: LayoutInflater by lazy {
        LayoutInflater.from(context)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): AirCraftTypeViewHolder {
        val view = mInflater.inflate(R.layout.aircraft_type_recycler_layout, parent, false)
        return AirCraftTypeViewHolder(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: AirCraftTypeViewHolder, position: Int) {
        val items = aircraftTypeList[position]

        Glide.with(context)
            .load(items.path)
            .into(holder.imageView)

        holder.name.text = items.name

        holder.itemView.setOnClickListener {
            items.path?.let { listener.onAirCraftItemClick(it,holder.name.text.toString()) }
        }

    }

    override fun getItemCount(): Int {
        return aircraftTypeList.size
    }

    interface AirCraftTypeInterface {
        fun onAirCraftItemClick(image: Uri,typeName:String)
    }
}