
package com.example.groupalarm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.ActivityProfileBinding
import com.example.groupalarm.databinding.ActivityProfileDetailsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.util.*

class ProfileDetailsActivity : AppCompatActivity() {
    lateinit var binding: ActivityProfileDetailsBinding

    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!


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


        // When the user closes the app
        val presenceUserRef = Firebase.database.getReference("users").child(currUserId).child("activityStatus")
        presenceUserRef.onDisconnect().setValue(Timestamp(Calendar.getInstance().time))
    }

    override fun onResume() {
        super.onResume()
        val database = Firebase.database
        val usersRef = database.getReference("users").child(currUserId)
        usersRef.child("activityStatus").setValue(true)
    }

}