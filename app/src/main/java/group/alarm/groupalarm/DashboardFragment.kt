package group.alarm.groupalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.EventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.ktx.Firebase
import group.alarm.groupalarm.adapter.AlarmAdapter
import group.alarm.groupalarm.data.Alarm
import group.alarm.groupalarm.data.User
import group.alarm.groupalarm.databinding.FragmentDashboardBinding
import java.util.Calendar
import java.util.Date


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [DashboardFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class DashboardFragment : Fragment() {
    lateinit var binding: FragmentDashboardBinding

    lateinit var alarmManager: AlarmManager

    val firestore = FirebaseFirestore.getInstance()
    val currUserEmail = FirebaseAuth.getInstance().currentUser!!.email!!
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!

    companion object {
        @JvmStatic
        fun newInstance() = DashboardFragment()
        const val COLLECTION_ALARMS = "alarms"
        const val ALARM_REQUEST_CODE = "alarmRequestCode"
        var alarmIntents = hashMapOf<String, PendingIntent>()
        var alarmIds = hashMapOf<Alarm, String>()
        var alarmTitles = hashMapOf<String, String>()
    }

    lateinit var alarmsDb: CollectionReference
    lateinit var usersDb: CollectionReference

    lateinit var listener: ListenerRegistration
    private lateinit var adapter: AlarmAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentDashboardBinding.inflate(
            inflater, container, false)

// TODO
//        val stopAlarmId = intent.getStringExtra("alarmId")
//        if(stopAlarmId != null) {
//            var pendingIntentToBeRemoved = DashboardActivity.alarmIntents.get(stopAlarmId)
//            if (pendingIntentToBeRemoved != null) {
//                alarmManager.cancel(pendingIntentToBeRemoved)
//            }
//        }

        adapter = AlarmAdapter(
            getActivity() as DashActivity,
            FirebaseAuth.getInstance().currentUser!!.uid
        )
        binding.recyclerAlarms.adapter = adapter

        //TODO Very naive solution to the toggles resetting when scrolling fast, need to work on it again more later
        // Source: https://stackoverflow.com/questions/50328655/recyclerview-items-values-reset-when-scrolling-down
        // This is also in CreateAlarmActivity
        binding.recyclerAlarms.setItemViewCacheSize(500)

        binding.addAlarmFab.setOnClickListener {
            val intentDetails = Intent()
            intentDetails.setClass(
                getActivity() as DashActivity, CreateAlarmActivity::class.java
            )
            startActivity(Intent(intentDetails))
        }

        binding.alarmInviteFab.setOnClickListener {
            val intentDetails = Intent()
            intentDetails.setClass(
                getActivity() as DashActivity, AlarmInvitesActivity::class.java
            )
            startActivity(Intent(intentDetails))
        }

        // Displays the number of pending alarm invites if there is at least 1
        getNumberOfPendingAlarmInvites()

        //TODO
        alarmManager =
            (requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager)!!
        // Sends the alarms the user is part of to the adapter
        getAllUserAlarms()

        // Turn/Leave alarm off/on
//        userChanges()

        // When the user closes the app
        val presenceUserRef = Firebase.database.getReference("users").child(currUserId).child("activityStatus")
        presenceUserRef.onDisconnect().setValue(Timestamp(Calendar.getInstance().time))


        return binding.root
    }

    override fun onResume() {
        super.onResume()

    }

    fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
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
                        getActivity() as DashActivity, "Error: ${e.message}",
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
                            if (alarm.acceptedUsers!!.contains(currUserId)) {
                                if (docChange.type == DocumentChange.Type.ADDED) {
                                    adapter.notifyDataSetChanged()
                                    /*Todo
                                       this probably needs to be implemented furthermore once I add a remove friend functionality */
                                } else if (docChange.type == DocumentChange.Type.REMOVED) {
                                    adapter.notifyDataSetChanged()
                                } else if (docChange.type == DocumentChange.Type.MODIFIED) {
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


        // TODO
        //  There might be an issue with the db this as well, if there is an issue, check here
        val alarmsRef = firestore.collection("alarms")
        alarmsDb = alarmsRef

        val eventListener = object : EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?,
                                 e: FirebaseFirestoreException?) {
                if (e != null) {
                    Toast.makeText(
                        getActivity() as DashActivity, "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                for (docChange in querySnapshot?.documentChanges!!) {
                    firestore.collection("users").document(currUserId)
                        .get().addOnSuccessListener { documentSnapshot ->
                            val user = documentSnapshot.toObject(User::class.java)
                            if (user!= null) {
                                if (docChange.type == DocumentChange.Type.ADDED) {
                                    val alarm = docChange.document.toObject(Alarm::class.java)
                                    DashboardFragment.alarmIds[alarm] = docChange.document.id
                                    DashboardFragment.alarmTitles[docChange.document.id] = alarm.title
                                    // If alarmId is in user's accepted field and it's not already displayed, then add it to the alarm list and set the alarm
                                    if(user.acceptedAlarms.contains(docChange.document.id) && !adapter.alreadyHasAlarmDisplayed(docChange.document.id)) {
                                        adapter.addAlarmToList(alarm, docChange.document.id)
                                        adapter.notifyDataSetChanged()

                                        if(user.activeAlarms.contains(docChange.document.id)) {

                                            val alarmTime = alarm.time.toDate()
                                            val calendar = Calendar.getInstance().apply { time = alarmTime }

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
                                                val intent = Intent(getActivity() as DashActivity, AlarmReceiver::class.java)
                                                intent.putExtra("alarmId", docChange.document.id)
                                                val requestCode = docChange.document.id.hashCode() // Use the alarm's ID as the request code
                                                val pendingIntent = PendingIntent.getBroadcast(
                                                    getActivity()?.getApplicationContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                                                alarmIntents.put(docChange.document.id, pendingIntent)

                                                if(alarm.time.toDate() >= Calendar.getInstance().time) {
                                                    alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
                                                }
                                            }
                                            else {
                                                val recurringCalendar = Calendar.getInstance()
                                                recurringCalendar.time = Date()
                                                recurringCalendar.set(Calendar.HOUR_OF_DAY, calendar.get(Calendar.HOUR_OF_DAY))
                                                recurringCalendar.set(Calendar.MINUTE, calendar.get(Calendar.MINUTE))
                                                recurringCalendar.set(Calendar.SECOND, 0)

                                                if(alarm.recurringDays.contains("M")) {
                                                    val intent = Intent(getActivity() as DashActivity, AlarmReceiver::class.java)
                                                    intent.putExtra("alarmId", docChange.document.id)
                                                    val requestCode = docChange.document.id.hashCode()+1 // Use the alarm's ID as the request code
                                                    val pendingIntent = PendingIntent.getBroadcast(getActivity()?.getApplicationContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                                                    alarmIntents.put(docChange.document.id, pendingIntent)


                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                                                    alarmManager.setRepeating(
                                                        AlarmManager.RTC_WAKEUP,
                                                        recurringCalendar.timeInMillis,
                                                        604800000,
                                                        pendingIntent
                                                    )
                                                }
                                                if(alarm.recurringDays.contains("T")) {
                                                    val intent = Intent(getActivity() as DashActivity, AlarmReceiver::class.java)
                                                    intent.putExtra("alarmId", docChange.document.id)
                                                    val requestCode = docChange.document.id.hashCode()+2 // Use the alarm's ID as the request code
                                                    val pendingIntent = PendingIntent.getBroadcast(getActivity()?.getApplicationContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                                                    alarmIntents.put(docChange.document.id, pendingIntent)

                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.TUESDAY)
                                                    alarmManager.setRepeating(
                                                        AlarmManager.RTC_WAKEUP,
                                                        recurringCalendar.timeInMillis,
                                                        604800000,
                                                        pendingIntent
                                                    )
                                                }
                                                if(alarm.recurringDays.contains("W")) {
                                                    val intent = Intent(getActivity() as DashActivity, AlarmReceiver::class.java)
                                                    intent.putExtra("alarmId", docChange.document.id)
                                                    val requestCode = docChange.document.id.hashCode()+3 // Use the alarm's ID as the request code
                                                    val pendingIntent = PendingIntent.getBroadcast(getActivity()?.getApplicationContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                                                    alarmIntents.put(docChange.document.id, pendingIntent)

                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.WEDNESDAY)
                                                    alarmManager.setRepeating(
                                                        AlarmManager.RTC_WAKEUP,
                                                        recurringCalendar.timeInMillis,
                                                        604800000,
                                                        pendingIntent
                                                    )
                                                }
                                                if(alarm.recurringDays.contains("Th")) {
                                                    val intent = Intent(getActivity() as DashActivity, AlarmReceiver::class.java)
                                                    intent.putExtra("alarmId", docChange.document.id)
                                                    val requestCode = docChange.document.id.hashCode()+4 // Use the alarm's ID as the request code
                                                    val pendingIntent = PendingIntent.getBroadcast(getActivity()?.getApplicationContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                                                    alarmIntents.put(docChange.document.id, pendingIntent)

                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.THURSDAY)

                                                    alarmManager.setRepeating(
                                                        AlarmManager.RTC_WAKEUP,
                                                        recurringCalendar.timeInMillis,
                                                        604800000,
                                                        pendingIntent
                                                    )
                                                }
                                                if(alarm.recurringDays.contains("F")) {
                                                    val intent = Intent(getActivity() as DashActivity, AlarmReceiver::class.java)
                                                    intent.putExtra("alarmId", docChange.document.id)
                                                    val requestCode = docChange.document.id.hashCode()+5 // Use the alarm's ID as the request code
                                                    val pendingIntent = PendingIntent.getBroadcast(getActivity()?.getApplicationContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                                                    alarmIntents.put(docChange.document.id, pendingIntent)

                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY)
                                                    alarmManager.setRepeating(
                                                        AlarmManager.RTC_WAKEUP,
                                                        recurringCalendar.timeInMillis,
                                                        604800000,
                                                        pendingIntent
                                                    )
                                                }
                                                if(alarm.recurringDays.contains("Sa")) {
                                                    val intent = Intent(getActivity() as DashActivity, AlarmReceiver::class.java)
                                                    intent.putExtra("alarmId", docChange.document.id)
                                                    val requestCode = docChange.document.id.hashCode()+6 // Use the alarm's ID as the request code
                                                    val pendingIntent = PendingIntent.getBroadcast(getActivity()?.getApplicationContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                                                    alarmIntents.put(docChange.document.id, pendingIntent)

                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SATURDAY)
                                                    alarmManager.setRepeating(
                                                        AlarmManager.RTC_WAKEUP,
                                                        recurringCalendar.timeInMillis,
                                                        604800000,
                                                        pendingIntent
                                                    )
                                                }
                                                if(alarm.recurringDays.contains("Su")) {
                                                    val intent = Intent(getActivity() as DashActivity, AlarmReceiver::class.java)
                                                    intent.putExtra("alarmId", docChange.document.id)
                                                    val requestCode = docChange.document.id.hashCode()+7 // Use the alarm's ID as the request code
                                                    val pendingIntent = PendingIntent.getBroadcast(getActivity()?.getApplicationContext(), requestCode, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                                                    alarmIntents.put(docChange.document.id, pendingIntent)

                                                    recurringCalendar.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                                                    alarmManager.setRepeating(
                                                        AlarmManager.RTC_WAKEUP,
                                                        recurringCalendar.timeInMillis,
                                                        604800000,
                                                        pendingIntent
                                                    )
                                                }
                                            }

                                        }

                                        // If alarmId is in user's accepted field and it's already displayed then do nothing
                                    } else {
                                    }
                                } else if (docChange.type == DocumentChange.Type.REMOVED) {
                                    adapter.removeAlarmByKey(docChange.document.id)
                                    adapter.notifyDataSetChanged()
                                    var pendingIntentToBeRemoved = DashboardFragment.alarmIntents.get(docChange.document.id)
                                    if (pendingIntentToBeRemoved != null) {
                                        alarmManager.cancel(pendingIntentToBeRemoved)
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