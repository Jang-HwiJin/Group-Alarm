package com.example.groupalarm

import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.DatePicker
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import com.example.groupalarm.adapter.FriendAlarmInviteAdapter
import com.example.groupalarm.data.*
import com.example.groupalarm.databinding.ActivityCreateAlarmBinding
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.ktx.Firebase
import java.text.SimpleDateFormat
import java.util.*


class CreateAlarmActivity : AppCompatActivity() {

    lateinit var binding: ActivityCreateAlarmBinding

    val firestore = FirebaseFirestore.getInstance()


    private var daysofWeekList = mutableListOf<String>()
    var summaryDaysofWeekList: ArrayList<String> = ArrayList()

    // This is to display the usernames with @ in front of it
    var addedUsernamesList: ArrayList<String> = ArrayList()
    // This is used to actually pass on the data to the db
    var inviteUsersList: ArrayList<String> = ArrayList()

    var alarmDates: ArrayList<Timestamp> = ArrayList()

    val currUserEmail = FirebaseAuth.getInstance().currentUser!!.email!!
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!
    var currUsername = ""


    private lateinit var adapter: FriendAlarmInviteAdapter
    lateinit var friendsDb: CollectionReference
    lateinit var listener: ListenerRegistration



    var calendar = Calendar.getInstance()
    var dateCalendar = Calendar.getInstance()

    var mondayToggle = false
    var tuesdayToggle = false
    var wednesdayToggle = false
    var thursdayToggle = false
    var fridayToggle = false
    var saturdayToggle = false
    var sundayToggle = false

    @RequiresApi(Build.VERSION_CODES.M)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCreateAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = FriendAlarmInviteAdapter(this,
            FirebaseAuth.getInstance().currentUser!!.uid
        )

        binding.recyclerFriends.adapter = adapter

        //TODO Very naive solution to the toggles resetting when scrolling fast, need to work on it again more later
        // Source: https://stackoverflow.com/questions/50328655/recyclerview-items-values-reset-when-scrolling-down
        // This is also in DashboardActivity, AlarmChatsActivity
        binding.recyclerFriends.setItemViewCacheSize(100)


        // create an OnDateSetListener
        val dateSetListener = object : DatePickerDialog.OnDateSetListener {
            override fun onDateSet(view: DatePicker, year: Int, monthOfYear: Int,
                                   dayOfMonth: Int) {
                dateCalendar.set(Calendar.YEAR, year)
                dateCalendar.set(Calendar.MONTH, monthOfYear)
                dateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                updateDateInView()
            }
        }

