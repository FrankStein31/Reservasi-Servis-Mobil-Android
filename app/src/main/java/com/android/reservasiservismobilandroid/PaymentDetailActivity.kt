package com.android.reservasiservismobilandroid

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.reservasiservismobilandroid.databinding.ActivityPaymentDetailBinding
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class PaymentDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPaymentDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Detail Pembayaran"

        displayPaymentDetail()

        binding.btnPrintPdf.setOnClickListener {
            Toast.makeText(this, "Fitur cetak PDF akan segera hadir", Toast.LENGTH_SHORT).show()
        }
    }

    private fun displayPaymentDetail() {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displayFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))

        val serviceDate = intent.getStringExtra("service_date") ?: "-"
        val vehicleName = intent.getStringExtra("vehicle_name") ?: "-"
        val plateNumber = intent.getStringExtra("plate_number") ?: "-"
        val packageName = intent.getStringExtra("package_name") ?: "-"
        val bill = intent.getIntExtra("bill", 0)
        val method = intent.getStringExtra("method") ?: "-"
        val pay = intent.getIntExtra("pay", 0)
        val change = intent.getIntExtra("change", 0)
        val note = intent.getStringExtra("note") ?: "-"

        binding.apply {
            // Detail Reservasi
            tvServiceDate.text = "Tanggal Servis: ${formatDate(serviceDate, dateFormat, displayFormat)}"
            tvVehicle.text = "Kendaraan: $vehicleName ($plateNumber)"
            tvPackage.text = "Paket: $packageName"

            // Detail Pembayaran
            tvBill.text = "Total Biaya: ${currencyFormat.format(bill)}"
            tvPaymentMethod.text = "Metode Pembayaran: $method"
            tvStatus.text = "Status: Lunas"
            
            // Tambahan detail pembayaran
            val additionalDetails = StringBuilder()
                .append("Dibayar: ${currencyFormat.format(pay)}\n")
                .append("Kembalian: ${currencyFormat.format(change)}\n")
                .append("Catatan: $note")
                .toString()
            
            tvAdditionalInfo.text = additionalDetails
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