package group.alarm.groupalarm.adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import group.alarm.groupalarm.*
import group.alarm.groupalarm.data.Friends
import group.alarm.groupalarm.data.User
import group.alarm.groupalarm.data.Username
import group.alarm.groupalarm.databinding.FriendSearchRowBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class FriendSearchAdapter : RecyclerView.Adapter<FriendSearchAdapter.ViewHolder> {

    var context: Context
    var currentUid: String
    var currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!
    private var userList = mutableListOf<User>()
    private var userIdList = mutableListOf<String>()

    // Thought this would be used to check to see if users that show up are already friends, but might not need it
    private var usernameList = mutableListOf<String>()

    val firestore = FirebaseFirestore.getInstance()

    constructor(context: Context, uid: String) : super() {
        this.context = context
        this.currentUid = uid
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendSearchAdapter.ViewHolder {
        val binding = FriendSearchRowBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: FriendSearchAdapter.ViewHolder, position: Int) {
        var user = userList.get(holder.adapterPosition)
        var userDocId = userIdList.get(holder.adapterPosition)

        holder.bind(user, userDocId)
    }

    fun addUserToList(users: User, key: String) {
        if (users != null) {
            userList.add(users)
        }
        userIdList.add(key)
        notifyItemInserted(userList.lastIndex)
    }

    fun alreadyHasUserDisplayed(key: String): Boolean {
        return userIdList.contains(key)
    }

    fun clearUserList() {
        val userListSize = getItemCount()
        userList.clear()
        userIdList.clear()
        notifyItemRangeRemoved(0, userListSize);
    }

    fun removeUser(index: Int) {
        FirebaseFirestore.getInstance().collection(
            DashboardFragment.COLLECTION_ALARMS).document(
            userIdList[index]
        ).delete()

        userList.removeAt(index)
        userIdList.removeAt(index)
        notifyItemRemoved(index)
    }

    // when somebody else removes an object
    fun removeUserByKey(key: String) {
        val index = userIdList.indexOf(key)
        if (index != -1) {
            userList.removeAt(index)
            userIdList.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    inner class ViewHolder(val binding: FriendSearchRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User, userDocId: String) {
            val userUsername = user.username

            binding.username.text = "@"+ userUsername
            binding.displayName.text = user.displayName
            Glide.with(context)
                .load(user.profileImg)
                .into(binding.profilePicture)

            //TODO works now
            // But I would like it if it would load faster, I think if a user really tried, they could press send friend request
            // before the button changes so work on optimizing speed later, but works for now
            firestore.collection("friends")
                .whereEqualTo("userId1", currUserId)
                .get()
                .addOnSuccessListener { documents ->
                    if(documents.size() > 0) {
                        for (document in documents) {
                            val userDoc = document.toObject(Friends::class.java)
                            val friendId = userDoc.userId2
                            val status = userDoc.status
                            if (status == "accepted") {
                                // Get the friend's user document
                                firestore.collection("users").document(friendId).get()
                                    .addOnSuccessListener { snapshot ->
                                        val friend = snapshot.toObject(User::class.java)
                                        if (friend != null && user.username == friend.username) {
                                            binding.btnSendFriendRequest.text = "Friends"
                                            binding.btnSendFriendRequest.isClickable = false
                                            binding.btnSendFriendRequest.setBackgroundColor(Color.DKGRAY)
                                        }
                                    }
                            } else if (status == "pending") {
                                // Get the friend's user document
                                firestore.collection("users").document(friendId).get()
                                    .addOnSuccessListener { snapshot ->
                                        val friend = snapshot.toObject(User::class.java)
                                        if (friend != null && user.username == friend.username) {
                                            binding.btnSendFriendRequest.text = "Requested"
                                            binding.btnSendFriendRequest.isClickable = false
                                            binding.btnSendFriendRequest.setBackgroundColor(Color.DKGRAY)
                                        }
                                    }
                            }
                            else if (status == "declined") {
                                // Get the friend's user document
                                firestore.collection("users").document(friendId).get()
                                    .addOnSuccessListener { snapshot ->
                                        val friend = snapshot.toObject(User::class.java)
                                        if (friend != null && user.username == friend.username) {
                                            binding.btnSendFriendRequest.text = "Request"
                                            binding.btnSendFriendRequest.isClickable = true
                                        }
                                    }
                            } else {
                                binding.btnSendFriendRequest.text = "Request"
                                binding.btnSendFriendRequest.isClickable = true
                            }
                        }
                    }
                }


            binding.btnSendFriendRequest.setOnClickListener {
                FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERNAMES).
                document(user.username).get().
                addOnSuccessListener { documentSnapshot ->
                    val user = documentSnapshot.toObject(Username::class.java)
                    if(user != null) {
                        val receiverId = user.uid
                        sendFriendRequest(receiverId)
                        binding.btnSendFriendRequest.text = "Requested"
                        binding.btnSendFriendRequest.isClickable = false
                        binding.btnSendFriendRequest.setBackgroundColor(Color.DKGRAY)
                    }
                }.addOnFailureListener {
                    Toast.makeText(context, "User could not be found, please check the spelling", Toast.LENGTH_SHORT).show()
                }
            }

//            binding.btnViewProfile.setOnClickListener {
//                val intentDetails = Intent()
//                intentDetails.putExtra("Username", user.username)
//                intentDetails.putExtra("DisplayName", user.displayName)
//                intentDetails.putExtra("ProfileImgUrl", user.profileImg)
//
//                intentDetails.setClass(
//                    context, ProfileDetailsActivity::class.java
//                )
//                (context as DashActivity).startActivity(Intent(intentDetails))
//            }

            binding.cardView.setOnClickListener {
                val intentDetails = Intent()
                intentDetails.putExtra("Username", user.username)
                intentDetails.putExtra("DisplayName", user.displayName)
                intentDetails.putExtra("ProfileImgUrl", user.profileImg)

                intentDetails.setClass(
                    context, ProfileDetailsActivity::class.java
                )
                (context as DashActivity).startActivity(Intent(intentDetails))
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
                                    Toast.makeText(context, "Friend request sent", Toast.LENGTH_SHORT).show()
                                }
                                .addOnFailureListener {
                                    // Failed to send friend request
                                    Toast.makeText(context, "Friend request failed to send", Toast.LENGTH_SHORT).show()
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
                                                Toast.makeText(context, "Friend request accepted", Toast.LENGTH_SHORT).show()
                                            }
                                            .addOnFailureListener {
                                                // Failed to accept friend request
                                                Toast.makeText(context, "Friend request failed to accept", Toast.LENGTH_SHORT).show()
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
                                                Toast.makeText(context, "Failed to create current user's friendship document", Toast.LENGTH_SHORT).show()
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
                                        Toast.makeText(context, "Friend request sent", Toast.LENGTH_SHORT).show()
                                    }
                                    .addOnFailureListener {
                                        // Failed to send friend request
                                        Toast.makeText(context, "Friend request failed to send", Toast.LENGTH_SHORT).show()
                                    }
                            }
                        }
                }
            }
    }
}