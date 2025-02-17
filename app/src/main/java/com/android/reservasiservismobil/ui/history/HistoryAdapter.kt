package com.android.reservasiservismobil.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.reservasiservismobil.R
import java.text.NumberFormat
import java.util.Locale

class HistoryAdapter(private var transactions: List<HistoryTransaction>) :
    RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    class HistoryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val dateTextView: TextView = view.findViewById(R.id.text_history_date)
        val amountTextView: TextView = view.findViewById(R.id.text_history_amount)
        val timeTextView: TextView = view.findViewById(R.id.text_history_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_transaction, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val transaction = transactions[position]

        // Format amount with Indonesian Rupiah formatting
        val numberFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
        val formattedAmount = numberFormat.format(
            transaction.totalHarga.replace("Rp", "").replace(",", "").trim().toDoubleOrNull() ?: 0.0
        )

        holder.dateTextView.text = transaction.date
        holder.amountTextView.text = formattedAmount
        holder.timeTextView.text = transaction.time ?: "-"
    }

    override fun getItemCount() = transactions.size

    fun updateData(newTransactions: List<HistoryTransaction>) {
        transactions = newTransactions
        notifyDataSetChanged()
    }
}