package com.example.revice

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.revice.databinding.ActivityMainBinding
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {

    private lateinit var mainBinding: ActivityMainBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        auth = FirebaseAuth.getInstance()

        mainBinding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(mainBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val etEmail: EditText = mainBinding.etEmail
        val etPass: EditText = mainBinding.etPassword
        val btnLogin: Button = mainBinding.btnLogin
        val btnReg: Button = mainBinding.btnCreateAccount

        btnLogin.setOnClickListener{
            signIn(etEmail.text.toString(), etPass.text.toString())
        }

        btnReg.setOnClickListener{
            val intent = Intent(this, AccountCreation::class.java)
            startActivity(intent)
        }
    }

    private fun signIn(email: String, password: String) {
        // Check if email or password fields are empty
        if (email.isEmpty() && password.isEmpty()) {
            Toast.makeText(baseContext, "Email and password fields cannot be empty", Toast.LENGTH_SHORT).show()
            return
        }

        // Proceed with authentication if fields are not empty
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success
                    val user = auth.currentUser
                    Toast.makeText(baseContext, "Authentication successful for ${user?.email}",
                        Toast.LENGTH_SHORT).show()
                } else {
                    // If sign in fails, display a message to the user.
                    Toast.makeText(baseContext, "Authentication failed: ${task.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
        val intent = Intent(this, HomeScreen::class.java)
        startActivity(intent)
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, ReservationScreen::class.java)
        startActivity(intent)
    }
}