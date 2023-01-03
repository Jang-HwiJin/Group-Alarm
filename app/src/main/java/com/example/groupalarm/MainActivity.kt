package com.example.groupalarm

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION
import android.widget.Toast
import android.provider.Settings
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.groupalarm.adapter.AuthViewPagerAdapter
import com.example.groupalarm.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayoutMediator


class MainActivity : AppCompatActivity() {

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // enable alarm overlay in background
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())
                )
                startActivityForResult(intent, 101)
            }
        }

        val myViewPagerAdapter = AuthViewPagerAdapter(this, 2)

        binding.mainViewPager.adapter = myViewPagerAdapter

        var pageNames: Array<String> = resources.getStringArray(R.array.tab_names)
        TabLayoutMediator(binding.tabLayout, binding.mainViewPager) { tab, position ->
            tab.text = pageNames[position]
        }.attach()

        //mainViewPager.setPageTransformer(ZoomOutPageTransformer())
//            mainViewPager.setPageTransformer(DepthPageTransformer())
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