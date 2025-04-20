package com.android.reservasiservismobilandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.reservasiservismobilandroid.adapter.VehicleAdapter
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityVehiclesBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VehiclesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVehiclesBinding
    private lateinit var adapter: VehicleAdapter
    private lateinit var sharedPrefs: SharedPrefs
    private var vehicles = mutableListOf<Map<String, Any>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVehiclesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = SharedPrefs(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        setupRecyclerView()
        loadVehicles()

        binding.fabAddVehicle.setOnClickListener {
            startActivity(Intent(this, VehicleFormActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        adapter = VehicleAdapter(vehicles, 
            onEditClick = { vehicle ->
                // Edit vehicle
                val intent = Intent(this, VehicleFormActivity::class.java)
                intent.putExtra("vehicle_id", (vehicle["id"] as Double).toInt())
                startActivity(intent)
            },
            onDeleteClick = { vehicle ->
                // Show confirmation dialog before deleting
                android.app.AlertDialog.Builder(this)
                    .setTitle("Konfirmasi Hapus")
                    .setMessage("Apakah Anda yakin ingin menghapus kendaraan ini?")
                    .setPositiveButton("Ya") { _, _ ->
                        deleteVehicle((vehicle["id"] as Double).toInt())
                    }
                    .setNegativeButton("Tidak", null)
                    .show()
            }
        )
        binding.rvVehicles.apply {
            layoutManager = LinearLayoutManager(this@VehiclesActivity)
            adapter = this@VehiclesActivity.adapter
        }
    }

    private fun loadVehicles() {
        val customerId = sharedPrefs.getUserId()

        RetrofitClient.apiService.getVehicles(customerId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseData = response.body()
                    if (responseData?.get("status") == "success") {
                        val data = responseData["data"] as? List<*>
                        if (data != null) {
                            vehicles.clear()
                            vehicles.addAll(data.filterIsInstance<Map<String, Any>>())
                            adapter.notifyDataSetChanged()
                        }
                    } else {
                        Toast.makeText(this@VehiclesActivity, "Gagal memuat data kendaraan", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@VehiclesActivity, "Gagal memuat data kendaraan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@VehiclesActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadVehicles()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun deleteVehicle(id: Int) {
        // Tampilkan loading
        val progressDialog = android.app.ProgressDialog(this)
        progressDialog.setMessage("Menghapus kendaraan...")
        progressDialog.setCancelable(false)
        progressDialog.show()
        
        // Log untuk debugging
        android.util.Log.d("VehiclesActivity", "Menghapus kendaraan dengan ID: $id")
        
        RetrofitClient.apiService.deleteVehicle(mapOf(
            "id" to id.toString(),
            "customer_id" to sharedPrefs.getUserId().toString()
        )).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                progressDialog.dismiss()
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    android.util.Log.d("VehiclesActivity", "Response: $responseBody")
                    
                    if (responseBody?.get("status") == "success") {
                        Toast.makeText(this@VehiclesActivity, "Kendaraan berhasil dihapus", Toast.LENGTH_SHORT).show()
                        loadVehicles() // Refresh data
                    } else {
                        val message = responseBody?.get("message")?.toString() ?: "Gagal menghapus kendaraan"
                        Toast.makeText(this@VehiclesActivity, message, Toast.LENGTH_SHORT).show()
                        android.util.Log.e("VehiclesActivity", "Error: $message")
                    }
                } else {
                    try {
                        val errorBody = response.errorBody()?.string()
                        android.util.Log.e("VehiclesActivity", "Error: ${response.code()}, body: $errorBody")
                        Toast.makeText(this@VehiclesActivity, "Gagal menghapus kendaraan: ${response.code()}", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        android.util.Log.e("VehiclesActivity", "Error parsing error body: ${e.message}")
                        Toast.makeText(this@VehiclesActivity, "Gagal menghapus kendaraan", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                progressDialog.dismiss()
                android.util.Log.e("VehiclesActivity", "Network error: ${t.message}")
                Toast.makeText(this@VehiclesActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
} 