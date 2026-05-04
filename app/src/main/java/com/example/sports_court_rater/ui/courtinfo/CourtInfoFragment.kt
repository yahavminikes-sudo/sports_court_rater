package com.example.sports_court_rater.ui.courtinfo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.TooltipCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.sports_court_rater.R
import com.example.sports_court_rater.Review
import com.example.sports_court_rater.databinding.DialogAddReviewBinding
import com.example.sports_court_rater.databinding.DialogConfirmDeleteBinding
import com.example.sports_court_rater.databinding.FragmentCourtInfoBinding
import com.example.sports_court_rater.databinding.BottomSheetReviewOptionsBinding
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.squareup.picasso.Picasso
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@AndroidEntryPoint
class CourtInfoFragment : Fragment() {

    private var _binding: FragmentCourtInfoBinding? = null
    private val binding get() = _binding!!

    private val viewModel: CourtInfoViewModel by viewModels()
    private val args: CourtInfoFragmentArgs by navArgs()
    
    private lateinit var reviewAdapter: CourtReviewAdapter

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

        setupRecyclerView()
        setupSwipeToRefresh()
        
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

        binding.btnAddReview.setOnClickListener {
            showAddReviewDialog(null)
        }

        viewModel.loadCourtDetails(args.courtId)

        observeViewModel()
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadCourtDetails(args.courtId)
        }
        binding.swipeRefresh.setColorSchemeResources(R.color.primary_green)
        binding.appBar.addOnOffsetChangedListener(AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
            binding.swipeRefresh.isEnabled = verticalOffset == 0
        })
    }

    private fun setupRecyclerView() {
        reviewAdapter = CourtReviewAdapter { review ->
            showReviewOptionsBottomSheet(review)
        }
        binding.rvReviews.adapter = reviewAdapter
    }

    private fun showReviewOptionsBottomSheet(review: Review) {
        val bottomSheet = BottomSheetDialog(requireContext(), R.style.CustomBottomSheetDialogTheme)
        val binding = BottomSheetReviewOptionsBinding.inflate(layoutInflater)
        bottomSheet.setContentView(binding.root)

        binding.btnEdit.setOnClickListener {
            bottomSheet.dismiss()
            showAddReviewDialog(review)
        }

        binding.btnDelete.setOnClickListener {
            bottomSheet.dismiss()
            showDeleteReviewConfirmation(review)
        }

        bottomSheet.show()
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

    private fun showAddReviewDialog(reviewToEdit: Review?) {
        val dialogBinding = DialogAddReviewBinding.inflate(layoutInflater)
        
        val courtName = viewModel.court.value?.courtName ?: ""
        dialogBinding.tvCourtName.text = courtName
        TooltipCompat.setTooltipText(dialogBinding.tvCourtName, courtName)
        
        reviewToEdit?.let {
            dialogBinding.dialogRatingBar.rating = it.rating
            dialogBinding.etComment.setText(it.comment)
            dialogBinding.btnSubmit.text = getString(R.string.add_review_save_button)
            dialogBinding.tvCharCount.text = "${it.comment.length}/500"
        } ?: run {
            dialogBinding.tvCharCount.text = "0/500"
        }
        
        dialogBinding.etComment.addTextChangedListener {
            dialogBinding.tvCharCount.text = "${it?.length ?: 0}/500"
        }
        
        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCancel.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSubmit.setOnClickListener {
            val rating = dialogBinding.dialogRatingBar.rating
            val comment = dialogBinding.etComment.text.toString()
            if (rating > 0) {
                if (reviewToEdit == null) {
                    viewModel.addReview(rating, comment)
                } else {
                    viewModel.updateReview(reviewToEdit.id, rating, comment)
                }
                dialog.dismiss()
            } else {
                Toast.makeText(requireContext(), R.string.select_rating_error, Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun showDeleteConfirmation() {
        val dialogBinding = DialogConfirmDeleteBinding.inflate(layoutInflater)
        dialogBinding.tvDialogTitle.text = "מחק מגרש"
        dialogBinding.tvMessage.text = "האם אתה בטוח שברצונך למחוק את המגרש? פעולה זו לא ניתנת לביטול."

        val dialog = MaterialAlertDialogBuilder(requireContext(), R.style.TransparentDialog)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnDelete.setOnClickListener {
            viewModel.deleteCourt()
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
                    viewModel.court.collect { court ->
                        court?.let {
                            binding.tvCourtName.text = it.courtName
                            TooltipCompat.setTooltipText(binding.tvCourtName, it.courtName)
                            
                            binding.tvSportType.text = it.sportType
                            binding.tvDescription.text = it.description
                            binding.tvLocationValue.text = it.locationName ?: "${it.latitude}, ${it.longitude}"
                            
                            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                            binding.tvPostDate.text = it.date?.let { date -> dateFormat.format(date) } ?: ""
                            
                            val displayRating = if (it.averageRating > 0) it.averageRating else it.rating
                            binding.tvRatingScore.text = String.format(Locale.getDefault(), "%.1f", displayRating)
                            binding.ratingBar.rating = displayRating
                            
                            if (it.imageUrl.isNotEmpty()) {
                                binding.ivCourtImage.visibility = View.VISIBLE
                                binding.flDefaultEmojiContainer.visibility = View.GONE
                                Picasso.get()
                                    .load(it.imageUrl)
                                    .placeholder(R.drawable.ic_launcher_background)
                                    .into(binding.ivCourtImage)
                            } else {
                                binding.ivCourtImage.visibility = View.GONE
                                binding.flDefaultEmojiContainer.visibility = View.VISIBLE
                                binding.tvDefaultEmoji.text = when (it.sportType) {
                                    getString(R.string.basketball) -> getString(R.string.emoji_basketball)
                                    getString(R.string.football) -> getString(R.string.emoji_football)
                                    getString(R.string.tennis) -> getString(R.string.emoji_tennis)
                                    else -> getString(R.string.emoji_football)
                                }
                            }
                        }
                    }
                }

                launch {
                    viewModel.reviews.collect { reviews ->
                        reviewAdapter.submitList(reviews)
                        binding.tvReviewsLabel.text = getString(R.string.reviews_label, reviews.size)
                        binding.tvRatingCount.text = getString(R.string.rating_count_format, reviews.size)
                        binding.tvNoReviews.isVisible = reviews.isEmpty()
                        binding.rvReviews.isVisible = reviews.isNotEmpty()
                    }
                }

                launch {
                    viewModel.isCreator.collect { isCreator ->
                        binding.btnEdit.isVisible = isCreator
                        binding.btnDelete.isVisible = isCreator
                    }
                }

                launch {
                    viewModel.isLoading.collect { isLoading ->
                        binding.swipeRefresh.isRefreshing = isLoading
                    }
                }

                launch {
                    viewModel.deleteResult.collect { result ->
                        result?.onSuccess {
                            Toast.makeText(requireContext(), R.string.court_delete_success, Toast.LENGTH_SHORT).show()
                            findNavController().navigateUp()
                        }?.onFailure {
                            Toast.makeText(requireContext(), getString(R.string.court_delete_error, it.message), Toast.LENGTH_SHORT).show()
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
                            TooltipCompat.setTooltipText(binding.tvCreatorName, it.displayName)
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
