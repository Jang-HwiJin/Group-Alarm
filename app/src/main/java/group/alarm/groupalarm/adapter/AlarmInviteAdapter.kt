package group.alarm.groupalarm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import group.alarm.groupalarm.data.Alarm
import group.alarm.groupalarm.data.User
import group.alarm.groupalarm.databinding.AlarmInviteRowBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AlarmInviteAdapter : RecyclerView.Adapter<AlarmInviteAdapter.ViewHolder> {

    var context: Context
    var currentUid: String
    private var alarmList = mutableListOf<Alarm>()
    private var alarmIdList = mutableListOf<String>()

    val firestore = FirebaseFirestore.getInstance()
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid

    constructor(context: Context, uid: String) : super() {
        this.context = context
        this.currentUid = uid
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmInviteAdapter.ViewHolder {
        val binding = AlarmInviteRowBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return alarmList.size
    }

    override fun onBindViewHolder(holder: AlarmInviteAdapter.ViewHolder, position: Int) {
        var alarmInvite = alarmList.get(holder.adapterPosition)
        var alarmInviteDocId = alarmIdList.get(holder.adapterPosition)

        holder.bind(alarmInvite, alarmInviteDocId)
    }

    fun addAlarmToList(alarm: Alarm, key: String) {
        if (alarm != null) {
            alarmList.add(alarm)
        }
        alarmIdList.add(key)
        notifyItemInserted(alarmList.lastIndex)
    }

    fun alreadyHasAlarmDisplayed(key: String): Boolean {
        return alarmIdList.contains(key)
    }

    private fun removeAlarm(index: Int) {
//        FirebaseFirestore.getInstance().collection(
//            DashboardActivity.COLLECTION_ALARMS).document(
//            alarmIdList[index]
//        ).delete()

        alarmList.removeAt(index)
        alarmIdList.removeAt(index)
        notifyItemRemoved(index)
    }

    fun removeAlarmByKey(key: String) {
        val index = alarmIdList.indexOf(key)
        if (index != -1) {
            alarmList.removeAt(index)
            alarmIdList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(val binding: AlarmInviteRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(alarm: Alarm, alarmInviteDocId: String) {
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


            binding.btnAccept.setOnClickListener {
                acceptAlarmInvite(alarm, alarmInviteDocId)
                removeAlarm(adapterPosition)
            }

            binding.btnDecline.setOnClickListener {
                declineAlarmInvite(alarm, alarmInviteDocId)
                removeAlarm(adapterPosition)

            }
        }

    }

    fun acceptAlarmInvite(alarm: Alarm, alarmInviteDocId: String) {
        // If user accepts it, then we want to remove the alarmId from the invitedAlarms, add the alarmId to the user's acceptedAlarm and activeAlarm fields
        // We also want to add the user's id to the acceptedUsers in alarms and remove the user's id from invitedUsers in alarms collection
        // We also want to add the user's id to the chat corresponding to the alarm
        val userDocToUpdate = firestore.collection("users")
        val alarmRefToUpdate = firestore.collection("alarms")
        val alarmChatId = alarm.chatId
        val chatRefToUpdate = firestore.collection("chats")

        val user = firestore.collection("users").document(currUserId)
        user.get().addOnSuccessListener {
            userDocToUpdate.document(currUserId).update(
                "invitedAlarms", FieldValue.arrayRemove(alarmInviteDocId)
            )
        }
        userDocToUpdate.document(currUserId).update(
            "acceptedAlarms", FieldValue.arrayUnion(alarmInviteDocId),
            "activeAlarms", FieldValue.arrayUnion(alarmInviteDocId)
        )
        alarmRefToUpdate.document(alarmInviteDocId).update(
            "acceptedUsers", FieldValue.arrayUnion(currUserId),
            "invitedUsers", FieldValue.arrayRemove(currUserId),
        )
        chatRefToUpdate.document(alarmChatId).update(
            "users", FieldValue.arrayUnion(currUserId),
        )
    }

    fun declineAlarmInvite(alarm: Alarm, alarmInviteDocId: String) {
        // If user declines it, then we want to remove the alarmId from the invitedAlarms in the user collection
        // We also want to remove the user's id from invitedUsers in the alarms collection
        val userDocToUpdate = firestore.collection("users").document(currUserId)
        val alarmRefToUpdate = firestore.collection("alarms").document(alarmInviteDocId)

        userDocToUpdate.update(
            "invitedAlarms", FieldValue.arrayRemove(alarm)
        ).addOnSuccessListener {
            // Remove the userId from the invitedUsers in the alarm collection
            alarmRefToUpdate.update(
                "invitedUsers", FieldValue.arrayRemove(currUserId)
            ).addOnSuccessListener {
                Toast.makeText(context,
                    "Alarm declined", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(context,
                    "Failed to decline alarm", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun convertTimeForDisplay(time: Date): String {
        val format = SimpleDateFormat("hh:mm a")
        return format.format(time)
    }

    private fun convertDateForDisplay(time: Date): String {
        val format = SimpleDateFormat("EEE, MMM d")
        return format.format(time)
    }


}