package com.example.sports_court_rater.ui.profile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.widget.TooltipCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sports_court_rater.Review
import com.example.sports_court_rater.databinding.ItemReviewBinding

class ReviewAdapter(
    private val onReviewClick: (String) -> Unit,
    private val onReviewLongClick: (Review) -> Unit
) : ListAdapter<Review, ReviewAdapter.ReviewViewHolder>(ReviewDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val binding = ItemReviewBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ReviewViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ReviewViewHolder(private val binding: ItemReviewBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(review: Review) {
            binding.apply {
                tvCourtName.text = review.courtName
                TooltipCompat.setTooltipText(tvCourtName, review.courtName)

                tvDate.text = review.date
                rbRating.rating = review.rating
                tvComment.text = review.comment

                root.setOnClickListener {
                    onReviewClick(review.courtId)
                }

                root.setOnLongClickListener {
                    onReviewLongClick(review)
                    true
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
