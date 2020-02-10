package com.digilock.nl.tablet.main

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.digilock.nl.tablet.R
import kotlinx.android.synthetic.main.device_single_item.view.*

class DevicesRecyclerViewAdapter(val mDeviceList: List<DeviceData>) :
        RecyclerView.Adapter<DevicesRecyclerViewAdapter.VH>() {


    private var listener: ItemClickListener? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent!!.context).inflate(R.layout.device_single_item, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder?.itemView.device_name.text = mDeviceList[position].deviceName
        holder?.itemView.device_mac.text = mDeviceList[position].deviceHardwareAddress
        holder?.itemView.device_ip.text = mDeviceList[position].deviceIPAddress
    }

    override fun getItemCount(): Int {
        return mDeviceList.size
    }

    inner class VH(itemView: View?) : RecyclerView.ViewHolder(itemView!!){

        var label: TextView? = itemView?.findViewById(R.id.largeLabel)

        init {
            itemView?.setOnClickListener{
                listener?.itemClicked(mDeviceList[adapterPosition])
            }
        }
    }

    fun setItemClickListener(listener: ItemClickListener){
        this.listener = listener
    }

    interface ItemClickListener{
        fun itemClicked(deviceData: DeviceData)
    }
}