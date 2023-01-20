package com.example.groupalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
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

        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(this, AlarmReceiver::class.java)


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

        // Displays the number of pending alarm invites if there is at least 1
        getNumberOfPendingAlarmInvites()

        // Sends the alarms the user is part of to the adapter
        getAllUserAlarms()
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
                            if (alarm.acceptedUsers!!.contains(currUserId)) {
                                if (docChange.type == DocumentChange.Type.ADDED && !adapter.alreadyHasAlarmDisplayed(docChange.document.id)) {
                                    adapter.addAlarmToList(alarm, docChange.document.id)
                                    adapter.notifyDataSetChanged()


                                    // TODO
                                    //  Much much left to do, solve alarm problem
//                                    val intent = Intent(this@DashboardActivity, AlarmReceiver::class.java)
//                                    intent.putExtra(ALARM_REQUEST_CODE, docChange.document.id)
//                                    val pendingIntent = PendingIntent.getBroadcast(applicationContext, docChange.document.id.hashCode(), intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
//                                    alarmIntents.put(docChange.document.id, pendingIntent)
//                                    var calendar = Calendar.getInstance()
////                                    calendar.timeInMillis = System.currentTimeMillis()
//
//                                    var time = alarm.time.seconds * 1000
//
//                                    if (Calendar.AM_PM == 0)
//                                        time += (1000 * 60 * 60 * 12);
//                                    else
//                                        time += (1000 * 60 * 60 * 24);
//
//                                    calendar.timeInMillis = alarm.time.seconds * 1000
//
////                                    val hour = calendar.get(Calendar.HOUR_OF_DAY)
////                                    val minute = calendar.get(Calendar.MINUTE)
//////                                    val second = calendar.get(Calendar.SECOND)
////
////                                    calendar.set(Calendar.HOUR_OF_DAY, hour)
////                                    calendar.set(Calendar.MINUTE, minute)
//////                                    calendar.set(Calendar.SECOND, second)
//
//                                    // If alarm is not recurring, set exact date
//                                    if(!alarm.isRecurring) {
//
////                                        val month = calendar.get(Calendar.MONTH)
////                                        val day = calendar.get(Calendar.DAY_OF_MONTH)
////                                        val year = calendar.get(Calendar.YEAR)
////
////
////                                        calendar.set(Calendar.MONTH, month)
////                                        calendar.set(Calendar.DATE, day)
////                                        calendar.set(Calendar.YEAR, year)
//
//                                        Toast.makeText(
//                                            this@DashboardActivity, "Alarm is not recurring",
//                                            Toast.LENGTH_LONG
//                                        ).show()
//                                        alarmManager.setExact(AlarmManager.RTC_WAKEUP, time, pendingIntent)
//
//                                    } else {
//                                        // Set the alarm to repeat on certain days if recurring
//                                        val recurringDays = alarm.recurringDays.toIntArray()
//                                        if (recurringDays.isNotEmpty()) {
//                                            alarmManager.setRepeating(
//                                                AlarmManager.RTC_WAKEUP,
//                                                calendar.timeInMillis,
//                                                AlarmManager.INTERVAL_DAY * 7,
//                                                pendingIntent
//                                            )
//                                        }
//                                    }

                                    //TODO something about updating once remove friend is available, might be already fixed idk yet
                                } else if (docChange.type == DocumentChange.Type.REMOVED) {
                                    adapter.removeAlarmByKey(docChange.document.id)
                                    adapter.notifyDataSetChanged()

                                    var pendingIntentToBeRemoved = alarmIntents.get(docChange.document.id)
                                    if (pendingIntentToBeRemoved != null) {
                                        alarmManager.cancel(pendingIntentToBeRemoved)
                                    }
                                } else if (docChange.type == DocumentChange.Type.MODIFIED) {
                                    if (!adapter.alreadyHasAlarmDisplayed(docChange.document.id)) {
//                                        adapter.removeRequestByKey(docChange.document.id)
//                                        adapter.addAlarmToList(alarm, docChange.document.id)
                                    }
                                    adapter.notifyDataSetChanged()

                                    //TODO still need to work on this
//                                    var pendingIntent = alarmIntents.getOrPut(docChange.document.id) {
//                                        val intent = Intent(this@DashboardActivity, AlarmReceiver::class.java)
//                                        intent.putExtra(ALARM_REQUEST_CODE, docChange.document.id)
//                                        PendingIntent.getBroadcast(
//                                            applicationContext,
//                                            alarm.time.toInt(),
//                                            intent,
//                                            FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
//                                        )
//                                    }

                                }
                            }
                        }
                    }
                }
            }
        }
        listener = alarmsDb.addSnapshotListener(eventListener)

    }

    fun List<String>.toIntArray(): IntArray {
        val dayMapping = mapOf("M" to Calendar.MONDAY, "Tu" to Calendar.TUESDAY, "W" to Calendar.WEDNESDAY,
            "Th" to Calendar.THURSDAY, "F" to Calendar.FRIDAY, "Sa" to Calendar.SATURDAY, "Su" to Calendar.SUNDAY)
        return map { dayMapping[it] }.filterNotNull().toIntArray()
    }

    override fun onDestroy() {
        super.onDestroy()
        listener.remove()
    }


}