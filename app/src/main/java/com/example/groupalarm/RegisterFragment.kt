package com.example.groupalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TimePicker
import android.widget.Toast
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.FragmentRegisterBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

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
        binding.btnRegister.setOnClickListener {
            registerUser()
        }
    }

    private fun registerUser() {
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
                )
                usersCollection.document(FirebaseAuth.getInstance().currentUser!!.email!!).set(newUser)

                Toast.makeText(
                    requireActivity(),
                    getString(R.string.registrationSuccess),
                    Toast.LENGTH_LONG
                ).show()
            }.addOnFailureListener{
                Toast.makeText(
                    requireActivity(),
                    "Error: ${it.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }


    private fun isFormValid(): Boolean {
        return when {
            binding.etUsername.text.isEmpty() -> {
                binding.etEmail.error = getString(R.string.fieldCannotBeEmpty)
                false
            }
            binding.etEmail.text.isEmpty() -> {
                binding.etEmail.error = getString(R.string.fieldCannotBeEmpty)
                false
            }
            binding.etPassword.text.isEmpty() -> {
                binding.etPassword.error = getString(R.string.passwordCannotBeEmpty)
                false
            }
            else -> true
        }
    }
}