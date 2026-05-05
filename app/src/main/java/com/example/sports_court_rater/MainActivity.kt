package com.example.sports_court_rater

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.graphics.toColorInt
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.sports_court_rater.databinding.ActivityMainBinding
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupStatusBarAndNavigationBar()
        setupNavigation()
    }

    private fun setupStatusBarAndNavigationBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            window.navigationBarColor = Color.WHITE
            val insetsController = WindowInsetsControllerCompat(window, window.decorView)
            insetsController.isAppearanceLightNavigationBars = true
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                window.navigationBarDividerColor = "#1f000000".toColorInt()
            } catch (e: Exception) {

            }
        }
    }

    private fun setupNavigation() {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
        val navController = navHostFragment.navController
        
        val navGraph = navController.navInflater.inflate(R.navigation.nav_graph)
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            navGraph.setStartDestination(R.id.homeFragment)
        } else {
            navGraph.setStartDestination(R.id.loginFragment)
        }
        navController.graph = navGraph

        binding.bottomNavigation.setupWithNavController(navController)

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
