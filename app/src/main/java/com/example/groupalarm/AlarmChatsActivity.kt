package com.example.groupalarm

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Message
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.bumptech.glide.Glide
import com.example.groupalarm.adapter.AlarmInviteAdapter
import com.example.groupalarm.adapter.ChatsAdapter
import com.example.groupalarm.adapter.MoreAlarmInviteAdapter
import com.example.groupalarm.data.Alarm
import com.example.groupalarm.data.Chats
import com.example.groupalarm.data.Messages
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.ActivityAlarmChatsBinding
import com.example.groupalarm.dialog.LeaveAlarmDialog
import com.google.android.material.navigation.NavigationView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.ktx.Firebase
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.collections.ArrayList


class AlarmChatsActivity : AppCompatActivity() {
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navView: NavigationView
    private lateinit var toggle: ActionBarDrawerToggle

    lateinit var binding: ActivityAlarmChatsBinding

    val firestore = FirebaseFirestore.getInstance()
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!

    private lateinit var adapter: ChatsAdapter
    lateinit var messagesDb: CollectionReference
    lateinit var listener: ListenerRegistration


    override fun onCreate(savedInstanceState: Bundle?) {
        var alarm: Alarm
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmChatsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        title = ""



        adapter = ChatsAdapter(this,
            FirebaseAuth.getInstance().currentUser!!.uid
        )

        binding.recyclerChats.adapter = adapter
        binding.recyclerChats.smoothScrollToPosition(100)

        //TODO Very naive solution to the toggles resetting when scrolling fast, need to work on it again more later
        // Source: https://stackoverflow.com/questions/50328655/recyclerview-items-values-reset-when-scrolling-down
        // This is also in CreateAlarmActivity, and DashboardActivity
        binding.recyclerChats.setItemViewCacheSize(100)

        val alarmId = intent.getStringExtra("AlarmId")

        val usersRef = FirebaseFirestore.getInstance().collection("users")

        if (alarmId != null) {
            firestore.collection("alarms").document(alarmId)
                .get().addOnSuccessListener { alarmDoc ->
                    val alarm = alarmDoc.toObject(Alarm::class.java)
                    if (alarmDoc != null) {
                        val chatRef =
                            alarm?.let {
                                FirebaseFirestore.getInstance().collection("chats").document(
                                    it.chatId)
                            }
                        if (chatRef != null) {
                            chatRef.get().addOnSuccessListener {
                                if (it.exists()) {
                                    val users = it.get("users") as List<String>
                                    if (!users.contains(currUserId)) {
                                        finish()
                                    } else {
                                        addMenuItemInNavMenuDrawer(alarmId)
                                    }
                                } else {
                                    finish()
                                }
                            }
                        }
                    }
                    else {
                        finish()
                    }
                }.addOnFailureListener {
                    finish()
                }
        } else {
            finish()
        }

        drawerLayout = binding.drawerLayout
        navView = binding.navigationView


        // Setup the navigation drawer toggle button
        toggle = ActionBarDrawerToggle(
            this, drawerLayout, R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        getSupportActionBar()?.setDisplayHomeAsUpEnabled(true);


        // Handle navigation drawer item clicks
//        navView.setNavigationItemSelectedListener { menuItem ->
//            when (menuItem.itemId) {
//                R.id.nav_chat -> {
//                    // Handle chat item click
//                }
//                R.id.nav_settings -> {
//                    // Handle settings item click
//                }
//            }
//            drawerLayout.closeDrawer(GravityCompat.START)
//            true
//        }


        if (alarmId != null) {
            setupNavigationView(alarmId)
            firestore.collection("alarms").document(alarmId)
                .get().addOnSuccessListener { docSnapshot ->
                    val alarmDoc = docSnapshot.toObject(Alarm::class.java)
                    if (alarmDoc != null) {
                        alarm = alarmDoc
                        getAllMessages(alarm)
                    }
                }
        }

        //TODO find out how I will get the user online status to be updated dynamically
//        binding.constraintLayout.setOnClickListener {
//            if (alarmId != null) {
//                addMenuItemInNavMenuDrawer(alarmId)
//            }
//        }

        binding.btnSendMessage.setOnClickListener {
            if (alarmId != null) {
                firestore.collection("alarms").document(alarmId)
                    .get().addOnSuccessListener { docSnapshot ->
                        val alarmDoc = docSnapshot.toObject(Alarm::class.java)
                        if (alarmDoc != null) {
                            alarm = alarmDoc

                            if(binding.messageBox.text.toString().trim().isNotEmpty()) {
                                val chatsCollection = firestore.collection("chats").document(alarm.chatId)
                                val messagesRef = FirebaseFirestore.getInstance().collection("messages").document()
                                val currentTime = Timestamp(Calendar.getInstance().time)
                                val newMessage = Messages(
                                    currUserId,
                                    binding.messageBox.text.toString(),
                                    currentTime,
                                    "text",
                                    alarm.chatId
                                )
                                messagesRef.set(newMessage)

                                if (chatsCollection != null) {
                                    chatsCollection.update(
                                        "lastMessageTimestamp", currentTime,
                                    )
                                    binding.messageBox.text.clear()
                                }
                            } else {
                                binding.messageBox.error = "Message cannot be empty"
                            }
                        }
                    }
            }

        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return if (toggle.onOptionsItemSelected(item)) {
            true
        } else super.onOptionsItemSelected(item)
    }

    fun getBitmapFromURL(src: String): Bitmap? {
        return try {
//            Log.e("src", src)
            val url = URL(src)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.setDoInput(true)
            connection.connect()
            val input: InputStream = connection.getInputStream()
            val myBitmap = BitmapFactory.decodeStream(input)
//            Log.e("Bitmap", "returned")
            myBitmap
        } catch (e: IOException) {
            e.printStackTrace()
//            Log.e("Exception", e.getMessage())
            null
        }
    }

    private fun setupNavigationView(alarmId: String) {
        val headerView = navView.getHeaderView(0)
        val profilePicImg = headerView.findViewById<ImageView>(R.id.profile_picture)
        val userName = headerView.findViewById<TextView>(R.id.user_name)
        val displayName = headerView.findViewById<TextView>(R.id.display_name)
        val inviteUsers = headerView.findViewById<Button>(R.id.invite_users)
        val leaveAlarm = headerView.findViewById<Button>(R.id.leave_alarm)

        firestore.collection("users").document(currUserId)
            .get().addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)
                if(user != null) {
                    userName.text = "Username: " + user.username
                    displayName.text = "Display name: " + user.displayName
                    Glide.with(this).load(user.profileImg).into(
                        profilePicImg
                    )
                }
            }
        val alarmTitle = headerView.findViewById<TextView>(R.id.alarm_title)
        firestore.collection("alarms").document(alarmId)
            .get().addOnSuccessListener { documentSnapshot ->
                val alarm = documentSnapshot.toObject(Alarm::class.java)
                if(alarm != null) {
                    alarmTitle.text = alarm.title
                }
            }

        inviteUsers.setOnClickListener {
            val intentDetails = Intent()
            intentDetails.setClass(
                this, MoreAlarmInviteActivity::class.java
            )
            intentDetails.putExtra(
                "AlarmId", alarmId
            )
            startActivity(Intent(intentDetails))
        }

        leaveAlarm.setOnClickListener {
            val leaveAlarmDialog = LeaveAlarmDialog(alarmId)
            leaveAlarmDialog.show(
                supportFragmentManager,
                getString(R.string.leave_alarm_confirmation)
            )
        }
    }

