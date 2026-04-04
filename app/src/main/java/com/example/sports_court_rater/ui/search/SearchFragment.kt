package com.example.sports_court_rater.ui.search

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.sports_court_rater.R
import com.example.sports_court_rater.databinding.FragmentSearchBinding
import com.example.sports_court_rater.ui.home.CourtAdapter
import com.google.android.material.chip.Chip
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import androidx.core.view.isVisible

@AndroidEntryPoint
class SearchFragment : Fragment() {

    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SearchViewModel by viewModels()
    private lateinit var adapter: CourtAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        adapter = CourtAdapter { courtId ->
            val action = SearchFragmentDirections.actionSearchFragmentToCourtInfoFragment(courtId)
            findNavController().navigate(directions = action)
        }
        binding.rvResults.adapter = adapter
    }

    private fun setupListeners() {
        binding.etSearch.addTextChangedListener { text ->
            viewModel.onSearchQueryChanged(text?.toString() ?: "")
        }

        binding.cgSports.setOnCheckedStateChangeListener { group, checkedIds ->
            val checkedChip = group.findViewById<Chip>(checkedIds.firstOrNull() ?: -1)
            val sport = checkedChip?.tag?.toString() ?: "All"
            viewModel.onSportSelected(sport)
        }

        binding.rbMinRating.setOnRatingBarChangeListener { _, rating, _ ->
            viewModel.onMinimumRatingChanged(rating)
        }

        binding.btnFilter.setOnClickListener {
            val isCurrentlyVisible = binding.cvFilters.isVisible
            val newVisibility = !isCurrentlyVisible
            binding.cvFilters.isVisible = newVisibility
            binding.btnFilter.isSelected = newVisibility
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.availableSports.collect { sports ->
                        updateSportChips(sports)
                    }
                }
                launch {
                    viewModel.filteredCourts.collect { courts ->
                        adapter.submitList(courts)
                        binding.tvResultsCount.text = getString(R.string.courts_found_format, courts.size)
                    }
                }
            }
        }
    }

    private fun updateSportChips(sports: List<String>) {
        val currentSelected = viewModel.selectedSport.value
        binding.cgSports.removeAllViews()
        
        sports.forEach { sport ->
            val chip = Chip(requireContext()).apply {
                text = if (sport == "All") getString(R.string.all_sports) else sport
                tag = sport
                isCheckable = true
                isCheckedIconVisible = false
                id = View.generateViewId()
                isChecked = sport == currentSelected
                chipBackgroundColor = ContextCompat.getColorStateList(context, R.color.chip_background_color_selector)
                chipStrokeWidth = 0f
                rippleColor = ColorStateList.valueOf(Color.TRANSPARENT)
                setTextColor(ContextCompat.getColorStateList(context, R.color.chip_text_color_selector))
            }
            binding.cgSports.addView(chip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
