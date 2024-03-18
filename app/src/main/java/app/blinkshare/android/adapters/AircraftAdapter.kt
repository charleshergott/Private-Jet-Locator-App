package app.blinkshare.android.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import app.blinkshare.android.R
import app.blinkshare.android.databinding.AircraftItemListBinding
import app.blinkshare.android.model.AircraftResult

class AircraftAdapter(private val list: List<AircraftResult>, private val mListener: OnItemClickListener): RecyclerView.Adapter<AircraftAdapter.ViewHolder>() {
    private lateinit var context: Context
    inner class ViewHolder(val binding: AircraftItemListBinding, val listener: OnItemClickListener): RecyclerView.ViewHolder(binding.root) {
        fun onBind(position: Int){
            binding.tvName.text = list[position].aircraft_type_name
            if(list[position].isSelected){
                binding.selected.setBackgroundResource(R.drawable.selected)
            }
            else{
                binding.selected.setBackgroundResource(R.drawable.un_selected)
            }
            binding.root.setOnClickListener {
                listener.onItemClick(position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        context = parent.context
        val binding: AircraftItemListBinding = DataBindingUtil.inflate(LayoutInflater.from(parent.context),
            R.layout.aircraft_item_list,parent,false)
        return ViewHolder(binding,mListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.onBind(position)
    }

    override fun getItemCount(): Int = list.size

    interface OnItemClickListener{
        fun onItemClick(position: Int)
    }
}