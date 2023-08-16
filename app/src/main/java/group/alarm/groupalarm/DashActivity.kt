package group.alarm.groupalarm

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
import android.widget.Toast
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import group.alarm.groupalarm.adapter.DashViewPagerAdapter
import group.alarm.groupalarm.databinding.ActivityDashBinding
import com.google.android.material.tabs.TabLayoutMediator


class DashActivity : AppCompatActivity() {

    lateinit var binding: ActivityDashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDashBinding.inflate(layoutInflater)
        setContentView(binding.root)
//
//        // enable alarm overlay in background
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!Settings.canDrawOverlays(this)) {
//                val intent = Intent(
//                    ACTION_MANAGE_OVERLAY_PERMISSION,
//                    Uri.parse("package:" + getPackageName())
//                )
//                startActivityForResult(intent, 101)
//            }
//        }

        val myViewPagerAdapter = DashViewPagerAdapter(this, 4)

        binding.mainViewPager.adapter = myViewPagerAdapter

        var pageNames: Array<String> = resources.getStringArray(R.array.dash_tab_names)
        TabLayoutMediator(binding.tabLayout, binding.mainViewPager) { tab, position ->
            tab.text = pageNames[position]
        }.attach()

        //mainViewPager.setPageTransformer(ZoomOutPageTransformer())
//            mainViewPager.setPageTransformer(DepthPageTransformer())
    }

    override fun onResume() {
        super.onResume()
    }

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 101) {
            if (!Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "This feature is crucial to this app", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

}