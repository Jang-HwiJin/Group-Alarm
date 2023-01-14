
package com.example.groupalarm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.ActivityProfileBinding
import com.example.groupalarm.databinding.ActivityProfileDetailsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileDetailsActivity : AppCompatActivity() {
    lateinit var binding: ActivityProfileDetailsBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileDetailsBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val userUsername = intent.getStringExtra("Username")
        val userDisplayName = intent.getStringExtra("DisplayName")
        val userProfileImgUrl = intent.getStringExtra("ProfileImgUrl")

        binding.username.text = "@" + userUsername
        binding.displayName.text = userDisplayName
        Glide.with(this)
            .load(userProfileImgUrl)
            .into(binding.profilePicture)


    }

}