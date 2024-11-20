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

        etEmail.setText("${auth.currentUser?.email}")

        btnLogOut.setOnClickListener{
            auth.signOut()
            var intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        btnDevices.setOnClickListener{
            var intent = Intent(this, Devices::class.java)
            startActivity(intent)
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