package group.alarm.groupalarm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import group.alarm.groupalarm.databinding.ActivitySettingBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.util.*

class SettingActivity : AppCompatActivity() {
    lateinit var binding: ActivitySettingBinding

    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid


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
            val userRef = FirebaseFirestore.getInstance().collection("users").document(currUserId)
            val update = mapOf("activityStatus" to false)
            userRef.update(update)

            // Turning user off
            val presenceUserRef = Firebase.database.getReference("users").child(currUserId).child("activityStatus")
            presenceUserRef.setValue(Timestamp(Calendar.getInstance().time))

            startActivity(intent)
            finish()
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