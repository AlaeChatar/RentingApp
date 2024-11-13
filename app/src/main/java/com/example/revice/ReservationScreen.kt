package com.example.revice

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.revice.databinding.ActivityReservationScreenBinding

class ReservationScreen : AppCompatActivity() {

    private lateinit var reservationBinding : ActivityReservationScreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        reservationBinding = ActivityReservationScreenBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(reservationBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnLocation = reservationBinding.btnLocation

        btnLocation.setOnClickListener{
            val intent = Intent(this, MapScreen::class.java)
            startActivity(intent)
        }
    }
}