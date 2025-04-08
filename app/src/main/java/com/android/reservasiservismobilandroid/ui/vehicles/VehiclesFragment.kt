package com.android.reservasiservismobilandroid.ui.vehicles

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.reservasiservismobilandroid.VehicleFormActivity
import com.android.reservasiservismobilandroid.adapter.VehicleAdapter
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.FragmentVehiclesBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class VehiclesFragment : Fragment() {
    private var _binding: FragmentVehiclesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: VehicleAdapter
    private lateinit var sharedPrefs: SharedPrefs
    private var vehicles = mutableListOf<Map<String, Any>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVehiclesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPrefs = SharedPrefs(requireContext())

        setupRecyclerView()
        setupSwipeRefresh()
        setupClickListeners()
        loadVehicles()
    }

    private fun setupRecyclerView() {
        adapter = VehicleAdapter(vehicles) { vehicle ->
            // Edit vehicle
            val intent = Intent(requireContext(), VehicleFormActivity::class.java)
            intent.putExtra("vehicle_id", (vehicle["id"] as Double).toInt())
            startActivity(intent)
        }
        binding.rvVehicles.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@VehiclesFragment.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadVehicles()
        }
    }

    private fun setupClickListeners() {
        binding.fabAddVehicle.setOnClickListener {
            startActivity(Intent(requireContext(), VehicleFormActivity::class.java))
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
                        Toast.makeText(requireContext(), "Gagal memuat data kendaraan", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat data kendaraan", Toast.LENGTH_SHORT).show()
                }
                binding.swipeRefresh.isRefreshing = false
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(requireContext(), "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
                binding.swipeRefresh.isRefreshing = false
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 