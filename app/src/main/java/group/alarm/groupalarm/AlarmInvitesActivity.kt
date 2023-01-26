package group.alarm.groupalarm

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import group.alarm.groupalarm.adapter.AlarmInviteAdapter
import group.alarm.groupalarm.data.Alarm
import group.alarm.groupalarm.data.User
import group.alarm.groupalarm.databinding.ActivityAlarmInvitesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*

class AlarmInvitesActivity : AppCompatActivity() {
    
    lateinit var binding: ActivityAlarmInvitesBinding

    var firestore = FirebaseFirestore.getInstance()
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid


    private lateinit var adapter: AlarmInviteAdapter
    lateinit var alarmsDb: CollectionReference
    lateinit var listener: ListenerRegistration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAlarmInvitesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = AlarmInviteAdapter(this,
            FirebaseAuth.getInstance().currentUser!!.uid
        )

        binding.recyclerAlarmInvites.adapter = adapter

        getPendingAlarmInvites()
    }

    private fun getPendingAlarmInvites() {
        val alarmsRef = firestore.collection("alarms")
        val usersRef = firestore.collection("users")
        alarmsDb = firestore.collection("friends")


        val eventListener = object : EventListener<QuerySnapshot> {
            override fun onEvent(querySnapshot: QuerySnapshot?,
                                 e: FirebaseFirestoreException?) {
                if (e != null) {
                    Toast.makeText(
                        this@AlarmInvitesActivity, "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    return
                }

                // Find the userId in the invitedUsers list in the alarms collection and add alarm invite to recycler
                val query = alarmsRef.whereArrayContains("invitedUsers", currUserId)
                query.get().addOnSuccessListener { documents ->
                    for (document in documents) {
                        val alarm = document.toObject(Alarm::class.java)
                        if(alarm != null && !adapter.alreadyHasAlarmDisplayed(document.id)) {
                            adapter.addAlarmToList(alarm, document.id)
                            adapter.notifyDataSetChanged()
                        }
                    }
                }.addOnFailureListener {
                    Toast.makeText(this@AlarmInvitesActivity,
                        "Error while retrieving pending alarm invites", Toast.LENGTH_SHORT).show()
                }

                for (docChange in querySnapshot?.getDocumentChanges()!!) {
//                    // If new alarm is added
                    FirebaseFirestore.getInstance().collection("alarm").document().get().addOnSuccessListener { documentSnapshot ->
                        val alarm = documentSnapshot.toObject(Alarm::class.java)
                        if (alarm != null ) {
                            if (alarm.invitedUsers?.contains(currUserId) == true) {
                                if (docChange.type == DocumentChange.Type.ADDED) {
//                                        adapter.addRequestsToList(request, docChange.document.id)
                                    adapter.notifyDataSetChanged()
//                                    /*Todo
//                                       this probably needs to be implemented furthermore once I add a remove friend functionality */
                                } else if (docChange.type == DocumentChange.Type.REMOVED) {
                                    adapter.removeAlarmByKey(docChange.document.id)
                                    adapter.notifyDataSetChanged()
                                } else if (docChange.type == DocumentChange.Type.MODIFIED) {
                                    val request = docChange.document.toObject(User::class.java)
                                    if (!adapter.alreadyHasAlarmDisplayed(docChange.document.id)) {
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
        listener = alarmsDb.addSnapshotListener(eventListener)

    }
    
    
}