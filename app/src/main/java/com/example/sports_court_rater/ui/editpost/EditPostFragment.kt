package com.example.sports_court_rater.ui.editpost

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.TooltipCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.sports_court_rater.R
import com.example.sports_court_rater.databinding.FragmentAddCourtBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.io.File
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.example.sports_court_rater.databinding.BottomSheetImagePickerBinding
import androidx.core.content.FileProvider

@AndroidEntryPoint
class EditPostFragment : Fragment() {

    private var _binding: FragmentAddCourtBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditPostViewModel by viewModels()
    private val args: EditPostFragmentArgs by navArgs()
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var selectedImageUri: Uri? = null
    private var currentSelectedSport: String = ""
    private var latestTmpUri: Uri? = null

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { isSuccess ->
        if (isSuccess) {
            latestTmpUri?.let { uri ->
                selectedImageUri = uri
                binding.ivCourtImage.setImageURI(uri)
                binding.ivCourtImage.visibility = View.VISIBLE
                binding.placeholderContainer.visibility = View.GONE
            }
        }
    }

    private val pickImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.ivCourtImage.setImageURI(it)
            binding.ivCourtImage.visibility = View.VISIBLE
            binding.placeholderContainer.visibility = View.GONE
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
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        setupInitialData()
        setupListeners()
        observeViewModel()
    }

    private fun setupInitialData() {
        val court = args.court
        currentSelectedSport = court.sportType
        binding.apply {
            tvToolbarTitle.text = getString(R.string.edit_court_header)
            etCourtName.setText(court.courtName)
            etDescription.setText(court.description)
            ratingBar.rating = court.rating

            val headerTitle = "ערוך את ''${court.courtName}''"
            tvToolbarTitle.text = headerTitle
            TooltipCompat.setTooltipText(tvToolbarTitle, court.courtName)
            
            updateSportSelectionUI(currentSelectedSport)
            
            if (court.imageUrl.isNotEmpty()) {
                Picasso.get().load(court.imageUrl).into(ivCourtImage)
                ivCourtImage.visibility = View.VISIBLE
                placeholderContainer.visibility = View.GONE
            } else {
                ivCourtImage.visibility = View.GONE
                placeholderContainer.visibility = View.VISIBLE
            }
            
            btnPostCourt.text = "עדכון מגרש"
            
            // In Edit mode, location cannot be updated
            etLocation.isEnabled = false
            btnCurrentLocation.isEnabled = false
            btnCurrentLocation.alpha = 0.5f
        }
    }

    private fun setupListeners() {
        binding.btnSelectImage.setOnClickListener {
            showImagePickerDialog()
        }

        binding.llBasketball.setOnClickListener {
            currentSelectedSport = getString(R.string.basketball)
            updateSportSelectionUI(currentSelectedSport)
        }
        binding.llFootball.setOnClickListener {
            currentSelectedSport = getString(R.string.football)
            updateSportSelectionUI(currentSelectedSport)
        }
        binding.llTennis.setOnClickListener {
            currentSelectedSport = getString(R.string.tennis)
            updateSportSelectionUI(currentSelectedSport)
        }

        binding.btnPostCourt.setOnClickListener {
            val name = binding.etCourtName.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val rating = binding.ratingBar.rating
            
            if (name.isNotEmpty()) {
                viewModel.updatePost(name, currentSelectedSport, rating, description, selectedImageUri)
            } else {
                Toast.makeText(requireContext(), "נא להזין שם מגרש", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnBack.setOnClickListener {
            handleBackNavigation()
        }
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

    private fun updateSportSelectionUI(selectedSport: String) {
        val selectedBg = R.drawable.bg_chip_selected
        val unselectedBg = R.drawable.bg_rounded_input
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        val unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.gray_700)

        binding.apply {
            llBasketball.setBackgroundResource(if (selectedSport == getString(R.string.basketball)) selectedBg else unselectedBg)
            tvBasketballLabel.setTextColor(if (selectedSport == getString(R.string.basketball)) selectedTextColor else unselectedTextColor)

            llFootball.setBackgroundResource(if (selectedSport == getString(R.string.football)) selectedBg else unselectedBg)
            tvFootballLabel.setTextColor(if (selectedSport == getString(R.string.football)) selectedTextColor else unselectedTextColor)

            llTennis.setBackgroundResource(if (selectedSport == getString(R.string.tennis)) selectedBg else unselectedBg)
            tvTennisLabel.setTextColor(if (selectedSport == getString(R.string.tennis)) selectedTextColor else unselectedTextColor)
        }
    }

    private fun observeViewModel() {
        viewModel.locationName.observe(viewLifecycleOwner) { name ->
            binding.etLocation.setText(name)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is EditPostViewModel.EditPostState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnPostCourt.isEnabled = false
                        }
                        is EditPostViewModel.EditPostState.Success -> {
                            binding.progressBar.visibility = View.GONE
                            Toast.makeText(requireContext(), "עודכן בהצלחה", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                        is EditPostViewModel.EditPostState.Error -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnPostCourt.isEnabled = true
                            Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            binding.progressBar.visibility = View.GONE
                            binding.btnPostCourt.isEnabled = true
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
