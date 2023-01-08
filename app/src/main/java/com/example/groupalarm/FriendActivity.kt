package com.example.groupalarm

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.groupalarm.adapter.FriendSearchAdapter
import com.example.groupalarm.data.Friends
import com.example.groupalarm.data.User
import com.example.groupalarm.data.Username
import com.example.groupalarm.databinding.ActivityFriendBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class FriendActivity : AppCompatActivity() {
    lateinit var binding: ActivityFriendBinding

    val firestore = FirebaseFirestore.getInstance()

    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!
    var currUsername = ""


    private lateinit var adapter: FriendSearchAdapter


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendBinding.inflate(layoutInflater)
        setContentView(binding.root)

        FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERS)
            .document(currUserId).get().
            addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)
                if (user != null) {
                    currUsername = user.username
                }
            }

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
                            if (counter > 0) {
                                binding.fakeButtonForFriendRequestNumber.show()
                                binding.numPendingRequestsNotif.visibility = View.VISIBLE
                                binding.numPendingRequestsNotif.text = counter.toString()
                            }
                            else {
                                binding.fakeButtonForFriendRequestNumber.hide()
                                binding.numPendingRequestsNotif.visibility = View.GONE
                            }
                        }
                    }.addOnFailureListener {
                    }
            }
        }.addOnFailureListener {
        }


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



        binding.btnSearch.setOnClickListener {
//            search(currUsername ,binding.searchUserBar.text.toString())
//            binding.recyclerFriends.adapter = adapter
            FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERNAMES).
            document(binding.searchUserBar.text.toString()).get().
            addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(Username::class.java)
                if(user != null) {
                    val receiverId = user.uid
                    sendFriendRequest(receiverId)
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Username could not be found, please check the spelling", Toast.LENGTH_SHORT).show()
            }
        }



        //IDEA
        /*
        Implement a recyclerview here for friends list, it'll basically be their username with their picture
        and on the top right, there'll be a fab where you can add friends and also another button on top with all the friend requests

         */

    }

    private fun showAcceptDeclineDialog(friendshipId: String, userId: String) {
        val builder = AlertDialog.Builder(this)
            .setTitle("Friend request pending")
            .setMessage("Would you like to accept or decline the friend request from $userId?")
            .setPositiveButton("Accept") { _, _ ->
                // Accept the friend request
                acceptFriendRequest(friendshipId, userId)
                Toast.makeText(this,
                    "Friend request accepted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Decline") { _, _ ->
                // Decline the friend request
                declineFriendRequest(friendshipId, userId)
                Toast.makeText(this,
                    "Friend request declined", Toast.LENGTH_SHORT).show()
            }
        builder.show()
    }


    fun searchUsers(currentUsername: String, username: (Any) -> Unit) {
        val usersRef = firestore.collection("usernames")
        val query = usersRef
            .whereEqualTo("usernames", username)
            .whereNotEqualTo("usernames", currentUsername)
            .limit(10)

        query.get().addOnSuccessListener { documents ->
            val users = documents.map { doc ->
                doc.toObject(User::class.java)
            }
//            callback(users)
        }
    }

    fun search(currentUsername: String, queryUsername: String) {
        searchUsers(queryUsername) { searchResults ->
            adapter = FriendSearchAdapter(this, searchResults as List<User>)
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
                                            .add(mapOf("userId1" to currentUserId, "userId2" to userId, "status" to "accepted"))
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
                                    .add(mapOf("userId1" to currentUserId, "userId2" to userId, "status" to "pending"))
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
                                }
                                .addOnFailureListener {
                                    // Failed to accept friend request
                                }
                        } else {
                            // Create a new friendship document for the current user with status "accepted"
                            firestore.collection("friends")
                                .add(mapOf("userId1" to currentUserId, "userId2" to otherUserId, "status" to "accepted"))
                                .addOnSuccessListener {
                                    // Friend request accepted
                                }
                                .addOnFailureListener {
                                    // Failed to create friendship document for the current user
                                    Toast.makeText(this, "Failed to create current user's friendship document", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        // Failed to check for existing friendship document
                        Toast.makeText(this, "Failed to check for existing friendship document for current user", Toast.LENGTH_SHORT).show()                    }
            }
            .addOnFailureListener {
                // Failed to accept friend request
                Toast.makeText(this, "Failed to accept friend request", Toast.LENGTH_SHORT).show()            }
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
                            }
                        } else {
                            // Create a new friendship document for the current user
                            db.collection("friends")
                                .add(mapOf("userId1" to currentUserId, "userId2" to otherUserId, "status" to "declined"))
                                .addOnSuccessListener {
                                    // Friend request accepted
                                }
                                .addOnFailureListener {
                                    // Failed to create friendship document for the current user
                                    Toast.makeText(this, "Failed to create current user's friendship document", Toast.LENGTH_SHORT).show()
                                }
                        }
                    }
                    .addOnFailureListener {
                        // Failed to check for existing friendship document
                        Toast.makeText(this, "Failed to check for existing friendship document for current user", Toast.LENGTH_SHORT).show()                    }
            }
            .addOnFailureListener {
                // Failed to accept friend request
                Toast.makeText(this, "Failed to decline friend request", Toast.LENGTH_SHORT).show()            }
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