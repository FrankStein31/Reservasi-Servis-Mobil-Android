package com.android.reservasiservismobilandroid.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.android.reservasiservismobilandroid.PaymentDetailActivity
import com.android.reservasiservismobilandroid.databinding.ItemPaymentBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class PaymentAdapter(
    private val payments: List<Map<String, Any>>,
    private val onViewDetailClick: (Map<String, Any>) -> Unit
) : RecyclerView.Adapter<PaymentAdapter.PaymentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PaymentViewHolder {
        val binding = ItemPaymentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PaymentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: PaymentViewHolder, position: Int) {
        holder.bind(payments[position])
    }

    override fun getItemCount() = payments.size

    inner class PaymentViewHolder(private val binding: ItemPaymentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(payment: Map<String, Any>) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

            val date = payment["service_date"]?.toString() ?: ""
            val displayDate = try {
                val dateObj = dateFormat.parse(date)
                if (dateObj != null) displayFormat.format(dateObj) else date
            } catch (e: Exception) {
                date
            }

            binding.apply {
                tvServiceDate.text = displayDate
                tvBill.text = "Total: ${currencyFormat.format((payment["bill"] as Double).toInt())}"
                tvPaymentMethod.text = "Metode: ${payment["method"]}"
                tvStatus.text = "Status: Lunas"

                btnViewDetails.setOnClickListener {
                    onViewDetailClick(payment)
                }

            }
        }
    }
} 