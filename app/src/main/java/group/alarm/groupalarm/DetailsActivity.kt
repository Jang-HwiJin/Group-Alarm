package group.alarm.groupalarm

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import group.alarm.groupalarm.data.User
import group.alarm.groupalarm.databinding.ActivityDetailsBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class DetailsActivity : AppCompatActivity() {

    lateinit var binding: ActivityDetailsBinding

    val firestore = FirebaseFirestore.getInstance()

    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intentTitle = intent.getStringExtra("AlarmTitle")
        val intentTime = intent.getLongExtra("AlarmTime", 0)
        val intentOwner = intent.getStringExtra("AlarmOwner")
        val intentUserList = intent.getSerializableExtra("AlarmUserList") as ArrayList<User>


        binding.alarmTitle.text = intentTitle.toString()
        binding.alarmTime.text = convertTimeForDisplay(intentTime)
        binding.alarmOwner.text = intentOwner.toString()


        var userNames: ArrayList<String> = ArrayList()
        for (i in 0 until intentUserList.size) {
            var username = intentUserList.get(i).username

            userNames.add(username)
        }
        binding.alarmUserList.text = userNames.toString().replace("[","").replace("]","")


        binding.backBtn.setOnClickListener {
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

    private fun convertTimeForDisplay(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("hh:mm a")
        return format.format(date)
    }

}