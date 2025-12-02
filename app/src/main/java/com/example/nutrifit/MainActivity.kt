package com.example.nutrifit

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.nutrifit.databinding.ActivityMainBinding
import com.example.nutrifit.ui.DashboardFragment
import com.example.nutrifit.ui.GoalsFragment
import com.example.nutrifit.ui.MealsFragment
import com.example.nutrifit.ui.ProfileFragment
import com.example.nutrifit.ui.WorkoutsFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        NotificationHelper.ensureChannel(this)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            )
            if (permission != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 42)
            }
        }

        handleNotificationDestination()

        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_dashboard -> {
                    loadFragment(DashboardFragment())
                    true
                }
                R.id.nav_workouts -> {
                    loadFragment(WorkoutsFragment())
                    true
                }
                R.id.nav_meals -> {
                    loadFragment(MealsFragment())
                    true
                }
                R.id.nav_goals -> {
                    loadFragment(GoalsFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationDestination()
    }

    private fun handleNotificationDestination() {
        val destination = intent?.getStringExtra("destination")

        when (destination) {
            "meals" -> {
                loadFragment(MealsFragment())
                binding.bottomNav.selectedItemId = R.id.nav_meals
            }
            "workouts" -> {
                loadFragment(WorkoutsFragment())
                binding.bottomNav.selectedItemId = R.id.nav_workouts
            }
            else -> {
                loadFragment(DashboardFragment())
                binding.bottomNav.selectedItemId = R.id.nav_dashboard
            }
        }
    }

    private fun loadFragment(fragment: androidx.fragment.app.Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.main_fragment_container, fragment)
            .commit()
    }
}





