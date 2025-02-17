package com.android.reservasiservismobil.ui.history

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.reservasiservismobil.databinding.FragmentHistoryBinding

class HistoryFragment : Fragment() {
    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var historyViewModel: HistoryViewModel
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize ViewModel - CORRECTED LINE
        historyViewModel = ViewModelProvider(this).get(HistoryViewModel::class.java)

        // Setup RecyclerView
        historyAdapter = HistoryAdapter(emptyList())
        binding.recyclerViewHistory.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = historyAdapter
        }

        // Get NIM from SharedPreferences
        val sharedPref = requireActivity().getSharedPreferences("login_data", Context.MODE_PRIVATE)
        val nim = sharedPref.getString("nim", "") ?: ""

        // Show loading
        binding.progressBarHistory.visibility = View.VISIBLE

        // Observe history data
        historyViewModel.historyList.observe(viewLifecycleOwner) { transactions ->
            binding.progressBarHistory.visibility = View.GONE

            if (transactions.isNotEmpty()) {
                historyAdapter.updateData(transactions)
                binding.textNoHistory.visibility = View.GONE
                binding.recyclerViewHistory.visibility = View.VISIBLE
            } else {
                binding.textNoHistory.visibility = View.VISIBLE
                binding.recyclerViewHistory.visibility = View.GONE
            }
        }

        // Observe error messages
        historyViewModel.errorMessage.observe(viewLifecycleOwner) { errorMsg ->
            binding.progressBarHistory.visibility = View.GONE
            Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_SHORT).show()
        }

        // Fetch history
        if (nim.isNotEmpty()) {
            historyViewModel.fetchHistoryForNIM(nim)
        } else {
            Toast.makeText(requireContext(), "NIM tidak ditemukan", Toast.LENGTH_SHORT).show()
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}