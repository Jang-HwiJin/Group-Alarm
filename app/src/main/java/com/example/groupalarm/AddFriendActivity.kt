package com.example.groupalarm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.groupalarm.databinding.ActivityAddFriendBinding

class AddFriendActivity : AppCompatActivity() {
    lateinit var binding: ActivityAddFriendBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddFriendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.bottomMenuNavigation.setSelectedItemId(R.id.addFriends)
        binding.bottomMenuNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.addFriends -> {
                    false
                }
                R.id.profile -> {
                    val intent = Intent(this@AddFriendActivity, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.home -> {
                    val intent = Intent(this@AddFriendActivity, ScrollingActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.settings -> {
                    val intent = Intent(this@AddFriendActivity, SettingActivity::class.java)
                    startActivity(intent)
                    true
                }
            }
            false
        }


        //IDEA
        /*
        Implement a recyclerview here for friends list, it'll basically be their username with their picture
        and on the top right, there'll be a fab where you can add friends and also another button on top with all the friend requests

         */

    }


}