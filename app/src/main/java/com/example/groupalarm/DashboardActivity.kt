package com.example.groupalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.groupalarm.adapter.AlarmAdapter
import com.example.groupalarm.data.Alarm
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.ActivityDashboardBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.ktx.Firebase
import java.util.*


class DashboardActivity : AppCompatActivity() {

    lateinit var binding: ActivityDashboardBinding

    lateinit var alarmManager: AlarmManager

    val firestore = FirebaseFirestore.getInstance()
    val currUserEmail = FirebaseAuth.getInstance().currentUser!!.email!!
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!

    companion object {
        const val COLLECTION_ALARMS = "alarms"
        const val ALARM_REQUEST_CODE = "alarmRequestCode"
        var alarmIntents = hashMapOf<String, PendingIntent>()
        var alarmIds = hashMapOf<Alarm, String>()
        var alarmTitles = hashMapOf<String, String>()
    }

    lateinit var alarmsDb: CollectionReference
    lateinit var listener: ListenerRegistration
    private lateinit var adapter: AlarmAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)


        adapter = AlarmAdapter(this,
            FirebaseAuth.getInstance().currentUser!!.uid
        )
        binding.recyclerAlarms.adapter = adapter

        //TODO Very naive solution to the toggles resetting when scrolling fast, need to work on it again more later
        // Source: https://stackoverflow.com/questions/50328655/recyclerview-items-values-reset-when-scrolling-down
        // This is also in CreateAlarmActivity
        binding.recyclerAlarms.setItemViewCacheSize(100)


        binding.addAlarmFab.setOnClickListener {
            val intentDetails = Intent()
            intentDetails.setClass(
                this, CreateAlarmActivity::class.java
            )
            startActivity(Intent(intentDetails))
        }

        binding.alarmInviteFab.setOnClickListener {
            val intentDetails = Intent()
            intentDetails.setClass(
                this, AlarmInvitesActivity::class.java
            )
            startActivity(Intent(intentDetails))
        }

        alarmManager = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager

        // Displays the number of pending alarm invites if there is at least 1
        getNumberOfPendingAlarmInvites()

        // Sends the alarms the user is part of to the adapter
        getAllUserAlarms()

        binding.bottomMenuNavigation.setSelectedItemId(R.id.home)
        binding.bottomMenuNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    false
                }
                R.id.profile -> {
                    val intent = Intent(this@DashboardActivity, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.settings -> {
                    val intent = Intent(this@DashboardActivity, SettingActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.friends -> {
                    val intent = Intent(this@DashboardActivity, FriendActivity::class.java)
                    startActivity(intent)
                    true
                }
            }
            false
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

    private fun getNumberOfPendingAlarmInvites()
    {
        val alarmsRef = firestore.collection("alarms")
        alarmsDb = alarmsRef

        // Doing this to get the number of pending alarm invites
        val eventListener = object : EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?,
                                 e: FirebaseFirestoreException?) {
                if (e != null) {
                    Toast.makeText(
                        this@DashboardActivity, "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                var counter = 0
                val query = alarmsRef.whereArrayContains("invitedUsers", currUserId)
                query.get().addOnSuccessListener { documents ->
                    for (document in documents) {
                        val alarm = document.toObject(Alarm::class.java)
                        if(alarm != null) {
                            counter += 1
                            if (counter > 0) {
                                binding.fakeButtonForAlarmInviteNumber.show()
                                binding.numPendingRequestsNotif.visibility = View.VISIBLE
                                binding.numPendingRequestsNotif.text = counter.toString()
                            }
                        }
                    }
                }.addOnFailureListener {

                }

                for (docChange in querySnapshot?.getDocumentChanges()!!) {
                    // If new request is added
                    FirebaseFirestore.getInstance().collection("alarms").document().get().addOnSuccessListener { documentSnapshot ->
                        val alarm = documentSnapshot.toObject(Alarm::class.java)
                        if (alarm != null ) {
                            if (alarm.acceptedUsers?.contains(currUserId) == true) {
                                if (docChange.type == DocumentChange.Type.ADDED) {
                                    adapter.notifyDataSetChanged()
                                    /*Todo
                                       this probably needs to be implemented furthermore once I add a remove friend functionality */
                                } else if (docChange.type == DocumentChange.Type.REMOVED) {
                                    adapter.notifyDataSetChanged()
                                } else if (docChange.type == DocumentChange.Type.MODIFIED) {
                                    val request = docChange.document.toObject(User::class.java)
                                    adapter.notifyDataSetChanged()
                                }
                            }


                        }
                    }
                }
                if(counter == 0) {
                    binding.fakeButtonForAlarmInviteNumber.hide()
                    binding.numPendingRequestsNotif.text = counter.toString()
                    binding.numPendingRequestsNotif.visibility = View.GONE
                }
            }
        }
        listener = alarmsDb.addSnapshotListener(eventListener)
    }

    private fun getAllUserAlarms() {
        val alarmsRef = firestore.collection("alarms")
        val usersRef = firestore.collection("users")
        alarmsDb = firestore.collection("friends")


        val eventListener = object : EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?,
                                 e: FirebaseFirestoreException?) {
                if (e != null) {
                    Toast.makeText(
                        this@DashboardActivity, "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                // Find the userId in the invitedUsers list in the alarms collection and add alarm invite to recycler
                val query = alarmsRef.whereArrayContains("acceptedUsers", currUserId)
                query.get().addOnSuccessListener { documents ->
                    for (document in documents) {
                        val alarm = document.toObject(Alarm::class.java)
                        if(alarm != null && !adapter.alreadyHasAlarmDisplayed(document.id)) {
                            adapter.addAlarmToList(alarm, document.id)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(this@DashboardActivity,
                        "Error while retrieving pending alarm invites", Toast.LENGTH_SHORT).show()
                }

                for (docChange in querySnapshot?.getDocumentChanges()!!) {
                    FirebaseFirestore.getInstance().collection("alarms").document().get().addOnSuccessListener { documentSnapshot ->
                        val alarm = documentSnapshot.toObject(Alarm::class.java)
                        if (alarm != null ) {
                            if (alarm.acceptedUsers?.contains(currUserId) == true && !adapter.alreadyHasAlarmDisplayed(docChange.document.id)) {
                                if (docChange.type == DocumentChange.Type.ADDED) {
//                                        adapter.addRequestsToList(request, docChange.document.id)
                                    adapter.notifyDataSetChanged()
//                                    /*Todo
//                                       this probably needs to be implemented furthermore once I add a remove friend functionality */
                                } else if (docChange.type == DocumentChange.Type.REMOVED) {
                                    adapter.removeAlarmByKey(docChange.document.id)
                                    adapter.notifyDataSetChanged()
                                } else if (docChange.type == DocumentChange.Type.MODIFIED) {
                                    if (!adapter.alreadyHasAlarmDisplayed(docChange.document.id)) {
//                                        adapter.removeRequestByKey(docChange.document.id)
//                                            adapter.addAlarmToList(alarm, docChange.document.id)
                                    }
                                    adapter.notifyDataSetChanged()

                                }
                            }
                        }
                    }
                }
            }
        }
        listener = alarmsDb.addSnapshotListener(eventListener)

    }

    override fun onDestroy() {
        super.onDestroy()
        listener.remove()
    }

}