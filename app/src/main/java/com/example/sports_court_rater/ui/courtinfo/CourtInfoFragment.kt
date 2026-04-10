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
import com.example.sports_court_rater.Review
import com.example.sports_court_rater.databinding.DialogAddReviewBinding
import com.example.sports_court_rater.databinding.FragmentCourtInfoBinding
import com.example.sports_court_rater.databinding.BottomSheetReviewOptionsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
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
        AlertDialog.Builder(requireContext())
            .setTitle("מחיקת דירוג")
            .setMessage("האם אתה בטוח שברצונך למחוק את הדירוג שלך?")
            .setPositiveButton("מחק") { _, _ ->
                viewModel.deleteReview(review)
            }
            .setNegativeButton("ביטול", null)
            .show()
    }

    private fun showAddReviewDialog(reviewToEdit: Review?) {
        val dialogBinding = DialogAddReviewBinding.inflate(layoutInflater)

        reviewToEdit?.let {
            dialogBinding.dialogRatingBar.rating = it.rating
            dialogBinding.etComment.setText(it.comment)
        }

        AlertDialog.Builder(requireContext())
            .setView(dialogBinding.root)
            .setPositiveButton(if (reviewToEdit == null) R.string.submit_button else R.string.add_review_save_button) { _, _ ->
                val rating = dialogBinding.dialogRatingBar.rating
                val comment = dialogBinding.etComment.text.toString()
                if (rating > 0) {
                    if (reviewToEdit == null) {
                        viewModel.addReview(rating, comment)
                    } else {
                        viewModel.updateReview(reviewToEdit.id, rating, comment)
                    }
                } else {
                    Toast.makeText(requireContext(), "אנא בחר דירוג", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.cancel_button, null)
            .show()
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
                            binding.tvLocationValue.text = it.locationName.ifEmpty {
                                getString(R.string.court_location_placeholder)
                            }
                            binding.tvRatingScore.text = String.format(Locale.getDefault(), "%.1f", it.rating)
                            binding.ratingBar.rating = it.rating
                            
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
