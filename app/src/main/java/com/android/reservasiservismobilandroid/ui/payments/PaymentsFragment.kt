package com.android.reservasiservismobilandroid.ui.payments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.reservasiservismobilandroid.PaymentDetailActivity
import com.android.reservasiservismobilandroid.adapter.PaymentAdapter
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.FragmentPaymentsBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class PaymentsFragment : Fragment() {
    private var _binding: FragmentPaymentsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: PaymentAdapter
    private lateinit var sharedPrefs: SharedPrefs
    private var payments = mutableListOf<Map<String, Any>>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPaymentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPrefs = SharedPrefs(requireContext())

        setupRecyclerView()
        setupSwipeRefresh()
        loadPayments()
    }

    private fun setupRecyclerView() {
        adapter = PaymentAdapter(
            payments,
            onViewDetailClick = { payment ->
                val intent = Intent(requireContext(), PaymentDetailActivity::class.java)
                intent.putExtra("service_id", (payment["service_id"] as Double).toInt())
                intent.putExtra("service_date", payment["service_date"].toString())
                intent.putExtra("vehicle_name", payment["vehicle_name"].toString())
                intent.putExtra("plate_number", payment["plate_number"].toString())
                intent.putExtra("package_name", payment["package_name"].toString())
                intent.putExtra("bill", (payment["bill"] as Double).toInt())
                intent.putExtra("method", payment["method"].toString())
                intent.putExtra("pay", (payment["pay"] as Double).toInt())
                intent.putExtra("change", (payment["change"] as Double).toInt())
                intent.putExtra("note", payment["note"]?.toString() ?: "-")
                startActivity(intent)
            }
        )
        binding.rvPayments.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@PaymentsFragment.adapter
        }
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            loadPayments()
        }
    }

    private fun loadPayments() {
        val customerId = sharedPrefs.getUserId()

        RetrofitClient.apiService.getPaymentStatus(customerId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseData = response.body()
                    if (responseData?.get("status") == "success") {
                        val data = responseData["data"] as? List<*>
                        if (data != null) {
                            payments.clear()
                            payments.addAll(data.filterIsInstance<Map<String, Any>>())
                            adapter.notifyDataSetChanged()
                        }
                    } else {
                        Toast.makeText(requireContext(), "Gagal memuat data pembayaran", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat data pembayaran", Toast.LENGTH_SHORT).show()
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