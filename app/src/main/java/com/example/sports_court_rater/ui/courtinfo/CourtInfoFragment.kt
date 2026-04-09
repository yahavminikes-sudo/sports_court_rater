package com.example.sports_court_rater.ui.courtinfo

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.sports_court_rater.R
import com.example.sports_court_rater.databinding.FragmentCourtInfoBinding
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.Locale

@AndroidEntryPoint
class CourtInfoFragment : Fragment() {

    private var _binding: FragmentCourtInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CourtInfoViewModel by viewModels()
    private val args: CourtInfoFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCourtInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            findNavController().navigateUp()
        }

        binding.btnEdit.setOnClickListener {
            viewModel.court.value?.let { court ->
                val action = CourtInfoFragmentDirections.actionCourtInfoFragmentToEditPostFragment(court)
                findNavController().navigate(action)
            }
        }

        binding.btnDelete.setOnClickListener {
            showDeleteConfirmation()
        }

        viewModel.loadCourtDetails(args.courtId)

        observeViewModel()
    }

    private fun showDeleteConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("מחיקת מגרש")
            .setMessage("האם אתה בטוח שברצונך למחוק את המגרש?")
            .setPositiveButton("מחק") { _, _ ->
                viewModel.deleteCourt()
            }
            .setNegativeButton("ביטול", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.court.collect { court ->
                        court?.let {
                            binding.tvCourtName.text = it.courtName
                            binding.tvSportType.text = it.sportType
                            binding.tvDescription.text = it.description
                            binding.tvLocationValue.text = getString(R.string.court_location_placeholder)
                            binding.tvRatingScore.text = String.format(Locale.getDefault(), "%.1f", it.rating)
                            binding.ratingBar.rating = it.rating
                            
                            if (it.imageUrl.isNotEmpty()) {
                                Picasso.get()
                                    .load(it.imageUrl)
                                    .placeholder(R.drawable.ic_launcher_background)
                                    .into(binding.ivCourtImage)
                            }
                        }
                    }
                }

                launch {
                    viewModel.isCreator.collect { isCreator ->
                        binding.btnEdit.isVisible = isCreator
                        binding.btnDelete.isVisible = isCreator
                    }
                }

                launch {
                    viewModel.deleteResult.collect { result ->
                        result?.onSuccess {
                            Toast.makeText(requireContext(), "המגרש נמחק בהצלחה", Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }?.onFailure {
                            Toast.makeText(requireContext(), "המחיקה נכשלה: ${it.message}", Toast.LENGTH_SHORT).show()
                            viewModel.resetDeleteResult()
                        }
                    }
                }

                launch {
                    viewModel.weather.collect { weather ->
                        weather?.let {
                            val temp = it.main.temp
                            val description = it.weather.firstOrNull()?.description ?: "N/A"
                            val iconCode = it.weather.firstOrNull()?.icon
                            
                            binding.tvWeatherInfo.text = getString(
                                R.string.weather_format,
                                temp,
                                description
                            )
                            
                            if (iconCode != null) {
                                val iconUrl = "https://openweathermap.org/img/wn/$iconCode@2x.png"
                                Picasso.get().load(iconUrl).into(binding.ivWeatherIcon)
                            }
                        }
                    }
                }

                launch {
                    viewModel.creator.collect { user ->
                        user?.let {
                            binding.tvCreatorName.text = it.displayName
                            if (it.profilePictureUrl.isNotEmpty()) {
                                Picasso.get()
                                    .load(it.profilePictureUrl)
                                    .placeholder(R.drawable.ic_person)
                                    .into(binding.ivCreatorImage)
                            }
                        }
                    }
                }

                launch {
                    viewModel.error.collect { error ->
                        error?.let {
                            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
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
