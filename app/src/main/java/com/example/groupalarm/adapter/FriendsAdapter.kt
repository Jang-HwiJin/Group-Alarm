package com.example.groupalarm.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.groupalarm.*
import com.example.groupalarm.data.Friends
import com.example.groupalarm.data.User
import com.example.groupalarm.data.Username
import com.example.groupalarm.databinding.FriendSearchRowBinding
import com.example.groupalarm.databinding.FriendsRowBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FriendsAdapter : RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    var context: Context
    var currentUid: String
    private var friendsList = mutableListOf<User>()
    private var friendshipIdList = mutableListOf<String>()
    var currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!


    constructor(context: Context, uid: String) : super() {
        this.context = context
        this.currentUid = uid
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = FriendsRowBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return friendsList.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        var friend = friendsList.get(holder.adapterPosition)
        var friendDocId = friendshipIdList.get(holder.adapterPosition)

        holder.bind(friend, friendDocId)
    }

    fun addFriendsToList(friends: User, key: String) {
        if (friends != null) {
            friendsList.add(friends)
        }
        friendshipIdList.add(key)
        notifyItemInserted(friendsList.lastIndex)
    }

    fun alreadyHasFriendDisplayed(key: String): Boolean {
        FirebaseAuth.AuthStateListener {

        }
        return friendshipIdList.contains(key)
    }

//    if(isFormValid() && imgUrl.isNotEmpty()) {
//        docToUpdate.update(
//            "displayName", binding.editNewDisplayName.text.toString(),
//            "profileImg", imgUrl
//        )
//            .addOnSuccessListener {
//                Toast.makeText(this,
//                    "Profile changes saved", Toast.LENGTH_SHORT).show()
//
//                finish()
//            }
//            .addOnFailureListener{
//                Toast.makeText(this,
//                    "Error: ${it.message}",
//                    Toast.LENGTH_SHORT).show()
//            }
//    }


    fun removeFriend(userId: String) {
//        // Set the friendship for the current user to "declined"
//        FirebaseFirestore.getInstance().collection("friends").document(
//            friendshipIdList[index]
//        ).update(
//            "status", "declined"
//        ).addOnSuccessListener {
//            Toast.makeText(context, "Friend successfully removed", Toast.LENGTH_SHORT).show()
//        }.addOnFailureListener {
//            Toast.makeText(context, "Failed to remove friend", Toast.LENGTH_SHORT).show()
//        }
//
//        // Set the friend status to "declined' for the removed user as well
//        FirebaseFirestore.getInstance().collection("friends")
//            .whereEqualTo("userId1", currUserId)
//            .whereEqualTo("userId2", userId)
//            .whereEqualTo("status", "accepted")
//
//        friendsList.removeAt(index)
//        friendshipIdList.removeAt(index)
//        notifyItemRemoved(index)
    }

    fun removeFriendByKey(key: String) {
        val index = friendshipIdList.indexOf(key)

        FirebaseFirestore.getInstance().collection("friends").document(key)
            .update(
            "status", "declined"
        ).addOnSuccessListener {
            Toast.makeText(context, "Friend successfully removed", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to remove friend", Toast.LENGTH_SHORT).show()
        }

        // Set the friend status to "declined' for the removed user as well
        FirebaseFirestore.getInstance().collection("friends").document(key)
            .get().addOnSuccessListener { documentSnapshot ->
                val userDocument = documentSnapshot.toObject(Friends::class.java)
                if (userDocument != null) {
                    val otherUserId = userDocument.userId2
                    FirebaseFirestore.getInstance().collection("friends")
                        .whereEqualTo("userId1", otherUserId)
                        .whereEqualTo("userId2", currentUid)
                        .get().addOnSuccessListener { otherDocumentSnapshot ->
                            for (document in otherDocumentSnapshot) {
                                if (document != null) {
                                    val otherUserDocId = document.id
                                        FirebaseFirestore.getInstance().collection("friends").document(otherUserDocId)
                                            .update("status", "declined")
                                }
                            }
                        }.addOnFailureListener {
                            Toast.makeText(context, "Failed to find other user's friendship doc and update to status to declined", Toast.LENGTH_SHORT).show()
                        }
                }
            }

        if (index != -1) {
            friendsList.removeAt(index)
            friendshipIdList.removeAt(index)
            notifyItemRemoved(index)
        }
    }


    inner class ViewHolder(val binding: FriendsRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(friend: User, friendDocId: String) {
            binding.username.text = "@"+ friend.username
            binding.displayName.text = friend.displayName
            Glide.with(context)
                .load(friend.profileImg)
                .into(binding.profilePicture)



            binding.btnViewProfile.setOnClickListener {
                val intentDetails = Intent()
                intentDetails.putExtra("Username", friend.username)
                intentDetails.putExtra("DisplayName", friend.displayName)
                intentDetails.putExtra("ProfileImgUrl", friend.profileImg)

                intentDetails.setClass(
                    context, ProfileDetailsActivity::class.java
                )
                (context as FriendsViewActivity).startActivity(Intent(intentDetails))
            }

            binding.btnRemoveFriend.setOnClickListener {
                removeFriendByKey(friendDocId)
            }

            }
    }
}


