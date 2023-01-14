package com.example.groupalarm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.SearchView
import com.example.groupalarm.adapter.FriendSearchAdapter
import com.example.groupalarm.adapter.FriendsAdapter
import com.example.groupalarm.data.Friends
import com.example.groupalarm.data.User
import com.example.groupalarm.data.Username
import com.example.groupalarm.databinding.ActivityFriendBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class FriendActivity : AppCompatActivity() {
    lateinit var binding: ActivityFriendBinding

    val firestore = FirebaseFirestore.getInstance()

    val currUserEmail = FirebaseAuth.getInstance().currentUser!!.email!!
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!
//    var currUsername = ""


    private lateinit var adapter: FriendSearchAdapter
    lateinit var friendsDb: CollectionReference
    lateinit var listener: ListenerRegistration


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = FriendSearchAdapter(this,
            FirebaseAuth.getInstance().currentUser!!.uid
        )
        binding.recyclerFriends.adapter = adapter

//        FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERS)
//            .document(currUserId).get().
//            addOnSuccessListener { documentSnapshot ->
//                val user = documentSnapshot.toObject(User::class.java)
//                if (user != null) {
//                    currUsername = user.username
//                }
//            }


        var currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!
        val friendsRef = firestore.collection("friends")
        friendsDb = FirebaseFirestore.getInstance().collection("friends")
        // Find friend documents where current user is the requested, userId2, and status is "pending" in live time
        val eventListener = object : EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?,
                                 e: FirebaseFirestoreException?) {
                if (e != null) {
                    Toast.makeText(
                        this@FriendActivity, "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
                var counter = 0

                val query = friendsRef
            .whereEqualTo("userId2", currUserId)
            .whereEqualTo("status", "pending")
        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                val senderId = document["userId1"] as String
                firestore.collection("users").document(senderId).get()
                    .addOnSuccessListener { snapshot ->
                        val requester = snapshot.toObject(User::class.java)
                        if (requester != null) {
                            counter += 1
                            if (counter > 0) {
                                binding.fakeButtonForFriendRequestNumber.show()
                                binding.numPendingRequestsNotif.visibility = View.VISIBLE
                                binding.numPendingRequestsNotif.text = counter.toString()
                            }
                        }
                    }.addOnFailureListener {

                    }
            }
        }.addOnFailureListener {

        }
                for (docChange in querySnapshot?.getDocumentChanges()!!) {
                    // If new request is added
                    FirebaseFirestore.getInstance().collection("friends").document().get().addOnSuccessListener { documentSnapshot ->
                        val user = documentSnapshot.toObject(Friends::class.java)
                        if (user != null ) {
                            if (user.userId2 == currUserId && user.status == "pending") {
                                if (docChange.type == DocumentChange.Type.ADDED) {
                                    adapter.notifyDataSetChanged()
                                    /*Todo
                                       this probably needs to be implemented furthermore once I add a remove friend functionality */
                                } else if (docChange.type == DocumentChange.Type.REMOVED) {
                                    adapter.notifyDataSetChanged()
                                } else if (docChange.type == DocumentChange.Type.MODIFIED) {
                                    val request = docChange.document.toObject(User::class.java)
                                    adapter.notifyDataSetChanged()
                                }
                            }


                        }
                    }
                }
                if(counter == 0) {
                        binding.fakeButtonForFriendRequestNumber.hide()
                        binding.numPendingRequestsNotif.text = counter.toString()
                        binding.numPendingRequestsNotif.visibility = View.GONE
                }
            }
        }
        listener = friendsDb.addSnapshotListener(eventListener)



        binding.btnFriendsView.setOnClickListener {
            val intentDetails = Intent()
            intentDetails.setClass(
                this@FriendActivity, FriendsViewActivity::class.java
            )
            startActivity(Intent(intentDetails))
        }

        binding.btnFriendsRequest.setOnClickListener {
            val intentDetails = Intent()
            intentDetails.setClass(
                this@FriendActivity, FriendRequestActivity::class.java
            )
            startActivity(Intent(intentDetails))
        }


        binding.bottomMenuNavigation.setSelectedItemId(R.id.friends)
        binding.bottomMenuNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.friends -> {
                    false
                }
                R.id.profile -> {
                    val intent = Intent(this@FriendActivity, ProfileActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.home -> {
                    val intent = Intent(this@FriendActivity, ScrollingActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.settings -> {
                    val intent = Intent(this@FriendActivity, SettingActivity::class.java)
                    startActivity(intent)
                    true
                }
            }
            false
        }

        searchUsernames("")

        binding.searchUserBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                adapter.clearUserList()
                searchUsernames(query)
                return false
            }
            override fun onQueryTextChange(newText: String): Boolean {
                adapter.clearUserList()
                searchUsernames(newText)
                return false
            }
        })
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
                            adapter.addUserToList(user, document.id)
                        }
                    }
                } else {
                    Toast.makeText(this, "No results found", Toast.LENGTH_SHORT).show()
                }
            }
    }


    fun sendFriendRequest(userId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
        // Check if the current user has already sent a friend request to this user
        firestore.collection("friends")
            .whereEqualTo("userId1", currentUserId)
            .whereEqualTo("userId2", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.size() > 0) {
                    // A friendship document already exists, check the status
                    for (document in documents) {
                        val status = document["status"] as String
                        if (status == "pending") {

                        } else if (status == "accepted"){

                        } else if (status == "declined") {
                            // The other user declined the friend request, but we are sending it again
                            // Update the status to "pending"
                            firestore.collection("friends")
                                .document(document.id)
                                .update("status", "pending")
                                .addOnSuccessListener {
                                    // Friend request sent
                                    Toast.makeText(this, "Friend request sent", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    // Failed to send friend request
                                    Toast.makeText(this, "Friend request failed to send", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                } else {
                    // Check if the other user has already sent a friend request to the current user
                    firestore.collection("friends")
                        .whereEqualTo("userId1", userId)
                        .whereEqualTo("userId2", currentUserId)
                        .get()
                        .addOnSuccessListener { documents ->
                            if (documents.size() > 0) {
                                // A friendship document already exists for the other user, check the status
                                for (document in documents) {
                                    val status = document["status"] as String
                                    if (status == "pending") {
                                        // The other user has already sent a friend request, update the status to "accepted"
                                        firestore.collection("friends")
                                            .document(document.id)
                                            .update("status", "accepted")
                                            .addOnSuccessListener {
                                                // Friend request accepted
                                                Toast.makeText(this, "Friend request accepted", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                // Failed to accept friend request
                                                Toast.makeText(this, "Friend request failed to accept", Toast.LENGTH_SHORT).show()
                                            }

                                        // Create a new friendship document for the current user with status "accepted"
                                        firestore.collection("friends")
                                            .add(mapOf("userId1" to currentUserId,
                                                "userId2" to userId,
                                                "status" to "accepted"))
                                            .addOnSuccessListener {
                                                // Friend request accepted
                                            }
                                            .addOnFailureListener {
                                                // Failed to create friendship document for the current user
                                                Toast.makeText(this, "Failed to create current user's friendship document", Toast.LENGTH_SHORT).show()
                                            }
                                    } else if (status == "declined") {
                                    }
                                }
                            } else {
                                // No friendship document exists, create a new friendship document with status "pending"
                                firestore.collection("friends")
                                    .add(mapOf("userId1" to currentUserId,
                                        "userId2" to userId,
                                        "status" to "pending"))
                                    .addOnSuccessListener {
                                        // Friend request sent
                                        Toast.makeText(this, "Friend request sent", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        // Failed to send friend request
                                        Toast.makeText(this, "Friend request failed to send", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                }
            }
    }

    fun getfriendstatus(userId1: String, userId2: String): String {
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

    // Using this to get the number of pending friend requests
    private fun getPendingFriendRequests() : Int {
        var currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!
        val friendsRef = firestore.collection("friends")
        var counter = 0
                // Find friend documents where current user is the requested, userId2, and status is "pending"
                val query = friendsRef
                    .whereEqualTo("userId2", currUserId)
                    .whereEqualTo("status", "pending")
                query.get().addOnSuccessListener { documents ->
                    for (document in documents) {
                        val senderId = document["userId1"] as String
                        firestore.collection("users").document(senderId).get()
                            .addOnSuccessListener { snapshot ->
                                val requester = snapshot.toObject(User::class.java)
                                if (requester != null) {
                                    counter += 1
                                }
                            }.addOnFailureListener {
//                                Toast.makeText(context,
//                                    "Error while retrieving pending requesters' documents", Toast.LENGTH_SHORT).show()
                            }
                    }
                }.addOnFailureListener {
//                    Toast.makeText(this,
//                        "Error while retrieving pending invites", Toast.LENGTH_SHORT).show()
                }
        return counter
    }
}