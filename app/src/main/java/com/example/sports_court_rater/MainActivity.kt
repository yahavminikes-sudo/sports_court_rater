package com.example.sports_court_rater

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.graphics.toColorInt
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.sports_court_rater.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        window.navigationBarColor = Color.WHITE
        window.navigationBarDividerColor = "#1f000000".toColorInt()
        val insetsController = WindowInsetsControllerCompat(window, window.decorView)
        insetsController.isAppearanceLightNavigationBars = true

        setupNavigation()
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        
        // Connect BottomNavigationView with NavController
        binding.bottomNavigation.setupWithNavController(navController)

        // Setup dynamic start destination based on auth state
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            navGraph.setStartDestination(R.id.homeFragment)
        } else {
            navGraph.setStartDestination(R.id.loginFragment)
        }
        navController.graph = navGraph

        // Hide Bottom Navigation on Login/SignUp screens
        navController.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.signUpFragment -> {
                    binding.bottomNavigation.visibility = View.GONE
                }
                else -> {
                    binding.bottomNavigation.visibility = View.VISIBLE
                }
            }
        }
    }
}
