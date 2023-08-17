package group.alarm.groupalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import group.alarm.groupalarm.data.User
import group.alarm.groupalarm.data.Username
import group.alarm.groupalarm.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"



/**
 * A simple [Fragment] subclass.
 * Use the [RegisterFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class RegisterFragment : Fragment() {
    lateinit var binding: FragmentRegisterBinding

    lateinit var timePicker: TimePicker
    lateinit var pendingIntent: PendingIntent
    lateinit var alarmManager: AlarmManager

    companion object {
        @JvmStatic
        fun newInstance() = RegisterFragment()
        const val COLLECTION_USERS= "users"
        const val COLLECTION_USERNAMES = "usernames"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentRegisterBinding.inflate(
            inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()

        binding.btnRegister.setSafeOnClickListener()  {
            registerUser()
        }
    }

    fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    private fun registerUser() {
        if (isFormValid()) {
            var usernameAlreadyExists = false
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("usernames").document(binding.etUsername.text.toString()).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val document = task.result
                    if (document.exists()) {
                        binding.etUsername.error = getString(R.string.errorNewUsername)

                        usernameAlreadyExists = false
                    } else {
                        usernameAlreadyExists = true

                        if (isFormValid()) {
                            FirebaseAuth.getInstance().createUserWithEmailAndPassword(
                                binding.etEmail.text.toString(),
                                binding.etPassword.text.toString()
                            ).addOnSuccessListener {
                                // add new user to database
                                val usersCollection = FirebaseFirestore.getInstance().collection(COLLECTION_USERS)
                                val newUser = User(
                                    binding.etUsername.text.toString(),
                                    FirebaseAuth.getInstance().currentUser!!.email!!,
                                    binding.displayName.text.toString(),
                                )
                                usersCollection.document(FirebaseAuth.getInstance().currentUser!!.uid!!).set(newUser)

                                // add usernames to the user database
                                val usernamesCollection = FirebaseFirestore.getInstance().collection(COLLECTION_USERNAMES)
                                val newUsername = Username(
                                    FirebaseAuth.getInstance().currentUser!!.uid!!,
                                )
                                usernamesCollection.document(binding.etUsername.text.toString()).set(newUsername)

                                Toast.makeText(
                                    requireActivity(),
                                    getString(R.string.registrationSuccess),
                                    Toast.LENGTH_LONG
                                ).show()
                                val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!

                                val userRef = FirebaseFirestore.getInstance().collection("users").document(currUserId)
                                val updates = mapOf("activityStatus" to true)
                                userRef.update(updates)

                                // Letting user become online
                                val database = Firebase.database
                                val usersRef = database.getReference("users")
                                usersRef.child(currUserId).child("activityStatus").setValue(true)

                                loginUser()
                            }.addOnFailureListener{
                                Toast.makeText(
                                    requireActivity(),
                                    "Error: ${it.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    }
                }
            }
        }

    }


    private fun isFormValid(): Boolean {
        return when {
            binding.displayName.text.isEmpty() -> {
                binding.displayName.error = getString(R.string.fieldCannotBeEmpty)
                false
            }
            binding.displayName.length() !in 2..15 -> {
                binding.displayName.error = getString(R.string.errorDisplayNameLength)
                false
            }
            binding.etUsername.text.isEmpty() -> {
                binding.etUsername.error = getString(R.string.fieldCannotBeEmpty)
                false
            }
            binding.etUsername.length() !in 3..15 -> {
                binding.etUsername.error = getString(R.string.errorUsernameLength)
                false
            }
//            usernameAlreadyExists -> {
//                binding.etUsername.error = getString(R.string.errorNewUsername)
//                false
//            }
            binding.etEmail.text.isEmpty() -> {
                binding.etEmail.error = getString(R.string.fieldCannotBeEmpty)
                false
            }
            binding.etPassword.text.isEmpty() -> {
                binding.etPassword.error = getString(R.string.passwordCannotBeEmpty)
                false
            }
            else ->{
                true
            }

        }
    }

    private fun loginUser() {
        if (isFormValid()) {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(
                binding.etEmail.text.toString(),
                binding.etPassword.text.toString()
            ).addOnSuccessListener {
                startActivity(Intent(requireActivity(), DashActivity::class.java))
            }.addOnFailureListener{
                Toast.makeText(
                    requireActivity(),
                    "Error: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}