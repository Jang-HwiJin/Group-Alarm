package group.alarm.groupalarm.adapter

import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import group.alarm.groupalarm.DashboardFragment
import group.alarm.groupalarm.FriendFragment
import group.alarm.groupalarm.LoginFragment
import group.alarm.groupalarm.ProfileFragment
import group.alarm.groupalarm.RegisterFragment
import group.alarm.groupalarm.SettingFragment

class DashViewPagerAdapter(activity: AppCompatActivity, val itemsCount: Int) :
    FragmentStateAdapter(activity) {

    override fun getItemCount(): Int {
        return itemsCount
    }

    override fun createFragment(position: Int): Fragment {
//        return if (position == 0) {
//            SettingFragment()
//        } else {
//            ProfileFragment()
//        }

        return when (position) {
            0 -> DashboardFragment()
            1 -> FriendFragment()
            2 -> ProfileFragment()
            3 -> SettingFragment()
            else -> DashboardFragment()
        }
    }
}