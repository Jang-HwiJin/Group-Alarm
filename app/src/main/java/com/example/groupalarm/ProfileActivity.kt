
package com.example.groupalarm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.ActivityProfileBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.util.*

class ProfileActivity : AppCompatActivity() {
    lateinit var binding: ActivityProfileBinding

    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!

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
                    val intent = Intent(this@ProfileActivity, DashboardActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.settings -> {
                    val intent = Intent(this@ProfileActivity, SettingActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.friends -> {
                    val intent = Intent(this@ProfileActivity, FriendActivity::class.java)
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

        // Displaying user's information
        FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERS)
            .document(userId).get().
            addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)
                if (user != null) {
                    binding.username.text = "@" + user.username
                    binding.displayName.text = user.displayName
                    if(user.profileImg != "") {
                        Glide.with(this)
                            .load(user.profileImg)
                            .into(binding.profilePicture)
                    }
                }
            }


        binding.editProfileBtn.setOnClickListener {
            val intentDetails = Intent()
            intentDetails.setClass(
                this, EditProfileActivity::class.java
            )
            startActivity(Intent(intentDetails))
        }


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