package com.android.reservasiservismobil

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject

class LoginActivity : AppCompatActivity() {
    private lateinit var etNim: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvRegister: TextView

    // Ganti URL sesuai dengan alamat server Anda
    private val URL = "http://192.168.197.75/kasir_toko/api_android/login.php"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        if (getSupportActionBar() != null) {
            getSupportActionBar()?.hide();
        }

        // Inisialisasi views
        etNim = findViewById(R.id.etNim)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegister)

        // Handle login button click
        btnLogin.setOnClickListener {
            loginUser()
        }

        // Handle register text click
        tvRegister.setOnClickListener {
            val intent = Intent(this@LoginActivity, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun loginUser() {
        val nim = etNim.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validasi input
        if (nim.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Mohon isi semua field", Toast.LENGTH_SHORT).show()
            return
        }

        // Membuat request ke server
        val stringRequest = object : StringRequest(
            Request.Method.POST, URL,
            Response.Listener { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    val success = jsonResponse.getBoolean("success")
                    val message = jsonResponse.getString("message")

                    if (success) {
                        // Ambil data user
                        val userData = jsonResponse.getJSONObject("data")
                        val nama = userData.getString("nama")
                        val nim = userData.getString("nim")

                        // Simpan data login ke SharedPreferences
                        val sharedPref = getSharedPreferences("login_data", MODE_PRIVATE)
                        with(sharedPref.edit()) {
                            putString("nama", nama)
                            putString("nim", nim)
                            putBoolean("is_logged_in", true)
                            apply()
                        }

                        // Redirect ke MainActivity
                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()

                        Toast.makeText(this, "Selamat datang, $nama", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
                Log.e("Login", "Error :  ${error.message}")
            }) {
            override fun getParams(): Map<String, String> {
                val params = HashMap<String, String>()
                params["nim"] = nim
                params["password"] = password
                return params
            }
        }
        // Menambahkan request ke RequestQueue
        Volley.newRequestQueue(this).add(stringRequest)
    }
//    private fun loginUser() {
//        val nim = etNim.text.toString().trim()
//        val password = etPassword.text.toString().trim()
//
//        // Validasi input
//        if (nim.isEmpty() || password.isEmpty()) {
//            Toast.makeText(this, "Mohon isi semua field", Toast.LENGTH_SHORT).show()
//            return
//        }
//
//        val stringRequest = object : StringRequest(
//            Request.Method.POST, URL,
//            Response.Listener { response ->
//                try {
//                    val jsonResponse = JSONObject(response)
//                    val success = jsonResponse.getBoolean("success")
//                    val message = jsonResponse.getString("message")
//
//                    if (success) {
//                        // Ambil data user lengkap
//                        val userData = jsonResponse.getJSONObject("data")
//                        val nama = userData.getString("nama")
//                        val nim = userData.getString("nim")
//                        val saldo = userData.getDouble("saldo")
//
//                        // Simpan data login ke SharedPreferences
//                        val sharedPref = getSharedPreferences("login_data", MODE_PRIVATE)
//                        with(sharedPref.edit()) {
//                            putString("nama", nama)
//                            putString("nim", nim)
//                            putFloat("saldo", saldo.toFloat()) // Simpan saldo
//                            putBoolean("is_logged_in", true)
//                            apply()
//                        }
//
//                        // Optional: Simpan riwayat top up
//                        val histories = userData.getJSONArray("histories")
//                        // Proses histories sesuai kebutuhan
//
//                        // Redirect ke MainActivity
//                        val intent = Intent(this@LoginActivity, MainActivity::class.java)
//                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
//                        startActivity(intent)
//                        finish()
//
//                        Toast.makeText(this, "Selamat datang, $nama", Toast.LENGTH_SHORT).show()
//                    } else {
//                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
//                    }
//                } catch (e: Exception) {
//                    Toast.makeText(this, "Terjadi kesalahan: ${e.message}", Toast.LENGTH_SHORT).show()
//                }
//            },
//            Response.ErrorListener { error ->
//                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
//                Log.e("Login", "Error: ${error.message}")
//            }) {
//            override fun getParams(): Map<String, String> {
//                val params = HashMap<String, String>()
//                params["nim"] = nim
//                params["password"] = password
//                return params
//            }
//        }
//
//        Volley.newRequestQueue(this).add(stringRequest)
//    }
}