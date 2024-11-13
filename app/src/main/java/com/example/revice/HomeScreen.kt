package com.example.revice

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.revice.databinding.ActivityHomeScreenBinding
import com.google.firebase.auth.FirebaseAuth

class HomeScreen : AppCompatActivity() {

    private lateinit var homeScreenBinding: ActivityHomeScreenBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()

        homeScreenBinding = ActivityHomeScreenBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(homeScreenBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val ivProfile = homeScreenBinding.ivProfile

        ivProfile.setOnClickListener{
            auth.signOut()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }
}