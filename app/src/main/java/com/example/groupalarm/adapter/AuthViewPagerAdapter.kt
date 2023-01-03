package com.example.groupalarm.adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.groupalarm.LoginFragment
import com.example.groupalarm.RegisterFragment

class AuthViewPagerAdapter(activity: AppCompatActivity, val itemsCount: Int) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return itemsCount
    }

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            LoginFragment()
        } else {
            RegisterFragment()
        }
    }
}