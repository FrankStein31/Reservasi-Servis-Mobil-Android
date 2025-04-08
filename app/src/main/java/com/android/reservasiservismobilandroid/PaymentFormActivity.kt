package com.android.reservasiservismobilandroid

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.reservasiservismobilandroid.api.ApiService
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityPaymentFormBinding
import com.midtrans.sdk.corekit.callback.TransactionFinishedCallback
import com.midtrans.sdk.corekit.core.MidtransSDK
import com.midtrans.sdk.corekit.core.themes.CustomColorTheme
import com.midtrans.sdk.corekit.models.snap.TransactionResult
import com.midtrans.sdk.uikit.SdkUIFlowBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PaymentFormActivity : AppCompatActivity(), TransactionFinishedCallback {

    private lateinit var binding: ActivityPaymentFormBinding
    private lateinit var apiService: ApiService
    private var serviceId: Int = -1
    private var bill: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPaymentFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi Midtrans
        SdkUIFlowBuilder.init()
            .setClientKey("SB-Mid-client-61XuGAwQ8Bj8LxSS") 
            .setContext(this)
            .setTransactionFinishedCallback(this)
            .setColorTheme(CustomColorTheme("#FFE51255", "#B61548", "#FFE51255"))
            .buildSDK()

        // Inisialisasi API Service
        apiService = RetrofitClient.apiService

        // Ambil data dari intent
        serviceId = intent.getIntExtra("service_id", -1)
        bill = intent.getDoubleExtra("bill", 0.0)

        if (serviceId == -1 || bill == 0.0) {
            Toast.makeText(this, "Data servis tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        binding.tvBill.text = "Total Tagihan: Rp${bill}"
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            createPayment()
        }
    }

    private fun createPayment() {
        val paymentData = mapOf(
            "service_id" to serviceId,
            "bill" to bill
        )

        apiService.createPayment(paymentData).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody != null && responseBody["status"] == "success") {
                        val data = responseBody["data"] as? Map<String, Any>
                        val snapToken = data?.get("snap_token") as? String
                        
                        if (snapToken != null) {
                            MidtransSDK.getInstance().startPaymentUiFlow(this@PaymentFormActivity, snapToken)
                        } else {
                            Toast.makeText(this@PaymentFormActivity, "Token pembayaran tidak valid", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@PaymentFormActivity, "Gagal membuat pembayaran", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@PaymentFormActivity, "Terjadi kesalahan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@PaymentFormActivity, "Koneksi gagal", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onTransactionFinished(result: TransactionResult) {
        when (result.status) {
            TransactionResult.STATUS_SUCCESS -> {
                Toast.makeText(this, "Pembayaran berhasil", Toast.LENGTH_SHORT).show()
                finish()
            }
            TransactionResult.STATUS_PENDING -> {
                Toast.makeText(this, "Pembayaran pending", Toast.LENGTH_SHORT).show()
                finish()
            }
            TransactionResult.STATUS_FAILED -> {
                Toast.makeText(this, "Pembayaran gagal", Toast.LENGTH_SHORT).show()
            }
        }
    }
} 