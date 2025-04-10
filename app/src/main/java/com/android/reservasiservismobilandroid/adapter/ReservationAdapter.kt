package com.android.reservasiservismobilandroid.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.reservasiservismobilandroid.ReservationDetailActivity
import com.android.reservasiservismobilandroid.databinding.ItemReservationBinding
import java.text.SimpleDateFormat
import java.util.Locale

class ReservationAdapter(
    private val reservations: List<Map<String, Any>>,
    private val onCancelClick: (Int) -> Unit
) : RecyclerView.Adapter<ReservationAdapter.ReservationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservationViewHolder {
        val binding = ItemReservationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReservationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReservationViewHolder, position: Int) {
        holder.bind(reservations[position])
    }

    override fun getItemCount() = reservations.size

    inner class ReservationViewHolder(private val binding: ItemReservationBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reservation: Map<String, Any>) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())

            val date = reservation["reservation_date"]?.toString() ?: ""
            val displayDate = try {
                val dateObj = dateFormat.parse(date)
                if (dateObj != null) displayFormat.format(dateObj) else date
            } catch (e: Exception) {
                date
            }

            val status = reservation["service_status"]?.toString() ?: "Pending"

            binding.apply {
                tvServiceDate.text = displayDate
                tvVehicle.text = "${reservation["vehicle_name"]} (${reservation["plate_number"]})"
                tvPackage.text = reservation["package_name"].toString()
                tvStatus.text = when(status) {
                    "Pending" -> "Menunggu"
                    "Process" -> "Sedang Diproses"
                    "Finish" -> "Selesai"
                    else -> "Belum Diproses"
                }

                // Tampilkan tombol batalkan hanya jika status masih Pending/Menunggu
                btnCancel.visibility = if (status == "Pending") View.VISIBLE else View.GONE

                btnViewDetails.setOnClickListener {
                    val intent = Intent(itemView.context, ReservationDetailActivity::class.java)
                    intent.putExtra("reservation_id", (reservation["id"] as Double).toInt())
                    
                    // Tambahkan service_id jika tersedia
                    if (reservation.containsKey("service_id") && reservation["service_id"] != null) {
                        val serviceId = when (val sid = reservation["service_id"]) {
                            is Double -> sid.toInt()
                            is Int -> sid
                            is String -> sid.toIntOrNull() ?: -1
                            else -> -1
                        }
                        if (serviceId > 0) {
                            intent.putExtra("service_id", serviceId)
                        }
                    }
                    
                    intent.putExtra("reservation_date", date)
                    intent.putExtra("reservation_time", reservation["reservation_time"].toString())
                    intent.putExtra("vehicle_name", reservation["vehicle_name"].toString())
                    intent.putExtra("plate_number", reservation["plate_number"].toString())
                    intent.putExtra("package_name", reservation["package_name"].toString())
                    intent.putExtra("complaint", reservation["vehicle_complaint"].toString())
                    intent.putExtra("status", status)
                    itemView.context.startActivity(intent)
                }

                btnCancel.setOnClickListener {
                    val reservationId = (reservation["id"] as Double).toInt()
                    onCancelClick(reservationId)
                }
            }
        }
    }
} 