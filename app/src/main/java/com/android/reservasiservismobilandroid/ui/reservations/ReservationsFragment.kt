package com.android.reservasiservismobilandroid.ui.reservations

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
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
        adapter = ReservationAdapter(reservations) { reservationId ->
            showCancelDialog(reservationId)
        }
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
        binding.swipeRefresh.isRefreshing = true
        
        val customerId = sharedPrefs.getUserId()
        
        RetrofitClient.apiService.getReservations(customerId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                binding.swipeRefresh.isRefreshing = false
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.get("status") == "success") {
                        val data = responseBody["data"] as? List<*>
                        if (data != null && data.isNotEmpty()) {
                            // Tampilkan data reservasi
                            val reservations = data.filterIsInstance<Map<String, Any>>()
                            displayReservations(reservations)
                            binding.tvEmpty.visibility = View.GONE
                        } else {
                            // Tampilkan pesan kosong
                            binding.tvEmpty.visibility = View.VISIBLE
                        }
                    } else {
                        binding.tvEmpty.visibility = View.VISIBLE
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat reservasi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                binding.swipeRefresh.isRefreshing = false
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayReservations(reservations: List<Map<String, Any>>) {
        if (reservations.isEmpty()) {
            binding.tvEmpty.visibility = View.VISIBLE
            binding.rvReservations.visibility = View.GONE
        } else {
            binding.tvEmpty.visibility = View.GONE
            binding.rvReservations.visibility = View.VISIBLE
            this.reservations.clear()
            this.reservations.addAll(reservations)
            adapter.notifyDataSetChanged()
        }
    }

    private fun showCancelDialog(reservationId: Int) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Konfirmasi")
        builder.setMessage("Apakah Anda yakin ingin membatalkan reservasi ini?")
        builder.setPositiveButton("Ya") { _, _ ->
            cancelReservation(reservationId)
        }
        builder.setNegativeButton("Tidak") { dialog, _ ->
            dialog.dismiss()
        }
        builder.create().show()
    }

    private fun cancelReservation(reservationId: Int) {
        val data = mapOf("id" to reservationId.toString())
        
        RetrofitClient.apiService.deleteReservation(data).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.get("status") == "success") {
                        Toast.makeText(requireContext(), "Reservasi berhasil dibatalkan", Toast.LENGTH_SHORT).show()
                        loadReservations() // Refresh data
                    } else {
                        val message = responseBody?.get("message")?.toString() ?: "Gagal membatalkan reservasi"
                        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal membatalkan reservasi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        loadReservations() // Refresh saat kembali ke fragment
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 