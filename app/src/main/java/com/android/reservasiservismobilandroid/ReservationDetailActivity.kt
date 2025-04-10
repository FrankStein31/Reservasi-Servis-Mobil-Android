package com.android.reservasiservismobilandroid

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityReservationDetailBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class ReservationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservationDetailBinding
    private lateinit var sharedPrefs: SharedPrefs
    private var reservationId: Int = -1
    private var serviceId: Int = -1
    private var bill: Double = 0.0
    private var status: String = "Pending"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detail Reservasi"

        sharedPrefs = SharedPrefs(this)

        // Ambil data dari intent
        reservationId = intent.getIntExtra("reservation_id", -1)
        serviceId = intent.getIntExtra("service_id", -1)
        status = intent.getStringExtra("status") ?: "Pending"

        if (reservationId != -1) {
            loadReservationDetail(reservationId)
        } else {
            Toast.makeText(this, "Data reservasi tidak valid", Toast.LENGTH_SHORT).show()
            finish()
        }

        setupClickListeners()
    }

    private fun loadReservationDetail(reservationId: Int) {
        RetrofitClient.apiService.getReservationDetail(reservationId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.get("status") == "success") {
                        val data = responseBody["data"] as? Map<*, *>
                        if (data != null) {
                            displayReservationDetail(data)
                        }
                    } else {
                        Toast.makeText(this@ReservationDetailActivity, "Gagal memuat detail reservasi", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@ReservationDetailActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayReservationDetail(data: Map<*, *>) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        val date = data["reservation_date"]?.toString() ?: ""
        val displayDate = try {
            val dateObj = dateFormat.parse(date)
            if (dateObj != null) displayFormat.format(dateObj) else date
        } catch (e: Exception) {
            date
        }

        status = data["service_status"]?.toString() ?: "Pending"
        
        // Ambil service_id jika ada
        if (data["service_id"] != null) {
            serviceId = when (val sid = data["service_id"]) {
                is Double -> sid.toInt()
                is Int -> sid
                is String -> sid.toIntOrNull() ?: -1
                else -> -1
            }
        }

        // Hitung bill dari package_detail
        val packageDetail = data["package_detail"]?.toString()
        if (!packageDetail.isNullOrEmpty()) {
            try {
                val packageDetailObj = JSONObject(packageDetail)
                var totalPrice = 0.0
                
                // Cek apakah ada harga paket
                if (packageDetailObj.has("price")) {
                    totalPrice += packageDetailObj.getDouble("price")
                }
                
                // Cek apakah ada produk
                if (packageDetailObj.has("products")) {
                    val productsStr = packageDetailObj.getString("products")
                    val productPairs = productsStr.split(",")
                    
                    for (pair in productPairs) {
                        val parts = pair.split(":")
                        if (parts.size >= 3) {
                            val price = parts[2].toDoubleOrNull() ?: 0.0
                            totalPrice += price
                        }
                    }
                }
                
                // Default 50K jika totalPrice masih 0
                if (totalPrice == 0.0) {
                    totalPrice = 50000.0
                }
                
                bill = totalPrice
                binding.tvBill.text = "Total Biaya: ${currencyFormat.format(bill)}"
                binding.tvBill.visibility = View.VISIBLE
            } catch (e: Exception) {
                // Default jika terjadi error dalam parsing
                bill = 50000.0
                binding.tvBill.text = "Total Biaya: ${currencyFormat.format(bill)}"
                binding.tvBill.visibility = View.VISIBLE
            }
        } else {
            // Default jika tidak ada package_detail
            bill = 50000.0
            binding.tvBill.text = "Total Biaya: ${currencyFormat.format(bill)}"
            binding.tvBill.visibility = View.VISIBLE
        }

        binding.tvServiceDate.text = "Tanggal Servis: $displayDate"
        binding.tvTime.text = "Waktu: ${data["reservation_time"]}"
        binding.tvVehicle.text = "Kendaraan: ${data["vehicle_name"]} (${data["plate_number"]})"
        binding.tvPackage.text = "Paket: ${data["package_name"]}"
        binding.tvComplaint.text = "Keluhan: ${data["vehicle_complaint"] ?: "-"}"
        binding.tvStatus.text = "Status: ${when(status) {
            "Pending" -> "Menunggu"
            "Process" -> "Sedang Diproses"
            "Finish" -> "Selesai"
            "Paid" -> "Sudah Dibayar"
            else -> status
        }}"

        updateUIBasedOnStatus()
    }

    private fun updateUIBasedOnStatus() {
        binding.apply {
            // Tampilkan/sembunyikan tombol berdasarkan status
            btnCancel.visibility = if (status == "Pending") View.VISIBLE else View.GONE
            
            // Tombol bayar hanya muncul saat status "Finish"
            btnPay.visibility = if (status == "Finish") View.VISIBLE else View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnPay.setOnClickListener {
            if (serviceId != -1 && bill > 0) {
                val intent = Intent(this, PaymentFormActivity::class.java)
                intent.putExtra("service_id", serviceId)
                intent.putExtra("bill", bill)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Data pembayaran belum siap", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            showCancelConfirmationDialog()
        }
    }

    private fun showCancelConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Batalkan Reservasi")
            .setMessage("Apakah Anda yakin ingin membatalkan reservasi ini?")
            .setPositiveButton("Ya") { _, _ ->
                cancelReservation()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    private fun cancelReservation() {
        val data = mapOf("id" to reservationId.toString())
        
        RetrofitClient.apiService.deleteReservation(data).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.get("status") == "success") {
                        Toast.makeText(this@ReservationDetailActivity, "Reservasi berhasil dibatalkan", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        val message = responseBody?.get("message")?.toString() ?: "Gagal membatalkan reservasi"
                        Toast.makeText(this@ReservationDetailActivity, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ReservationDetailActivity, "Gagal membatalkan reservasi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@ReservationDetailActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
} 