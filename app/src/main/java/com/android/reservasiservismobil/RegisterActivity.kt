package com.android.reservasiservismobil

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.DefaultRetryPolicy
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class RegisterActivity : AppCompatActivity() {
    private lateinit var etName: EditText
    private lateinit var etNis: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var tvLogin: TextView

    // Ganti URL sesuai dengan alamat server Anda
    private val URL = "http://192.168.197.75/kasir_toko/api_android/register.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        if (getSupportActionBar() != null) {
            getSupportActionBar()?.hide();
        }

        // Inisialisasi views
        etName = findViewById(R.id.etName)
        etNis = findViewById(R.id.etNis)
        etPassword = findViewById(R.id.etPassword)
        btnRegister = findViewById(R.id.btnRegister)
        tvLogin = findViewById(R.id.tvLogin)

        btnRegister.setOnClickListener {
            registerUser()
        }

        tvLogin.setOnClickListener {
            val intent = Intent(this@RegisterActivity, LoginActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerUser() {
        val name = etName.text.toString().trim()
        val nim = etNis.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validasi input
        if (name.isEmpty() || nim.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Mohon isi semua field", Toast.LENGTH_SHORT).show()
            return
        }

        // Membuat request ke server
        val stringRequest = object : StringRequest(
            Request.Method.POST, URL,
            Response.Listener { response ->
                try {
                    // Log response untuk debugging
                    Log.d("RegisterResponse", response)

                    val jsonResponse = JSONObject(response)
                    val success = jsonResponse.getBoolean("success")
                    val message = jsonResponse.getString("message")

                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

                    if (success) {
                        finish() // Kembali ke activity sebelumnya
                    }
                } catch (e: Exception) {
                    Log.e("RegisterError", "Error parsing JSON: ${e.message}")
                    Toast.makeText(this, "Terjadi kesalahan sistem", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Log.e("RegisterError", "Volley Error: ${error.message}")
                Toast.makeText(this, "Gagal terhubung ke server", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["nama"] = name
                params["nim"] = nim
                params["password"] = password
                return params
            }
        }

        // Set timeout yang lebih lama (opsional)
        stringRequest.retryPolicy = DefaultRetryPolicy(
            30000, // 30 detik timeout
            0, // no retry
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        // Menambahkan request ke RequestQueue
        Volley.newRequestQueue(this).add(stringRequest)
    }
}