package com.android.reservasiservismobilandroid

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityPaymentsBinding
import com.android.reservasiservismobilandroid.databinding.ItemPaymentBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class PaymentsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentsBinding
    private lateinit var adapter: PaymentAdapter
    private lateinit var sharedPrefs: SharedPrefs
    private var payments = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = SharedPrefs(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        loadPayments()
    }

    private fun setupRecyclerView() {
        adapter = PaymentAdapter(
            payments,
            onViewDetailClick = { payment ->
                val intent = Intent(this, PaymentDetailActivity::class.java)
                intent.putExtra("service_id", (payment["service_id"] as Double).toInt())
                intent.putExtra("service_date", payment["service_date"].toString())
                intent.putExtra("vehicle_name", payment["vehicle_name"].toString())
                intent.putExtra("plate_number", payment["plate_number"].toString())
                intent.putExtra("package_name", payment["package_name"].toString())
                intent.putExtra("bill", (payment["bill"] as Double).toInt())
                intent.putExtra("method", payment["method"].toString())
                intent.putExtra("pay", (payment["pay"] as Double).toInt())
                intent.putExtra("change", (payment["change"] as Double).toInt())
                intent.putExtra("note", payment["note"]?.toString() ?: "-")
                startActivity(intent)
            },
        )
        binding.rvPayments.apply {
            layoutManager = LinearLayoutManager(this@PaymentsActivity)
            adapter = this@PaymentsActivity.adapter
        }
    }

    private fun loadPayments() {
        val customerId = sharedPrefs.getUserId()

        RetrofitClient.apiService.getPaymentStatus(customerId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseData = response.body()
                    if (responseData?.get("status") == "success") {
                        val data = responseData["data"] as? List<*>
                        if (data != null) {
                            payments.clear()
                            payments.addAll(data.filterIsInstance<Map<String, Any>>())
                            adapter.notifyDataSetChanged()
                        }
                    } else {
                        Toast.makeText(this@PaymentsActivity, "Gagal memuat data pembayaran", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@PaymentsActivity, "Gagal memuat data pembayaran", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@PaymentsActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private inner class PaymentAdapter(
        private val payments: List<Map<String, Any>>,
        private val onViewDetailClick: (Map<String, Any>) -> Unit,
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

                val serviceDate = payment["service_date"]?.toString() ?: ""
                val displayDate = try {
                    val date = dateFormat.parse(serviceDate)
                    if (date != null) displayFormat.format(date) else serviceDate
                } catch (e: Exception) {
                    serviceDate
                }

                val bill = (payment["bill"] as Double).toInt()

                binding.apply {
                    tvServiceDate.text = "Tanggal Servis: $displayDate"
                    tvBill.text = "Biaya: ${currencyFormat.format(bill)}"
                    tvPaymentMethod.text = "Metode Pembayaran: ${payment["method"]}"
                    tvStatus.text = "Status: Lunas"

                    btnViewDetails.setOnClickListener { onViewDetailClick(payment) }
                }
            }
        }
    }
} 