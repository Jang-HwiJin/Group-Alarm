package com.example.groupalarm

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.groupalarm.databinding.FragmentLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase


// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [LoginFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class LoginFragment : Fragment() {
    lateinit var binding: FragmentLoginBinding

    companion object {
        @JvmStatic
        fun newInstance() = LoginFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentLoginBinding.inflate(
            inflater, container, false)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!

            // Once user signs, update the lastActive to show that the user signed in
            val userRef = FirebaseFirestore.getInstance().collection("users").document(currUserId)
            val updates = mapOf("activityStatus" to true)
            userRef.update(updates)

            // Letting user become online
            val database = Firebase.database
            val usersRef = database.getReference("users").child(currUserId)
            usersRef.child("activityStatus").setValue(true)

            // User is signed in
            val i = Intent(context, DashboardActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        } else {
            // User is signed out
        }

        return binding.root
    }

    override fun onResume() {
        super.onResume()

        binding.btnLogin.setSafeOnClickListener {
            loginUser()
        }
    }

    fun View.setSafeOnClickListener(onSafeClick: (View) -> Unit) {
        val safeClickListener = SafeClickListener {
            onSafeClick(it)
        }
        setOnClickListener(safeClickListener)
    }

    private fun loginUser() {
        if (isFormValid()) {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(
                binding.etEmail.text.toString(),
                binding.etPassword.text.toString()
            ).addOnSuccessListener {
                Toast.makeText(
                    requireActivity(),
                    getString(R.string.loginSuccess),
                    Toast.LENGTH_LONG
                ).show()
                val currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!
                val userRef = FirebaseFirestore.getInstance().collection("users").document(currUserId)
                val updates = mapOf("activityStatus" to true)
                userRef.update(updates)

                // Letting user become online
                val database = Firebase.database
                val usersRef = database.getReference("users").child(currUserId)
                usersRef.child("activityStatus").setValue(true)

                startActivity(Intent(requireActivity(), DashboardActivity::class.java))
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