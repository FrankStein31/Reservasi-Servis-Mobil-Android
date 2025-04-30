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
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.net.Uri
import com.android.reservasiservismobilandroid.utils.PdfGenerator

class ReservationDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReservationDetailBinding
    private lateinit var sharedPrefs: SharedPrefs
    private var reservationId: Int = -1
    private var serviceId: Int = -1
    private var bill: Double = 0.0
    private val TAG = "ReservationDetail"
    
    private val STORAGE_PERMISSION_CODE = 101
    private var reservationData: Map<*, *>? = null
    private var productsList = mutableListOf<Map<String, Any>>()

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
                            // Log semua data untuk debug
                            Log.d(TAG, "API Response: $data")
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
            // Simpan data untuk pembuatan PDF
            reservationData = data
            
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
            productsList.clear()
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
            binding.tvServiceDate.text = "$formattedDate"
            binding.tvTime.text = "${data["reservation_time"]}"
            binding.tvVehicle.text = "$vehicleText"
            binding.tvPackage.text = "$packageName"
            binding.tvComplaint.text = "${data["vehicle_complaint"]}"
            binding.tvBill.text = "Total Biaya: ${currencyFormat.format(totalBill)}"
            binding.tvBill.visibility = View.VISIBLE
            
            val status = data["service_status"]?.toString() ?: "Pending"
            binding.tvStatus.text = "Status: ${
                when(status) {
                    "Pending" -> "Menunggu"
                    "Process" -> "Sedang Diproses"
                    "Finish" -> "Selesai || Lunas"
                    "Selesai" -> "Servis Selesai || Belum Bayar"
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
                binding.tvProductsTotal.text = "${currencyFormat.format(totalBill)}"
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
                    binding.btnPrintPdf.visibility = View.GONE
                }
                "Process" -> {
                    binding.btnCancel.visibility = View.GONE
                    binding.btnPay.visibility = View.GONE
                    binding.btnPrintPdf.visibility = View.VISIBLE
                }
                "Selesai" -> {
                    binding.btnCancel.visibility = View.GONE
                    
                    // Cek apakah sudah ada pembayaran berdasarkan jumlah record di tabel payments
                    val paymentExists = data["payment_exists"]
                    val serviceId = data["service_id"]?.toString()
                    
                    Log.d(TAG, "Service ID: $serviceId, Payment Exists: $paymentExists (${paymentExists?.javaClass?.name})")
                    
                    // Konversi nilai ke integer untuk pengecekan yang lebih pasti
                    val paymentCount = when (paymentExists) {
                        is Int -> paymentExists
                        is Long -> paymentExists.toInt()
                        is Double -> paymentExists.toInt()
                        is String -> paymentExists.toIntOrNull() ?: 0
                        else -> 0
                    }
                    
                    Log.d(TAG, "Payment Count: $paymentCount")
                    
                    if (paymentCount > 0) {
                        // Jika ada pembayaran (nilai > 0), sembunyikan tombol bayar
                        Log.d(TAG, "Payment exists, hiding pay button")
                        binding.btnPay.visibility = View.GONE
                    } else {
                        // Jika tidak ada pembayaran, tampilkan tombol bayar
                        Log.d(TAG, "No payment exists, showing pay button")
                        binding.btnPay.visibility = View.VISIBLE
                    }
                    
                    // Tampilkan tombol PDF jika sudah selesai
                    binding.btnPrintPdf.visibility = View.VISIBLE
                }
                "Finish" -> {
                    binding.btnCancel.visibility = View.GONE
                    binding.btnPay.visibility = View.GONE
                    binding.btnPrintPdf.visibility = View.VISIBLE
                }
                "Cancelled" -> {
                    binding.btnCancel.visibility = View.GONE
                    binding.btnPay.visibility = View.GONE
                    binding.btnPrintPdf.visibility = View.GONE
                }
                else -> {
                    binding.btnCancel.visibility = View.VISIBLE
                    binding.btnPay.visibility = View.GONE
                    binding.btnPrintPdf.visibility = View.GONE
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
                startActivityForResult(intent, PAYMENT_REQUEST_CODE)
            } else {
                Toast.makeText(this, "Data pembayaran belum siap", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            showCancelConfirmationDialog()
        }
        
        binding.btnPrintPdf.setOnClickListener {
            if (checkStoragePermission()) {
                generatePdf()
            } else {
                requestStoragePermission()
            }
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

    private fun generatePdf() {
        if (reservationData == null) {
            Toast.makeText(this, "Data reservasi tidak tersedia", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading
        val progressDialog = android.app.ProgressDialog(this)
        progressDialog.setMessage("Membuat PDF...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        
        try {
            val pdfUri = PdfGenerator.createReservationPdf(
                this,
                reservationData as Map<String, Any?>,
                productsList,
                bill
            )
            
            progressDialog.dismiss()
            
            if (pdfUri != null) {
                showPdfActionDialog(pdfUri)
            } else {
                Toast.makeText(this, "Gagal membuat PDF", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            progressDialog.dismiss()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e(TAG, "Error generating PDF: ${e.message}")
            e.printStackTrace()
        }
    }
    
    private fun showPdfActionDialog(pdfUri: Uri) {
        val options = arrayOf("Lihat PDF", "Bagikan PDF")
        
        AlertDialog.Builder(this)
            .setTitle("PDF Berhasil Dibuat")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> PdfGenerator.viewPdf(this, pdfUri)
                    1 -> PdfGenerator.sharePdf(this, pdfUri, "Bukti Reservasi Servis Mobil")
                }
            }
            .setPositiveButton("Tutup", null)
            .show()
    }
    
    private fun checkStoragePermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            true // Android 10+ menggunakan scoped storage
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    private fun requestStoragePermission() {
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                STORAGE_PERMISSION_CODE
            )
        }
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                generatePdf()
            } else {
                Toast.makeText(this, "Izin penyimpanan diperlukan untuk membuat PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PAYMENT_REQUEST_CODE && resultCode == RESULT_OK) {
            val paymentCompleted = data?.getBooleanExtra("payment_completed", false) ?: false
            if (paymentCompleted) {
                // Refresh halaman untuk memuat status terbaru
                loadReservationDetail(reservationId)
            }
        }
    }

    companion object {
        private const val PAYMENT_REQUEST_CODE = 100
    }
} 