        binding.btnChooseDate.setOnClickListener(object : View.OnClickListener {
            override fun onClick(view: View) {
                DatePickerDialog(this@CreateAlarmActivity,
                    dateSetListener,
                    // set DatePickerDialog to point to today's date when it loads up
                    dateCalendar.get(Calendar.YEAR),
                    dateCalendar.get(Calendar.MONTH),
                    dateCalendar.get(Calendar.DAY_OF_MONTH)).show()
            }

        })

//        binding.monday.setOnClickListener {
//            if(mondayToggle) {
//                daysofWeekList.remove("M")
//                binding.monday.setTypeface(null, Typeface.NORMAL)
////                binding.monday.setTextColor(Color.parseColor ("#FFFFFF"))
//                binding.monday.setBackgroundResource(android.R.color.transparent)
//                mondayToggle = false
//                updateRepeatedDaysSummary()
//            } else {
//                daysofWeekList.add("M")
//                binding.monday.setTypeface(null, Typeface.BOLD)
////                binding.monday.setTextColor(Color.parseColor ("#13005A"))
//                binding.monday.setBackgroundResource(com.example.groupalarm.R.drawable.circle_shape)
//                mondayToggle = true
//                updateRepeatedDaysSummary()
//            }
//        }
//        binding.tuesday.setOnClickListener {
//            if(tuesdayToggle) {
//                daysofWeekList.remove("T")
//                binding.tuesday.setTypeface(null, Typeface.NORMAL)
////                binding.tuesday.setTextColor(Color.parseColor ("#FFFFFF"))
//                binding.tuesday.setBackgroundResource(android.R.color.transparent)
//                tuesdayToggle = false
//                updateRepeatedDaysSummary()
//            } else {
//                daysofWeekList.add("T")
//                binding.tuesday.setTypeface(null, Typeface.BOLD)
////                binding.tuesday.setTextColor(Color.parseColor ("#13005A"))
//                binding.tuesday.setBackgroundResource(com.example.groupalarm.R.drawable.circle_shape)
//                tuesdayToggle = true
//                updateRepeatedDaysSummary()
//            }
//        }
//        binding.wednesday.setOnClickListener {
//            if(wednesdayToggle) {
//                daysofWeekList.remove("W")
//                binding.wednesday.setTypeface(null, Typeface.NORMAL)
////                binding.wednesday.setTextColor(Color.parseColor ("#FFFFFF"))
//                binding.wednesday.setBackgroundResource(android.R.color.transparent)
//                wednesdayToggle = false
//                updateRepeatedDaysSummary()
//            } else {
//                daysofWeekList.add("W")
//                binding.wednesday.setTypeface(null, Typeface.BOLD)
////                binding.wednesday.setTextColor(Color.parseColor ("#13005A"))
//                binding.wednesday.setBackgroundResource(com.example.groupalarm.R.drawable.circle_shape)
//                wednesdayToggle = true
//                updateRepeatedDaysSummary()
//            }
//        }
//        binding.thursday.setOnClickListener {
//            if(thursdayToggle) {
//                daysofWeekList.remove("Th")
//                binding.thursday.setTypeface(null, Typeface.NORMAL)
////                binding.thursday.setTextColor(Color.parseColor ("#FFFFFF"))
//                binding.thursday.setBackgroundResource(android.R.color.transparent)
//                thursdayToggle = false
//                updateRepeatedDaysSummary()
//            } else {
//                daysofWeekList.add("Th")
//                binding.thursday.setTypeface(null, Typeface.BOLD)
////                binding.thursday.setTextColor(Color.parseColor ("#13005A"))
//                binding.thursday.setBackgroundResource(com.example.groupalarm.R.drawable.circle_shape)
//                thursdayToggle = true
//                updateRepeatedDaysSummary()
//            }
//        }
//        binding.friday.setOnClickListener {
//            if(fridayToggle) {
//                daysofWeekList.remove("F")
//                binding.friday.setTypeface(null, Typeface.NORMAL)
////                binding.friday.setTextColor(Color.parseColor ("#FFFFFF"))
//                binding.friday.setBackgroundResource(android.R.color.transparent)
//                fridayToggle = false
//                updateRepeatedDaysSummary()
//            } else {
//                daysofWeekList.add("F")
//                binding.friday.setTypeface(null, Typeface.BOLD)
////                binding.friday.setTextColor(Color.parseColor ("#13005A"))
//                binding.friday.setBackgroundResource(com.example.groupalarm.R.drawable.circle_shape)
//                fridayToggle = true
//                updateRepeatedDaysSummary()
//
//            }
//        }
//        binding.saturday.setOnClickListener {
//            if(saturdayToggle) {
//                daysofWeekList.remove("Sa")
//                binding.saturday.setTypeface(null, Typeface.NORMAL)
////                binding.saturday.setTextColor(Color.parseColor ("#FFFFFF"))
//                binding.saturday.setBackgroundResource(android.R.color.transparent)
//                saturdayToggle = false
//                updateRepeatedDaysSummary()
//            } else {
//                daysofWeekList.add("Sa")
//                binding.saturday.setTypeface(null, Typeface.BOLD)
////                binding.saturday.setTextColor(Color.parseColor ("#13005A"))
//                binding.saturday.setBackgroundResource(com.example.groupalarm.R.drawable.circle_shape)
//                saturdayToggle = true
//                updateRepeatedDaysSummary()
//            }
//        }
//        binding.sunday.setOnClickListener {
//            if(sundayToggle) {
//                daysofWeekList.remove("Su")
//                binding.sunday.setTypeface(null, Typeface.NORMAL)
////                binding.sunday.setTextColor(Color.parseColor ("#FFFFFF"))
//                binding.sunday.setBackgroundResource(android.R.color.transparent)
//                sundayToggle = false
//                updateRepeatedDaysSummary()
//            } else {
//                daysofWeekList.add("Su")
//                binding.sunday.setTypeface(null, Typeface.BOLD)
////                binding.sunday.setTextColor(Color.parseColor ("#13005A"))
//                binding.sunday.setBackgroundResource(com.example.groupalarm.R.drawable.circle_shape)
//                sundayToggle = true
//                updateRepeatedDaysSummary()
//            }
//        }

