package com.example.sports_court_rater.ui.editpost

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
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.sports_court_rater.R
import com.example.sports_court_rater.databinding.FragmentAddCourtBinding
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditPostFragment : Fragment() {

    private var _binding: FragmentAddCourtBinding? = null
    private val binding get() = _binding!!

    private val viewModel: EditPostViewModel by viewModels()
    private val args: EditPostFragmentArgs by navArgs()

    private var selectedImageUri: Uri? = null

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

        setupInitialData()
        setupListeners()
        observeViewModel()
    }

    private fun setupInitialData() {
        val court = args.court
        binding.apply {
            etCourtName.setText(court.courtName)
            etDescription.setText(court.description)
            ratingBar.rating = court.rating
            etLocation.setText("${court.latitude}, ${court.longitude}")
            etLocation.isEnabled = false // Usually we don't edit location of an existing court
            btnUseCurrentLocation.visibility = View.GONE
            
            updateSportSelectionUI(court.sportType)
            
            if (court.imageUrl.isNotEmpty()) {
                Picasso.get().load(court.imageUrl).into(ivCourtImage)
                ivCourtImage.visibility = View.VISIBLE
                placeholderContainer.visibility = View.GONE
            }
            
            btnPostCourt.text = "עדכון מגרש"
        }
    }

    private fun setupListeners() {
        binding.btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        binding.btnPostCourt.setOnClickListener {
            val name = binding.etCourtName.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val rating = binding.ratingBar.rating
            val sport = args.court.sportType // Simplified: keeping current sport or you can add selection logic
            
            if (name.isNotEmpty()) {
                viewModel.updatePost(name, sport, rating, description, selectedImageUri)
            } else {
                Toast.makeText(requireContext(), "נא להזין שם מגרש", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnCancel.setOnClickListener {
            handleBackNavigation()
        }
    }

    private fun handleBackNavigation() {
        if (!findNavController().navigateUp()) {
            findNavController().navigate(R.id.homeFragment)
        }
    }

    private fun updateSportSelectionUI(selectedSport: String) {
        val selectedBg = R.drawable.bg_chip_selected
        val unselectedBg = R.drawable.bg_rounded_input
        val selectedTextColor = ContextCompat.getColor(requireContext(), R.color.white)
        val unselectedTextColor = ContextCompat.getColor(requireContext(), R.color.gray_700)

        binding.apply {
            llBasketball.setBackgroundResource(if (selectedSport == "basketball") selectedBg else unselectedBg)
            tvBasketballLabel.setTextColor(if (selectedSport == "basketball") selectedTextColor else unselectedTextColor)
            ivBasketballIcon.setColorFilter(if (selectedSport == "basketball") selectedTextColor else unselectedTextColor)

            llFootball.setBackgroundResource(if (selectedSport == "football") selectedBg else unselectedBg)
            tvFootballLabel.setTextColor(if (selectedSport == "football") selectedTextColor else unselectedTextColor)
            ivFootballIcon.setColorFilter(if (selectedSport == "football") selectedTextColor else unselectedTextColor)

            llTennis.setBackgroundResource(if (selectedSport == "tennis") selectedBg else unselectedBg)
            tvTennisLabel.setTextColor(if (selectedSport == "tennis") selectedTextColor else unselectedTextColor)
            ivTennisIcon.setColorFilter(if (selectedSport == "tennis") selectedTextColor else unselectedTextColor)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    when (state) {
                        is EditPostViewModel.EditPostState.Loading -> {
                            binding.progressBar.visibility = View.VISIBLE
                            binding.btnPostCourt.isEnabled = false
                        }
                        is EditPostViewModel.EditPostState.Success -> {
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
