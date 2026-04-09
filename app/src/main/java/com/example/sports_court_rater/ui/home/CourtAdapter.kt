package com.example.sports_court_rater.ui.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.sports_court_rater.Court
import com.example.sports_court_rater.databinding.ItemCourtBinding
import com.squareup.picasso.Picasso

class CourtAdapter(
    private val onCourtClick: (String) -> Unit,
    private val onCourtLongClick: ((Court) -> Unit)? = null
) : ListAdapter<Court, CourtAdapter.CourtViewHolder>(CourtDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CourtViewHolder {
        val binding = ItemCourtBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CourtViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CourtViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class CourtViewHolder(private val binding: ItemCourtBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(court: Court) {
            binding.apply {
                tvCourtName.text = court.courtName
                tvSportType.text = court.sportType
                rbRating.rating = court.rating
                tvLocation.text = "${court.latitude}, ${court.longitude}" 
                tvReviewCount.text = ""

                if (court.imageUrl.isNotEmpty()) {
                    Picasso.get()
                        .load(court.imageUrl)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_report_image)
                        .into(ivCourtImage)
                } else {
                    ivCourtImage.setImageResource(android.R.drawable.ic_menu_gallery)
                }

                root.setOnClickListener {
                    onCourtClick(court.id)
                }

                root.setOnLongClickListener {
                    onCourtLongClick?.invoke(court)
                    true
                }
            }
        }
    }

    class CourtDiffCallback : DiffUtil.ItemCallback<Court>() {
        override fun areItemsTheSame(oldItem: Court, newItem: Court): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Court, newItem: Court): Boolean {
            return oldItem == newItem
        }
    }
}
