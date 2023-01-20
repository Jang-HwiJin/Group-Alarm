package com.example.groupalarm

import android.R
import android.app.DatePickerDialog
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.DatePicker
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.core.view.MotionEventCompat
import com.example.groupalarm.adapter.FriendAlarmInviteAdapter
import com.example.groupalarm.adapter.FriendSearchAdapter
import com.example.groupalarm.adapter.MoreAlarmInviteAdapter
import com.example.groupalarm.data.*
import com.example.groupalarm.databinding.ActivityCreateAlarmBinding
import com.example.groupalarm.databinding.ActivityMoreAlarmInviteBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*


class MoreAlarmInviteActivity : AppCompatActivity() {

    lateinit var binding: ActivityMoreAlarmInviteBinding

    val firestore = FirebaseFirestore.getInstance()

    // This is to display the usernames with @ in front of it
    var addedUsernamesList: ArrayList<String> = ArrayList()
    // This is used to actually pass on the data to the db
    var inviteUsersList: ArrayList<String> = ArrayList()

    val currUserEmail = FirebaseAuth.getInstance().currentUser!!.email!!
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!
    var currUsername = ""


    private lateinit var adapter: MoreAlarmInviteAdapter
    lateinit var friendsDb: CollectionReference
    lateinit var listener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMoreAlarmInviteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val alarmId = intent.getStringExtra("AlarmId")

        adapter = MoreAlarmInviteAdapter(this,
            FirebaseAuth.getInstance().currentUser!!.uid
        )

        binding.recyclerFriends.adapter = adapter

        //TODO Very naive solution to the toggles resetting when scrolling fast, need to work on it again more later
        // Source: https://stackoverflow.com/questions/50328655/recyclerview-items-values-reset-when-scrolling-down
        // This is also in DashboardActivity, AlarmChatsActivity
        binding.recyclerFriends.setItemViewCacheSize(100)

        binding.searchUserBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                adapter.clearUserList()
                if (alarmId != null) {
                    searchUsernames(query, alarmId)
                }
                return false
            }
            override fun onQueryTextChange(newText: String): Boolean {
                adapter.clearUserList()
                if (alarmId != null) {
                    searchUsernames(newText, alarmId)
                }


                /* TODO
                    this is here because I am not sure how exactly to pass data from the adapter to this activity,
                    so until I figure out how to activate something and pass it, it will be like this.
                    The reason this works is because whenever the query changes, I called getAddedUsersList() from the adapter,
                    I need to call it somehow from the activity or maybe implement an interface like I read from stackoverflow
                 */
                addedUsernamesList.clear()
                for (i in 0 until adapter.getAddedUsersList().size) {
                    var users = "@" + adapter.getAddedUsersList().get(i)
                    addedUsernamesList.add(users)
                }
                binding.addedUsersList.text = "Added users:" + addedUsernamesList.toString().replace("[","").replace("]","")

                return false
            }
        })

        binding.btnSave.setSafeOnClickListener() {
            inviteUsersList.clear()
            for (i in 0 until adapter.getAddedUsersList().size) {
                var users = adapter.getAddedUsersList().get(i)
                inviteUsersList.add(users)
            }

            var alarmInvitedUsers: ArrayList<String> = ArrayList()

            for(username in inviteUsersList) {
                FirebaseFirestore.getInstance().collection("usernames").document(username)
                    .get().addOnSuccessListener { documentSnapshot ->
                        val user = documentSnapshot.toObject(Username::class.java)
                        if(user != null) {
                            val userId = user.uid
                            alarmInvitedUsers.add(userId)
                        }
                    }.addOnFailureListener {
                        Toast.makeText(this, "Failed to retrieve user id from user's username", Toast.LENGTH_SHORT).show()
                    }
            }
            if (alarmId != null) {
                sendAlarmInvitation(alarmId, alarmInvitedUsers)
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }

        if (alarmId != null) {
            searchUsernames("", alarmId)
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

    fun sendAlarmInvitation(alarmId: String,  alarmInvitedUsers: ArrayList<String>,) {

        val alarmRef = firestore.collection("alarms").document(alarmId)
        val usersRef = firestore.collection("users")

        // Update invitee's invitedAlarms field in the users collection
        // Update the alarm's invitedUsers field in the alarms collection
        firestore.collection("alarms").document(alarmId)
            .get().addOnSuccessListener { alarmDoc ->
                val alarm = alarmDoc.toObject(Alarm::class.java)
                if (alarm!= null) {
                    alarmInvitedUsers.forEach {
                        val user = usersRef.document(it)
                        if(alarm.acceptedUsers?.contains(it) == false) {
                            user.update("invitedAlarms", FieldValue.arrayUnion(alarmDoc.id))
                            alarmRef.update("invitedUsers", FieldValue.arrayUnion(it))
                        }
                    }
                }
            }
        finish()
    }

    fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    private fun searchUsernames(query: String, alarmId: String) {
        val firestore = FirebaseFirestore.getInstance()
        val query = firestore.collection("users")
            .orderBy("username")
            .startAt(query)
            .endAt(query + "\uf8ff")
            .limit(11)
        query.get()
            .addOnSuccessListener { documents ->
                if (documents.size() > 0) {
                    for(document in documents) {
                        val user = document.toObject(User::class.java)
                        if (user != null && user.email != currUserEmail && !adapter.alreadyHasUserDisplayed(document.id)) {
                            firestore.collection("friends")
                                .whereEqualTo("userId1", currUserId)
                                .get()
                                .addOnSuccessListener { friendDocuments ->
                                    for (friendDocument in friendDocuments) {
                                        val friendId = friendDocument["userId2"] as String
                                        val status = friendDocument["status"] as String
                                        if (status == "accepted") {
                                            // Get the friend's user document
                                            firestore.collection("users").document(friendId).get()
                                                .addOnSuccessListener { snapshot ->
                                                    val friend = snapshot.toObject(User::class.java)
                                                    if (friend != null && user.username == friend.username && !adapter.alreadyHasUserDisplayed(document.id)) {
                                                        // Only allow to show friends who are not in the alarm already
                                                        firestore.collection("alarms").document(alarmId)
                                                            .get().addOnSuccessListener { alarmDoc ->
                                                                val alarm = alarmDoc.toObject(Alarm::class.java)
                                                                if(alarm != null) {
                                                                    if(!alarm.acceptedUsers?.contains(document.id)!!) {
                                                                        adapter.addUserToList(user, document.id)
                                                                    }

                                                                }

                                                            }
                                                    }
                                                }
                                        }
                                    }
                                }
                        }
                    }
                } else {
                    Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show()
                }
            }
    }


}