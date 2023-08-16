package group.alarm.groupalarm.adapter

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import group.alarm.groupalarm.*
import group.alarm.groupalarm.data.Alarm
import group.alarm.groupalarm.data.User
import group.alarm.groupalarm.databinding.AlarmRowBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import java.text.SimpleDateFormat
import java.util.*

class AlarmAdapter : RecyclerView.Adapter<AlarmAdapter.ViewHolder> {

    val firestore = FirebaseFirestore.getInstance()
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid

    lateinit var context: Context
    lateinit var currentUid: String
    var  alarmList = mutableListOf<Alarm>()
    var  alarmKeys = mutableListOf<String>()

    lateinit var listener: ListenerRegistration
    lateinit var usersDb: CollectionReference



    constructor(context: DashActivity, uid: String) : super() {
        this.context = context
        this.currentUid = uid
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = AlarmRowBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)

        binding.toggleAlarm
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return alarmList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var alarm = alarmList.get(holder.adapterPosition)
        var alarmInviteDocId = alarmKeys.get(holder.adapterPosition)

        holder.bind(alarm, alarmInviteDocId)
    }


    //TODO trying to fix the recycler view keep recycling the toggles on and off
    // It worked somehow, no idea, keep an eye on this
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getItemViewType(position: Int): Int {
        return position
    }
    //TODO it is up to here


    fun addAlarmToList(alarm: Alarm, key: String) {
        alarmList.add(alarm)
        alarmKeys.add(key)
        notifyDataSetChanged()
        notifyItemInserted(alarmList.lastIndex)
    }

    fun alreadyHasAlarmDisplayed(key: String): Boolean {
        return alarmKeys.contains(key)
    }

    //Optional for now
    fun editAlarmByKey(alarm: Alarm, key: String) {
        val index = alarmKeys.indexOf(key)
        FirebaseFirestore.getInstance().collection(
            ScrollingActivity.COLLECTION_ALARMS).document(
            key
        ).update(
            mapOf(
                "title" to alarm.title,
                "time" to alarm.time,
                "isActive" to alarm.isActive,
            )
        )
        alarmList[index] = alarm
        notifyItemChanged(index)
    }

    fun editAlarmList(key: String, user: User, addingUser: Boolean) {
        val docToUpdate = FirebaseFirestore.getInstance().collection(
            ScrollingActivity.COLLECTION_ALARMS)
            .document(key)
        if (addingUser) {
            docToUpdate
            .update(
                "users", FieldValue.arrayUnion(user)
            )
        }
        else {
            docToUpdate.update("users", FieldValue.arrayRemove(user))
        }
    }

    fun clearAlarmList() {
        val alarmListSize = getItemCount()
        alarmList.clear()
        alarmKeys.clear()
        notifyItemRangeRemoved(0, alarmListSize)
    }


    // when I remove the post object
    private fun removeAlarm(index: Int) {
//        FirebaseFirestore.getInstance().collection(
//            ScrollingActivity.COLLECTION_ALARMS).document(
//            alarmKeys[index]
//        ).delete()

        alarmList.removeAt(index)
        alarmKeys.removeAt(index)
        notifyItemRemoved(index)
    }

    // when somebody else removes an object
    fun removeAlarmByKey(key: String) {
        val index = alarmKeys.indexOf(key)
        if (index != -1) {
            alarmList.removeAt(index)
            alarmKeys.removeAt(index)
            notifyItemRemoved(index)
        }
    }


    inner class ViewHolder(val binding: AlarmRowBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(alarm: Alarm, alarmInviteDocId: String) {
//            firestore.collection("alarms").document(alarmInviteDocId)
//                .get().addOnSuccessListener { document ->
//                    val alarm = document.toObject(Alarm::class.java)
//                    if (alarm == null) {
//                        binding.cardView.isClickable = false
//                    }
//                }

            firestore.collection("users").document(currUserId)
                .get()
                .addOnSuccessListener { document ->
                    val user = document.toObject(User::class.java)
                    if (user != null) {
                        val activeAlarmList = user.activeAlarms
                            if (activeAlarmList.contains(alarmInviteDocId)) {
                                // The alarm is in the user's activeAlarmList
                                binding.toggleAlarm.isChecked = true
                            } else {
                                binding.toggleAlarm.isChecked = false
                            }
                    }
                }

            val alarmOwnerId = alarm.owner
            firestore.collection("users").document(alarmOwnerId)
                .get().addOnSuccessListener { documentSnapshot ->
                    val user = documentSnapshot.toObject(User::class.java)
                    if(user != null) {
                        binding.alarmOwner.text = "Owner: " + user.username
                    }
                }
            val alarmTime = alarm.time
            val alarmTitle = alarm.title
            binding.alarmTitle.text = alarmTitle
            binding.alarmTime.text = convertTimeForDisplay(alarmTime.toDate())
            binding.toggleAlarm.isChecked = true

            // If the alarm is recurring, show the days
            if(alarm.isRecurring) {
                if(alarm.recurringDays.size == 7) {
                    binding.alarmDays.text = "Repeat everyday"
                }
                else if(alarm.recurringDays.size == 5 && !alarm.recurringDays.contains("Su") && !alarm.recurringDays.contains("Sa")) {
                    binding.alarmDays.text = "Repeat every weekday"
                }
                else {
                    var recurringDays = mutableListOf<String>()
                    for (i in 0 until alarm.recurringDays.size) {
                        var days = alarm.recurringDays.get(i)
                        recurringDays.add(days)
                    }
                    binding.alarmDays.text = "Repeat every " + recurringDays.toString().replace("[","").replace("]","")
                }
            }
            // If the alarm is for single day, display the date
            else {
                binding.alarmDays.text = convertDateForDisplay(alarmTime.toDate())
            }


            binding.toggleAlarm.setOnCheckedChangeListener { compoundButton, b ->
                if(compoundButton.isChecked) {
                    toggleOnOffAlarm(true, alarmInviteDocId)

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    val alarmTime = alarm.time.toDate()
                    val calendar = Calendar.getInstance().apply { time = alarmTime }
                    val intent = Intent(context, AlarmReceiver::class.java)
                    intent.putExtra("alarmId", alarmInviteDocId)
                    val requestCode = alarmInviteDocId.hashCode() // Use the alarm's ID as the request code
                    val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                    DashboardFragment.alarmIntents.put(alarmInviteDocId, pendingIntent)

                    val dayOfWeekMap = mapOf(
                        "M" to Calendar.MONDAY,
                        "T" to Calendar.TUESDAY,
                        "W" to Calendar.WEDNESDAY,
                        "Th" to Calendar.THURSDAY,
                        "F" to Calendar.FRIDAY,
                        "Sa" to Calendar.SATURDAY,
                        "Su" to Calendar.SUNDAY
                    )

                    if (!alarm.isRecurring) {
                        if(alarm.time.toDate() >= Calendar.getInstance().time) {
                            calendar.set(Calendar.SECOND, 0)
                            calendar.set(Calendar.MILLISECOND, 0)

                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                        }
                    }

                } else {
                    toggleOnOffAlarm(false, alarmInviteDocId)

                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                    var pendingIntentToBeRemoved = DashboardFragment.alarmIntents.get(alarmInviteDocId)
                    if (pendingIntentToBeRemoved != null) {
                        alarmManager.cancel(pendingIntentToBeRemoved)
                    }
//                    firestore.collection("users").document(currUserId)
//                        .get().addOnSuccessListener { userDoc ->
//                            val user = userDoc.toObject(User::class.java)
//                            if( user != null) {
//                                if(alarm != null && alarm.isActive && user.activeAlarms.contains(alarmInviteDocId)) {
//                                    val alarmTime = alarm.time.toDate()
//                                    val calendar = Calendar.getInstance().apply { time = alarmTime }
//                                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//                                    val intent = Intent(context, AlarmReceiver::class.java).apply {
//                                        putExtra("alarmId", alarmInviteDocId)
//                                    }
//                                    val requestCode = alarmInviteDocId.hashCode() // Use the alarm's ID as the request code
//                                    val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
//                                    alarmManager.cancel(pendingIntent)
//
//
//                                    val dayOfWeekMap = mapOf(
//                                        "M" to Calendar.MONDAY,
//                                        "T" to Calendar.TUESDAY,
//                                        "W" to Calendar.WEDNESDAY,
//                                        "Th" to Calendar.THURSDAY,
//                                        "F" to Calendar.FRIDAY,
//                                        "Sa" to Calendar.SATURDAY,
//                                        "Su" to Calendar.SUNDAY
//                                    )
//
//                                    if (alarm.recurringDays.isEmpty()) {
//                                        if(alarm.time.toDate() >= Calendar.getInstance().time) {
//                                            alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
//                                        }
//                                    } else {
//                                        val days = alarm.recurringDays.map { dayOfWeekMap[it] }
//                                        //TODO
//                                        // so basically i need to make it so that something like check if it is repeating on monday or sunday or something,
//                                        // and then I need to adjust the calendar and replace the alarmTIme.time for it so that the time is the same
//                                        // but the day is different.
//                                        // Something like Calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
//                                        // and have this for each day of the week with if statements
//                                        alarmManager.setRepeating(
//                                            AlarmManager.RTC_WAKEUP,
//                                            alarmTime.time,
//                                            AlarmManager.INTERVAL_DAY * 7,
//                                            pendingIntent
//                                        )
//                                    }
//                                } else if(alarm != null && alarm.isActive && !user.activeAlarms.contains(alarmInviteDocId)) {
//                                    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//
//                                    val intent = Intent(context, AlarmReceiver::class.java).apply {
//                                        putExtra("alarmId", alarmInviteDocId)
//                                    }
//                                    val requestCode = alarmInviteDocId.hashCode() // Use the alarm's ID as the request code
//                                    val pendingIntent = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
//                                    alarmManager.cancel(pendingIntent)
//
//                                }
//                            }
//                        }

                }
                }

            binding.cardView.setOnClickListener {
                val intentDetails = Intent()
                intentDetails.setClass(
                    context, group.alarm.groupalarm.AlarmChatsActivity::class.java
                )
                intentDetails.putExtra(
                    "AlarmId", alarmInviteDocId
                )
                (context as DashActivity).startActivity(Intent(intentDetails))
            }

//            userToggleAlarms(alarmInviteDocId)
            if(Calendar.getInstance().time >= alarm.time.toDate() && !alarm.isRecurring) {
                binding.toggleAlarm.isClickable = false
                binding.toggleAlarm.isChecked = false
            }
        }
    }

    fun toggleOnOffAlarm(status: Boolean, alarmInviteDocId: String) {
        val user = firestore.collection("users").document(currUserId)
        if(status) {
            user.update(
                "activeAlarms", FieldValue.arrayUnion(alarmInviteDocId))
        } else {
            user.update(
                "activeAlarms", FieldValue.arrayRemove(alarmInviteDocId))
        }
    }

