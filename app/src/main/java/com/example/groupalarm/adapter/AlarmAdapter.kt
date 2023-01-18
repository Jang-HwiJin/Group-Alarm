package com.example.groupalarm.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.groupalarm.*
import com.example.groupalarm.data.Alarm
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.AlarmRowBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class AlarmAdapter : RecyclerView.Adapter<AlarmAdapter.ViewHolder> {

    val firestore = FirebaseFirestore.getInstance()
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid

    lateinit var context: Context
    lateinit var currentUid: String
    var  alarmList = mutableListOf<Alarm>()
    var  alarmKeys = mutableListOf<String>()


    constructor(context: Context, uid: String) : super() {
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


    // when I remove the post object
    private fun removeAlarm(index: Int) {
        FirebaseFirestore.getInstance().collection(
            ScrollingActivity.COLLECTION_ALARMS).document(
            alarmKeys[index]
        ).delete()

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
                } else {
                    toggleOnOffAlarm(false, alarmInviteDocId)
                }
            }

            binding.cardView.setOnClickListener {
                val intentDetails = Intent()
                intentDetails.setClass(
                    context, AlarmChatsActivity::class.java
                )
                intentDetails.putExtra(
                    "AlarmId", alarmInviteDocId
                )
                (context as DashboardActivity).startActivity(Intent(intentDetails))


                // Send extra data later
                // Idk what this means

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


    private fun convertTimeForDisplay(time: Date): String {
        val format = SimpleDateFormat("hh:mm a")
        return format.format(time)
    }

    private fun convertDateForDisplay(time: Date): String {
        val format = SimpleDateFormat("EEE, MMM d")
        return format.format(time)
    }

}