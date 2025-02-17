package com.android.reservasiservismobil.ui.home

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.reservasiservismobil.databinding.FragmentHomeBinding
import com.android.reservasiservismobil.databinding.DialogTopupBinding
import com.android.volley.Request
import com.android.volley.Response
import com.android.reservasiservismobil.MultipartRequest
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var _dialogBinding: DialogTopupBinding? = null
    private var topUpDialog: Dialog? = null
    private val PICK_IMAGE_REQUEST = 1
    private var selectedImageUri: Uri? = null
    private val baseUrl = "http://192.168.197.75/kasir_toko/api_android"

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

        // Get saved user data
        val sharedPref = requireActivity().getSharedPreferences("login_data", 0)
        val nim = sharedPref.getString("nim", "") ?: ""

        // Load saldo
        loadSaldo(nim)

        // Load history
        loadHistory(nim)

        // Setup top up button
        binding.btnTopup.setOnClickListener {
            showTopUpDialog(nim)
        }
    }

    private fun loadSaldo(nim: String) {
        val queue = Volley.newRequestQueue(requireContext())
        val url = "$baseUrl/get_profile.php"

        val stringRequest = object : StringRequest(
            Method.POST,
            url,
            Response.Listener { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        val data = jsonResponse.getJSONObject("data")
                        val saldo = data.getDouble("saldo")
                        val formatRupiah = NumberFormat.getCurrencyInstance(Locale("id", "ID"))
                        binding.tvSaldo.text = formatRupiah.format(saldo)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                return hashMapOf("nim" to nim)
            }
        }
        queue.add(stringRequest)
    }

    private fun loadHistory(nim: String) {
        val queue = Volley.newRequestQueue(requireContext())
        val url = "$baseUrl/get_topup_history.php"

        val stringRequest = object : StringRequest(
            Method.POST,
            url,
            Response.Listener { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        val data = jsonResponse.getJSONArray("data")
                        // Setup RecyclerView with data
                        binding.rvHistory.apply {
                            layoutManager = LinearLayoutManager(context)
                            adapter = TopUpHistoryAdapter(data)
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                return hashMapOf("nim" to nim)
            }
        }
        queue.add(stringRequest)
    }

    private fun showTopUpDialog(nim: String) {
        topUpDialog = Dialog(requireContext())
        _dialogBinding = DialogTopupBinding.inflate(layoutInflater)
        _dialogBinding?.let { dialogBinding ->
            topUpDialog?.setContentView(dialogBinding.root)

            dialogBinding.btnSelectImage.setOnClickListener {
                val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(intent, PICK_IMAGE_REQUEST)
            }

            dialogBinding.btnSubmit.setOnClickListener {
                val nominal = dialogBinding.etNominal.text.toString()
                if (nominal.isNotEmpty() && selectedImageUri != null) {
                    uploadTopUpRequest(nim, nominal, selectedImageUri!!, topUpDialog!!)
                } else {
                    Toast.makeText(context, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show()
                }
            }

            topUpDialog?.show()
        }
    }

    private fun uploadTopUpRequest(nim: String, nominal: String, imageUri: Uri, dialog: Dialog) {
        val queue = Volley.newRequestQueue(requireContext())
        val url = "$baseUrl/topup_request.php"

        val request = object : MultipartRequest(
            Request.Method.POST,
            url,
            Response.Listener { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    if (jsonResponse.getBoolean("success")) {
                        Toast.makeText(context, "Top up request berhasil", Toast.LENGTH_SHORT).show()
                        dialog.dismiss()
                        loadHistory(nim)
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("HomeFragment", "Error Gagal: ${e.message}")
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("HomeFragment", "Error : ${error.message}")
            }) {
            override fun getParams(): Map<String, String> {
                return hashMapOf(
                    "nim" to nim,
                    "nominal" to nominal
                )
            }

            override fun getByteData(): Map<String, DataPart> {
                val imagePath = getRealPathFromURI(imageUri)
                val file = File(imagePath)
                return hashMapOf(
                    "fotobukti" to DataPart(
                        "bukti_${System.currentTimeMillis()}.jpg",
                        file.readBytes()
                    )
                )
            }
        }
        queue.add(request)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.data
            _dialogBinding?.ivBuktiTransfer?.let { imageView ->
                imageView.setImageURI(selectedImageUri)
                imageView.visibility = View.VISIBLE
            }
        }
    }

    private fun getRealPathFromURI(uri: Uri): String {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = requireActivity().contentResolver.query(uri, projection, null, null, null)
        val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.moveToFirst()
        val path = cursor?.getString(columnIndex ?: 0) ?: ""
        cursor?.close()
        return path
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        _dialogBinding = null
        topUpDialog = null
    }
}