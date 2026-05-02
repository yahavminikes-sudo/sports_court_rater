package com.example.sports_court_rater.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.sports_court_rater.databinding.FragmentHomeBinding
import com.example.sports_court_rater.util.crossFade
import com.example.sports_court_rater.util.startSkeletonAnimation
import com.example.sports_court_rater.util.stopSkeletonAnimation
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: CourtAdapter
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                getCurrentLocation()
            }
            else -> {
                Toast.makeText(requireContext(), "ההרשאה למיקום נדחתה", Toast.LENGTH_SHORT).show()
                // Revert to RATING if permission denied
                viewModel.setSortType(SortType.RATING)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupRecyclerView()
        setupSortButtons()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = CourtAdapter(
            onCourtClick = { courtId ->
                val action = HomeFragmentDirections.actionHomeFragmentToCourtInfoFragment(courtId)
                findNavController().navigate(action)
            }
        )
        binding.rvCourts.adapter = adapter
    }

    private fun setupSortButtons() {
        binding.btnSortRating.setOnClickListener {
            viewModel.setSortType(SortType.RATING)
        }
        binding.btnSortNew.setOnClickListener {
            viewModel.setSortType(SortType.NEW)
        }
        binding.btnSortNear.setOnClickListener {
            checkLocationPermissions()
        }
    }

    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                viewModel.setSortType(SortType.NEAR)
                getCurrentLocation()
            }
            else -> {
                requestPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    }

    private fun getCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        // Use HIGH_ACCURACY and fallback to lastLocation to solve the null location issue
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { location ->
                if (location != null) {
                    viewModel.setUserLocation(location.latitude, location.longitude)
                    viewModel.setSortType(SortType.NEAR)
                } else {
                    // Try last location if getCurrentLocation is null
                    fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                        if (lastLoc != null) {
                            viewModel.setUserLocation(lastLoc.latitude, lastLoc.longitude)
                            viewModel.setSortType(SortType.NEAR)
                        } else {
                            Toast.makeText(requireContext(), "לא ניתן לקבל מיקום. וודא שה-GPS פועל.", Toast.LENGTH_SHORT).show()
                            viewModel.setSortType(SortType.RATING)
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "שגיאה בקבלת מיקום: ${it.message}", Toast.LENGTH_SHORT).show()
                viewModel.setSortType(SortType.RATING)
            }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.courts.collect { courts ->
                        adapter.submitList(courts) {
                            binding.rvCourts.scrollToPosition(0)
                        }
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        handleLoadingState(isLoading)
                    }
                }
                launch {
                    viewModel.sortType.collect { sortType ->
                        updateSortUI(sortType)
                    }
                }
            }
        }
    }

    private fun handleLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.llSkeletonContainer.isVisible = true
            binding.rvCourts.isVisible = false
            binding.llSkeletonContainer.startSkeletonAnimation()
        } else {
            binding.llSkeletonContainer.stopSkeletonAnimation()
            binding.rvCourts.crossFade(true)
            binding.llSkeletonContainer.crossFade(false)
        }
    }

    private fun updateSortUI(selectedType: SortType) {
        binding.btnSortRating.isSelected = selectedType == SortType.RATING
        binding.btnSortNew.isSelected = selectedType == SortType.NEW
        binding.btnSortNear.isSelected = selectedType == SortType.NEAR
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
