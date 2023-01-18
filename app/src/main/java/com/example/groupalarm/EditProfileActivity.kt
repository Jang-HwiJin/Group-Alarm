package com.example.groupalarm

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.ActivityEditProfileBinding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.util.*


class EditProfileActivity : AppCompatActivity() {

    lateinit var binding: ActivityEditProfileBinding

    companion object {
        const val REQUEST_CAMERA_PERMISSION = 1001
    }

    val currUserId = FirebaseAuth.getInstance().currentUser!!.uid


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)

        setContentView(binding.root)



        FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERS)
            .document(currUserId).get().
            addOnSuccessListener { documentSnapshot ->
                val user = documentSnapshot.toObject(User::class.java)
                if (user != null) {
                    binding.editNewDisplayName.setText(user.displayName)
                    if(user.profileImg != "") {
                        Glide.with(this).load(user.profileImg).into(
                            binding.profilePicture)
                    } else {
                        // It will just use the default picture
                    }
                }
            }

//        val getImage= registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
//            binding.profilePicture.setImageURI(uri)
//        }



//        binding.btnChooseImage.setOnClickListener {
//            getImage.launch("image/*")
//        }
        var uploadBitmap: Bitmap? = null

        var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
                result ->
            if (result.resultCode == Activity.RESULT_OK){
                val data: Intent? = result.data
                uploadBitmap = data!!.extras!!.get("data") as Bitmap
                binding.profilePicture.setImageBitmap(uploadBitmap)
            }
        }

        fun openCamera() {
            val intentPhoto = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            resultLauncher.launch(intentPhoto)
        }

        fun requestNeededPermission() {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                        android.Manifest.permission.CAMERA)) {
                    Toast.makeText(this,
                        "I need it for camera", Toast.LENGTH_SHORT).show()
                }

                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    REQUEST_CAMERA_PERMISSION)

            } else {
                // we already have permission
            }
        }

        fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)

            when (requestCode) {
                REQUEST_CAMERA_PERMISSION -> {
                    if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "CAMERA perm granted", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "CAMERA perm NOT granted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

//        fun isFormValid(): Boolean {
//            return when {
//                binding.editNewDisplayName.text.isEmpty() -> {
//                    binding.displayName.error = getString(R.string.fieldCannotBeEmpty)
//                    false
//                }
//                binding.editNewDisplayName.length() !in 2..15 -> {
//                    binding.displayName.error = getString(R.string.errorDisplayNameLength)
//                    false
//                }
//                else ->{
//                    true
//                }
//
//            }
//        }

        fun saveProfileChange(imgUrl: String = "") {
            var currUserId = FirebaseAuth.getInstance().currentUser!!.uid!!
            val docToUpdate = FirebaseFirestore.getInstance().
            collection(RegisterFragment.COLLECTION_USERS)
                .document(currUserId)

            // Checking if new display name and image is proper, then updating in DB
            if(imgUrl.isNotEmpty()) {
                docToUpdate.update(
                    "displayName", binding.editNewDisplayName.text.toString(),
                    "profileImg", imgUrl
                )
                    .addOnSuccessListener {
                        Toast.makeText(this,
                            "Profile changes saved", Toast.LENGTH_SHORT).show()

                        finish()
                    }
                    .addOnFailureListener{
                        Toast.makeText(this,
                            "Error: ${it.message}",
                            Toast.LENGTH_SHORT).show()
                    }
            }
            else {
                docToUpdate.update(
                    "displayName", binding.editNewDisplayName.text.toString()
                )
                    .addOnSuccessListener {
                        Toast.makeText(this,
                            "Profile changes saved", Toast.LENGTH_SHORT).show()

                        finish()
                    }
                    .addOnFailureListener{
                        Toast.makeText(this,
                            "Error: ${it.message}",
                            Toast.LENGTH_SHORT).show()
                    }
            }
        }

        @Throws(Exception::class)
        fun saveProfileChangeWithImage() {
            // convert bitmap to JPEG and put it in a byte array
            val baos = ByteArrayOutputStream()
            uploadBitmap?.compress(Bitmap.CompressFormat.JPEG, 100, baos)
            val imageInBytes = baos.toByteArray()

            // Prepare the empty file in the cloud
            val storageRef = FirebaseStorage.getInstance().getReference()
            // Generate a random file name
            val newImage = URLEncoder.encode(UUID.randomUUID().toString(), "UTF-8") + ".jpg"
            // Create an empty images folder and create the new image file
            val newImagesRef = storageRef.child("images/$newImage")

            // Delete the old image from user profile img url
            FirebaseFirestore.getInstance().collection(RegisterFragment.COLLECTION_USERS)
                .document(currUserId).get().
                addOnSuccessListener { documentSnapshot ->
                    val user = documentSnapshot.toObject(User::class.java)
                    if (user != null && user.profileImg != "") {
                        val userProfileImgUrl = user.profileImg
                        // Retrieving a reference from url
                        val storageReference: StorageReference = FirebaseStorage.getInstance().getReferenceFromUrl(userProfileImgUrl)
                        storageReference.delete().addOnSuccessListener {
                            //File deleted successfully - used for tests
//                            Toast.makeText(this,
//                                "Image was successfully deleted in the backend",
//                                Toast.LENGTH_SHORT).show()
                        }.addOnFailureListener {
                            //Failed to delete - used for test
//                            Toast.makeText(this,
//                                "You failed in deleting the image in the database",
//                                Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            newImagesRef.putBytes(imageInBytes)
                .addOnFailureListener { exception ->
                    Toast.makeText(this@EditProfileActivity, exception.message, Toast.LENGTH_SHORT).show()
                    exception.printStackTrace()
                }.addOnSuccessListener { taskSnapshot ->
                    // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.

                    newImagesRef.downloadUrl.addOnCompleteListener(object: OnCompleteListener<Uri> {
                        override fun onComplete(task: Task<Uri>) {
                            // the public URL of the image is: task.result.toString()

                            saveProfileChange(task.result.toString())
                        }
                    })
                }
        }

        binding.btnTakePicture.setOnClickListener {
            requestNeededPermission()
            openCamera()
        }

        binding.btnSave.setOnClickListener {
            if (uploadBitmap != null) {
                try {
                    saveProfileChangeWithImage()
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
            } else {
                saveProfileChange()
            }
        }

        binding.btnCancel.setOnClickListener {
            finish()
        }


        // When the user closes the app
        val presenceUserRef = Firebase.database.getReference("users").child(currUserId).child("activityStatus")
        presenceUserRef.onDisconnect().setValue(Timestamp(Calendar.getInstance().time))
    }

    override fun onResume() {
        super.onResume()
        val database = Firebase.database
        val usersRef = database.getReference("users").child(currUserId)
        usersRef.child("activityStatus").setValue(true)
    }



}