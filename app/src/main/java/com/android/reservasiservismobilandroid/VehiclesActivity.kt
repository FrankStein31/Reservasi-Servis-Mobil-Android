package com.android.reservasiservismobilandroid

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityVehiclesBinding
import com.android.reservasiservismobilandroid.databinding.ItemVehicleBinding
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
        adapter = VehicleAdapter(vehicles) { vehicle ->
            // Edit vehicle
            val intent = Intent(this, VehicleFormActivity::class.java)
            intent.putExtra("vehicle_id", (vehicle["id"] as Double).toInt())
            startActivity(intent)
        }
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

    private inner class VehicleAdapter(
        private val vehicles: List<Map<String, Any>>,
        private val onEditClick: (Map<String, Any>) -> Unit
    ) : RecyclerView.Adapter<VehicleAdapter.VehicleViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VehicleViewHolder {
            val binding = ItemVehicleBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return VehicleViewHolder(binding)
        }

        override fun onBindViewHolder(holder: VehicleViewHolder, position: Int) {
            holder.bind(vehicles[position])
        }

        override fun getItemCount() = vehicles.size

        inner class VehicleViewHolder(private val binding: ItemVehicleBinding) : RecyclerView.ViewHolder(binding.root) {
            fun bind(vehicle: Map<String, Any>) {
                binding.apply {
                    tvVehicleName.text = vehicle["name"].toString()
                    tvVehicleBrand.text = vehicle["brand"].toString()
                    tvPlateNumber.text = vehicle["plate_number"].toString()
                    tvYear.text = vehicle["year"].toString()

                    btnEdit.setOnClickListener { onEditClick(vehicle) }
                    btnDelete.setOnClickListener {
                        deleteVehicle((vehicle["id"] as Double).toInt())
                    }
                }
            }
        }
    }

    private fun deleteVehicle(id: Int) {
        RetrofitClient.apiService.deleteVehicle(mapOf(
            "id" to id.toString(),
            "customer_id" to sharedPrefs.getUserId().toString()
        )).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@VehiclesActivity, "Kendaraan berhasil dihapus", Toast.LENGTH_SHORT).show()
                    loadVehicles()
                } else {
                    Toast.makeText(this@VehiclesActivity, "Gagal menghapus kendaraan", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@VehiclesActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
} 