package com.example.groupalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.*
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.groupalarm.adapter.AlarmAdapter
import com.example.groupalarm.data.Alarm
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.ActivityScrollingBinding
import com.example.groupalarm.dialog.AlarmDialog
import com.example.groupalarm.dialog.AlarmPermissionDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import java.util.*

class ScrollingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScrollingBinding

    lateinit var alarmManager: AlarmManager

    companion object {
        const val COLLECTION_ALARMS = "alarms"
        const val ALARM_REQUEST_CODE = "alarmRequestCode"
        var alarmIntents = hashMapOf<String, PendingIntent>()
        var alarmIds = hashMapOf<Alarm, String>()
        var alarmTitles = hashMapOf<String, String>()
    }

    lateinit var alarmDb: CollectionReference
    lateinit var listener: ListenerRegistration


    private lateinit var adapter: AlarmAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityScrollingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AlarmAdapter(this,
            FirebaseAuth.getInstance().currentUser!!.uid
        )
        binding.recyclerPosts.adapter = adapter


        setSupportActionBar(findViewById(R.id.toolbar))
        binding.toolbarLayout.title = title

        binding.fab.setOnClickListener {
            val itemDialog = AlarmDialog()
            itemDialog.show(supportFragmentManager, "Add an Alarm")
        }

        alarmManager = applicationContext.getSystemService(ALARM_SERVICE) as AlarmManager

        getAllAlarms()

    }

    private fun getAllAlarms() {
        alarmDb = FirebaseFirestore.getInstance().collection(COLLECTION_ALARMS)
        val userEmail = FirebaseAuth.getInstance().currentUser!!.email!!
        val eventListener = object : EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?,
                                 e: FirebaseFirestoreException?) {
                if (e != null) {
                    Toast.makeText(
                        this@ScrollingActivity, "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                for (docChange in querySnapshot?.getDocumentChanges()!!) {
                    // If new alarm is added

                    if (docChange.type == DocumentChange.Type.ADDED) {


                        // Currently only fire off alarms that are set after current system time

                        val alarm = docChange.document.toObject(Alarm::class.java)
                        alarmIds[alarm] = docChange.document.id
                        alarmTitles[docChange.document.id] = alarm.title
                        if(userEmail != alarm.owner && !alarm.users.map { o -> o.email }.contains(userEmail)) {
                            // Shows a dialog asking if user wants to accept or decline
                            // a newly created alarm
                            if (Date(alarm.time) >= Calendar.getInstance().time) {
                                val alarmPermissionDialog =
                                    AlarmPermissionDialog(docChange.document.id)
                                alarmPermissionDialog.show(
                                    supportFragmentManager,
                                    getString(R.string.alarmDecision)
                                )
                            }
                            else {
                                adapter.addAlarm(alarm, docChange.document.id)
                            }
                        }
                        // either the current user is the owner, or he/she's already in user list =>
                        // just add alarm row and set alarm
                        else {
                            adapter.addAlarm(alarm, docChange.document.id)
                            // set alarm
                            if (Date(alarm.time) >= Calendar.getInstance().time) {
                                val intent =
                                    Intent(this@ScrollingActivity, AlarmReceiver::class.java)

                                intent.putExtra(ALARM_REQUEST_CODE, docChange.document.id)
                                var pendingIntent = PendingIntent.getBroadcast(
                                    applicationContext,
                                    alarm.time.toInt(),
                                    intent,
                                    FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
                                )
                                alarmIntents.put(docChange.document.id, pendingIntent)
                                alarmManager.setExact(
                                    AlarmManager.RTC_WAKEUP,
                                    alarm.time,
                                    pendingIntent
                                )
                            }
                        }
                    } else if (docChange.type == DocumentChange.Type.REMOVED) {
                        adapter.removePostByKey(docChange.document.id)
                        var pendingIntentToBeRemoved = alarmIntents.get(docChange.document.id)
                        if (pendingIntentToBeRemoved != null) {
                            alarmManager.cancel(pendingIntentToBeRemoved)
                        }
                    } else if (docChange.type == DocumentChange.Type.MODIFIED) {
                        val alarm = docChange.document.toObject(Alarm::class.java)
                        var pendingIntent = alarmIntents.getOrPut(docChange.document.id) {
                            val intent = Intent(this@ScrollingActivity, AlarmReceiver::class.java)
                            intent.putExtra(ALARM_REQUEST_CODE, docChange.document.id)
                            PendingIntent.getBroadcast(
                                applicationContext,
                                alarm.time.toInt(),
                                intent,
                                FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT
                            )
                        }
                        if (!adapter.alreadyHasAlarmDisplayed(docChange.document.id)) {
                            alarmIds[alarm] = docChange.document.id
                            adapter.addAlarm(alarm, docChange.document.id)
                        }
                        if (alarm.users.map{o -> o.email}.contains(userEmail)) {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, alarm.time, pendingIntent);
                            }
                            else {
                                alarmManager.cancel(pendingIntent)
                            }
                    }
                }
            }
        }
        listener = alarmDb.addSnapshotListener(eventListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        listener.remove()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_scrolling, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}