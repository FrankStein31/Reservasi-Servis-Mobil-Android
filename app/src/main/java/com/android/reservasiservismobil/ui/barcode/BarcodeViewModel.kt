package com.android.reservasiservismobil.ui.barcode

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import android.util.Log

data class PaymentResult(
    val success: Boolean,
    val message: String
)

class BarcodeViewModel : ViewModel() {
    private val baseUrl = "http://192.168.197.75/kasir_toko/api_android/payment.php"

    fun processPayment(nim: String, amount: String, password: String): LiveData<PaymentResult> {
        val resultLiveData = MutableLiveData<PaymentResult>()

        viewModelScope.launch {
            val result = performPaymentRequest(nim, amount, password)
            resultLiveData.value = result
        }

        return resultLiveData
    }

    // Modify performPaymentRequest to add more robust error handling
    private suspend fun performPaymentRequest(nim: String, amount: String, password: String): PaymentResult =
        withContext(Dispatchers.IO) {
            try {
                val url = URL(baseUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.connectTimeout = 10000 // 10 seconds
                connection.readTimeout = 10000 // 10 seconds

                val postData = "nim=$nim&password=$password&amount=$amount"
                connection.outputStream.write(postData.toByteArray())

                val responseCode = connection.responseCode
                Log.e("PaymentRequest", "Response Code: $responseCode")

                val inputStream = if (responseCode == HttpURLConnection.HTTP_OK) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }

                val reader = BufferedReader(InputStreamReader(inputStream))
                val response = reader.readText()

                Log.e("PaymentRequest", "Raw Response: $response")

                try {
                    // Attempt to parse JSON
                    val jsonResponse = JSONObject(response)
                    PaymentResult(
                        success = jsonResponse.optBoolean("success", false),
                        message = jsonResponse.optString("message", "Unknown error")
                    )
                } catch (jsonException: Exception) {
                    // If JSON parsing fails, return a detailed error
                    Log.e("PaymentRequest", "JSON Parsing Error: ${jsonException.message}")
                    PaymentResult(
                        success = false,
                        message = "Invalid server response: $response"
                    )
                }
            } catch (e: Exception) {
                Log.e("PaymentRequest", "Network Error: ${e.message}")
                PaymentResult(false, "Network Error: ${e.message}")
            }
        }
}