package com.android.reservasiservismobilandroid

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityRegisterBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterActivity : AppCompatActivity() {
    private lateinit var binding: ActivityRegisterBinding
    private lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = SharedPrefs(this)

        binding.btnRegister.setOnClickListener {
            val name = binding.etName.text.toString()
            val username = binding.etUsername.text.toString()
            val email = binding.etEmail.text.toString()
            val password = binding.etPassword.text.toString()
            val gender = if (binding.rbMale.isChecked) "M" else "F"
            val phone = binding.etPhone.text.toString()
            val address = binding.etAddress.text.toString()

            if (name.isEmpty() || username.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Mohon isi semua field wajib", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            register(name, username, email, password, gender, phone, address)
        }

        binding.tvLogin.setOnClickListener {
            finish()
        }
    }

    private fun register(name: String, username: String, email: String, password: String, 
                        gender: String, phone: String, address: String) {
        val registerData = mutableMapOf<String, String>()
        registerData["name"] = name
        registerData["username"] = username
        registerData["email"] = email
        registerData["password"] = password
        registerData["gender"] = gender
        if (phone.isNotEmpty()) registerData["phone"] = phone
        if (address.isNotEmpty()) registerData["address"] = address

        RetrofitClient.apiService.register(registerData).enqueue(object : Callback<Map<String, Any>> {
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

                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
                        finish()
                    } else {
                        Toast.makeText(this@RegisterActivity, responseBody?.get("message") as? String 
                            ?: "Registrasi gagal", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@RegisterActivity, "Registrasi gagal", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@RegisterActivity, "Terjadi kesalahan: ${t.message}", 
                    Toast.LENGTH_SHORT).show()
            }
        })
    }
} 