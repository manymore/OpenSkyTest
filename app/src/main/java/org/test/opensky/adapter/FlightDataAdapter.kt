package org.test.opensky.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.test.opensky.MainActivity
import org.test.opensky.databinding.AdapterFlightDataBinding
import org.test.opensky.model.FlightDataRecordDecoded

class FlightDataAdapter(val activity: MainActivity) : RecyclerView.Adapter<MainViewHolder>() {

    var flightDataList = mutableListOf<FlightDataRecordDecoded>()

    fun setFlightData(flightData: List<FlightDataRecordDecoded>) {
        this.flightDataList = flightData.toMutableList()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = AdapterFlightDataBinding.inflate(inflater, parent, false)
        return MainViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MainViewHolder, position: Int) {
        val item = flightDataList[position]
        holder.binding.icao.text = item.icao24()
        holder.binding.callSign.text = item.callSign()
        holder.binding.longitude.text = item.longitude().toString()
        holder.binding.latitude.text = item.latitude().toString()

        holder.binding.root.setOnClickListener {
            activity.toggleMapView(true, item.key())
        }
    }

    override fun getItemCount(): Int {
        return flightDataList.size
    }
}

class MainViewHolder(val binding: AdapterFlightDataBinding) : RecyclerView.ViewHolder(binding.root) {
}
