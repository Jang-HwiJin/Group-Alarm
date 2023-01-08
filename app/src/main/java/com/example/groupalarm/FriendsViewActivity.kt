package com.example.groupalarm
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.groupalarm.adapter.FriendsAdapter
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.ActivityFriendsViewBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class FriendsViewActivity : AppCompatActivity() {

    lateinit var binding: ActivityFriendsViewBinding

    private lateinit var adapter: FriendsAdapter

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
        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
        val db = FirebaseFirestore.getInstance()

        // Get all friendship documents where the current user is involved in
        db.collection("friends")
            .whereEqualTo("userId1", currentUserId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    if (document["userId1"] == currentUserId) {
                        val friendId = document["userId2"] as String
                        val status = document["status"] as String
                        if (status == "accepted") {
                            // Get the friend's user document
                            db.collection("users").document(friendId).get()
                                .addOnSuccessListener { snapshot ->
                                    val friend = snapshot.toObject(User::class.java)
                                    if (friend != null) {
                                        adapter.addFriendsToList(friend, document.id)
                                    }
                                    adapter.notifyDataSetChanged()
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
    }
}