package com.example.groupalarm.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.groupalarm.EditProfileActivity
import com.example.groupalarm.FriendsViewActivity
import com.example.groupalarm.ProfileDetailsActivity
import com.example.groupalarm.ScrollingActivity
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.FriendSearchRowBinding
import com.example.groupalarm.databinding.FriendsRowBinding
import com.google.firebase.firestore.FirebaseFirestore

class FriendsAdapter : RecyclerView.Adapter<FriendsAdapter.ViewHolder> {

    var context: Context
    var currentUid: String
    private var friendsList = mutableListOf<User>()
    private var friendshipIdList = mutableListOf<String>()


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

    fun addFriendsToList(friends: User, key: String) {
        if (friends != null) {
            friendsList.add(friends)
        }
        friendshipIdList.add(key)
        notifyItemInserted(friendsList.lastIndex)
    }

    fun alreadyHasFriendDisplayed(key: String): Boolean {
        return friendshipIdList.contains(key)
    }

    fun removeFriend(index: Int) {
        FirebaseFirestore.getInstance().collection("friends").document(
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

            }

            }
    }
}


