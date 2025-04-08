package com.android.reservasiservismobilandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityLoginBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding
    private lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = SharedPrefs(this)

        // Cek jika sudah login
        if (sharedPrefs.isLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
            return
        }

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Email dan password harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            login(email, password)
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun login(email: String, password: String) {
        val loginData = mapOf(
            "email" to email,
            "password" to password
        )

        RetrofitClient.apiService.login(loginData).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.get("status") == "success") {
                        val userData = responseBody["data"] as Map<*, *>
                        
                        // Simpan data user ke SharedPreferences
                        sharedPrefs.saveUser(
                            userId = (userData["id"] as Double).toInt(),
                            name = userData["name"] as String,
                            email = userData["email"] as String
                        )

                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, responseBody?.get("message") as? String ?: "Login gagal", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@LoginActivity, "Login gagal", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
} 