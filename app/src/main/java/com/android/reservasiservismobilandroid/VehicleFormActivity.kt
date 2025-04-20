package com.android.reservasiservismobilandroid

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityVehicleFormBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VehicleFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityVehicleFormBinding
    private lateinit var sharedPrefs: SharedPrefs
    private var vehicleId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVehicleFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = SharedPrefs(this)
        vehicleId = intent.getIntExtra("vehicle_id", -1).takeIf { it != -1 }

        // Mengatur toolbar dan tombol kembali
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = if (vehicleId != null) "Edit Kendaraan" else "Tambah Kendaraan"

        if (vehicleId != null) {
            loadVehicle(vehicleId!!)
        }

        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString()
            val brand = binding.etBrand.text.toString()
            val year = binding.etYear.text.toString()
            val plateNumber = binding.etPlateNumber.text.toString()
            val chassisNumber = binding.etChassisNumber.text.toString()

            if (name.isEmpty() || brand.isEmpty() || plateNumber.isEmpty()) {
                Toast.makeText(this, "Mohon isi semua field wajib", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val vehicle = mapOf(
                "id" to (vehicleId ?: 0).toString(),
                "customer_id" to sharedPrefs.getUserId().toString(),
                "name" to name,
                "brand" to brand,
                "year" to year,
                "plate_number" to plateNumber,
                "chassis_number" to chassisNumber
            )

            if (vehicleId != null) {
                updateVehicle(vehicle)
            } else {
                createVehicle(vehicle)
            }
        }
    }

    private fun loadVehicle(id: Int) {
        RetrofitClient.apiService.getVehicles(sharedPrefs.getUserId()).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val data = response.body()?.get("data") as? List<*>
                    if (data != null) {
                        val vehicle = data.filterIsInstance<Map<String, Any>>()
                            .find { (it["id"] as Double).toInt() == id }
                        if (vehicle != null) {
                            binding.etName.setText(vehicle["name"].toString())
                            binding.etBrand.setText(vehicle["brand"].toString())
                            binding.etYear.setText(vehicle["year"].toString())
                            binding.etPlateNumber.setText(vehicle["plate_number"].toString())
                            binding.etChassisNumber.setText(vehicle["chassis_number"].toString())
                        }
                    }
                } else {
                    Toast.makeText(this@VehicleFormActivity, "Gagal memuat data kendaraan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@VehicleFormActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun createVehicle(vehicle: Map<String, String>) {
        RetrofitClient.apiService.addVehicle(vehicle).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@VehicleFormActivity, "Kendaraan berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@VehicleFormActivity, "Gagal menambahkan kendaraan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@VehicleFormActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateVehicle(vehicle: Map<String, String>) {
        RetrofitClient.apiService.updateVehicle(vehicle).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@VehicleFormActivity, "Kendaraan berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this@VehicleFormActivity, "Gagal memperbarui kendaraan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@VehicleFormActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
    
    // Menangani tombol kembali pada action bar
    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 