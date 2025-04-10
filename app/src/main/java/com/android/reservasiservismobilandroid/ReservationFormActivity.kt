package com.android.reservasiservismobilandroid

import android.app.Activity
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityReservationFormBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ReservationFormActivity : AppCompatActivity() {
    private lateinit var binding: ActivityReservationFormBinding
    private lateinit var sharedPrefs: SharedPrefs
    private var vehicles = mutableListOf<Map<String, Any>>()
    private var packages = mutableListOf<Map<String, Any>>()
    private var selectedVehicleId: Int = -1
    private var selectedPackageId: Int = -1
    private var selectedDate: String = ""
    
    private val timeSlots = listOf("09:00:00", "10:00:00", "11:00:00", "13:00:00", "14:00:00", "15:00:00")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReservationFormBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = SharedPrefs(this)

        setupDatePicker()
        setupTimeSpinner()
        loadVehicles()
        loadPackages()

        binding.btnSave.setOnClickListener {
            createReservation()
        }
    }

    private fun setupDatePicker() {
        binding.etDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(
                this,
                { _, selectedYear, selectedMonth, selectedDay ->
                    calendar.set(selectedYear, selectedMonth, selectedDay)
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                    selectedDate = dateFormat.format(calendar.time)
                    binding.etDate.setText(selectedDate)
                },
                year,
                month,
                day
            )
            
            // Set minimal date to tomorrow
            calendar.add(Calendar.DAY_OF_MONTH, 1)
            datePickerDialog.datePicker.minDate = calendar.timeInMillis
            
            datePickerDialog.show()
        }
    }

    private fun setupTimeSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, timeSlots.map { 
            it.substring(0, 5)
        })
        binding.spinnerTime.setAdapter(adapter)
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
                            
                            if (vehicles.isEmpty()) {
                                Toast.makeText(this@ReservationFormActivity, 
                                    "Anda belum memiliki kendaraan. Silakan tambahkan kendaraan terlebih dahulu.", 
                                    Toast.LENGTH_LONG).show()
                                finish()
                                return
                            }
                            
                            setupVehicleSpinner()
                        }
                    } else {
                        Toast.makeText(this@ReservationFormActivity, "Gagal memuat data kendaraan", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ReservationFormActivity, "Gagal memuat data kendaraan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@ReservationFormActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupVehicleSpinner() {
        val vehicleNames = vehicles.map { "${it["name"]} (${it["plate_number"]})" }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, vehicleNames)
        binding.spinnerVehicle.setAdapter(adapter)
        
        binding.spinnerVehicle.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            selectedVehicleId = try {
                when (val id = vehicles[position]["id"]) {
                    is Double -> id.toInt()
                    is String -> id.toInt()
                    else -> -1
                }
            } catch (e: Exception) {
                -1
            }
        }
        
        // Select first vehicle by default
        if (vehicles.isNotEmpty()) {
            binding.spinnerVehicle.setText(vehicleNames[0], false)
            selectedVehicleId = try {
                when (val id = vehicles[0]["id"]) {
                    is Double -> id.toInt()
                    is String -> id.toInt()
                    else -> -1
                }
            } catch (e: Exception) {
                -1
            }
        }
    }

    private fun loadPackages() {
        RetrofitClient.apiService.getPackages().enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseData = response.body()
                    
                    // Periksa status: bisa berupa Boolean 'true' atau String "success"
                    val status = responseData?.get("status")
                    val isSuccess = status == true || status == "success"
                    
                    if (isSuccess) {
                        val data = responseData?.get("data") as? List<*>
                        if (data != null) {
                            packages.clear()
                            packages.addAll(data.filterIsInstance<Map<String, Any>>())
                            setupPackageSpinner()
                        }
                    } else {
                        Toast.makeText(this@ReservationFormActivity, "Gagal memuat data paket servis", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ReservationFormActivity, "Gagal memuat data paket servis", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@ReservationFormActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupPackageSpinner() {
        val packageNames = packages.map { it["name"].toString() }
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, packageNames)
        binding.spinnerPackage.setAdapter(adapter)
        
        binding.spinnerPackage.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            selectedPackageId = try {
                when (val id = packages[position]["id"]) {
                    is Double -> id.toInt()
                    is String -> id.toInt()
                    else -> -1
                }
            } catch (e: Exception) {
                -1
            }
        }
        
        // Select first package by default
        if (packages.isNotEmpty()) {
            binding.spinnerPackage.setText(packageNames[0], false)
            selectedPackageId = try {
                when (val id = packages[0]["id"]) {
                    is Double -> id.toInt()
                    is String -> id.toInt()
                    else -> -1
                }
            } catch (e: Exception) {
                -1
            }
        }
    }

    private fun createReservation() {
        val complaint = binding.etComplaint.text.toString()
        val timePosition = timeSlots.indexOf(binding.spinnerTime.text.toString() + ":00")
        val time = if (timePosition >= 0) timeSlots[timePosition] else "09:00:00"

        if (selectedVehicleId == -1 || selectedPackageId == -1 || selectedDate.isEmpty() || complaint.isEmpty()) {
            Toast.makeText(this, "Semua field harus diisi", Toast.LENGTH_SHORT).show()
            return
        }

        val reservationData = mapOf(
            "customer_id" to sharedPrefs.getUserId().toString(),
            "vehicle_id" to selectedVehicleId.toString(),
            "package_id" to selectedPackageId.toString(),
            "vehicle_complaint" to complaint,
            "reservation_date" to selectedDate,
            "reservation_time" to time
        )

        RetrofitClient.apiService.createReservation(reservationData).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseData = response.body()
                    if (responseData?.get("status") == "success") {
                        Toast.makeText(this@ReservationFormActivity, "Reservasi berhasil dibuat", Toast.LENGTH_SHORT).show()
                        setResult(Activity.RESULT_OK)
                        finish()
                    } else {
                        Toast.makeText(this@ReservationFormActivity, responseData?.get("message") as? String 
                            ?: "Gagal membuat reservasi", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@ReservationFormActivity, "Gagal membuat reservasi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@ReservationFormActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 