package com.android.reservasiservismobilandroid.ui.reservations

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.reservasiservismobilandroid.ReservationFormActivity
import com.android.reservasiservismobilandroid.adapter.ReservationAdapter
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.FragmentReservationsBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ReservationsFragment : Fragment() {
    private var _binding: FragmentReservationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ReservationAdapter
    private lateinit var sharedPrefs: SharedPrefs
    private var reservations = mutableListOf<Map<String, Any>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReservationsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPrefs = SharedPrefs(requireContext())

        setupRecyclerView()
        setupSwipeRefresh()
        setupClickListeners()
        loadReservations()
    }

    private fun setupRecyclerView() {
        adapter = ReservationAdapter(reservations)
        binding.rvReservations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ReservationsFragment.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadReservations()
        }
    }

    private fun setupClickListeners() {
        binding.fabNewReservation.setOnClickListener {
            startActivity(Intent(requireContext(), ReservationFormActivity::class.java))
        }
    }

    private fun loadReservations() {
        val customerId = sharedPrefs.getUserId()

        RetrofitClient.apiService.getReservations(customerId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseData = response.body()
                    if (responseData?.get("status") == "success") {
                        val data = responseData["data"] as? List<*>
                        if (data != null) {
                            reservations.clear()
                            reservations.addAll(data.filterIsInstance<Map<String, Any>>())
                            adapter.notifyDataSetChanged()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Gagal memuat data reservasi", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat data reservasi", Toast.LENGTH_SHORT).show()
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