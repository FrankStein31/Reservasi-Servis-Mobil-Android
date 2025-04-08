package com.android.reservasiservismobilandroid.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.reservasiservismobilandroid.VehicleFormActivity
import com.android.reservasiservismobilandroid.VehiclesActivity
import com.android.reservasiservismobilandroid.ReservationFormActivity
import com.android.reservasiservismobilandroid.ReservationsActivity
import com.android.reservasiservismobilandroid.PaymentsActivity
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.FragmentHomeBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPrefs: SharedPrefs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        sharedPrefs = SharedPrefs(requireContext())
        
        binding.swipeRefresh.setOnRefreshListener {
            loadDashboardData()
        }

        setupClickListeners()
        loadDashboardData()
    }

    private fun setupClickListeners() {
        binding.apply {
            cardVehicles.setOnClickListener {
                startActivity(Intent(requireContext(), VehiclesActivity::class.java))
            }

            btnAddVehicle.setOnClickListener {
                startActivity(Intent(requireContext(), VehicleFormActivity::class.java))
            }

            cardReservations.setOnClickListener {
                startActivity(Intent(requireContext(), ReservationsActivity::class.java))
            }

            btnNewReservation.setOnClickListener {
                startActivity(Intent(requireContext(), ReservationFormActivity::class.java))
            }

            cardPayments.setOnClickListener {
                startActivity(Intent(requireContext(), PaymentsActivity::class.java))
            }

            btnViewPayments.setOnClickListener {
                startActivity(Intent(requireContext(), PaymentsActivity::class.java))
            }
        }
    }

    private fun loadDashboardData() {
        val customerId = sharedPrefs.getUserId()

        RetrofitClient.apiService.getVehicles(customerId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val data = response.body()?.get("data") as? List<*>
                    binding.tvVehicleCount.text = "${data?.size ?: 0} kendaraan terdaftar"
                }
                binding.swipeRefresh.isRefreshing = false
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(requireContext(), "Gagal memuat data: ${t.message}", Toast.LENGTH_SHORT).show()
                binding.swipeRefresh.isRefreshing = false
            }
        })

        RetrofitClient.apiService.getReservations(customerId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val data = response.body()?.get("data") as? List<*>
                    binding.tvReservationCount.text = "${data?.size ?: 0} reservasi aktif"
                }
                binding.swipeRefresh.isRefreshing = false
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(requireContext(), "Gagal memuat data: ${t.message}", Toast.LENGTH_SHORT).show()
                binding.swipeRefresh.isRefreshing = false
            }
        })

        RetrofitClient.apiService.getPaymentStatus(customerId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val data = response.body()?.get("data") as? List<*>
                    binding.tvPaymentCount.text = "${data?.size ?: 0} pembayaran"
                }
                binding.swipeRefresh.isRefreshing = false
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(requireContext(), "Gagal memuat data: ${t.message}", Toast.LENGTH_SHORT).show()
                binding.swipeRefresh.isRefreshing = false
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 