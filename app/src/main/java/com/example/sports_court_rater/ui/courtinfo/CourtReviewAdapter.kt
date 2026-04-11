package com.example.sports_court_rater.ui.courtinfo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sports_court_rater.Review
import com.example.sports_court_rater.databinding.ItemCourtReviewBinding
import com.squareup.picasso.Picasso
import com.example.sports_court_rater.R
import com.google.firebase.auth.FirebaseAuth

class CourtReviewAdapter(
    private val onReviewLongClick: (Review) -> Unit
) : ListAdapter<Review, CourtReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    private val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemCourtReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReviewViewHolder(private val binding: ItemCourtReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.apply {
                tvReviewerName.text = review.creatorName
                TooltipCompat.setTooltipText(tvReviewerName, review.creatorName)

                tvReviewDate.text = review.date
                rbRating.rating = review.rating
                tvReviewComment.text = review.comment

                if (review.creatorImageUrl.isNotEmpty()) {
                    Picasso.get()
                        .load(review.creatorImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .into(ivReviewerImage)
                } else {
                    ivReviewerImage.setImageResource(R.drawable.ic_person)
                }

                root.setOnLongClickListener {
                    if (review.creatorId == currentUserId) {
                        onReviewLongClick(review)
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    class ReviewDiffCallback : DiffUtil.ItemCallback<Review>() {
        override fun areItemsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Review, newItem: Review): Boolean {
            return oldItem == newItem
        }
    }
}
