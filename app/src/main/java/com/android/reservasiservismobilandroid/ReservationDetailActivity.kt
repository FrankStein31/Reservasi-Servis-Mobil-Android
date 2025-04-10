package com.android.reservasiservismobilandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.reservasiservismobilandroid.adapter.PackageProductAdapter
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
    private val TAG = "ReservationDetail"

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
        try {
            // Format tanggal
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val dateStr = data["reservation_date"]?.toString() ?: ""
            val date = dateFormat.parse(dateStr)
            val formattedDate = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(date ?: dateStr)

            // Format mata uang
            val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
            currencyFormat.maximumFractionDigits = 0

            // Ekstrak data paket dan produk dari package_detail
            val packageDetailStr = data["package_detail"]?.toString() ?: "{}"
            val packageDetail = JSONObject(packageDetailStr)
            
            val vehicleName = data["vehicle_name"]?.toString() ?: ""
            val plateNumber = data["plate_number"]?.toString() ?: ""
            val vehicleText = "$vehicleName ($plateNumber)"
            
            val packageName = data["package_name"]?.toString() ?: ""
            
            // Mengambil data produk dalam paket
            val productsList = mutableListOf<Map<String, Any>>()
            var totalBill = 0.0
            
            // Cek jika ada total price langsung
            if (packageDetail.has("price")) {
                totalBill = packageDetail.optDouble("price", 0.0)
            }
            
            // Cek format products dalam package_detail
            if (packageDetail.has("products")) {
                try {
                    // Coba parse sebagai array (format baru)
                    val productsArray = packageDetail.optJSONArray("products")
                    if (productsArray != null) {
                        // Format baru: products adalah array objek
                        for (i in 0 until productsArray.length()) {
                            val product = productsArray.getJSONObject(i)
                            val productId = product.optInt("id", 0)
                            val productName = product.optString("name", "")
                            val productPrice = product.optDouble("price", 0.0)
                            
                            productsList.add(mapOf(
                                "id" to productId,
                                "name" to productName,
                                "price" to productPrice
                            ))
                            
                            // Hitung total hanya jika belum ada price langsung
                            if (totalBill == 0.0) {
                                totalBill += productPrice
                            }
                        }
                    } else {
                        // Format lama: products adalah string
                        val productsStr = packageDetail.getString("products")
                        val productPairs = productsStr.split(",")
                        
                        for (pair in productPairs) {
                            val parts = pair.split(":")
                            if (parts.size >= 3) {
                                val productId = parts[0]
                                val productName = parts[1]
                                val productPrice = parts[2].toDoubleOrNull() ?: 0.0
                                
                                productsList.add(mapOf(
                                    "id" to productId,
                                    "name" to productName,
                                    "price" to productPrice
                                ))
                                
                                // Hitung total hanya jika belum ada price langsung
                                if (totalBill == 0.0) {
                                    totalBill += productPrice
                                }
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing products: ${e.message}")
                }
            }
            
            bill = totalBill
            
            // Update UI
            binding.tvServiceDate.text = "Tanggal Servis: $formattedDate"
            binding.tvTime.text = "Waktu: ${data["reservation_time"]}"
            binding.tvVehicle.text = "Kendaraan: $vehicleText"
            binding.tvPackage.text = "Paket: $packageName"
            binding.tvComplaint.text = "Keluhan: ${data["vehicle_complaint"]}"
            binding.tvBill.text = "Total Biaya: ${currencyFormat.format(totalBill)}"
            binding.tvBill.visibility = View.VISIBLE
            
            val status = data["service_status"]?.toString() ?: "Pending"
            binding.tvStatus.text = "Status: ${
                when(status) {
                    "Pending" -> "Menunggu"
                    "Process" -> "Sedang Diproses"
                    "Finish" -> "Selesai"
                    "Paid" -> "Lunas"
                    "Cancelled" -> "Dibatalkan"
                    else -> status
                }
            }"

            // Setup RecyclerView untuk produk
            if (productsList.isNotEmpty()) {
                binding.tvProductsTitle.visibility = View.VISIBLE
                binding.rvProducts.visibility = View.VISIBLE
                binding.rvProducts.layoutManager = LinearLayoutManager(this)
                binding.rvProducts.adapter = PackageProductAdapter(productsList)
                
                // Tampilkan total
                binding.tvProductsTotal.visibility = View.VISIBLE
                binding.tvProductsTotal.text = "Total: ${currencyFormat.format(totalBill)}"
            } else {
                binding.tvProductsTitle.visibility = View.GONE
                binding.rvProducts.visibility = View.GONE
                binding.tvProductsTotal.visibility = View.GONE
            }

            // Set visibility berdasarkan status
            when (status) {
                "Pending" -> {
                    binding.btnCancel.visibility = View.VISIBLE
                    binding.btnPay.visibility = View.GONE
                }
                "Process" -> {
                    binding.btnCancel.visibility = View.GONE
                    binding.btnPay.visibility = View.GONE
                }
                "Finish" -> {
                    binding.btnCancel.visibility = View.GONE
                    binding.btnPay.visibility = View.VISIBLE
                }
                "Paid" -> {
                    binding.btnCancel.visibility = View.GONE
                    binding.btnPay.visibility = View.GONE
                }
                "Cancelled" -> {
                    binding.btnCancel.visibility = View.GONE
                    binding.btnPay.visibility = View.GONE
                }
                else -> {
                    binding.btnCancel.visibility = View.VISIBLE
                    binding.btnPay.visibility = View.GONE
                }
            }
            
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error displaying reservation: ${e.message}")
            e.printStackTrace()
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