package com.example.soulmate.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.soulmate.tabfragments.PostsFragment
import com.example.soulmate.tabfragments.ShortsFragment

class ProfileTabAdapter : FragmentStateAdapter {

    private var userUID: String

    // Unified constructor for Fragment and FragmentActivity
    constructor(fragment: Fragment, userUID: String) : super(fragment) {
        this.userUID = userUID
    }

    constructor(activity: FragmentActivity, userUID: String) : super(activity) {
        this.userUID = userUID
    }

    override fun getItemCount(): Int {
        return 2 // Number of tabs (Posts, Shorts, About, Rewards)
    }

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> PostsFragment(userUID)     // Pass userUID to PostsFragment
            1 -> ShortsFragment(userUID)    // Pass userUID to ShortsFragment
            else -> Fragment() // Fallback to an empty fragment
        }
    }
}
