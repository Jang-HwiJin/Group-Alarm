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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import group.alarm.groupalarm.data.User
import group.alarm.groupalarm.databinding.FragmentSettingBinding
import java.util.Calendar


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [SettingFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class SettingFragment : Fragment() {
    lateinit var binding: FragmentSettingBinding
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!


    companion object {
        @JvmStatic
        fun newInstance() = SettingFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentSettingBinding.inflate(
            inflater, container, false)

        binding.signoutBtn.setOnClickListener{
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(getActivity() as DashActivity, MainActivity::class.java)
            val userRef = FirebaseFirestore.getInstance().collection("users").document(currUserId)
            val update = mapOf("activityStatus" to false)
            userRef.update(update)

            // Turning user off
            val presenceUserRef = Firebase.database.getReference("users").child(currUserId).child("activityStatus")
            presenceUserRef.setValue(Timestamp(Calendar.getInstance().time))

            startActivity(intent)
            activity?.finish()
        }
        // When the user closes the app
        val presenceUserRef = Firebase.database.getReference("users").child(currUserId).child("activityStatus")
        presenceUserRef.onDisconnect().setValue(Timestamp(Calendar.getInstance().time))
        return binding.root
    }

    override fun onResume() {
        super.onResume()

    }

    fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

}