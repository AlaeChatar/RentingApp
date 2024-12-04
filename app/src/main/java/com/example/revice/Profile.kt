package com.example.revice

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.revice.databinding.ActivityProfileBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class Profile : AppCompatActivity() {

    private lateinit var profileBinding: ActivityProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        profileBinding = ActivityProfileBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(profileBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnLogOut = profileBinding.btnLogOut
        val btnDevices = profileBinding.btnDevices
        val etEmail = profileBinding.etEmailDB
        val btnChangeEmail = profileBinding.btnChangeEmail
        val etCurPas = profileBinding.etCurrentPass
        val btnChangePass = profileBinding.btnChangePass
        val etChangeLoc = profileBinding.etChangeLoc
        val btnLocation = profileBinding.btnChangeLocation
        val etOldPass = profileBinding.etOldPass
        val etNewPassword = profileBinding.etNewPass1
        val etNewPassword2 = profileBinding.etNewPass2
        val btnBack = profileBinding.ivBackProfile

        // Set up a real-time listener on the user's Firestore document
        db.collection("users").document(auth.currentUser!!.uid)
            .addSnapshotListener { documentSnapshot, e ->
                if (e != null) {
                    Toast.makeText(this, "Failed to listen for updates: ${e.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (documentSnapshot != null && documentSnapshot.exists()) {
                    // Update the email and location fields when the document changes
                    val email = documentSnapshot.getString("email")
                    val location = documentSnapshot.getString("location")

                    email?.let { etEmail.setText(it) }
                    location?.let { etChangeLoc.setText(it) }
                }
            }

        btnLogOut.setOnClickListener{
            auth.signOut()
            var intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnDevices.setOnClickListener{
            var intent = Intent(this, Devices::class.java)
            startActivity(intent)
        }

        btnLocation.setOnClickListener{
            val user = auth.currentUser

            val userData = hashMapOf(
                "location" to etChangeLoc.text.toString()
            )

            // SetOptions.merge() to merge if field doesn't exist yet
            db.collection("users")
                .document(user!!.uid)
                .set(userData, SetOptions.merge())

            Toast.makeText(
                baseContext, "Location has been updated",
                Toast.LENGTH_SHORT
            ).show()
        }

        btnBack.setOnClickListener {
            val intent = Intent(this, HomeScreen::class.java)
            startActivity(intent)
        }

        btnChangePass.setOnClickListener{
            if (etOldPass.text.toString().isBlank() || etNewPassword.text.toString().isBlank() || etNewPassword2.text.toString().isBlank()) {
                Toast.makeText(
                    baseContext, "Please fill in all fields.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (etNewPassword.text.toString() != etNewPassword2.text.toString()) {
                Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val currentUserEmail = auth.currentUser!!.email!!

            // Reauthenticate the user
            val credentials = EmailAuthProvider.getCredential(currentUserEmail, etOldPass.text.toString())
            auth.currentUser?.reauthenticate(credentials)?.addOnCompleteListener { reauthTask ->
                if (reauthTask.isSuccessful) {
                    // Update the password
                    auth.currentUser?.updatePassword(etNewPassword.text.toString())?.addOnCompleteListener { updateTask ->
                        if (updateTask.isSuccessful) {
                            Toast.makeText(
                                baseContext, "Password updated successfully.",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                baseContext, "Password update failed: ${updateTask.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        baseContext, "Reauthentication failed: ${reauthTask.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        btnChangeEmail.setOnClickListener{
            val credentials = EmailAuthProvider.getCredential(auth.currentUser!!.email!!, etCurPas.text.toString())

            auth.currentUser?.reauthenticate(credentials)?.addOnCompleteListener{ t ->
                if (t.isSuccessful){
                    auth.currentUser?.verifyBeforeUpdateEmail(etEmail.text.toString())?.addOnCompleteListener{ task ->
                        if (task.isSuccessful){
                            db.collection("users")
                                .document(auth.currentUser!!.uid)
                                .update("email", etEmail.text.toString())

                            Toast.makeText(
                                baseContext, "Verification mail has been sent!",
                                Toast.LENGTH_SHORT
                            ).show()

                            auth.signOut()
                            var intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                        } else if (task.exception is FirebaseAuthInvalidCredentialsException) {
                            Toast.makeText(
                                baseContext, "Email update failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else if (task.exception is FirebaseAuthUserCollisionException){
                            Toast.makeText(
                                baseContext, "Email update failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            Toast.makeText(
                                baseContext, "Email update failed: ${task.exception?.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    Toast.makeText(
                        baseContext, "Email update failed: ${t.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }
}