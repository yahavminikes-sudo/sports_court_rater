package com.example.sports_court_rater.ui.profile

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.R
import com.example.sports_court_rater.databinding.BottomSheetCourtOptionsBinding
import com.example.sports_court_rater.databinding.FragmentProfileBinding
import com.example.sports_court_rater.ui.home.CourtAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
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

        setupUI()
        setupRecyclerViews()
        observeViewModel()
    }

    private fun setupUI() {
        refreshProfileHeader()

        binding.btnLogout.setOnClickListener {
            viewModel.logout()
            val action = ProfileFragmentDirections.actionProfileFragmentToLoginFragment()
            findNavController().navigate(action)
        }

        binding.tabMyCourts.setOnClickListener {
            isShowingCourts = true
            updateTabUI()
        }

        binding.tabMyRatings.setOnClickListener {
            isShowingCourts = false
            updateTabUI()
        }

        binding.btnEditName.setOnClickListener {
            showEditNameDialog()
        }

        binding.btnChangePhoto.setOnClickListener {
            // Handle change photo logic
        }
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
        val context = requireContext()
        val builder = AlertDialog.Builder(context)
        builder.setTitle("עדכון שם")

        val input = EditText(context)
        input.setText(viewModel.currentUser?.displayName)
        input.setSelection(input.text.length)
        
        val container = FrameLayout(context)
        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        val margin = (24 * resources.displayMetrics.density).toInt()
        params.setMargins(margin, margin / 2, margin, 0)
        input.layoutParams = params
        container.addView(input)
        
        builder.setView(container)

        builder.setPositiveButton("שמור") { dialog, _ ->
            val newName = input.text.toString().trim()
            if (newName.isNotEmpty()) {
                viewModel.updateDisplayName(newName)
            }
            dialog.dismiss()
        }
        builder.setNegativeButton("ביטול") { dialog, _ ->
            dialog.cancel()
        }

        builder.show()
    }

    private fun updateTabUI() {
        if (isShowingCourts) {
            binding.tabMyCourts.setBackgroundResource(R.drawable.bg_circle_white)
            binding.tabMyCourts.elevation = 4f
            binding.tabMyCourts.setTextColor(resources.getColor(R.color.black, null))
            
            binding.tabMyRatings.setBackgroundResource(0)
            binding.tabMyRatings.elevation = 0f
            binding.tabMyRatings.setTextColor(resources.getColor(R.color.gray_500, null))
            
            binding.rvMyPosts.adapter = courtAdapter
            binding.tvEmptyState.isVisible = viewModel.userCourts.value.isEmpty() && !viewModel.isLoading.value
            binding.tvEmptyState.text = getString(R.string.profile_empty_courts)
        } else {
            binding.tabMyRatings.setBackgroundResource(R.drawable.bg_circle_white)
            binding.tabMyRatings.elevation = 4f
            binding.tabMyRatings.setTextColor(resources.getColor(R.color.black, null))
            
            binding.tabMyCourts.setBackgroundResource(0)
            binding.tabMyCourts.elevation = 0f
            binding.tabMyCourts.setTextColor(resources.getColor(R.color.gray_500, null))
            
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
        
        reviewAdapter = ReviewAdapter { courtId ->
            val action = ProfileFragmentDirections.actionProfileFragmentToCourtInfoFragment(courtId)
            findNavController().navigate(action)
        }
        
        binding.rvMyPosts.adapter = courtAdapter
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

    private fun showDeleteConfirmation(court: Court) {
        AlertDialog.Builder(requireContext())
            .setTitle("מחיקת מגרש")
            .setMessage("האם אתה בטוח שברצונך למחוק את '${court.courtName}'?")
            .setPositiveButton("מחק") { _, _ ->
                viewModel.deleteCourt(court)
            }
            .setNegativeButton("ביטול", null)
            .show()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.userCourts.collect { courts ->
                        courtAdapter.submitList(courts)
                        binding.tvCourtsCount.text = courts.size.toString()
                        binding.tabMyCourts.text = "המגרשים שלי (${courts.size})"
                        if (isShowingCourts) {
                            binding.tvEmptyState.isVisible = courts.isEmpty() && !viewModel.isLoading.value
                        }
                    }
                }
                launch {
                    viewModel.userReviews.collect { reviews ->
                        reviewAdapter.submitList(reviews)
                        binding.tvRatingsCount.text = reviews.size.toString()
                        binding.tabMyRatings.text = "הדירוגים שלי (${reviews.size})"
                        if (!isShowingCourts) {
                            binding.tvEmptyState.isVisible = reviews.isEmpty() && !viewModel.isLoading.value
                        }
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.progressBar.isVisible = isLoading
                    }
                }
                launch {
                    viewModel.updateResult.collect { result ->
                        result?.onSuccess {
                            refreshProfileHeader()
                            Toast.makeText(requireContext(), "השם עודכן בהצלחה", Toast.LENGTH_SHORT).show()
                            viewModel.resetUpdateResult()
                        }?.onFailure {
                            Toast.makeText(requireContext(), "עדכון השם נכשל: ${it.message}", Toast.LENGTH_SHORT).show()
                            viewModel.resetUpdateResult()
                        }
                    }
                }
                launch {
                    viewModel.deleteResult.collect { result ->
                        result?.onSuccess {
                            Toast.makeText(requireContext(), "המגרש נמחק בהצלחה", Toast.LENGTH_SHORT).show()
                            viewModel.resetDeleteResult()
                        }?.onFailure {
                            Toast.makeText(requireContext(), "המחיקה נכשלה: ${it.message}", Toast.LENGTH_SHORT).show()
                            viewModel.resetDeleteResult()
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
