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
import group.alarm.groupalarm.databinding.FragmentProfileBinding
import java.util.Calendar


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [ProfileFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class ProfileFragment : Fragment() {
    lateinit var binding: FragmentProfileBinding
    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!

    companion object {
        @JvmStatic
        fun newInstance() = ProfileFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(
            inflater, container, false)


        // Displaying the user's profile
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        //Displaying user's email
        val userEmail = FirebaseAuth.getInstance().currentUser!!.email!!
        binding.userEmail.text = userEmail

        // Displaying user's information
        FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERS)
            .document(userId).get().
            addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)
                if (user != null) {
                    binding.displayName.text = user.displayName
                    binding.username.text = "@" + user.username
                    if(user.profileImg != "") {
                        Glide.with(this)
                            .load(user.profileImg)
                            .into(binding.profilePicture)
                    }
                }
            }


        binding.editProfileBtn.setOnClickListener {
            startActivity(Intent(requireActivity(), EditProfileActivity::class.java))
        }


        // When the user closes the app
        val presenceUserRef = Firebase.database.getReference("users").child(currUserId).child("activityStatus")
        presenceUserRef.onDisconnect().setValue(Timestamp(Calendar.getInstance().time))
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        val userId = FirebaseAuth.getInstance().currentUser!!.uid

        FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERS)
            .document(userId).get().
            addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)
                if (user != null) {
                    binding.displayName.text = user.displayName
                    binding.username.text = "@" + user.username
                    if(user.profileImg != "") {
                        Glide.with(this)
                            .load(user.profileImg)
                            .into(binding.profilePicture)
                    }
                }
            }
    }

    fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

}