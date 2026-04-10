package com.example.sports_court_rater.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: HomeViewModel by viewModels()
    private lateinit var adapter: CourtAdapter

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
            viewModel.setSortType(SortType.NEAR)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.courts.collect { courts ->
                        adapter.submitList(courts)
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
