package com.example.groupalarm
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.groupalarm.adapter.FriendsAdapter
import com.example.groupalarm.data.Friends
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.ActivityFriendsViewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class FriendsViewActivity : AppCompatActivity() {

    lateinit var binding: ActivityFriendsViewBinding

    private lateinit var adapter: FriendsAdapter

    lateinit var friendsDb: CollectionReference
    lateinit var listener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFriendsViewBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = FriendsAdapter(this,
            FirebaseAuth.getInstance().currentUser!!.uid
        )

        binding.recyclerFriends.adapter = adapter

        // Get the current user's friends
        getAllUserFriends()
    }

    private fun getAllUserFriends() {
        friendsDb = FirebaseFirestore.getInstance().collection("friends")

        val eventListener = object : EventListener<QuerySnapshot> {
            override fun onEvent(
                querySnapshot: QuerySnapshot?,
                e: FirebaseFirestoreException?
            ) {
                if (e != null) {
                    Toast.makeText(
                        this@FriendsViewActivity, "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                val currUserId = FirebaseAuth.getInstance().currentUser!!.uid
                val db = FirebaseFirestore.getInstance()

                // Get all friendship documents where the current user is involved in
                db.collection("friends")
                    .whereEqualTo("userId1", currUserId)
                    .get()
                    .addOnSuccessListener { documents ->
                        for (document in documents) {
                            if (document["userId1"] == currUserId) {
                                val friendId = document["userId2"] as String
                                val status = document["status"] as String
                                if (status == "accepted") {
                                    // Get the friend's user document
                                    db.collection("users").document(friendId).get()
                                        .addOnSuccessListener { snapshot ->
                                            val friend = snapshot.toObject(User::class.java)
                                            if (friend != null && !adapter.alreadyHasFriendDisplayed(document.id)) {
                                                adapter.addFriendsToList(friend, document.id)
                                            }
                                            adapter.notifyDataSetChanged()
                                        }.addOnFailureListener {
//                                            Toast.makeText(
//                                                this,
//                                                "Error while retrieving friend's documents",
//                                                Toast.LENGTH_SHORT
//                                            ).show()
                                        }
                                }
                            } else {
                                // There is no else because we will always have two friend documents
                                // per friends when they have it either accepted or declined
                                // There might arise an issue with pending since if one user sends a request but the other one doesnt accept it,
                                // There is only one document between those two people as of now.
                                // But for now this shouldn't be a problem
                            }
                        }
                    }

                for (docChange in querySnapshot?.getDocumentChanges()!!) {
                    // If new request is added
                    FirebaseFirestore.getInstance().collection("friends").document().get()
                        .addOnSuccessListener { documentSnapshot ->
                            val user = documentSnapshot.toObject(Friends::class.java)
                            if (user != null) {
                                if (user.userId1 == currUserId && user.status == "accepted") {
                                    if (docChange.type == DocumentChange.Type.ADDED) {
                                        val request = docChange.document.toObject(User::class.java)
//                                        adapter.addRequestsToList(request, docChange.document.id)
                                        adapter.notifyDataSetChanged()
                                        /*Todo
                                       this probably needs to be implemented furthermore once I add a remove friend functionality */
                                    } else if (docChange.type == DocumentChange.Type.REMOVED) {
                                        adapter.removeFriendByKey(docChange.document.id)
                                        adapter.notifyDataSetChanged()
                                    } else if (docChange.type == DocumentChange.Type.MODIFIED) {
                                        val request = docChange.document.toObject(User::class.java)
                                        if (!adapter.alreadyHasFriendDisplayed(docChange.document.id)) {
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