package com.example.revice

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.revice.databinding.ActivityReservationScreenBinding
import com.example.revice.models.Device
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.bumptech.glide.Glide

class ReservationScreen : AppCompatActivity() {

    private lateinit var reservationBinding: ActivityReservationScreenBinding

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

        val spinner: Spinner = reservationBinding.spnFilter
        // Create an ArrayAdapter using the string array and a default spinner layout.
        ArrayAdapter.createFromResource(
            this,
            R.array.category,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        val btnLocation = reservationBinding.btnLocation
        btnLocation.setOnClickListener {
            val intent = Intent(this, MapScreen::class.java)
            startActivity(intent)
        }

        // Load and display devices
        loadDevices()
    }

    private fun loadDevices() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val devicesList = mutableListOf<Device>()

                // Loop through all documents in the "users" collection
                for (document in querySnapshot) {
                    // Skip the current user's document
                    if (document.id == userId) {
                        continue // Skip the current user's data
                    }

                    // Get the "devices" field from the user document (an array)
                    val devices = document.get("devices") as? List<Map<String, Any>> ?: continue

                    // Loop through each device in the array
                    devices.forEach { deviceMap ->
                        // Extract the fields for each device
                        val deviceName = deviceMap["deviceName"] as? String ?: return@forEach
                        val devicePrice = (deviceMap["devicePrice"] as? Number)?.toDouble() ?: return@forEach
                        val deviceType = deviceMap["deviceType"] as? String ?: return@forEach
                        val deviceImage = deviceMap["deviceImage"] as? String ?: return@forEach
                        val deviceLocation = deviceMap["deviceLocation"] as? String ?: return@forEach

                        // Create a device object from the map
                        val device = Device(
                            deviceId = deviceMap["deviceId"] as? String ?: return@forEach,
                            deviceName = deviceName,
                            devicePrice = devicePrice,
                            deviceType = deviceType,
                            deviceImage = deviceImage,
                            deviceLocation = deviceLocation
                        )

                        // Add the device to the list
                        devicesList.add(device)
                    }
                }

                // Log the size of the devices list to see if devices were added
                Log.d("ReservationScreen", "Devices List Size: ${devicesList.size}")

                // Display the devices in the UI
                displayDevices(devicesList)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load devices: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun displayDevices(devicesList: List<Device>) {
        val llAllDevices = reservationBinding.llAllDevices
        llAllDevices.removeAllViews() // Clear existing views

        // Ensure that devicesList is not empty
        if (devicesList.isEmpty()) {
            Toast.makeText(this, "No devices available", Toast.LENGTH_SHORT).show()
            return
        }

        devicesList.forEach { device ->
            val deviceLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }

            // Create ImageView and load Base64 decoded image using Glide
            val ivAllDeviceImage = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                    marginEnd = 16
                }
                // Glide automatically handles Base64 image loading
                val decodedString = Base64.decode(device.deviceImage, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                Glide.with(this@ReservationScreen)
                    .load(decodedBitmap)
                    .into(this) // Use Glide to load the bitmap efficiently
            }
            deviceLayout.addView(ivAllDeviceImage)

            // Layout for text content
            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Device name
            val tvAllDeviceName = TextView(this).apply {
                text = device.deviceName
                textSize = 18f
            }

            // Device type
            val tvAllDeviceType = TextView(this).apply {
                text = device.deviceType
                setPadding(0, 4, 0, 0)
            }

            // Device price
            val tvAllDevicePrice = TextView(this).apply {
                text = "$${device.devicePrice}"
                setPadding(0, 4, 0, 0)
                setTextColor(getColor(android.R.color.holo_green_dark))
            }

            // Add text views to the text layout
            textLayout.addView(tvAllDeviceName)
            textLayout.addView(tvAllDeviceType)
            textLayout.addView(tvAllDevicePrice)

            // Add the text layout to the device layout
            deviceLayout.addView(textLayout)

            // Finally, add the entire device layout to the list of devices
            llAllDevices.addView(deviceLayout)
        }
    }
}
