package com.example.groupalarm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.groupalarm.adapter.FriendRequestAdapter
import com.example.groupalarm.data.Friends
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.ActivityFriendRequestBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import com.google.firebase.ktx.Firebase
import java.util.*

class FriendRequestActivity : AppCompatActivity() {


    lateinit var binding: ActivityFriendRequestBinding

    var firestore = FirebaseFirestore.getInstance()

    companion object {
        var requestIds = hashMapOf<User, String>()
    }

    private lateinit var adapter: FriendRequestAdapter
    lateinit var friendsDb: CollectionReference
    lateinit var listener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendRequestBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = FriendRequestAdapter(this,
            FirebaseAuth.getInstance().currentUser!!.uid
        )

        binding.recyclerFriends.adapter = adapter

        getPendingFriendRequests()

    }


    // Get a list of pending friend requests for a user
    private fun getPendingFriendRequests() {
        var currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!
        val friendsRef = firestore.collection("friends")
        friendsDb = FirebaseFirestore.getInstance().collection("friends")


        val eventListener = object : EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?,
                                 e: FirebaseFirestoreException?) {
                if (e != null) {
                    Toast.makeText(
                        this@FriendRequestActivity, "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }
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
                                if (requester != null && !adapter.alreadyHasRequestDisplayed(document.id)) {
                                    adapter.addRequestsToList(requester, document.id)
                                }
                                adapter.notifyDataSetChanged()
                            }.addOnFailureListener {
//                                Toast.makeText(context,
//                                    "Error while retrieving pending requesters' documents", Toast.LENGTH_SHORT).show()
                            }
                    }
                    adapter.notifyDataSetChanged()
                }.addOnFailureListener {
//                    Toast.makeText(this,
//                        "Error while retrieving pending invites", Toast.LENGTH_SHORT).show()
                }

                for (docChange in querySnapshot?.getDocumentChanges()!!) {
                    // If new request is added
                    FirebaseFirestore.getInstance().collection("friends").document().get().addOnSuccessListener { documentSnapshot ->
                            val user = documentSnapshot.toObject(Friends::class.java)
                            if (user != null ) {
                                if (user.userId2 == currUserId && user.status == "pending") {
                                    if (docChange.type == DocumentChange.Type.ADDED) {
                                        val request = docChange.document.toObject(User::class.java)
//                                        adapter.addRequestsToList(request, docChange.document.id)
                                        adapter.notifyDataSetChanged()
                                        /*Todo
                                           this probably needs to be implemented furthermore once I add a remove friend functionality */
                                    } else if (docChange.type == DocumentChange.Type.REMOVED) {
                                        adapter.removeRequestByKey(docChange.document.id)
                                        adapter.notifyDataSetChanged()
                                    } else if (docChange.type == DocumentChange.Type.MODIFIED) {
                                        val request = docChange.document.toObject(User::class.java)
                                        if (!adapter.alreadyHasRequestDisplayed(docChange.document.id)) {
//                                        adapter.removeRequestByKey(docChange.document.id)
//                                            adapter.addRequestsToList(request, docChange.document.id)

                                        }
                                        adapter.notifyDataSetChanged()
                                    }
                                }


                            }
                            }
                            }
                            }
                        }
        listener = friendsDb.addSnapshotListener(eventListener)

    }

}