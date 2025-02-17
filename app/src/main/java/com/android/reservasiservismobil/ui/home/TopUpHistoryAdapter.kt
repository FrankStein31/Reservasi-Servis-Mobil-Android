package com.android.reservasiservismobil.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.reservasiservismobil.R
import org.json.JSONArray
import java.text.NumberFormat
import java.util.Locale

class TopUpHistoryAdapter(private val historyData: JSONArray) :
    RecyclerView.Adapter<TopUpHistoryAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNominal: TextView = view.findViewById(R.id.tvNominal)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
        val tvDate: TextView = view.findViewById(R.id.tvDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_topup_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = historyData.getJSONObject(position)
        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        holder.tvNominal.text = formatRupiah.format(item.getDouble("nominal"))

        // Kondisi untuk status valid
        val validStatus = item.getInt("valid")
        when (validStatus) {
            1 -> { // Disetujui
                holder.tvStatus.apply {
                    text = "Disetujui"
                    setTextColor(context.getColor(R.color.white))
                    setBackgroundResource(R.drawable.status_background_green)
                    setPadding(32, 16, 32, 16) // Padding in pixels
                }
            }
            2 -> { // Ditolak
                holder.tvStatus.apply {
                    text = "Ditolak"
                    setTextColor(context.getColor(R.color.white))
                    setBackgroundResource(R.drawable.status_background_red)
                    setPadding(32, 16, 32, 16)
                }
            }
            else -> { // Menunggu
                holder.tvStatus.apply {
                    text = "Menunggu"
                    setTextColor(context.getColor(R.color.white))
                    setBackgroundResource(R.drawable.status_background_orange)
                    setPadding(32, 16, 32, 16)
                }
            }
        }
    }

    override fun getItemCount() = historyData.length()
}