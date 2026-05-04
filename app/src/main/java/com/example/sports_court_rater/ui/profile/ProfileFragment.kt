package com.example.sports_court_rater.ui.profile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.R
import com.example.sports_court_rater.Review
import com.example.sports_court_rater.databinding.BottomSheetCourtOptionsBinding
import com.example.sports_court_rater.databinding.BottomSheetReviewOptionsBinding
import com.example.sports_court_rater.databinding.DialogAddReviewBinding
import com.example.sports_court_rater.databinding.DialogConfirmDeleteBinding
import com.example.sports_court_rater.databinding.DialogEditNameBinding
import com.example.sports_court_rater.databinding.FragmentProfileBinding
import com.example.sports_court_rater.ui.home.CourtAdapter
import com.example.sports_court_rater.util.crossFade
import com.example.sports_court_rater.util.startSkeletonAnimation
import com.example.sports_court_rater.util.stopSkeletonAnimation
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var courtAdapter: CourtAdapter
    private lateinit var reviewAdapter: ReviewAdapter

    private var isShowingCourts = true

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerViews()
        setupUI()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadUserData() // Refresh data when returning to profile
    }

    private fun setupUI() {
        refreshProfileHeader()

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            val action = ProfileFragmentDirections.actionProfileFragmentToLoginFragment()
            findNavController().navigate(action)
        }

        binding.llCourtsCount.setOnClickListener {
            if (!isShowingCourts) {
                isShowingCourts = true
                updateTabUI()
            }
        }

        binding.llRatingsCount.setOnClickListener {
            if (isShowingCourts) {
                isShowingCourts = false
                updateTabUI()
            }
        }

        binding.btnEditName.setOnClickListener {
            showEditNameDialog()
        }

        binding.btnChangePhoto.setOnClickListener {
            // Handle change photo logic
        }
        
        updateTabUI()
    }

    private fun refreshProfileHeader() {
        val user = viewModel.currentUser
        val displayName = user?.displayName ?: "משתמש"
        binding.tvUserName.text = displayName
        binding.tvUserEmail.text = user?.email ?: ""
        
        binding.tvProfileInitial.text = displayName.take(1).uppercase()
        
        user?.photoUrl?.let {
            Picasso.get().load(it).placeholder(R.drawable.ic_person).into(binding.ivProfilePicture)
            binding.ivProfilePicture.isVisible = true
            binding.tvProfileInitial.isVisible = false
        } ?: run {
            binding.ivProfilePicture.isVisible = false
            binding.tvProfileInitial.isVisible = true
        }
    }

    private fun showEditNameDialog() {
        val dialogBinding = DialogEditNameBinding.inflate(layoutInflater)
        dialogBinding.etName.setText(viewModel.currentUser?.displayName)
        
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnSave.setOnClickListener {
            val newName = dialogBinding.etName.text.toString().trim()
            if (newName.isNotEmpty()) {
                viewModel.updateDisplayName(newName)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), R.string.enter_name_error, Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateTabUI() {
        val activeColor = ContextCompat.getColor(requireContext(), R.color.primary_green)
        val inactiveColor = ContextCompat.getColor(requireContext(), R.color.gray_500)

        if (isShowingCourts) {
            binding.tvCourtsCount.setTextColor(activeColor)
            binding.tvCourtsLabel.setTextColor(activeColor)
            binding.indicatorCourts.visibility = View.VISIBLE

            binding.tvRatingsCount.setTextColor(inactiveColor)
            binding.tvRatingsLabel.setTextColor(inactiveColor)
            binding.indicatorRatings.visibility = View.INVISIBLE

            binding.rvMyPosts.adapter = courtAdapter
            binding.tvEmptyState.isVisible = viewModel.userCourts.value.isEmpty() && !viewModel.isLoading.value
            binding.tvEmptyState.text = getString(R.string.profile_empty_courts)
        } else {
            binding.tvRatingsCount.setTextColor(activeColor)
            binding.tvRatingsLabel.setTextColor(activeColor)
            binding.indicatorRatings.visibility = View.VISIBLE

            binding.tvCourtsCount.setTextColor(inactiveColor)
            binding.tvCourtsLabel.setTextColor(inactiveColor)
            binding.indicatorCourts.visibility = View.INVISIBLE

            binding.rvMyPosts.adapter = reviewAdapter
            binding.tvEmptyState.isVisible = viewModel.userReviews.value.isEmpty() && !viewModel.isLoading.value
            binding.tvEmptyState.text = "עדיין לא דירגת מגרשים"
        }
    }

    private fun setupRecyclerViews() {
        courtAdapter = CourtAdapter(
            onCourtClick = { courtId ->
                val action = ProfileFragmentDirections.actionProfileFragmentToCourtInfoFragment(courtId)
                findNavController().navigate(action)
            },
            onCourtLongClick = { court ->
                showPostOptionsBottomSheet(court)
            }
        )
        
        reviewAdapter = ReviewAdapter(
            onReviewClick = { courtId ->
                val action = ProfileFragmentDirections.actionProfileFragmentToCourtInfoFragment(courtId)
                findNavController().navigate(action)
            },
            onReviewLongClick = { review ->
                showReviewOptionsBottomSheet(review)
            }
        )
        
        binding.rvMyPosts.adapter = if (isShowingCourts) courtAdapter else reviewAdapter
    }

    private fun showPostOptionsBottomSheet(court: Court) {
        val bottomSheet = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        val binding = BottomSheetCourtOptionsBinding.inflate(layoutInflater)
        bottomSheet.setContentView(binding.root)

        binding.tvCourtName.text = court.courtName

        binding.btnEdit.setOnClickListener {
            bottomSheet.dismiss()
            val action = ProfileFragmentDirections.actionProfileFragmentToEditPostFragment(court)
            findNavController().navigate(action)
        }

        binding.btnDelete.setOnClickListener {
            bottomSheet.dismiss()
            showDeleteConfirmation(court)
        }

        bottomSheet.show()
    }

    private fun showReviewOptionsBottomSheet(review: Review) {
        val bottomSheet = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        val binding = BottomSheetReviewOptionsBinding.inflate(layoutInflater)
        bottomSheet.setContentView(binding.root)

        binding.btnEdit.setOnClickListener {
            bottomSheet.dismiss()
            showEditReviewDialog(review)
        }

        binding.btnDelete.setOnClickListener {
            bottomSheet.dismiss()
            showDeleteReviewConfirmation(review)
        }

        bottomSheet.show()
    }

    private fun showEditReviewDialog(review: Review) {
        val dialogBinding = DialogAddReviewBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = getString(R.string.edit_review_dialog_title)
        dialogBinding.tvCourtName.text = review.courtName
        dialogBinding.dialogRatingBar.rating = review.rating
        dialogBinding.etComment.setText(review.comment)
        dialogBinding.btnSubmit.text = getString(R.string.add_review_save_button)
        
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnSubmit.setOnClickListener {
            val rating = dialogBinding.dialogRatingBar.rating
            val comment = dialogBinding.etComment.text.toString()
            if (rating > 0) {
                viewModel.updateReview(review.id, rating, comment)
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), R.string.select_rating_error, Toast.LENGTH_SHORT).show()
            }
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteReviewConfirmation(review: Review) {
        val dialogBinding = DialogConfirmDeleteBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = getString(R.string.delete_dialog_title)
        dialogBinding.tvMessage.text = getString(R.string.delete_dialog_message)
        
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnDelete.setOnClickListener {
            viewModel.deleteReview(review)
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun showDeleteConfirmation(court: Court) {
        val dialogBinding = DialogConfirmDeleteBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = "מחיקת מגרש"
        dialogBinding.tvMessage.text = "האם אתה בטוח שברצונך למחוק את '${court.courtName}'?"
        
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnDelete.setOnClickListener {
            viewModel.deleteCourt(court)
            dialog.dismiss()
        }

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userCourts.collect { courts ->
                        courtAdapter.submitList(courts)
                        binding.tvCourtsCount.text = courts.size.toString()
                        if (isShowingCourts) {
                            binding.tvEmptyState.isVisible = courts.isEmpty() && !viewModel.isLoading.value
                        }
                    }
                }
                launch {
                    viewModel.userReviews.collect { reviews ->
                        reviewAdapter.submitList(reviews)
                        binding.tvRatingsCount.text = reviews.size.toString()
                        if (!isShowingCourts) {
                            binding.tvEmptyState.isVisible = reviews.isEmpty() && !viewModel.isLoading.value
                        }
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        handleLoadingState(isLoading)
                    }
                }
                launch {
                    viewModel.updateResult.collect { result ->
                        result?.onSuccess {
                            refreshProfileHeader()
                            Toast.makeText(requireContext(), R.string.update_name_success, Toast.LENGTH_SHORT).show()
                            viewModel.resetUpdateResult()
                        }?.onFailure {
                            Toast.makeText(requireContext(), getString(R.string.update_name_error, it.message), Toast.LENGTH_SHORT).show()
                            viewModel.resetUpdateResult()
                        }
                    }
                }
                launch {
                    viewModel.deleteResult.collect { result ->
                        result?.onSuccess {
                            Toast.makeText(requireContext(), R.string.action_success, Toast.LENGTH_SHORT).show()
                            viewModel.resetDeleteResult()
                        }?.onFailure {
                            Toast.makeText(requireContext(), getString(R.string.action_failed, it.message), Toast.LENGTH_SHORT).show()
                            viewModel.resetDeleteResult()
                        }
                    }
                }
            }
        }
    }

    private fun handleLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.llSkeletonContainer.isVisible = true
            binding.rvMyPosts.isVisible = false
            binding.llSkeletonContainer.startSkeletonAnimation()
        } else {
            binding.llSkeletonContainer.stopSkeletonAnimation()
            binding.rvMyPosts.crossFade(true)
            binding.llSkeletonContainer.crossFade(false)
            updateTabUI() // Re-check empty state visibility
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