//    fun userToggleAlarms(alarmInviteDocId: String) {
//        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//        val usersRef = firestore.collection("users")
//        usersDb = usersRef
//
//
//        val userListener = object : EventListener<QuerySnapshot> {
//            override fun onEvent(querySnapshot: QuerySnapshot?,
//                                 e: FirebaseFirestoreException?) {
//                if (e != null) {
//                    Toast.makeText(
//                        context, "Error: ${e.message}",
//                        Toast.LENGTH_LONG
//                    ).show()
//                    return
//                }
//
//                for (docChange in querySnapshot?.documentChanges!!) {
//                    if (docChange.type == DocumentChange.Type.MODIFIED) {
//                        val user = docChange.document.toObject(User::class.java)
//                            // If the alarm is still accepted by current user but toggled off in activeAlarms, cancel the alarm
//                         if(user.acceptedAlarms.contains(alarmInviteDocId) && !user.activeAlarms.contains(alarmInviteDocId)) {
//                            var pendingIntentToBeRemoved = DashboardActivity.alarmIntents.get(alarmInviteDocId)
//                            if (pendingIntentToBeRemoved != null) {
//                                alarmManager.cancel(pendingIntentToBeRemoved)
//                            }
//                            // If the alarm is accepted but not in active alarms and toggled back on in activeAlarms, set the alarm back
//                        } else if (user.acceptedAlarms.contains(alarmInviteDocId) && user.activeAlarms.contains(alarmInviteDocId)){
////                            if (alarmId != null) {
////                                firestore.collection("alarms").document(alarmId)
////                                    .get().addOnSuccessListener { alarmDoc ->
////                                        val alarm = alarmDoc.toObject(Alarm::class.java)
////                                        if(alarm != null) {
////
////                                            val alarmTime = alarm.time.toDate()
////                                            val calendar = Calendar.getInstance().apply { time = alarmTime }
////                                            val intent = Intent(this@DashboardActivity, AlarmReceiver::class.java).apply {
////                                                putExtra("alarmId", docChange.document.id)
////                                            }
////                                            val requestCode = docChange.document.id.hashCode() // Use the alarm's ID as the request code
////                                            val pendingIntent = PendingIntent.getBroadcast(this@DashboardActivity, requestCode, intent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
////                                            alarmIntents.put(docChange.document.id, pendingIntent)
////
////                                            val dayOfWeekMap = mapOf(
////                                                "M" to Calendar.MONDAY,
////                                                "T" to Calendar.TUESDAY,
////                                                "W" to Calendar.WEDNESDAY,
////                                                "Th" to Calendar.THURSDAY,
////                                                "F" to Calendar.FRIDAY,
////                                                "Sa" to Calendar.SATURDAY,
////                                                "Su" to Calendar.SUNDAY
////                                            )
////
////                                            if (!alarm.isRecurring) {
////                                                if(alarm.time.toDate() >= Calendar.getInstance().time) {
////                                                    calendar.set(Calendar.SECOND, 0)
////                                                    calendar.set(Calendar.MILLISECOND, 0)
////
////                                                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
////                                                }
////                                            } else {
////                                                val recurringCalendar = Calendar.getInstance()
////                                                recurringCalendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
////                                                recurringCalendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
////                                                recurringCalendar.set(Calendar.SECOND, 0)
////                                                recurringCalendar.set(Calendar.MILLISECOND, 0);
////
////
////                                                if(alarm.recurringDays.contains("M")) {
////                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
////                                                    alarmManager.setInexactRepeating(
////                                                        AlarmManager.RTC_WAKEUP,
////                                                        recurringCalendar.timeInMillis,
////                                                        AlarmManager.INTERVAL_DAY * 7,
////                                                        pendingIntent
////                                                    )
////                                                }
////                                                if(alarm.recurringDays.contains("T")) {
////                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
////                                                    alarmManager.setInexactRepeating(
////                                                        AlarmManager.RTC_WAKEUP,
////                                                        recurringCalendar.timeInMillis,
////                                                        AlarmManager.INTERVAL_DAY * 7,
////                                                        pendingIntent
////                                                    )
////                                                }
////                                                if(alarm.recurringDays.contains("W")) {
////                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
////                                                    alarmManager.setInexactRepeating(
////                                                        AlarmManager.RTC_WAKEUP,
////                                                        recurringCalendar.timeInMillis,
////                                                        AlarmManager.INTERVAL_DAY * 7,
////                                                        pendingIntent
////                                                    )
////                                                }
////                                                if(alarm.recurringDays.contains("Th")) {
////                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)
////                                                    alarmManager.setInexactRepeating(
////                                                        AlarmManager.RTC_WAKEUP,
////                                                        recurringCalendar.timeInMillis,
////                                                        AlarmManager.INTERVAL_DAY * 7,
////                                                        pendingIntent
////                                                    )
////                                                }
////                                                if(alarm.recurringDays.contains("F")) {
////                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
////                                                    alarmManager.setInexactRepeating(
////                                                        AlarmManager.RTC_WAKEUP,
////                                                        recurringCalendar.timeInMillis,
////                                                        AlarmManager.INTERVAL_DAY * 7,
////                                                        pendingIntent
////                                                    )
////                                                }
////                                                if(alarm.recurringDays.contains("Sa")) {
////                                                    Toast.makeText(this@DashboardActivity,
////                                                        "We set the recurring for unday" + Date(recurringCalendar.timeInMillis), Toast.LENGTH_SHORT).show()
////                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
////                                                    alarmManager.setInexactRepeating(
////                                                        AlarmManager.RTC_WAKEUP,
////                                                        recurringCalendar.timeInMillis,
////                                                        AlarmManager.INTERVAL_DAY * 7,
////                                                        pendingIntent
////                                                    )
////                                                }
////                                                if(alarm.recurringDays.contains("Su")) {
////                                                    Toast.makeText(this@DashboardActivity,
////                                                        "We set the recurring for unday" + Date(recurringCalendar.timeInMillis), Toast.LENGTH_SHORT).show()
////                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
////                                                    alarmManager.setInexactRepeating(
////                                                        AlarmManager.RTC_WAKEUP,
////                                                        recurringCalendar.timeInMillis,
////                                                        AlarmManager.INTERVAL_DAY * 7,
////                                                        pendingIntent
////                                                    )
////                                                }
////                                            }
////
////                                        }
////
////                                    }
////                            }
//                        }
//                        else {
//
//                        }
//                    }
//                }
//            }
//        }
//        listener = usersDb.addSnapshotListener(userListener)
//    }


    private fun convertTimeForDisplay(time: Date): String {
        val format = SimpleDateFormat("hh:mm a")
        return format.format(time)
    }

    private fun convertDateForDisplay(time: Date): String {
        val format = SimpleDateFormat("EEE, MMM d")
        return format.format(time)
    }

}