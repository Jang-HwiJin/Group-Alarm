
package com.example.groupalarm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.groupalarm.data.Alarm
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.ActivityProfileBinding
import com.example.groupalarm.databinding.ActivityScrollingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ProfileActivity : AppCompatActivity() {
    lateinit var binding: ActivityProfileBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityProfileBinding.inflate(layoutInflater)

        setContentView(binding.root)

        binding.bottomMenuNavigation.setSelectedItemId(R.id.profile)
        binding.bottomMenuNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.profile -> {
                    false
                }
                R.id.home -> {
                    val intent = Intent(this@ProfileActivity, ScrollingActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.settings -> {
                    val intent = Intent(this@ProfileActivity, SettingActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.addFriends -> {
                    val intent = Intent(this@ProfileActivity, AddFriendActivity::class.java)
                    startActivity(intent)
                    true
                }
            }
            false
        }

        // Displaying the user's profile
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        //Displaying user's email
        val userEmail = FirebaseAuth.getInstance().currentUser!!.email!!
        binding.userEmail.text = userEmail

        // Displaying user's username
        FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERS)
            .document(userId).get().
            addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)
                if (user != null) {
                    binding.username.text = user.username
                }
            }

        // Displaying user's display Name
        FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERS)
            .document(userId).get().
            addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)
                if (user != null) {
                    binding.displayName.text = user.displayName
                }
            }


        binding.editProfileBtn.setOnClickListener {
            val intentDetails = Intent()
            intentDetails.setClass(
                this, EditProfileActivity::class.java
            )
            startActivity(Intent(intentDetails))

            // Send extra data later
//            intentDetails.putExtra(
//                "AlarmTitle", alarm.title
//            )
        }


    }

}