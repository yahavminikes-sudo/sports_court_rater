package com.example.sports_court_rater.util

import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import com.example.sports_court_rater.R

fun ViewGroup.startSkeletonAnimation() {
    val pulseAnimation = AnimationUtils.loadAnimation(context, R.anim.pulse)
    fun applyPulse(view: View) {
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                applyPulse(view.getChildAt(i))
            }
        } else if (view.id != View.NO_ID) {
            // Check if it's a skeleton view (typically has background or is a View)
            view.startAnimation(pulseAnimation)
        }
    }
    applyPulse(this)
}

fun ViewGroup.stopSkeletonAnimation() {
    fun clearAnims(view: View) {
        view.clearAnimation()
        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                clearAnims(view.getChildAt(i))
            }
        }
    }
    clearAnims(this)
}

/**
 * Cross-fade visibility transition.
 */
fun View.crossFade(show: Boolean, duration: Long = 300) {
    if (show) {
        this.alpha = 0f
        this.visibility = View.VISIBLE
        this.animate()
            .alpha(1f)
            .setDuration(duration)
            .setListener(null)
            .start()
    } else {
        this.animate()
            .alpha(0f)
            .setDuration(duration)
            .withEndAction {
                this.visibility = View.GONE
                this.alpha = 1f
            }
            .start()
    }
}
