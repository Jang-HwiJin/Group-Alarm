package com.example.groupalarm.adapter

import android.R.attr.data
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.ColorInt
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.groupalarm.*
import com.example.groupalarm.data.*
import com.example.groupalarm.databinding.FriendAlarmInviteRowBinding
import com.example.groupalarm.databinding.MessagesRowBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*


class ChatsAdapter : RecyclerView.Adapter<ChatsAdapter.ViewHolder> {
    var context: Context
    var currentUid: String
    var currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!
    private var messageList = mutableListOf<Messages>()
    private var messageIdList = mutableListOf<String>()


    val firestore = FirebaseFirestore.getInstance()

    constructor(context: Context, uid: String) : super() {
        this.context = context
        this.currentUid = uid
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatsAdapter.ViewHolder {
        val binding = MessagesRowBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    override fun onBindViewHolder(holder: ChatsAdapter.ViewHolder, position: Int) {
        var message = messageList.get(holder.adapterPosition)
        var messageDocId = messageIdList.get(holder.adapterPosition)

        holder.bind(message, messageDocId)
    }

    fun addMessageToList(message: Messages, key: String) {
        if (message != null) {
            messageList.add(message)
        }
        messageIdList.add(key)
        notifyItemInserted(messageList.lastIndex)
    }

    fun alreadyHasMessageDisplayed(key: String): Boolean {
        return messageIdList.contains(key)
    }

    fun clearMessageList() {
        val messageListSize = getItemCount()
        messageList.clear()
        messageIdList.clear()
        notifyItemRangeRemoved(0, messageListSize);
    }

    fun removeMessage(index: Int) {
        FirebaseFirestore.getInstance().collection(
            DashboardActivity.COLLECTION_ALARMS).document(
            messageIdList[index]
        ).delete()

        messageList.removeAt(index)
        messageIdList.removeAt(index)
        notifyItemRemoved(index)
    }

//    // when somebody else removes an object
//    fun removeMessageByKey(key: String) {
//        val index = messageIdList.indexOf(key)
//        if (index != -1) {
//            messageList.removeAt(index)
//            messageIdList.removeAt(index)
//            notifyItemRemoved(index)
//        }
//    }

    inner class ViewHolder(val binding: MessagesRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Messages, messageDocId: String) {

            firestore.collection("users").document(message.sender)
                .get().addOnSuccessListener { senderDoc ->
                    val sender = senderDoc.toObject(User::class.java)
                    if (sender != null) {
                        binding.displayName.text = sender.displayName
                        if(sender.profileImg != "") {
                            Glide.with(context).load(sender.profileImg).into(
                                binding.profilePicture
                            )
                        }

                    }
                }
            binding.message.text = message.text
            val simpleDate = SimpleDateFormat("M/dd/yyyy hh:mm a")
            val currentDate = simpleDate.format(message.timestamp.toDate())
            binding.messagePostTime.text = currentDate
        }
    }

    fun removeMessageByKey(key: String) {
        val index = messageIdList.indexOf(key)

        FirebaseFirestore.getInstance().collection("friends").document(key)
            .update(
                "status", "declined"
            ).addOnSuccessListener {
                Toast.makeText(context, "Friend successfully removed", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to remove friend", Toast.LENGTH_SHORT).show()
            }

        // Set the friend status to "declined' for the removed user as well
        FirebaseFirestore.getInstance().collection("friends").document(key)
            .get().addOnSuccessListener { documentSnapshot ->
                val userDocument = documentSnapshot.toObject(Friends::class.java)
                if (userDocument != null) {
                    val otherUserId = userDocument.userId2
                    FirebaseFirestore.getInstance().collection("friends")
                        .whereEqualTo("userId1", otherUserId)
                        .whereEqualTo("userId2", currentUid)
                        .get().addOnSuccessListener { otherDocumentSnapshot ->
                            for (document in otherDocumentSnapshot) {
                                if (document != null) {
                                    val otherUserDocId = document.id
                                    FirebaseFirestore.getInstance().collection("friends").document(otherUserDocId)
                                        .update("status", "declined")
                                }
                            }
                        }.addOnFailureListener {
                            Toast.makeText(context, "Failed to find other user's friendship doc and update to status to declined", Toast.LENGTH_SHORT).show()
                        }
                }
            }

        if (index != -1) {
            messageList.removeAt(index)
            messageIdList.removeAt(index)
            notifyItemRemoved(index)
        }
    }


}