        binding.searchUserBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                adapter.clearUserList()
                searchUsernames(query)
                return false
            }
            override fun onQueryTextChange(newText: String): Boolean {
                adapter.clearUserList()
                searchUsernames(newText)


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
//                binding.addedUsersList.text = "Added users:" + addedUsernamesList.toString().replace("[","").replace("]","")

                return false
            }
        })

        binding.btnSave.setSafeOnClickListener() {

            // Make sure that the alarm title is not empty and at least 3 characters long
            if(binding.etAlarmTitle.text.isNotEmpty() && binding.etAlarmTitle.length() in 3..15) {

                inviteUsersList.clear()
                for (i in 0 until adapter.getAddedUsersList().size) {
                    var users = adapter.getAddedUsersList().get(i)
                    inviteUsersList.add(users)
                }

                // This is where I start getting the data to send out the invitation and create the alarm
                calendar.set(Calendar.HOUR_OF_DAY, binding.timePicker.hour)
                calendar.set(Calendar.MINUTE, binding.timePicker.minute)
                calendar.set(Calendar.SECOND, 0)


                dateCalendar.set(Calendar.HOUR_OF_DAY, binding.timePicker.hour)
                dateCalendar.set(Calendar.MINUTE, binding.timePicker.minute)
                dateCalendar.set(Calendar.SECOND, 0)



                var alarmTime = Timestamp(dateCalendar.time)


                var timeInMillis: Long = dateCalendar.getTimeInMillis()
                if (timeInMillis - System.currentTimeMillis() < 0) {
                    //if its in past, add one day
                    timeInMillis += 86400000
                    alarmTime = Timestamp(Date(timeInMillis))
                    alarmDates.add(alarmTime)
                } else {
                    alarmDates.add(Timestamp(dateCalendar.time))
                }

                var alarmTitle = binding.etAlarmTitle.text.toString()
                var alarmOwner = currUserId
                var alarmDate = alarmDates
                var alarmInvitedUsers: ArrayList<String> = ArrayList()
                var alarmAcceptedUsers: ArrayList<String> = ArrayList()

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

                // If any of the recurrences are active, then only the recurring days will be added, no the specific date the alarm will be on
                // This if occurs when the recurring is active
                if(mondayToggle || tuesdayToggle || wednesdayToggle || thursdayToggle || fridayToggle || saturdayToggle || sundayToggle) {
                    sendAlarmInvitation(alarmTitle, alarmOwner, alarmTime, alarmDate,
                        alarmInvitedUsers, alarmAcceptedUsers, true, true, summaryDaysofWeekList, "")
                } else {
                    sendAlarmInvitation(alarmTitle, alarmOwner, alarmTime, alarmDate,
                        alarmInvitedUsers, alarmAcceptedUsers, true, false, summaryDaysofWeekList, "")
                }
            } else {
                binding.etAlarmTitle.error = "Enter an alarm title between 3 and 15 characters"
            }


        }

        binding.btnCancel.setOnClickListener {
            val intentDetails = Intent()
            intentDetails.setClass(
                this, DashboardActivity::class.java
            )
            startActivity(Intent(intentDetails))
            finish()
        }

        binding.timePicker.setOnTimeChangedListener { timePicker, i, i2 ->
            updateRepeatedDaysSummary()
        }

        searchUsernames("")

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

    override fun onStop() {
        super.onStop()
        finish()
    }

    fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateDateInView() {
        val myFormat = "MM/dd/yyyy" // mention the format you need
        val sdf = SimpleDateFormat(myFormat, Locale.US)
        mondayToggle = false
        tuesdayToggle = false
        wednesdayToggle = false
        thursdayToggle = false
        fridayToggle = false
        saturdayToggle= false
        sundayToggle = false
//        binding.monday.setBackgroundResource(android.R.color.transparent)
//        binding.tuesday.setBackgroundResource(android.R.color.transparent)
//        binding.wednesday.setBackgroundResource(android.R.color.transparent)
//        binding.thursday.setBackgroundResource(android.R.color.transparent)
//        binding.friday.setBackgroundResource(android.R.color.transparent)
//        binding.saturday.setBackgroundResource(android.R.color.transparent)
//        binding.sunday.setBackgroundResource(android.R.color.transparent)
//        binding.monday.setTypeface(null, Typeface.NORMAL)
//        binding.tuesday.setTypeface(null, Typeface.NORMAL)
//        binding.wednesday.setTypeface(null, Typeface.NORMAL)
//        binding.thursday.setTypeface(null, Typeface.NORMAL)
//        binding.friday.setTypeface(null, Typeface.NORMAL)
//        binding.saturday.setTypeface(null, Typeface.NORMAL)
//        binding.sunday.setTypeface(null, Typeface.NORMAL)
        daysofWeekList.clear()
        updateRepeatedDaysSummary()

        binding.repeatDaySummary.text = sdf.format(dateCalendar.time)
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun updateRepeatedDaysSummary() {
        if(mondayToggle && tuesdayToggle && wednesdayToggle && thursdayToggle && fridayToggle && saturdayToggle && sundayToggle) {
            summaryDaysofWeekList.clear()
            for (i in 0 until daysofWeekList.size) {
                var day = daysofWeekList.get(i)
                summaryDaysofWeekList.add(day)
            }
            binding.repeatDaySummary.text = "Repeat every day"
        }
        else if(mondayToggle && tuesdayToggle && wednesdayToggle && thursdayToggle && fridayToggle && !saturdayToggle && !sundayToggle) {
            summaryDaysofWeekList.clear()
            for (i in 0 until daysofWeekList.size) {
                var day = daysofWeekList.get(i)
                summaryDaysofWeekList.add(day)
            }
            binding.repeatDaySummary.text = "Repeat every weekday"
        }
        else if(mondayToggle || tuesdayToggle || wednesdayToggle || thursdayToggle || fridayToggle || saturdayToggle || sundayToggle) {
            summaryDaysofWeekList.clear()
            for (i in 0 until daysofWeekList.size) {
                var day = daysofWeekList.get(i)
                summaryDaysofWeekList.add(day)
            }
            binding.repeatDaySummary.text = "Repeat every " + summaryDaysofWeekList.toString().replace("[","").replace("]","")
        }
        else {
            summaryDaysofWeekList.clear()
            calendar.set(Calendar.HOUR_OF_DAY, binding.timePicker.hour)
            calendar.set(Calendar.MINUTE, binding.timePicker.minute)

            if (calendar.getTime() >= Calendar.getInstance().time) {
                binding.repeatDaySummary.text = "Today"
            }
            else {
                binding.repeatDaySummary.text = "Tomorrow"
            }

        }
    }

    private fun searchUsernames(query: String) {
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
                                .addOnSuccessListener { documents ->
                                    for (document in documents) {
                                        val friendId = document["userId2"] as String
                                        val status = document["status"] as String
                                        if (status == "accepted") {
                                            // Get the friend's user document
                                            firestore.collection("users").document(friendId).get()
                                                .addOnSuccessListener { snapshot ->
                                                    val friend = snapshot.toObject(User::class.java)
                                                    if (friend != null && user.username == friend.username && !adapter.alreadyHasUserDisplayed(document.id)) {
                                                        adapter.addUserToList(user, document.id)

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

    fun sendAlarmInvitation(alarmTitle: String, alarmOwner: String, alarmTime: Timestamp, alarmDate: ArrayList<Timestamp>,
                            alarmInvitedUsers: ArrayList<String>, acceptedUsers: ArrayList<String>,
                            isActive: Boolean, recurring: Boolean, recurringDays: ArrayList<String>, chatId: String) {

        val newAlarmRef = firestore.collection("alarms").document()
        val usersRef = firestore.collection("users")
        val chatsRef = firestore.collection("chats")

        val alarmId = newAlarmRef.id
        acceptedUsers.add(currUserId)

        // New Alarm object to be created and added
        // TODO
        //  Most recent: Its not working again//
        //  ---------------------
        //  Seems to be working for now actually, dont know why but Ill keep this here just in case it goes wrong and
        //  alarmInvitedUsers doesn't update properly again
        //  -------------------------------------
        //  for some reason the alarmInvitedUsers isnt being carried on over to the db, only my id for some reason
        //  Im working around it currently by updating it manually below right after I make the chats document
        val newAlarm = Alarm (
            alarmTitle,
            alarmOwner,
            alarmTime,
            alarmDate,
            alarmInvitedUsers,
            acceptedUsers,
            isActive,
            recurring,
            recurringDays,
            chatId,
                )
        newAlarmRef.set(newAlarm)

        // Todo another todo below this, but we do it again here and one more time belwo for safety,
        //  I need to find out why this is happening
        newAlarmRef.update("invitedUsers", alarmInvitedUsers)

            .addOnSuccessListener {
                // Updating the current user's (owner) alarms field
                val userRef = usersRef.document(currUserId)
                userRef.update("acceptedAlarms", FieldValue.arrayUnion(alarmId),
                    "activeAlarms", FieldValue.arrayUnion(alarmId))

                //inviting users to the alarm
                alarmInvitedUsers.forEach {
                    val user = usersRef.document(it)
                    user.update("invitedAlarms", FieldValue.arrayUnion(alarmId))
                }

                // Only adding the current user id (owner) so that only people who accept the alarm
                // get access to the group chat
                var initialUser: ArrayList<String> = ArrayList()
                initialUser.add(currUserId)

                // Creating a new document in the chats collection
                val newChatRef = chatsRef.document()
                val chatId = newChatRef.id
                val chat = Chats(
                    initialUser,
                    ArrayList(),
                    Timestamp(Calendar.getInstance().getTime()),
                    alarmId,
                    )
                newChatRef.set(chat)
                // Updating the alarm's chatId field
                newAlarmRef.update(
                    "chatId", chatId,
                    "invitedUsers", alarmInvitedUsers)
                val intentDetails = Intent()
                intentDetails.setClass(
                    this, DashboardActivity::class.java
                )
                startActivity(Intent(intentDetails))
                finish()
            }
            .addOnFailureListener {
                // Handle failed updates
                Toast.makeText(this, "Failed to create new alarm", Toast.LENGTH_SHORT).show()
            }


    }

    private fun getAllUserFriends() {
        friendsDb = FirebaseFirestore.getInstance().collection("friends")

        val eventListener = object : EventListener<QuerySnapshot> {
            override fun onEvent(
                querySnapshot: QuerySnapshot?,
                e: FirebaseFirestoreException?
            ) {
                if (e != null) {
                    Toast.makeText(
                        this@CreateAlarmActivity, "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val currUserId = FirebaseAuth.getInstance().currentUser!!.uid
                val db = FirebaseFirestore.getInstance()

                // Get all friendship documents where the current user is involved in
                db.collection("friends")
                    .whereEqualTo("userId1", currUserId)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            if (document["userId1"] == currUserId) {
                                val friendId = document["userId2"] as String
                                val status = document["status"] as String
                                if (status == "accepted") {
                                    // Get the friend's user document
                                    db.collection("users").document(friendId).get()
                                        .addOnSuccessListener { snapshot ->
                                            val friend = snapshot.toObject(User::class.java)
                                            if (friend != null && !adapter.alreadyHasUserDisplayed(document.id)) {
                                                adapter.addUserToList(friend, document.id)
                                            }
                                            adapter.notifyDataSetChanged()
                                        }.addOnFailureListener {
//                                            Toast.makeText(
//                                                this,
//                                                "Error while retrieving friend's documents",
//                                                Toast.LENGTH_SHORT
//                                            ).show()
                                        }
                                }
                            } else {
                                // There is no else because we will always have two friend documents
                                // per friends when they have it either accepted or declined
                                // There might arise an issue with pending since if one user sends a request but the other one doesnt accept it,
                                // There is only one document between those two people as of now.
                                // But for now this shouldn't be a problem
                            }
                        }
                    }

                for (docChange in querySnapshot?.getDocumentChanges()!!) {
                    // If new request is added
                    FirebaseFirestore.getInstance().collection("friends").document().get()
                        .addOnSuccessListener { documentSnapshot ->
                            val user = documentSnapshot.toObject(Friends::class.java)
                            if (user != null) {
                                if (user.userId1 == currUserId && user.status == "accepted") {
                                    if (docChange.type == DocumentChange.Type.ADDED) {
                                        val request = docChange.document.toObject(User::class.java)
//                                        adapter.addRequestsToList(request, docChange.document.id)
                                        adapter.notifyDataSetChanged()
                                        /*Todo this probably needs to be implemented furthermore once I add a remove friend functionality */
                                    } else if (docChange.type == DocumentChange.Type.REMOVED) {
                                        adapter.removeFriendByKey(docChange.document.id)
                                        adapter.notifyDataSetChanged()
                                    } else if (docChange.type == DocumentChange.Type.MODIFIED) {
                                        val request = docChange.document.toObject(User::class.java)
                                        if (!adapter.alreadyHasUserDisplayed(docChange.document.id)) {
//                                        adapter.removeRequestByKey(docChange.document.id)
//                                            adapter.addRequestsToList(request, docChange.document.id)

                                        }
                                        adapter.notifyDataSetChanged()
                                    }
                                }
                            }
                        }
                }
            }
        }
        listener = friendsDb.addSnapshotListener(eventListener)

    }



}