    fun userOnOffUserCount(acceptedUsers: ArrayList<String>, activeUsersList: ArrayList<String>, inactiveUsersList: ArrayList<String>) {
        val navView = binding.navigationView
        val menu = navView.menu

        //TODO
        // Just using dummy and useless calls to get the right callback order, otherwise user count for activity doesnt update properly
        // need to fix and refactor later
        val userRef = FirebaseFirestore.getInstance().collection("users").document(currUserId)
        userRef.get().addOnSuccessListener {

            // Removing the previous submenus
            menu.clear()

            userRef.get().addOnSuccessListener {
                val subOnline = menu.addSubMenu("Online Members - " + activeUsersList.size)
                val subOffline = menu.addSubMenu("Offline Members - " + inactiveUsersList.size)
                for (userId in acceptedUsers) {
                    val userRef = FirebaseFirestore.getInstance().collection("users").document(userId)
                    userRef.get().addOnSuccessListener { userSnapshot ->
                        val user = userSnapshot.toObject(User::class.java)
                        // Do something with the user object, such as adding it to a list or displaying it on the screen
                        if(user != null) {
                            if(activeUsersList.contains(userId)) {
                                subOnline.add(user.username)
                            } else {
                                subOffline.add(user.username)
                            }
                        }
                    }
                }
                navView.invalidate()
            }
        }


    }

