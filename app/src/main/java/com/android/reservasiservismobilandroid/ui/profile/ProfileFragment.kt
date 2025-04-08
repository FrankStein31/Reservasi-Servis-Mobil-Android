package com.android.reservasiservismobilandroid.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.reservasiservismobilandroid.EditProfileActivity
import com.android.reservasiservismobilandroid.LoginActivity
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.FragmentProfileBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var sharedPrefs: SharedPrefs

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        sharedPrefs = SharedPrefs(requireContext())
        loadProfile()
        setupClickListeners()
    }

    private fun loadProfile() {
        val customerId = sharedPrefs.getUserId()
        RetrofitClient.apiService.getProfile(customerId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.get("status") == "success") {
                        val userData = responseBody["data"] as? Map<*, *>
                        if (userData != null) {
                            binding.apply {
                                tvName.text = userData["name"].toString()
                                tvEmail.text = userData["email"].toString()
                                tvPhone.text = "No. HP: ${userData["phone"]?.toString() ?: "-"}"
                                tvAddress.text = "Alamat: ${userData["address"]?.toString() ?: "-"}"
                            }
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Gagal memuat profil", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun setupClickListeners() {
        binding.apply {
            btnEditProfile.setOnClickListener {
                startActivity(Intent(requireContext(), EditProfileActivity::class.java))
            }

            btnLogout.setOnClickListener {
                sharedPrefs.clearUser()
                startActivity(Intent(requireContext(), LoginActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                requireActivity().finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        loadProfile() // Refresh data setelah kembali dari edit profile
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
} 