package com.example.sports_court_rater.ui.addcourt

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.sports_court_rater.R
import com.example.sports_court_rater.databinding.FragmentAddCourtBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.sports_court_rater.databinding.BottomSheetImagePickerBinding
import androidx.core.content.FileProvider

@AndroidEntryPoint
class AddCourtFragment : Fragment() {

    private var _binding: FragmentAddCourtBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AddCourtViewModel by viewModels()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var latestTmpUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let { uri ->
                viewModel.setImageUri(uri)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                getCurrentLocation()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                getCurrentLocation()
            }
            else -> {
                Toast.makeText(requireContext(), R.string.location_permission_denied, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setImageUri(it)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddCourtBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners() {
        binding.btnCurrentLocation.setOnClickListener {
            checkLocationPermissions()
        }

        binding.llBasketball.setOnClickListener {
            viewModel.setSport(getString(R.string.basketball))
        }
        binding.llFootball.setOnClickListener {
            viewModel.setSport(getString(R.string.football))
        }
        binding.llTennis.setOnClickListener {
            viewModel.setSport(getString(R.string.tennis))
        }

        binding.btnSelectImage.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnPostCourt.setOnClickListener {
            publishCourt()
        }

        binding.btnBack.setOnClickListener {
            handleBackNavigation()
        }

        // Add court location can be manually edited
        binding.etLocation.isFocusable = true
        binding.etLocation.isFocusableInTouchMode = true
        binding.etLocation.isClickable = true
    }

    private fun handleBackNavigation() {
        if (!findNavController().navigateUp()) {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun showImagePickerDialog() {
        val bottomSheet = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        val pickerBinding = BottomSheetImagePickerBinding.inflate(layoutInflater)
        bottomSheet.setContentView(pickerBinding.root)

        pickerBinding.btnCamera.setOnClickListener {
            bottomSheet.dismiss()
            takePhoto()
        }

        pickerBinding.btnGallery.setOnClickListener {
            bottomSheet.dismiss()
            pickImageLauncher.launch("image/*")
        }

        bottomSheet.show()
    }

    private fun takePhoto() {
        val tmpFile = File.createTempFile("tmp_image_file", ".png", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        latestTmpUri = FileProvider.getUriForFile(
            requireContext(),
            "${requireContext().packageName}.fileprovider",
            tmpFile
        )
        takePictureLauncher.launch(latestTmpUri)
    }

    private fun publishCourt() {
        val courtName = binding.etCourtName.text.toString().trim()
        val locationText = binding.etLocation.text.toString().trim()
        val rating = binding.ratingBar.rating

        // Reset previous errors
        binding.etCourtName.error = null
        binding.etLocation.error = null

        when {
            courtName.isEmpty() -> {
                binding.etCourtName.error = requireContext().getString(R.string.court_name_label_required)
                binding.etCourtName.requestFocus()
            }
            locationText.isEmpty() -> {
                binding.etLocation.error = requireContext().getString(R.string.location_label_required)
                Toast.makeText(requireContext(), R.string.location_label_required, Toast.LENGTH_SHORT).show()
            }
            rating == 0f -> {
                Toast.makeText(
                    requireContext(),
                    R.string.rating_label_required,
                    Toast.LENGTH_SHORT
                ).show()
            }
            else -> {
                submitCourtData(courtName, locationText, rating)
            }
        }
    }

    private fun submitCourtData(courtName: String, locationText: String, rating: Float) {
        val description = binding.etDescription.text.toString().trim()
        viewModel.postCourt(courtName, description, rating, locationText)
    }

    private fun observeViewModel() {
        viewModel.locationName.observe(viewLifecycleOwner) { name ->
            binding.etLocation.setText(name)
        }
        
        viewModel.selectedSport.observe(viewLifecycleOwner) { sport ->
            updateSportSelectionUI(sport)
        }
        viewModel.imageUri.observe(viewLifecycleOwner) { uri ->
            if (uri != null) {
                binding.ivCourtImage.setImageURI(uri)
                binding.ivCourtImage.visibility = View.VISIBLE
                binding.placeholderContainer.visibility = View.GONE
            } else {
                binding.ivCourtImage.visibility = View.GONE
                binding.placeholderContainer.visibility = View.VISIBLE
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is AddCourtViewModel.AddCourtState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnPostCourt.isEnabled = false
                        }
                        is AddCourtViewModel.AddCourtState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(
                                requireContext(),
                                R.string.court_published_success,
                                Toast.LENGTH_SHORT
                            ).show()

                            val navOptions = NavOptions.Builder()
                                .setPopUpTo(R.id.homeFragment, false)
                                .build()
                            findNavController().navigate(R.id.profileFragment, null, navOptions)

                            viewModel.resetState()
                        }
                        is AddCourtViewModel.AddCourtState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnPostCourt.isEnabled = true
                            Toast.makeText(requireContext(), getString(R.string.action_failed, state.message), Toast.LENGTH_SHORT).show()
                        }
                        is AddCourtViewModel.AddCourtState.Idle -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnPostCourt.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    private fun updateSportSelectionUI(selectedSport: String) {
        val selectedBg = R.drawable.bg_chip_selected
        val unselectedBg = R.drawable.bg_rounded_input
        
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        val unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.gray_700)

        // Reset all
        binding.llBasketball.setBackgroundResource(unselectedBg)
        binding.tvBasketballLabel.setTextColor(unselectedTextColor)

        binding.llFootball.setBackgroundResource(unselectedBg)
        binding.tvFootballLabel.setTextColor(unselectedTextColor)

        binding.llTennis.setBackgroundResource(unselectedBg)
        binding.tvTennisLabel.setTextColor(unselectedTextColor)

        // Apply selected
        when (selectedSport) {
            getString(R.string.basketball) -> {
                binding.llBasketball.setBackgroundResource(selectedBg)
                binding.tvBasketballLabel.setTextColor(selectedTextColor)
            }
            getString(R.string.football) -> {
                binding.llFootball.setBackgroundResource(selectedBg)
                binding.tvFootballLabel.setTextColor(selectedTextColor)
            }
            getString(R.string.tennis) -> {
                binding.llTennis.setBackgroundResource(selectedBg)
                binding.tvTennisLabel.setTextColor(selectedTextColor)
            }
        }
    }

    private fun checkLocationPermissions() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
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

        try {
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        viewModel.setLocation(location.latitude, location.longitude)
                    } else {
                        // Fallback to last location
                        try {
                            fusedLocationClient.lastLocation.addOnSuccessListener { lastLoc ->
                                if (lastLoc != null) {
                                    viewModel.setLocation(lastLoc.latitude, lastLoc.longitude)
                                } else {
                                    Toast.makeText(requireContext(), R.string.location_gps_error, Toast.LENGTH_SHORT).show()
                                }
                            }
                        } catch (e: SecurityException) {
                            Toast.makeText(requireContext(), R.string.location_gps_error, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), getString(R.string.error_fetching_location, it.message), Toast.LENGTH_SHORT).show()
                }
        } catch (e: SecurityException) {
            Toast.makeText(requireContext(), R.string.location_gps_error, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
