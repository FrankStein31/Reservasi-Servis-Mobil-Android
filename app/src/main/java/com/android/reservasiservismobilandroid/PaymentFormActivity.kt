package com.android.reservasiservismobilandroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityPaymentFormBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import com.midtrans.sdk.corekit.callback.TransactionFinishedCallback
import com.midtrans.sdk.corekit.core.MidtransSDK
import com.midtrans.sdk.corekit.core.TransactionRequest
import com.midtrans.sdk.corekit.core.themes.CustomColorTheme
import com.midtrans.sdk.corekit.models.BillingAddress
import com.midtrans.sdk.corekit.models.CustomerDetails
import com.midtrans.sdk.corekit.models.ItemDetails
import com.midtrans.sdk.corekit.models.ShippingAddress
import com.midtrans.sdk.corekit.models.snap.TransactionResult
import com.midtrans.sdk.uikit.SdkUIFlowBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class PaymentFormActivity : AppCompatActivity(), TransactionFinishedCallback {

    private lateinit var binding: ActivityPaymentFormBinding
    private lateinit var sharedPrefs: SharedPrefs
    private var serviceId: Int = 0
    private var bill: Double = 0.0
    private var packagePrice: Double = 0.0
    private var serviceFee: Double = 50000.0
    private var isLoading: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pembayaran"

        sharedPrefs = SharedPrefs(this)

        // Set up Midtrans SDK
        initMidtransSDK()

        // Get data from intent
        serviceId = intent.getIntExtra("service_id", 0)
        bill = intent.getDoubleExtra("bill", 0.0)
        
        if (serviceId <= 0) {
            Toast.makeText(this, "Data pembayaran tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Set up button click
        binding.btnPay.setOnClickListener {
            createPayment()
        }
        
        // Get payment details
        getPaymentDetails()
    }

    private fun initMidtransSDK() {
        SdkUIFlowBuilder.init()
            .setClientKey("SB-Mid-client-zcwBqnfkJXTUkl5i")
            .setContext(this)
            .setTransactionFinishedCallback(this)
            .setMerchantBaseUrl("https://api.sandbox.midtrans.com")
            .enableLog(true)
            .setColorTheme(CustomColorTheme("#6200EE", "#3700B3", "#BB86FC"))
            .buildSDK()
    }

    private fun getPaymentDetails() {
        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.GONE
        
        RetrofitClient.apiService.getPaymentDetail(serviceId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                binding.progressBar.visibility = View.GONE
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    when (responseBody?.get("status")) {
                        "success" -> {
                            // Pembayaran sudah dilakukan
                            Toast.makeText(this@PaymentFormActivity, "Pembayaran sudah dilakukan sebelumnya", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        "ready" -> {
                            // Data pembayaran siap, bisa langsung bayar
                            binding.contentLayout.visibility = View.VISIBLE
                            binding.btnPay.isEnabled = true
                            binding.messageText.text = "Data pembayaran siap"
                            
                            val data = responseBody["data"] as? Map<*, *>
                            if (data != null && data.containsKey("bill")) {
                                bill = when (val billValue = data["bill"]) {
                                    is Int -> billValue.toDouble()
                                    is Double -> billValue
                                    is String -> billValue.toDoubleOrNull() ?: bill
                                    else -> bill
                                }
                                
                                if (bill > 0) {
                                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                                    binding.tvBill.text = "Total Tagihan: ${currencyFormat.format(bill)}"
                                }
                            }
                        }
                        "pending" -> {
                            // Belum bisa bayar
                            binding.contentLayout.visibility = View.VISIBLE
                            binding.btnPay.isEnabled = false
                            binding.messageText.text = responseBody["message"] as? String ?: "Pembayaran belum dapat diproses"
                            
                            val data = responseBody["data"] as? Map<*, *>
                            if (data != null && data.containsKey("bill")) {
                                bill = when (val billValue = data["bill"]) {
                                    is Int -> billValue.toDouble()
                                    is Double -> billValue
                                    is String -> billValue.toDoubleOrNull() ?: bill
                                    else -> bill
                                }
                                
                                if (bill > 0) {
                                    val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                                    binding.tvBill.text = "Total Tagihan: ${currencyFormat.format(bill)}"
                                }
                            }
                        }
                        else -> {
                            binding.contentLayout.visibility = View.VISIBLE
                            binding.btnPay.isEnabled = false
                            binding.messageText.text = responseBody?.get("message") as? String ?: "Gagal memuat data pembayaran"
                        }
                    }
                } else {
                    binding.contentLayout.visibility = View.VISIBLE
                    binding.btnPay.isEnabled = false
                    binding.messageText.text = "Gagal memuat data pembayaran"
                }
            }
            
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE
                binding.btnPay.isEnabled = false
                binding.messageText.text = "Error: ${t.message}"
            }
        })
    }

    private fun createPayment() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnPay.isEnabled = false
        
        val paymentData = mapOf(
            "service_id" to serviceId.toString(),
            "bill" to bill.toString(),
            "method" to "Midtrans"
        )
        
        RetrofitClient.apiService.createPayment(paymentData).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                binding.progressBar.visibility = View.GONE
                binding.btnPay.isEnabled = true
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.get("status") == "success") {
                        val data = responseBody["data"] as? Map<*, *>
                        if (data != null && data.containsKey("snap_token")) {
                            val snapToken = data["snap_token"] as String
                            MidtransSDK.getInstance().startPaymentUiFlow(this@PaymentFormActivity, snapToken)
                        } else {
                            Toast.makeText(this@PaymentFormActivity, "Snap token tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@PaymentFormActivity, responseBody?.get("message") as? String ?: "Gagal memproses pembayaran", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@PaymentFormActivity, "Gagal memproses pembayaran", Toast.LENGTH_SHORT).show()
                }
            }
            
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                binding.btnPay.isEnabled = true
                Toast.makeText(this@PaymentFormActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onTransactionFinished(result: TransactionResult) {
        if (result.response != null) {
            when (result.status) {
                TransactionResult.STATUS_SUCCESS -> {
                    Toast.makeText(this, "Pembayaran Berhasil", Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                }
                TransactionResult.STATUS_PENDING -> {
                    Toast.makeText(this, "Pembayaran Tertunda, silahkan selesaikan dalam 24 jam", Toast.LENGTH_LONG).show()
                    setResult(RESULT_OK)
                    finish()
                }
                TransactionResult.STATUS_FAILED -> {
                    Toast.makeText(this, "Pembayaran Gagal: ${result.response.statusMessage}", Toast.LENGTH_LONG).show()
                }
                TransactionResult.STATUS_INVALID -> {
                    Toast.makeText(this, "Pembayaran Tidak Valid", Toast.LENGTH_LONG).show()
                }
            }
        } else {
            Toast.makeText(this, "Pembayaran Dibatalkan", Toast.LENGTH_LONG).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 