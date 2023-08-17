package group.alarm.groupalarm

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.ktx.Firebase
import group.alarm.groupalarm.adapter.FriendSearchAdapter
import group.alarm.groupalarm.data.User
import group.alarm.groupalarm.databinding.FragmentFriendBinding
import java.util.Calendar
import androidx.appcompat.widget.SearchView
import group.alarm.groupalarm.data.Friends
import com.google.firebase.firestore.*
import com.google.firebase.firestore.EventListener
import java.util.*



// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [FriendFragment.newInstance] factory method to
 * create an instance of getActivity() as DashActivity fragment.
 */
class FriendFragment : Fragment() {
    lateinit var binding: FragmentFriendBinding
    val firestore = FirebaseFirestore.getInstance()

    val currUserEmail = FirebaseAuth.getInstance().currentUser!!.email!!
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!

    private lateinit var adapter: FriendSearchAdapter
    lateinit var friendsDb: CollectionReference
    lateinit var listener: ListenerRegistration

    companion object {
        @JvmStatic
        fun newInstance() = FriendFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for getActivity() as DashActivity fragment
        binding = FragmentFriendBinding.inflate(
            inflater, container, false)

        adapter = FriendSearchAdapter(getActivity() as DashActivity,
            FirebaseAuth.getInstance().currentUser!!.uid
        )
        binding.recyclerFriends.adapter = adapter

        val friendsRef = firestore.collection("friends")
        friendsDb = FirebaseFirestore.getInstance().collection("friends")
        // Find friend documents where current user is the requested, userId2, and status is "pending" in live time
        val eventListener = object : EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?,
                                 e: FirebaseFirestoreException?) {
                if (e != null) {
                    Toast.makeText(
                        getActivity() as DashActivity, "Error: ${e.message}",
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
                                       getActivity() as DashActivity probably needs to be implemented furthermore once I add a remove friend functionality */
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
                getActivity() as DashActivity, FriendsViewActivity::class.java
            )
            startActivity(Intent(intentDetails))
        }

        binding.btnFriendsRequest.setOnClickListener {
            val intentDetails = Intent()
            intentDetails.setClass(
                getActivity() as DashActivity, FriendRequestActivity::class.java
            )
            startActivity(Intent(intentDetails))
        }


//        binding.bottomMenuNavigation.setSelectedItemId(R.id.friends)
//        binding.bottomMenuNavigation.setOnItemSelectedListener { item ->
//            when (item.itemId) {
//                R.id.friends -> {
//                    false
//                }
//                R.id.profile -> {
//                    val intent = Intent(getActivity() as DashActivity, ProfileActivity::class.java)
//                    startActivity(intent)
//                    true
//                }
//                R.id.home -> {
//                    val intent = Intent(getActivity() as DashActivity, DashboardActivity::class.java)
//                    startActivity(intent)
//                    true
//                }
//                R.id.settings -> {
//                    val intent = Intent(getActivity() as DashActivity, SettingActivity::class.java)
//                    startActivity(intent)
//                    true
//                }
//            }
//            false
//        }

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
        // When the user closes the app
        val presenceUserRef = Firebase.database.getReference("users").child(currUserId).child("activityStatus")
        presenceUserRef.onDisconnect().setValue(Timestamp(Calendar.getInstance().time))

        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val database = Firebase.database
        val usersRef = database.getReference("users").child(currUserId)
        usersRef.child("activityStatus").setValue(true)
    }

    private fun searchUsernames(query: String) {
        val firestore = FirebaseFirestore.getInstance()
        val friendsRef = firestore.collection("friends")
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
                        // Check to see if it is the user themself or if they are already displayed
                        if (user != null && user.email != currUserEmail && !adapter.alreadyHasUserDisplayed(document.id)) {
                            // Only show non-friends
                            friendsRef
                                .whereEqualTo("userId1", currUserId)
                                .whereEqualTo("userId2", document.id)
                                .whereEqualTo("status", "accepted")
                                .get().addOnSuccessListener { querySnapshot ->
                                    // If it is == 0, that means the users are not friends, so display
                                    if(querySnapshot.size() == 0) {
                                        adapter.addUserToList(user, document.id)

                                    }
                                }
                        }
                    }
                } else {
                    Toast.makeText(getActivity() as DashActivity, "No results found", Toast.LENGTH_SHORT).show()
                }
            }
    }


    fun sendFriendRequest(userId: String) {
        val currentUserId = FirebaseAuth.getInstance().currentUser!!.uid
        // Check if the current user has already sent a friend request to getActivity() as DashActivity user
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
                                    Toast.makeText(getActivity() as DashActivity, "Friend request sent", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    // Failed to send friend request
                                    Toast.makeText(getActivity() as DashActivity, "Friend request failed to send", Toast.LENGTH_SHORT).show()
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
                                                Toast.makeText(getActivity() as DashActivity, "Friend request accepted", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                // Failed to accept friend request
                                                Toast.makeText(getActivity() as DashActivity, "Friend request failed to accept", Toast.LENGTH_SHORT).show()
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
                                                Toast.makeText(getActivity() as DashActivity, "Failed to create current user's friendship document", Toast.LENGTH_SHORT).show()
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
                                        Toast.makeText(getActivity() as DashActivity, "Friend request sent", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        // Failed to send friend request
                                        Toast.makeText(getActivity() as DashActivity, "Friend request failed to send", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                }
            }
    }

    // Using getActivity() as DashActivity to get the number of pending friend requests
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
//                    Toast.makeText(getActivity() as DashActivity,
//                        "Error while retrieving pending invites", Toast.LENGTH_SHORT).show()
        }
        return counter
    }

    fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

}