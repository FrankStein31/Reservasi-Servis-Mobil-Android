package com.android.reservasiservismobilandroid

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.reservasiservismobilandroid.api.RetrofitClient
import com.android.reservasiservismobilandroid.databinding.ActivityEditProfileBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class EditProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityEditProfileBinding
    private lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = SharedPrefs(this)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        loadProfile()
        setupClickListeners()
    }

    private fun loadProfile() {
        val customerId = sharedPrefs.getUserId()

        RetrofitClient.apiService.getProfile(customerId).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseData = response.body()
                    if (responseData?.get("status") == "success") {
                        val customer = responseData["data"] as Map<*, *>
                        displayProfile(customer)
                    } else {
                        Toast.makeText(this@EditProfileActivity, "Gagal memuat data profile", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@EditProfileActivity, "Gagal memuat data profile", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@EditProfileActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun displayProfile(customer: Map<*, *>) {
        binding.apply {
            etName.setText(customer["name"].toString())
            etEmail.setText(customer["email"].toString())
            if (customer["gender"] == "F") rbFemale.isChecked = true
            etPhone.setText(customer["phone"]?.toString() ?: "")
            etAddress.setText(customer["address"]?.toString() ?: "")
        }
    }

    private fun setupClickListeners() {
        binding.btnSave.setOnClickListener {
            val name = binding.etName.text.toString()
            val email = binding.etEmail.text.toString()
            val gender = if (binding.rbMale.isChecked) "M" else "F"
            val phone = binding.etPhone.text.toString()
            val address = binding.etAddress.text.toString()
            val password = binding.etPassword.text.toString()

            if (name.isEmpty() || email.isEmpty()) {
                Toast.makeText(this, "Nama dan email harus diisi", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            updateProfile(name, email, gender, phone, address, password)
        }
    }

    private fun updateProfile(name: String, email: String, gender: String, phone: String, address: String, password: String) {
        val data = mutableMapOf<String, String>()
        data["id"] = sharedPrefs.getUserId().toString()
        data["name"] = name
        data["email"] = email
        data["gender"] = gender
        if (phone.isNotEmpty()) data["phone"] = phone
        if (address.isNotEmpty()) data["address"] = address
        if (password.isNotEmpty()) data["password"] = password

        RetrofitClient.apiService.updateProfile(data).enqueue(object : Callback<Map<String, Any>> {
            override fun onResponse(call: Call<Map<String, Any>>, response: Response<Map<String, Any>>) {
                if (response.isSuccessful) {
                    val responseData = response.body()
                    if (responseData?.get("status") == "success") {
                        val customer = responseData["data"] as Map<*, *>
                        
                        // Update data di SharedPreferences
                        sharedPrefs.saveUser(
                            userId = sharedPrefs.getUserId(),
                            name = customer["name"] as String,
                            email = customer["email"] as String
                        )

                        Toast.makeText(this@EditProfileActivity, "Profile berhasil diupdate", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@EditProfileActivity, responseData?.get("message") as? String 
                            ?: "Gagal update profile", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@EditProfileActivity, "Gagal update profile", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<Map<String, Any>>, t: Throwable) {
                Toast.makeText(this@EditProfileActivity, "Terjadi kesalahan: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
} 