package com.example.groupalarm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.groupalarm.data.Friends
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.FriendRequestsRowBinding
import com.google.firebase.firestore.FirebaseFirestore

class FriendRequestAdapter : RecyclerView.Adapter<FriendRequestAdapter.ViewHolder> {
    lateinit var context: Context
    lateinit var currentUid: String

    val firestore = FirebaseFirestore.getInstance()

    var  friendsList = mutableListOf<Friends>()

    constructor(context: Context, uid: String) : super() {
        this.context = context
        this.currentUid = uid
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestAdapter.ViewHolder {
        val binding = FriendRequestsRowBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FriendRequestAdapter.ViewHolder, position: Int) {

    }

    override fun getItemCount(): Int {
        return friendsList.size

    }

    inner class ViewHolder(val binding: FriendRequestsRowBinding) : RecyclerView.ViewHolder(binding.root) {
    }

    // Send a friend request
    fun sendFriendRequest(senderId: String, receiverId: String) {
        val friendsRef = firestore.collection("friends")
        val friends = hashMapOf(
            "userId1" to senderId,
            "userId2" to receiverId,
            "status" to "pending"
        )
        friendsRef.add(friends)
    }

    // Accept a friend request
    fun acceptFriendRequest(senderId: String, receiverId: String) {
        val friendsRef = firestore.collection("friends")
        val query = friendsRef
            .whereEqualTo("userId1", senderId)
            .whereEqualTo("userId2", receiverId)
            .whereEqualTo("status", "pending")
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val friendsId = document.id
                val friendsRef = friendsRef.document(friendsId)
                friendsRef.update("status", "accepted")
            }
        }
    }

    // Decline a friend request
    fun declineFriendRequest(senderId: String, receiverId: String) {
        val friendsRef = firestore.collection("friends")
        val query = friendsRef
            .whereEqualTo("userId1", senderId)
            .whereEqualTo("userId2", receiverId)
            .whereEqualTo("status", "pending")
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val friendsId = document.id
                val friendsRef = friendsRef.document(friendsId)
                friendsRef.update("status", "declined")
            }
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
                val friendsId = document.id
                val friendsRef = friendsRef.document(friendsId)
                friendsRef.delete()
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
                val friendsId = document.id
                val friendsRef = friendsRef.document(friendsId)
                friendsRef.delete()
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
    fun getfriendsStatus(userId1: String, userId2: String): String {
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

    fun getfriendsId(userId1: String, userId2: String) {
        val friendsRef = firestore.collection("friends")
        val query = friendsRef
            .whereEqualTo("userId1", userId1)
            .whereEqualTo("userId2", userId2)
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val friendsId = document.id
                // Do something with the friends ID
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
        val friends = hashMapOf(
            "userId1" to userId1,
            "userId2" to userId2,
            "status" to "blocked"
        )
        friendsRef.add(friends)
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
                val friendsId = document.id
                val friendsRef = friendsRef.document(friendsId)
                friendsRef.delete()
            }
        }
    }
}