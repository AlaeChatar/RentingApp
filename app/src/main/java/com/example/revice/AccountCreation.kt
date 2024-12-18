package com.example.revice

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.revice.databinding.ActivityAccountCreationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AccountCreation : AppCompatActivity() {

    private lateinit var signUpBinding: ActivityAccountCreationBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        signUpBinding = ActivityAccountCreationBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(signUpBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etMail = signUpBinding.etEmailSignUp
        val etPass1 = signUpBinding.etPasswordSignUp1
        val etPass2 = signUpBinding.etPasswordSignUp2
        val btnReg = signUpBinding.btnSignUp
        val btnRetLogin = signUpBinding.btnRetLogin

        btnReg.setOnClickListener{
            registerUser(etMail.text.toString(), etPass1.text.toString(), etPass2.text.toString())
        }

        btnRetLogin.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    private fun registerUser(email: String, password: String, confirmPassword: String) {
        // Check if email or password fields are empty
        if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            Toast.makeText(baseContext, "Email and password fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Check if the passwords match
        if (password != confirmPassword) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        // Proceed with registration if passwords match
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Registration success
                    val user = auth.currentUser
                    Toast.makeText(
                        baseContext, "Registration successful for ${user?.email}",
                        Toast.LENGTH_SHORT
                    ).show()

                    val deviceList = emptyList<Devices>()

                    val userData = hashMapOf(
                        "userId" to user?.uid,
                        "email" to user?.email,
                        "devices" to deviceList
                    )

                    db.collection("users")
                        .document(user!!.uid)
                        .set(userData)
                        .addOnCompleteListener{
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                        }
                } else {
                    // If registration fails, display a message to the user.
                    Toast.makeText(
                        baseContext, "Registration failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

}