package com.android.reservasiservismobilandroid.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.reservasiservismobilandroid.databinding.ItemVehicleBinding

class VehicleAdapter(
    private val vehicles: List<Map<String, Any>>,
    private val onEditClick: (Map<String, Any>) -> Unit,
    private val onDeleteClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
        val binding = ItemVehicleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VehicleViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
        holder.bind(vehicles[position])
    }

    override fun getItemCount() = vehicles.size

    inner class VehicleViewHolder(private val binding: ItemVehicleBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(vehicle: Map<String, Any>) {
            binding.apply {
                tvVehicleName.text = vehicle["name"].toString()
                tvVehicleBrand.text = "Merek: ${vehicle["brand"]}"
                tvPlateNumber.text = "Plat Nomor: ${vehicle["plate_number"]}"
                tvYear.text = "Tahun: ${vehicle["year"] ?: "-"}"

                btnEdit.setOnClickListener {
                    onEditClick(vehicle)
                }

                btnDelete.setOnClickListener {
                    onDeleteClick(vehicle)
                }
            }
        }
    }
} 