package com.android.reservasiservismobilandroid

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityPaymentFormBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import com.midtrans.sdk.corekit.callback.TransactionFinishedCallback
import com.midtrans.sdk.corekit.core.MidtransSDK
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Pembayaran"

        sharedPrefs = SharedPrefs(this)

        // Get data from intent
        serviceId = intent.getIntExtra("service_id", 0)
        bill = intent.getDoubleExtra("bill", 0.0)
        
        if (serviceId <= 0) {
            Toast.makeText(this, "Data pembayaran tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        
        // Inisialisasi Midtrans SDK
        initMidtransSdk()
        
        // Set up button click
        binding.btnPay.setOnClickListener {
            createPayment()
        }
        
        // Get payment details
        getPaymentDetails()
    }

    private fun initMidtransSdk() {
        try {
            val clientKey = "SB-Mid-client-CGmzPqJNEWcIRSj7"
            val baseUrl = "http://192.168.0.56/api_reservasiservismobil/api_android/midtrans_callback.php/"
            
            SdkUIFlowBuilder.init()
                .setContext(this)
                .setClientKey(clientKey)
                .setTransactionFinishedCallback(this)
                .setMerchantBaseUrl(baseUrl)
                .enableLog(true)
                .buildSDK()
                
            Log.d("Midtrans", "SDK Initialized Successfully")
        } catch (e: Exception) {
            Log.e("Midtrans", "Error initializing SDK: ${e.message}")
            e.printStackTrace()
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
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
                            Toast.makeText(this@PaymentFormActivity, "Pembayaran sudah dilakukan", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        "ready" -> {
                            binding.contentLayout.visibility = View.VISIBLE
                            binding.btnPay.isEnabled = true
                            binding.messageText.text = "Data pembayaran siap"
                            
                            val data = responseBody["data"] as? Map<*, *>
                            if (data != null && data.containsKey("bill")) {
                                when (val billValue = data["bill"]) {
                                    is Int -> bill = billValue.toDouble()
                                    is Double -> bill = billValue
                                    is String -> bill = billValue.toDoubleOrNull() ?: bill
                                }
                                
                                val currencyFormat = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                                binding.tvBill.text = "Total: ${currencyFormat.format(bill)}"
                            }
                        }
                        "pending" -> {
                            binding.contentLayout.visibility = View.VISIBLE
                            binding.btnPay.isEnabled = false
                            binding.messageText.text = "Pembayaran belum dapat diproses"
                        }
                        else -> {
                            binding.contentLayout.visibility = View.VISIBLE
                            binding.btnPay.isEnabled = false
                            binding.messageText.text = "Gagal memuat data"
                        }
                    }
                } else {
                    binding.contentLayout.visibility = View.VISIBLE
                    binding.btnPay.isEnabled = false
                    binding.messageText.text = "Gagal memuat data"
                }
            }
            
            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                binding.contentLayout.visibility = View.VISIBLE
                binding.btnPay.isEnabled = false
                binding.messageText.text = "Error koneksi"
                Log.e("Payment", "Error: ${t.message}")
            }
        })
    }

    private fun createPayment() {
        val payment = HashMap<String, String>()
        payment["service_id"] = serviceId.toString()
        payment["bill"] = bill.toString()
        payment["method"] = "Midtrans"
        
        binding.progressBar.visibility = View.VISIBLE
        binding.btnPay.isEnabled = false

        RetrofitClient.apiService.createPayment(payment).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                binding.progressBar.visibility = View.GONE
                binding.btnPay.isEnabled = true

                if (response.isSuccessful) {
                    val responseData = response.body()
                    Log.d("Payment", "Response: $responseData")
                    
                    if (responseData?.get("status") == "success") {
                        val snapToken = responseData["snap_token"]?.toString()
                        
                        if (!snapToken.isNullOrEmpty()) {
                            try {
                                MidtransSDK.getInstance().startPaymentUiFlow(this@PaymentFormActivity, snapToken)
                            } catch (e: Exception) {
                                Log.e("Payment", "Error starting payment: ${e.message}")
                                e.printStackTrace()
                                Toast.makeText(this@PaymentFormActivity, "Error memulai pembayaran", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(this@PaymentFormActivity, "Token kosong", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        val message = responseData?.get("message")?.toString() ?: "Pembayaran gagal"
                        Toast.makeText(this@PaymentFormActivity, message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    try {
                        val errorBody = response.errorBody()?.string()
                        Log.e("Payment", "Error: ${response.code()}, body: $errorBody")
                    } catch (e: Exception) {
                        Log.e("Payment", "Error parsing error body: ${e.message}")
                    }
                    Toast.makeText(this@PaymentFormActivity, "Error: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                binding.progressBar.visibility = View.GONE
                binding.btnPay.isEnabled = true
                Log.e("Payment", "Network error: ${t.message}")
                Toast.makeText(this@PaymentFormActivity, "Error koneksi", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onTransactionFinished(result: TransactionResult?) {
        when (result?.status) {
            TransactionResult.STATUS_SUCCESS -> {
                Toast.makeText(this, "Pembayaran Berhasil!", Toast.LENGTH_LONG).show()
                finish()
            }
            TransactionResult.STATUS_PENDING -> {
                Toast.makeText(this, "Pembayaran dalam proses", Toast.LENGTH_LONG).show()
                finish()
            }
            TransactionResult.STATUS_FAILED -> {
                Toast.makeText(this, "Pembayaran Gagal", Toast.LENGTH_LONG).show()
            }
            else -> {
                Toast.makeText(this, "Transaksi selesai", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 