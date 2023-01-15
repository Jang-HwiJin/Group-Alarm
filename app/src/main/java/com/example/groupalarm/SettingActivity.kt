package com.example.groupalarm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.groupalarm.databinding.ActivitySettingBinding
import com.google.firebase.auth.FirebaseAuth

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
                    val intent = Intent(this@SettingActivity, DashboardActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.profile -> {
                    val intent = Intent(this@SettingActivity, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.friends -> {
                    val intent = Intent(this@SettingActivity, FriendActivity::class.java)
                    startActivity(intent)
                    true
                }
            }
            false
        }

        binding.signoutBtn.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this@SettingActivity, MainActivity::class.java)
            startActivity(intent)


        }
    }
}