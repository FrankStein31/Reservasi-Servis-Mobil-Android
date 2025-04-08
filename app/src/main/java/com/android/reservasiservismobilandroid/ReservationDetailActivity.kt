package com.android.reservasiservismobilandroid

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.reservasiservismobilandroid.api.ApiService
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityReservationDetailBinding
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReservationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservationDetailBinding
    private lateinit var apiService: ApiService
    private var serviceId: Int = -1
    private var bill: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detail Reservasi"

        apiService = RetrofitClient.apiService

        // Ambil data dari intent
        val reservationId = intent.getIntExtra("reservation_id", -1)
        serviceId = intent.getIntExtra("service_id", -1)

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
        binding.apply {
            tvServiceDate.text = "Tanggal Servis: ${data["reservation_date"]}"
            tvVehicle.text = "Kendaraan: ${data["vehicle_name"]} (${data["plate_number"]})"
            tvPackage.text = "Paket: ${data["package_name"]}"
            tvComplaint.text = "Keluhan: ${data["vehicle_complaint"] ?: "-"}"
            tvStatus.text = "Status: ${data["service_status"] ?: "Belum Diproses"}"

            // Ambil service_id dan bill jika ada
            if (data["service_id"] != null) {
                serviceId = when (val sid = data["service_id"]) {
                    is Double -> sid.toInt()
                    is Int -> sid
                    is String -> sid.toIntOrNull() ?: -1
                    else -> -1
                }
            }

            // Ambil bill dari package_detail
            val packageDetail = data["package_detail"]?.toString()
            if (!packageDetail.isNullOrEmpty()) {
                try {
                    val packageJson = org.json.JSONObject(packageDetail)
                    bill = packageJson.optDouble("price", 0.0)
                } catch (e: Exception) {
                    bill = 0.0
                }
            }

            // Tampilkan tombol bayar hanya jika status pending
            btnPay.visibility = if (data["service_status"] == "Pending") View.VISIBLE else View.GONE
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
                // Implementasi pembatalan reservasi
                Toast.makeText(this, "Fitur pembatalan akan segera hadir", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 