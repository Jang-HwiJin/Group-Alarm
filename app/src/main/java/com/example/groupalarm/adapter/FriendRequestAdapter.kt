package com.example.groupalarm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.groupalarm.ScrollingActivity
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.FriendRequestsRowBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestAdapter : RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {

    var context: Context
    var currentUid: String
    private var requestList = mutableListOf<User>()
    private var requestIdList = mutableListOf<String>()
    
    val firestore = FirebaseFirestore.getInstance()
    
    constructor(context: Context, uid: String) : super() {
        this.context = context
        this.currentUid = uid
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestAdapter.ViewHolder {
        val binding = FriendRequestsRowBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return requestList.size
    }

    override fun onBindViewHolder(holder: FriendRequestAdapter.ViewHolder, position: Int) {
        var requester = requestList.get(holder.adapterPosition)
        var requestDocId = requestIdList.get(holder.adapterPosition)

        holder.bind(requester, requestDocId)
    }

    fun addRequestsToList(requesters: User, key: String) {
        if (requesters != null) {
            requestList.add(requesters)
        }
        requestIdList.add(key)
        notifyItemInserted(requestList.lastIndex)
    }

    fun alreadyHasRequestDisplayed(key: String): Boolean {
        return requestIdList.contains(key)
    }

    private fun removeRequest(index: Int) {
        FirebaseFirestore.getInstance().collection(
            ScrollingActivity.COLLECTION_ALARMS).document(
            requestIdList[index]
        ).delete()

        requestList.removeAt(index)
        requestIdList.removeAt(index)
        notifyItemRemoved(index)
    }

    // when somebody else removes an object
    fun removeRequestByKey(key: String) {
        val index = requestIdList.indexOf(key)
        if (index != -1) {
            requestList.removeAt(index)
            requestIdList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(val binding: FriendRequestsRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(requester: User, requestDocId: String) {
            val requesterUsername = requester.username

            binding.username.text = requesterUsername
            binding.displayName.text = requester.displayName
            Glide.with(context)
                .load(requester.profileImg)
                .into(binding.profilePicture)

            binding.btnAccept.setOnClickListener {
                FirebaseFirestore.getInstance().collection("usernames").document(requesterUsername)
                    .get().addOnSuccessListener { document ->
                        if (document != null) {
                            var requesterId = document.get("uid") as String
                            acceptFriendRequest(requestDocId, requesterId)
                        }
                    }
                removeRequest(adapterPosition)
            }

            binding.btnDecline.setOnClickListener {
                FirebaseFirestore.getInstance().collection("usernames").document(requesterUsername)
                    .get().addOnSuccessListener { document ->
                        if (document != null) {
                            var requesterId = document.get("uid") as String
                            declineFriendRequest(requestDocId, requesterId)
                        }
                    }
                removeRequest(adapterPosition)

            }
        }
    }

    fun acceptFriendRequest(friendshipId: String, otherUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid

        // Update the provided friendship document to have status "accepted"
        firestore.collection("friends")
            .document(friendshipId)
            .update("status", "accepted")
            .addOnSuccessListener {
                // Check if the current user already has a friendship document with the other user
                firestore.collection("friends")
                    .whereEqualTo("userId1", currentUserId)
                    .whereEqualTo("userId2", otherUserId)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.size() > 0) {
                            // Update the current user's friendship document to have status "accepted"
                            firestore.collection("friends")
                                .document(documents.first().id)
                                .update("status", "accepted")
                                .addOnSuccessListener {
                                    // Friend request accepted
                                    Toast.makeText(context, "Accepted friend request", Toast.LENGTH_SHORT).show()

                                }
                                .addOnFailureListener {
                                    // Failed to accept friend request
                                    Toast.makeText(context, "Failed to accept friend request", Toast.LENGTH_SHORT).show()

                                }
                        } else {
                            // Create a new friendship document for the current user with status "accepted"
                            firestore.collection("friends")
                                .add(mapOf("userId1" to currentUserId, "userId2" to otherUserId, "status" to "accepted"))
                                .addOnSuccessListener {
                                }
                                .addOnFailureListener {
                                    // Failed to create friendship document for the current user
                                    Toast.makeText(context, "Failed to create current user's friendship document", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        // Failed to check for existing friendship document
                        Toast.makeText(context, "Failed to check for existing friendship document for current user", Toast.LENGTH_SHORT).show()                    }
            }
            .addOnFailureListener {
                // Failed to accept friend request
                Toast.makeText(context, "Failed to accept friend request", Toast.LENGTH_SHORT).show()            }
    }


    fun declineFriendRequest(friendshipId: String, otherUserId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val db = FirebaseFirestore.getInstance()

        // Update the status of the friendship document to "declined"
        db.collection("friends")
            .document(friendshipId)
            .update("status", "declined")
            .addOnSuccessListener {
                // Check if the current user has a friendship document with the other user
                db.collection("friends")
                    .whereEqualTo("userId1", currentUserId)
                    .whereEqualTo("userId2", otherUserId)
                    .get()
                    .addOnSuccessListener { documents ->
                        if (documents.size() > 0) {
                            // Update the status of the current user's friendship document to "declined"
                            for (document in documents) {
                                db.collection("friends")
                                    .document(document.id)
                                    .update("status", "declined")
                                Toast.makeText(context, "Declined friend request", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            // Create a new friendship document for the current user
                            db.collection("friends")
                                .add(mapOf("userId1" to currentUserId, "userId2" to otherUserId, "status" to "declined"))
                                .addOnSuccessListener {

                                }
                                .addOnFailureListener {
                                    // Failed to create friendship document for the current user
                                    Toast.makeText(context, "Failed to create current user's friendship document", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        // Failed to check for existing friendship document
                        Toast.makeText(context, "Failed to check for existing friendship document for current user", Toast.LENGTH_SHORT).show()                    }
            }
            .addOnFailureListener {
                // Failed to accept friend request
                Toast.makeText(context, "Failed to decline friend request", Toast.LENGTH_SHORT).show()            }
    }


}