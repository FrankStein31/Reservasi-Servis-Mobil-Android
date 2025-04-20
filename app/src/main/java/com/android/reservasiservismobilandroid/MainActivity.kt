package com.android.reservasiservismobilandroid

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import com.android.reservasiservismobilandroid.databinding.ActivityMainBinding
import com.android.reservasiservismobilandroid.utils.SharedPrefs

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var sharedPrefs: SharedPrefs

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPrefs = SharedPrefs(this)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Konfigurasi AppBar agar menganggap destinasi ini sebagai top-level
        val appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.navigation_home, 
                R.id.navigation_vehicles, 
                R.id.navigation_reservations,
                R.id.navigation_payments,
                R.id.navigation_profile
            )
        )
        
        binding.bottomNav.setupWithNavController(navController)
    }
    
    // Override metode onBackPressed untuk menangani navigasi back
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Jika ada destinasi sebelumnya, kembali ke sana. Jika tidak, tutup aplikasi
        if (!navController.popBackStack()) {
            super.onBackPressed()
        }
    }
} 