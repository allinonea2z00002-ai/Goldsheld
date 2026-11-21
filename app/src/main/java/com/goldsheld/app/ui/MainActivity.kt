package com.goldsheld.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.goldsheld.app.R
import com.goldsheld.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.nav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_home -> HomeFragment()
                R.id.nav_protect -> ProtectionFragment()
                R.id.nav_stats -> StatsFragment()
                R.id.nav_location -> LocationFragment()
                else -> SettingsFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
            true
        }
        if (savedInstanceState == null) {
            binding.nav.selectedItemId = R.id.nav_home
        }
    }
}
