package com.example.groupalarm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.groupalarm.data.Alarm
import com.example.groupalarm.data.Friends
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.FriendSearchRowBinding
import com.example.groupalarm.databinding.FriendsRowBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendSearchAdapter : RecyclerView.Adapter<FriendSearchAdapter.ViewHolder> {
    lateinit var context: Context
    lateinit var searchuserList: List<User>

    val firestore = FirebaseFirestore.getInstance()
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid

    data class SearchResult(
        val userId: String,
        val username: String,
        val displayName: String,
        val profileImageUrl: String
    )
    var  searchResults = mutableListOf<SearchResult>()
    var  friendsList = mutableListOf<Friends>()

    constructor(context: Context, searchuserList: List<User>) : super() {
        this.context = context
        this.searchuserList = searchuserList
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendSearchAdapter.ViewHolder {
        val binding = FriendSearchRowBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendSearchAdapter.ViewHolder, position: Int) {
        var post = searchResults.get(holder.adapterPosition)
        holder.bind(post)
    }

    override fun getItemCount(): Int {
        return friendsList.size

    }

    inner class ViewHolder(val binding: FriendSearchRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(searchResult: SearchResult) {
            binding.username.text = searchResult.username
            binding.displayName.text = searchResult.displayName
            Glide.with(itemView)
                .load(searchResult.profileImageUrl)
                .into(binding.profilePicture)
            binding.btnSendFriendRequest.setOnClickListener {
                sendFriendRequest(currUserId, searchResult.userId)
            }
        }
    }

    // Send a friend request
    private fun sendFriendRequest(userId: String, friendId: String) {
        // Check if a friendship document already exists for these two users
        FirebaseFirestore.getInstance().collection("friendships")
            .whereEqualTo("userId1", userId)
            .whereEqualTo("userId2", friendId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.size() > 0) {
                    // A friendship document already exists
                    val friendship = documents.first()
                    val status = friendship["status"] as String
                    if (status == "pending") {
                        // There is already a pending friend request
                        if (friendship["userId1"] == userId) {
                            // The current user is user1, so accept the request
                            acceptFriendRequest(friendship.id, friendId)
                        } else {
                            // The current user is user2, so accept the request
                            acceptFriendRequest(friendship.id, userId)
                        }
                    } else {
                        // The users are already friends or the request was declined
                        Toast.makeText(context, "Friendship already exists", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    // No friendship document exists, so we can add one
                    val friendship = mapOf(
                        "userId1" to userId,
                        "userId2" to friendId,
                        "status" to "pending"
                    )
                    FirebaseFirestore.getInstance().collection("friendships")
                        .add(friendship)
                        .addOnSuccessListener {
                            Toast.makeText(context, "Friend request sent", Toast.LENGTH_SHORT).show()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error sending friend request: $e", Toast.LENGTH_SHORT).show()
                        }
                }
            }
    }

    // Accept a friend request
    private fun acceptFriendRequest(friendshipId: String, userId: String) {
        // Update the friendship document with a status of "accepted"
        FirebaseFirestore.getInstance().collection("friendships")
            .document(friendshipId)
            .update("status", "accepted")
            .addOnSuccessListener {
                Toast.makeText(context, "Friend request accepted", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error accepting friend request: $e", Toast.LENGTH_SHORT).show()
            }
    }

    // Decline a friend request
    private fun declineFriendRequest(friendshipId: String) {
        // Update the friendship document with a status of "declined"
        FirebaseFirestore.getInstance().collection("friendships")
            .document(friendshipId)
            .update("status", "declined")
            .addOnSuccessListener {
                Toast.makeText(context, "Friend request declined", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error declining friend request: $e", Toast.LENGTH_SHORT).show()
            }
    }

    // Check if two users are friends
    fun areFriends(userId1: String, userId2: String): Boolean {
        val friendsRef = firestore.collection("friends")
        val query = friendsRef
            .whereEqualTo("userId1", userId1)
            .whereEqualTo("userId2", userId2)
            .whereEqualTo("status", "accepted")
        var areFriends = false
        query.get().addOnSuccessListener { documents ->
            if (documents.size() > 0) {
                areFriends = true
            }
        }
        return areFriends
    }

    private fun showAcceptDeclineDialog(friendshipId: String, userId: String) {
        val builder = AlertDialog.Builder(context)
            .setTitle("Friend request pending")
            .setMessage("Would you like to accept or decline the friend request?")
            .setPositiveButton("Accept") { _, _ ->
                // Accept the friend request
                acceptFriendRequest(friendshipId, userId)
            }
            .setNegativeButton("Decline") { _, _ ->
                // Decline the friend request
                declineFriendRequest(friendshipId)
            }
        builder.show()
    }



    // Search for users
    fun searchUsers(currentUsername: String, username: String, callback: (List<User>) -> Unit) {
        val usersRef = firestore.collection("usernames")
        val query = usersRef
            .whereEqualTo("usernames", username)
            .whereNotEqualTo("usernames", currentUsername)
            .limit(10)

        query.get().addOnSuccessListener { documents ->
            val users = documents.map { doc ->
                doc.toObject(User::class.java)
            }
            callback(users)
        }
    }

    // Search for friends
    fun searchFriends(currentUserId: String, username: String, callback: (List<User>) -> Unit) {
        val friendsRef = firestore.collection("friends")
        val query = friendsRef
            .whereEqualTo("user1", currentUserId)
            .whereEqualTo("status", "accepted")
            .limit(10)

        query.get().addOnSuccessListener { documents ->
            val friendIds = documents.map { doc ->
                doc.get("user2") as String
            }

            val usersRef = firestore.collection("users")
            val usersQuery = usersRef
                .whereIn("id", friendIds)
                .whereEqualTo("username", username)

            usersQuery.get().addOnSuccessListener { documents ->
                val users = documents.map { doc ->
                    doc.toObject(User::class.java)
                }
                callback(users)
            }
        }
    }

    // Get a list of a user's friends
    fun getFriends(userId: String): List<String> {
        val friendsRef = firestore.collection("friends")
        val query1 = friendsRef
            .whereEqualTo("userId1", userId)
            .whereEqualTo("status", "accepted")
        val query2 = friendsRef
            .whereEqualTo("userId2", userId)
            .whereEqualTo("status", "accepted")
        val friends = mutableListOf<String>()
        query1.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val friendId = document.get("userId2") as String
                friends.add(friendId)
            }
        }
        query2.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val friendId = document.get("userId1") as String
                friends.add(friendId)
            }
        }
        return friends
    }

    // Get a list of pending friend requests for a user
    fun getPendingFriendRequests(userId: String): List<String> {
        val friendsRef = firestore.collection("friends")
        val query = friendsRef
            .whereEqualTo("userId2", userId)
            .whereEqualTo("status", "pending")
        val requests = mutableListOf<String>()
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val senderId = document.get("userId1") as String
                requests.add(senderId)
            }
        }
        return requests
    }

    // Cancel a sent friend request
    fun cancelFriendRequest(senderId: String, receiverId: String) {
        val friendsRef = firestore.collection("friends")
        val query = friendsRef
            .whereEqualTo("userId1", senderId)
            .whereEqualTo("userId2", receiverId)
            .whereEqualTo("status", "pending")
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val friendshipId = document.id
                val friendshipRef = friendsRef.document(friendshipId)
                friendshipRef.delete()
            }
        }
    }

    // Remove a friend
    fun removeFriend(userId1: String, userId2: String) {
        val friendsRef = firestore.collection("friends")
        val query = friendsRef
            .whereEqualTo("userId1", userId1)
            .whereEqualTo("userId2", userId2)
            .whereEqualTo("status", "accepted")
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val friendshipId = document.id
                val friendshipRef = friendsRef.document(friendshipId)
                friendshipRef.delete()
            }
        }
    }

    // Get a list of users who have sent a friend request to a user
    fun getFriendRequestsSent(userId: String): List<String> {
        val friendsRef = firestore.collection("friends")
        val query = friendsRef
            .whereEqualTo("userId1", userId)
            .whereEqualTo("status", "pending")
        val requests = mutableListOf<String>()
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val receiverId = document.get("userId2") as String
                requests.add(receiverId)
            }
        }
        return requests
    }

    // Get the status of a friend relationship between two users
    fun getFriendshipStatus(userId1: String, userId2: String): String {
        val friendsRef = firestore.collection("friends")
        val query = friendsRef
            .whereEqualTo("userId1", userId1)
            .whereEqualTo("userId2", userId2)
        var status = "not_friends"
        query.get().addOnSuccessListener { documents ->
            if (documents.size() > 0) {
                val document = documents.first()
                status = document.get("status") as String
            }
        }
        return status
    }

    fun getFriendshipId(userId1: String, userId2: String) {
        val friendsRef = firestore.collection("friends")
        val query = friendsRef
            .whereEqualTo("userId1", userId1)
            .whereEqualTo("userId2", userId2)
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val friendshipId = document.id
                // Do something with the friendship ID
            }
        }
    }

    // Get a list of users who have been blocked by a user
    fun getBlockedUsers(userId: String): List<String> {
        val friendsRef = firestore.collection("friends")
        val query = friendsRef
            .whereEqualTo("userId1", userId)
            .whereEqualTo("status", "blocked")
        val blocked = mutableListOf<String>()
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val blockedId = document.get("userId2") as String
                blocked.add(blockedId)
            }
        }
        return blocked
    }

    // Block a user
    fun blockUser(userId1: String, userId2: String) {
        val friendsRef = firestore.collection("friends")
        val friendship = hashMapOf(
            "userId1" to userId1,
            "userId2" to userId2,
            "status" to "blocked"
        )
        friendsRef.add(friendship)
    }

    // Unblock a user
    fun unblockUser(userId1: String, userId2: String) {
        val friendsRef = firestore.collection("friends")
        val query = friendsRef
            .whereEqualTo("userId1", userId1)
            .whereEqualTo("userId2", userId2)
            .whereEqualTo("status", "blocked")
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val friendshipId = document.id
                val friendshipRef = friendsRef.document(friendshipId)
                friendshipRef.delete()
            }
        }
    }
}