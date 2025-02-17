package com.android.reservasiservismobil.ui.profile

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.android.reservasiservismobil.LoginActivity
import com.android.reservasiservismobil.databinding.FragmentProfileBinding
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale

class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val URL = "http://192.168.197.75/kasir_toko/api_android/get_profile.php"

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

        // Get saved user data
        val sharedPref = requireActivity().getSharedPreferences("login_data", 0)
        val nim = sharedPref.getString("nim", "") ?: ""
        val nama = sharedPref.getString("nama", "") ?: ""

        // Set initial data
        binding.tvNama.text = nama
        binding.tvNim.text = nim
        binding.tvNamaHeader.text = nama
        binding.tvNimHeader.text = nim

        // Load profile data including saldo
        loadProfileData(nim)

        // Setup logout button
        binding.btnLogout.setOnClickListener {
            val builder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
            builder.setTitle("Konfirmasi Logout")
            builder.setMessage("Apakah Anda yakin ingin logout?")
            builder.setPositiveButton("Ya") { dialog, _ ->
                // Clear shared preferences
                requireActivity().getSharedPreferences("login_data", 0)
                    .edit()
                    .clear()
                    .apply()

                // Redirect to login
                val intent = Intent(requireActivity(), LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                requireActivity().finish()

                dialog.dismiss() // Tutup dialog
            }
            builder.setNegativeButton("Batal") { dialog, _ ->
                dialog.dismiss() // Tutup dialog jika pengguna memilih batal
            }
            builder.create().show() // Tampilkan dialog
        }
    }

    private fun loadProfileData(nim: String) {
        // Create a request queue
        val queue = Volley.newRequestQueue(requireContext())

        // Create string request
        val stringRequest = object : StringRequest(
            Method.POST,  // HTTP Method
            URL,         // URL
            Response.Listener { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        val data = jsonResponse.getJSONObject("data")
                        val saldo = data.getDouble("saldo")

                        // Format saldo to Indonesian Rupiah
                        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                        binding.tvSaldo.text = formatRupiah.format(saldo)
                    } else {
                        Toast.makeText(context, jsonResponse.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            // Override getParams to send POST parameters
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["nim"] = nim
                return params
            }
        }

        // Add request to queue
        queue.add(stringRequest)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}