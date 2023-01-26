package group.alarm.groupalarm.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import group.alarm.groupalarm.*
import group.alarm.groupalarm.data.Alarm
import group.alarm.groupalarm.databinding.LeaveAlarmDialogBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class LeaveAlarmDialog(val alarmId: String): DialogFragment() {
    lateinit var dialogViewBinding: LeaveAlarmDialogBinding


    val firestore = FirebaseFirestore.getInstance()
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBuilder = AlertDialog.Builder(requireContext())

        dialogViewBinding = LeaveAlarmDialogBinding.inflate(
            requireActivity().layoutInflater
        )
        dialogBuilder.setView(dialogViewBinding.root)

        dialogBuilder.setTitle(getString(R.string.leave_alarm_confirmation))

        dialogBuilder.setPositiveButton(getString(R.string.accept)) {
                dialog, which ->
            onResume()

        }
        dialogBuilder.setNegativeButton(getString(R.string.decline)) {
                dialog, which ->
        }

        return dialogBuilder.create()
    }

    override fun onResume() {
        super.onResume()
        val dialog = dialog as AlertDialog
        val positiveButton = dialog.getButton(Dialog.BUTTON_POSITIVE)

        val currUserEmail = FirebaseAuth.getInstance().currentUser!!.email!!

        val usersRef = firestore.collection("users")
        val alarmsRef = firestore.collection("alarms")
        val chatsRef = firestore.collection("chats")
        val messagesRef = firestore.collection("messages")


        // Remove all alarm related stuff from the user
        /*
        TODO
            there is still quite a bit of use cases I have to check
            For instance, I know one that I have to think of is that once the owner leaves and the users are still in the alarm,
            then I will have to remove the alarmIds from the all the users who have the alarm for the acceptedAlarms and the activeAlarms fields
         */
        positiveButton.setOnClickListener {
            // I also need to remove it from the AlarmAdapter recyclerview list


            // I need to get the alarmId out of the user's activealarms and alarms field in the users collection
            // I need to get the userId out of the chat's users field in the chats collection
            // I need to get the userId out of the alarm's acceptedUsers field in the alarms collection

            // Remove alarmId from the acceptedAlarms and activeAlarms field. If its not in the activeAlarms, it'll just skip it.
            usersRef.document(currUserId).update(
                "acceptedAlarms", FieldValue.arrayRemove(alarmId),
                "activeAlarms", FieldValue.arrayRemove(alarmId),
            )
            // Remove user's id from the alarm's acceptedUsers field
            alarmsRef.document(alarmId).update(
                "acceptedUsers", FieldValue.arrayRemove(currUserId),
            )
            // Remove user's id from the chat's users field and if owner, remove all related documents
            alarmsRef.document(alarmId)
                .get().addOnSuccessListener { alarmDoc ->
                    val alarm = alarmDoc.toObject(Alarm::class.java)
                    if (alarm!= null) {
                        // If the user is the owner of the alarm
                        // Then delete the chat, the messages, the alarm's from the users alarm lists, and then the alarm in that order
                        if (alarm.owner == currUserId) {
                            // Delete the chat document
                            chatsRef.document(alarm.chatId).delete()
                            // Delete the messages documents
                            messagesRef.whereEqualTo("chatId", alarm.chatId)
                                .get()
                                .addOnSuccessListener { documents ->
                                    for (document in documents) {
                                        document.reference.delete()
                                    }
                                }
                            // Remove the alarm's id from the users' invited, accepted, and active lists
                            for(userId in alarm.acceptedUsers!!) {
                                usersRef.document(userId).update("acceptedAlarms", FieldValue.arrayRemove(alarmId))
                                usersRef.document(userId).update("activeAlarms", FieldValue.arrayRemove(alarmId))
                            }
                            for(userId in alarm.invitedUsers!!) {
                                usersRef.document(userId).update("invitedAlarms", FieldValue.arrayRemove(alarmId))
                            }
                            // Finally, delete the alarm
                            alarmsRef.document(alarmDoc.id).delete()
                        } else {
                            // Since user is not owner, there is still at least one user in the alarm
                            chatsRef.document(alarm.chatId).update(
                                "users", FieldValue.arrayRemove(currUserId),
                            )
                        }
                    }
                }
            dialog.dismiss()
            val intentDetails = Intent()
            intentDetails.setClass(
                context as group.alarm.groupalarm.AlarmChatsActivity, DashboardActivity::class.java
            )
            startActivity(Intent(intentDetails))
            (context as group.alarm.groupalarm.AlarmChatsActivity).finish()
        }
    }
}