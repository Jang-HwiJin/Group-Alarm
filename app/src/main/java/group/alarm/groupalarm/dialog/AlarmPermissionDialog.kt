package group.alarm.groupalarm.dialog

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import group.alarm.groupalarm.R
import group.alarm.groupalarm.RegisterFragment
import group.alarm.groupalarm.ScrollingActivity
import group.alarm.groupalarm.ScrollingActivity.Companion.alarmTitles
import group.alarm.groupalarm.data.User
import group.alarm.groupalarm.databinding.AlarmPermissionDialogBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore


class AlarmPermissionDialog(docChange: String) : DialogFragment() {
    val thisDocChange = docChange

    lateinit var dialogViewBinding: AlarmPermissionDialogBinding


    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialogBuilder = AlertDialog.Builder(requireContext())

        dialogViewBinding = AlarmPermissionDialogBinding.inflate(
            requireActivity().layoutInflater
        )
        dialogBuilder.setView(dialogViewBinding.root)

        dialogBuilder.setTitle(getString(R.string.inviteForAlarm, alarmTitles.get(thisDocChange)))

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

        val userEmail = FirebaseAuth.getInstance().currentUser!!.email!!
        val userId = FirebaseAuth.getInstance().currentUser!!.uid!!

        positiveButton.setOnClickListener {
            FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERS)
                .document(userId).get().
                addOnSuccessListener { documentSnapshot ->
                    val user = documentSnapshot.toObject(User::class.java)
                    editUserList(thisDocChange, user!!, true)
                }
            System.out.println(getString(R.string.inviteAccepted))
            dialog.dismiss()
        }
    }

    fun editUserList(key: String, user: User, addingUser: Boolean) {
        val docToUpdate = FirebaseFirestore.getInstance().collection(
            ScrollingActivity.COLLECTION_ALARMS)
            .document(key)
        if (addingUser) {
            docToUpdate.update(
                    "users", FieldValue.arrayUnion(user))
        }
        else {
            docToUpdate.update("users", FieldValue.arrayRemove(user))
        }
    }
}