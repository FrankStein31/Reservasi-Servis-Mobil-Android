package com.android.reservasiservismobil.ui.history

import android.util.Log
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

// Data class to represent a single history transaction
data class HistoryTransaction(
    val id: Int,
    val nim: String,
    val totalHarga: String,
    val date: String,
    val time: String? = null
)

class HistoryViewModel : ViewModel() {
    private val baseUrl = "http://192.168.197.75/kasir_toko/api_android/get_history.php"

    private val _historyList = MutableLiveData<List<HistoryTransaction>>()
    val historyList: LiveData<List<HistoryTransaction>> = _historyList

    private val _errorMessage = MutableLiveData<String>()
    val errorMessage: LiveData<String> = _errorMessage

    fun fetchHistoryForNIM(nim: String) {
        viewModelScope.launch {
            try {
                val result = fetchHistoryData(nim)
                _historyList.value = result
            } catch (e: Exception) {
                _errorMessage.value = "Error fetching history: ${e.message}"
                Log.e("HistoryViewModel", "Error fetching history", e)
            }
        }
    }

    private suspend fun fetchHistoryData(nim: String): List<HistoryTransaction> =
        withContext(Dispatchers.IO) {
            val url = URL("$baseUrl?nim=$nim")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            try {
                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    val reader = BufferedReader(InputStreamReader(connection.inputStream))
                    val response = reader.readText()
                    reader.close()

                    parseHistoryResponse(response)
                } else {
                    throw Exception("Server error: $responseCode")
                }
            } finally {
                connection.disconnect()
            }
        }

    private fun parseHistoryResponse(jsonResponse: String): List<HistoryTransaction> {
        val transactions = mutableListOf<HistoryTransaction>()

        try {
            val jsonObject = JSONObject(jsonResponse)
            val success = jsonObject.getBoolean("success")

            if (success) {
                val dataArray = jsonObject.getJSONArray("data")

                for (i in 0 until dataArray.length()) {
                    val item = dataArray.getJSONObject(i)
                    transactions.add(
                        HistoryTransaction(
                            id = item.getInt("id_h"),
                            nim = item.getString("nim"),
                            totalHarga = item.getString("totalharga"),
                            date = item.getString("date"),
                            time = item.optString("time", null)
                        )
                    )
                }
            } else {
                val message = jsonObject.getString("message")
                throw Exception(message)
            }
        } catch (e: Exception) {
            Log.e("HistoryViewModel", "Parsing error", e)
            throw e
        }

        return transactions
    }
}