    private fun addMenuItemInNavMenuDrawer(alarmId: String) {
        val navView = binding.navigationView
        val menu = navView.menu

        var acceptedUsers = ArrayList<String>()
        var activeUsersList = ArrayList<String>()
        var inactiveUsersList = ArrayList<String>()

        val alarmsRef = FirebaseFirestore.getInstance().collection("alarms").document(alarmId)
        val presenceUserRef = Firebase.database.getReference("users")

        alarmsRef.get().addOnSuccessListener { alarmSnapshot ->
            acceptedUsers = alarmSnapshot.get("acceptedUsers") as ArrayList<String>
//            Toast.makeText(
//                this@AlarmChatsActivity, "Size of acceptedUsers is" + acceptedUsers.size + " User id is " + acceptedUsers[0],
//                Toast.LENGTH_LONG
//            ).show()

            // Determine if user is active or not
            for(user in acceptedUsers) {
                presenceUserRef.child(user).child("activityStatus").get().addOnSuccessListener { dataSnapshot ->
                    if(dataSnapshot!= null) {
                        if(dataSnapshot.value == true) {
                            activeUsersList.add(user)
                        } else {
                            inactiveUsersList.add(user)
                        }

                    }

                }
            }

            //TODO
            // Another fake dummy call to get the correct callback order
            alarmsRef.get().addOnSuccessListener {
                userOnOffUserCount(acceptedUsers, activeUsersList, inactiveUsersList)
            }
        }

    }

    private fun getAllMessages(alarm: Alarm) {

        val alarmsRef = firestore.collection("alarms")
        val usersRef = firestore.collection("users")
        val messagesRef = firestore.collection("messages")
        messagesDb = firestore.collection("messages")


        val eventListener = object : EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?,
                                 e: FirebaseFirestoreException?) {
                if (e != null) {
                    Toast.makeText(
                        this@AlarmChatsActivity, "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val messagesRef = FirebaseFirestore.getInstance().collection("messages").limit(50)
                val query = messagesRef.whereEqualTo("chatId", alarm.chatId)
                val messagesTask = query.addSnapshotListener { documents, exception ->
                    if (exception != null) {
                        return@addSnapshotListener
                    }
                    if (documents!= null) {
                        val numMessages = documents.size()
//                        binding.recyclerChats.smoothScrollToPosition(numMessages - 1)
                        val messages = documents.toObjects(Messages::class.java)
                        val sortedMessages = messages.orEmpty().sortedBy { it.timestamp }
                        adapter.clearMessageList()
                        if (documents != null) {
                            for (i in 0 until documents.size()) {
                                val message = sortedMessages[i]
                                val messageId = documents.documents[i].id
                                if (!adapter.alreadyHasMessageDisplayed(messageId)) {
                                    adapter.addMessageToList(message, messageId)
                                    adapter.notifyDataSetChanged()
                                }
                            }
                        }
                    }
                }

//
//                val listener = query.addSnapshotListener { documents, exception ->
//                    if (exception != null) {
//                        return@addSnapshotListener
//                    }
//                    if (documents != null) {
//                        for (document in documents) {
//                            if (document != null && !adapter.alreadyHasMessageDisplayed(document.id)) {
//                                val message = document.toObject(Messages::class.java)
//                                adapter.addMessageToList(message, document.id)
//                                adapter.notifyDataSetChanged()
//
//                            }
//                        }
//                    }
//                }
                //TODO
                // Not sure if I will allow for message deletes in the future but if I do, I need to implement below

//                    .addOnFailureListener {
//                    Toast.makeText(this@AlarmChatsActivity,
//                        "Error while retrieving chat messages. Please try again later.", Toast.LENGTH_SHORT).show()
//                }

//                for (docChange in querySnapshot?.getDocumentChanges()!!) {
////                    // If new alarm is added
//                    FirebaseFirestore.getInstance().collection("alarm").document().get().addOnSuccessListener { documentSnapshot ->
//                        val alarm = documentSnapshot.toObject(Alarm::class.java)
//                        if (alarm != null ) {
//                            if (alarm.invitedUsers?.contains(currUserId) == true) {
//                                if (docChange.type == DocumentChange.Type.ADDED) {
////                                        adapter.addRequestsToList(request, docChange.document.id)
//                                    adapter.notifyDataSetChanged()
////                                    /*Todo
////                                       this probably needs to be implemented furthermore once I add a remove friend functionality */
//                                } else if (docChange.type == DocumentChange.Type.REMOVED) {
//                                    adapter.removeAlarmByKey(docChange.document.id)
//                                    adapter.notifyDataSetChanged()
//                                } else if (docChange.type == DocumentChange.Type.MODIFIED) {
//                                    val request = docChange.document.toObject(User::class.java)
//                                    if (!adapter.alreadyHasAlarmDisplayed(docChange.document.id)) {
////                                        adapter.removeRequestByKey(docChange.document.id)
////                                            adapter.addRequestsToList(request, docChange.document.id)
//                                    }
//                                    adapter.notifyDataSetChanged()
//                                }
//                            }
//
//
//                        }
//                    }
//                }
            }
        }
        listener = messagesDb.addSnapshotListener(eventListener)

    }

}