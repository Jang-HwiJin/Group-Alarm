package com.example.groupalarm.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.groupalarm.ScrollingActivity
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.FriendsRowBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class FriendsAdapter : RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    lateinit var context: Context
    lateinit var currentUid: String
    var friendsList = mutableListOf<User>()
    var friendshipIdList = mutableListOf<String>()


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
        holder.bind(friend)
    }

    fun addFriendsToList(friends: User?, key: String) {
        if (friends != null) {
            friendsList.add(friends)
        }
        friendshipIdList.add(key)
        notifyItemInserted(friendsList.lastIndex)
    }

    fun alreadyHasAlarmDisplayed(key: String): Boolean {
        return friendshipIdList.contains(key)
    }

    private fun removeFriend(index: Int) {
        FirebaseFirestore.getInstance().collection(
            ScrollingActivity.COLLECTION_ALARMS).document(
            friendshipIdList[index]
        ).delete()

        friendsList.removeAt(index)
        friendshipIdList.removeAt(index)
        notifyItemRemoved(index)
    }

    // when somebody else removes an object
    fun removeFriendByKey(key: String) {
        val index = friendshipIdList.indexOf(key)
        if (index != -1) {
            friendsList.removeAt(index)
            friendshipIdList.removeAt(index)
            notifyItemRemoved(index)
        }
    }





    inner class ViewHolder(val binding: FriendsRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(friend: User) {
            binding.username.text = friend.username
            binding.displayName.text = friend.displayName
            Glide.with(context)
                .load(friend.profileImg)
                .into(binding.profilePicture)

//            FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERS).
//            document(friendId).get().addOnSuccessListener { documentSnapshot ->
//                val friend = documentSnapshot.toObject(User::class.java)
//                if (friend != null) {
//                    binding.username.text = friend.username
//                    binding.displayName.text = friend.displayName
//                    Glide.with(context)
//                        .load(friend.profileImg)
//                        .into(binding.profilePicture)
//                }
//                }

            }


    }
}


