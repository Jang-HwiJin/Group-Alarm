package com.example.groupalarm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.groupalarm.databinding.ActivityProfileBinding
import com.example.groupalarm.databinding.ActivityScrollingBinding
import com.example.groupalarm.databinding.ActivitySettingBinding

class SettingActivity : AppCompatActivity() {
    lateinit var binding: ActivitySettingBinding



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.bottomMenuNavigation.setSelectedItemId(R.id.settings)


        binding.bottomMenuNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.settings -> {
                    false
                }
                R.id.home -> {
                    val intent = Intent(this@SettingActivity, ScrollingActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.profile -> {
                    val intent = Intent(this@SettingActivity, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
            }
            false
        }
    }
}