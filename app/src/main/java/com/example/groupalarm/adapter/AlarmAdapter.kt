package com.example.groupalarm.adapter

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View.GONE
import android.os.Bundle
import android.view.View.INVISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.groupalarm.*
import com.example.groupalarm.ScrollingActivity.Companion.alarmIds
import com.example.groupalarm.ScrollingActivity.Companion.alarmIntents
import com.example.groupalarm.databinding.PostRowBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.example.groupalarm.data.Alarm
import com.example.groupalarm.data.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class AlarmAdapter : RecyclerView.Adapter<AlarmAdapter.ViewHolder> {

    lateinit var context: Context
    lateinit var currentUid: String
    var  alarmList = mutableListOf<Alarm>()
    var  alarmKeys = mutableListOf<String>()


    constructor(context: Context, uid: String) : super() {
        this.context = context
        this.currentUid = uid
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = PostRowBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return alarmList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var post = alarmList.get(holder.adapterPosition)
        holder.bind(post)
    }

    fun addAlarm(alarm: Alarm, key: String) {
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

    fun editUserList(key: String, user: User, addingUser: Boolean) {
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
    private fun removePost(index: Int) {
        FirebaseFirestore.getInstance().collection(
            ScrollingActivity.COLLECTION_ALARMS).document(
            alarmKeys[index]
        ).delete()

        alarmList.removeAt(index)
        alarmKeys.removeAt(index)
        notifyItemRemoved(index)
    }

    // when somebody else removes an object
    fun removePostByKey(key: String) {
        val index = alarmKeys.indexOf(key)
        if (index != -1) {
            alarmList.removeAt(index)
            alarmKeys.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(val binding: PostRowBinding) : RecyclerView.ViewHolder(binding.root){
        fun bind(alarm: Alarm) {
            val userEmail = FirebaseAuth.getInstance().currentUser!!.email!!
            val currUser =  FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERS)
                .document(userEmail).get()
            val alarmManager = context.getSystemService(AppCompatActivity.ALARM_SERVICE) as AlarmManager

            binding.alarmTitle.text = alarm.title
            binding.alarmTime.text = convertTimeForDisplay(alarm.time)
            binding.alarmOwner.text = context.getString(R.string.alarmOwner, alarm.owner)
            // Only allow toggling if alarm's time is >= current time
            if (alarm.owner != userEmail) {
                binding.btnDelete.visibility = INVISIBLE
            }
            if (Date(alarm.time) < Calendar.getInstance().time) {
                binding.btnToggleAlarm.isChecked = false
                binding.btnToggleAlarm.isEnabled = false
            }
            else {
                binding.btnToggleAlarm.isChecked = alarm.users.map { a -> a.email }
                    .contains(FirebaseAuth.getInstance().currentUser!!.email!!)
            }

            var alarmUsers = alarm.users.toMutableList()

            binding.btnToggleAlarm.setOnClickListener {
                val pendingIntent = PendingIntent.getBroadcast(context, alarm.time.toInt(), Intent(context, AlarmReceiver::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
                if (binding.btnToggleAlarm.isChecked) {
                    // Add myself back to user list
                    currUser.addOnSuccessListener { documentSnapshot ->
                            val user = documentSnapshot.toObject(User::class.java)
                            editUserList(alarmIds.get(alarm)!!, user!!, true)
                            alarmUsers.add(user)
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to add myself to user list of this alarm", Toast.LENGTH_LONG).show()
                        }

                }
                else {
                    // remove myself from user list
                    FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERS)
                        .document(userEmail).get()
                        .addOnSuccessListener { documentSnapshot ->
                            val user = documentSnapshot.toObject(User::class.java)
                            editUserList(alarmIds.get(alarm)!!, user!!, false)
                            alarmUsers.remove(user)
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to remove myself from this alarm", Toast.LENGTH_LONG).show()
                        }
                }
            }

            binding.btnDelete.setOnClickListener {
               removePost(adapterPosition)
            }


//            if (currentUid == alarm.uid) {
//                binding.btnDelete.visibility = View.VISIBLE
//            } else {
//                binding.btnDelete.visibility = View.GONE
//            }

//            binding.btnDelete.setOnClickListener {
//                FirebaseFirestore.getInstance().collection(
//                    CreatePostActivity.COLLECTION_POSTS
//                ).document(
//                    postKeys[adapterPosition]
//                ).delete()
//            }

            binding.btnDetails.setOnClickListener {
                val intentDetails = Intent()
                intentDetails.setClass(
                    (context as ScrollingActivity), DetailsActivity::class.java
                )
                intentDetails.putExtra(
                    "AlarmTitle", alarm.title
                )
                intentDetails.putExtra(
                    "AlarmTime", alarm.time
                )
                intentDetails.putExtra(
                    "AlarmOwner", alarm.owner
                )

                val bundle = Bundle()
                intentDetails.putExtra(
                    "AlarmUserList", ArrayList(alarmUsers)
                )

                (context as ScrollingActivity).startActivity(Intent(intentDetails))
            }

//            if (post.imgUrl != "") {
//                binding.ivPhoto.visibility = View.VISIBLE
//
//                Glide.with(context).load(post.imgUrl).into(
//                    binding.ivPhoto)
//            } else {
//                binding.ivPhoto.visibility = View.GONE
//            }

        }
    }

    private fun convertTimeForDisplay(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("hh:mm a")
        return format.format(date)
    }

}