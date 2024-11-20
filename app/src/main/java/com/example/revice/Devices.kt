package com.example.revice

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.revice.databinding.ActivityDevicesBinding
import com.example.revice.databinding.ActivityProfileBinding

class Devices : AppCompatActivity() {
    private lateinit var devicesBinding: ActivityDevicesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        devicesBinding = ActivityDevicesBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(devicesBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnAdd = devicesBinding.btnAdd
        btnAdd.setOnClickListener{
            var intent = Intent(this, DeviceCreation::class.java)
            startActivity(intent)
        }
    }
}