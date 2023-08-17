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
import group.alarm.groupalarm.databinding.FriendAlarmInviteRowBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class FriendAlarmInviteAdapter : RecyclerView.Adapter<FriendAlarmInviteAdapter.ViewHolder> {

    var context: Context
    var currentUid: String
    var currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!
    private var userList = mutableListOf<User>()
    private var userIdList = mutableListOf<String>()
    private var invitedUsersList = mutableListOf<String>()

    val firestore = FirebaseFirestore.getInstance()

    constructor(context: Context, uid: String) : super() {
        this.context = context
        this.currentUid = uid
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendAlarmInviteAdapter.ViewHolder {
        val binding = FriendAlarmInviteRowBinding
            .inflate(LayoutInflater.from(parent.context), parent, false)

        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return userList.size
    }

    override fun onBindViewHolder(holder: FriendAlarmInviteAdapter.ViewHolder, position: Int) {
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

    inner class ViewHolder(val binding: FriendAlarmInviteRowBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User, userDocId: String) {
            val userUsername = user.username

            binding.username.text = "@" + userUsername
            binding.displayName.text = user.displayName
            Glide.with(context)
                .load(user.profileImg)
                .into(binding.profilePicture)

            if(invitedUsersList.contains(user.username)) {
                binding.btnSendAlarmInvite.text = "Added"
                binding.btnSendAlarmInvite.setTextColor(
                    ContextCompat.getColor(context, R.color.white)
                )
                binding.btnSendAlarmInvite.setBackgroundColor(Color.DKGRAY)
            }
            else {
                binding.btnSendAlarmInvite.text = "Invite"
                binding.btnSendAlarmInvite.setBackgroundColor(
                    ContextCompat.getColor(context, R.color.btn_color_std_bg)
                )
                binding.btnSendAlarmInvite.setTextColor(
                    ContextCompat.getColor(context, R.color.white)
                )
            }


                // Checking to see if user is already added to the list, if not, add
            binding.btnSendAlarmInvite.setOnClickListener {
                if(invitedUsersList.contains(user.username)) {
                    removeUsersFromInviteList(user.username)

                    binding.btnSendAlarmInvite.text = " Invite"
                    binding.btnSendAlarmInvite.setBackgroundColor(
                        ContextCompat.getColor(context, R.color.btn_color_std_bg)
                    )
                    binding.btnSendAlarmInvite.setTextColor(
                        ContextCompat.getColor(context, R.color.white)
                    )
                }
                else {
                    addUsersToInviteList(user.username)

                    binding.btnSendAlarmInvite.text = "Already Added"
                    binding.btnSendAlarmInvite.setTextColor(
                        ContextCompat.getColor(context, R.color.white)
                    )
                    binding.btnSendAlarmInvite.setBackgroundColor(Color.DKGRAY)
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
//                (context as CreateAlarmActivity).startActivity(Intent(intentDetails))
//            }

            binding.cardView.setOnClickListener {
                val intentDetails = Intent()
                intentDetails.putExtra("Username", user.username)
                intentDetails.putExtra("DisplayName", user.displayName)
                intentDetails.putExtra("ProfileImgUrl", user.profileImg)

                intentDetails.setClass(
                    context, ProfileDetailsActivity::class.java
                )
                (context as CreateAlarmActivity).startActivity(Intent(intentDetails))
            }

        }
    }

    private fun addUsersToInviteList(username: String) {
        invitedUsersList.add(username)
    }

    private fun removeUsersFromInviteList(username: String) {
        invitedUsersList.remove(username)
    }

    fun getAddedUsersList(): MutableList<String> {
        return invitedUsersList
    }

    fun removeFriendByKey(key: String) {
        val index = userIdList.indexOf(key)

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
            userList.removeAt(index)
            userIdList.removeAt(index)
            notifyItemRemoved(index)
        }
    }


}