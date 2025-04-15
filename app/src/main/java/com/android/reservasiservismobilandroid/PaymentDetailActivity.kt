package com.android.reservasiservismobilandroid

import android.Manifest
import android.app.ProgressDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.android.reservasiservismobilandroid.databinding.ActivityPaymentDetailBinding
import com.android.reservasiservismobilandroid.utils.PdfGenerator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class PaymentDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentDetailBinding
    private val TAG = "PaymentDetailActivity"
    private val STORAGE_PERMISSION_CODE = 101
    
    // Data untuk PDF
    private var paymentId: Int = 0
    private var serviceDate: String = "-"
    private var vehicleName: String = "-"
    private var plateNumber: String = "-"
    private var packageName: String = "-"
    private var bill: Int = 0
    private var method: String = "-"
    private var pay: Int = 0
    private var change: Int = 0
    private var note: String = "-"
    private var customerName: String = "-"
    private var paymentDate: String = "-"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detail Pembayaran"

        displayPaymentDetail()

        binding.btnPrintPaymentPdf.setOnClickListener {
            if (checkStoragePermission()) {
                generatePaymentPdf()
            } else {
                requestStoragePermission()
            }
        }
    }

    private fun displayPaymentDetail() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        // Simpan data untuk PDF
        paymentId = intent.getIntExtra("id", 0)
        serviceDate = intent.getStringExtra("service_date") ?: "-"
        vehicleName = intent.getStringExtra("vehicle_name") ?: "-"
        plateNumber = intent.getStringExtra("plate_number") ?: "-"
        packageName = intent.getStringExtra("package_name") ?: "-"
        bill = intent.getIntExtra("bill", 0)
        method = intent.getStringExtra("method") ?: "-"
        pay = intent.getIntExtra("pay", 0)
        change = intent.getIntExtra("change", 0)
        note = intent.getStringExtra("note") ?: "-"
        customerName = intent.getStringExtra("customer_name") ?: "-"
        paymentDate = intent.getStringExtra("created_at") ?: "-"

        binding.apply {
            // Detail Reservasi
            tvServiceDate.text = formatDate(serviceDate, dateFormat, displayFormat)
            tvVehicle.text = "$vehicleName ($plateNumber)"
            tvPackage.text = packageName

            // Detail Pembayaran
            tvBill.text = currencyFormat.format(bill)
            tvPaymentMethod.text = method
            tvStatus.text = "LUNAS"
            
            // Tambahan detail pembayaran
            val additionalDetails = StringBuilder()
                .append("Dibayar: ${currencyFormat.format(pay)}\n")
                .append("Kembalian: ${currencyFormat.format(change)}\n")
                .append("Catatan: $note")
                .toString()
            
            tvAdditionalInfo.text = additionalDetails
        }
    }
    
    private fun generatePaymentPdf() {
        // Show loading
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage("Membuat PDF...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        
        try {
            // Buat dua map untuk data
            val paymentData = mapOf(
                "id" to paymentId,
                "bill" to bill,
                "pay" to pay,
                "change" to change,
                "method" to method,
                "note" to note,
                "created_at" to paymentDate
            )
            
            val serviceData = mapOf(
                "service_date" to serviceDate,
                "vehicle_name" to vehicleName,
                "plate_number" to plateNumber,
                "package_name" to packageName
            )
            
            val pdfUri = PdfGenerator.createPaymentPdf(
                this,
                paymentData,
                serviceData,
                customerName
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
                    1 -> PdfGenerator.sharePdf(this, pdfUri, "Bukti Pembayaran Servis Mobil")
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
                generatePaymentPdf()
            } else {
                Toast.makeText(this, "Izin penyimpanan diperlukan untuk membuat PDF", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatDate(dateStr: String, inputFormat: SimpleDateFormat, outputFormat: SimpleDateFormat): String {
        return try {
            val date = inputFormat.parse(dateStr)
            if (date != null) outputFormat.format(date) else dateStr
        } catch (e: Exception) {
            dateStr
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 