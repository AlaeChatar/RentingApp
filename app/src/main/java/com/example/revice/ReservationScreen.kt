package com.example.revice

import android.content.Intent
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.revice.databinding.ActivityReservationScreenBinding
import com.example.revice.models.Device
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.graphics.BitmapFactory
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button

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

        // Initialize spinner and set up the listener
        val spinner: Spinner = reservationBinding.spnFilter
        ArrayAdapter.createFromResource(
            this,
            R.array.filter,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        spinner.setOnItemSelectedListener(object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parentView: AdapterView<*>, selectedItemView: View?, position: Int, id: Long) {
                // Get the selected category
                val selectedCategory = parentView.getItemAtPosition(position).toString()

                // Load devices based on the selected category
                loadDevices(selectedCategory)
            }

            override fun onNothingSelected(parentView: AdapterView<*>) {
                // Optionally, handle when no item is selected
            }
        })

        val btnLocation = reservationBinding.btnLocation
        btnLocation.setOnClickListener {
            val intent = Intent(this, MapScreen::class.java)
            startActivity(intent)
        }

        // Load and display devices
        loadDevices("All")
    }

    private fun loadDevices(selectedCategory: String) {
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

                        // Only add the device if it matches the selected category
                        if (selectedCategory == "All" || deviceType == selectedCategory) {
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

        if (devicesList.isEmpty()) {
            // Show a message if no devices are available
            val noDevicesMessage = TextView(this).apply {
                text = "No devices available"
                textSize = 16f
                setPadding(16, 16, 16, 16)
            }
            llAllDevices.addView(noDevicesMessage)
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

            // ImageView for the device image
            val ivAllDeviceImage = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                    marginEnd = 16
                }
                val decodedString = Base64.decode(device.deviceImage, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                Glide.with(this@ReservationScreen)
                    .load(decodedBitmap)
                    .into(this)
            }
            deviceLayout.addView(ivAllDeviceImage)

            // Text Layout for device details
            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            // Add device details to text layout
            textLayout.addView(TextView(this).apply { text = device.deviceName; textSize = 18f })
            textLayout.addView(TextView(this).apply { text = device.deviceType; setPadding(0, 4, 0, 0) })
            textLayout.addView(TextView(this).apply {
                text = "$${device.devicePrice}"
                setPadding(0, 4, 0, 0)
                setTextColor(getColor(android.R.color.holo_green_dark))
            })

            // Add "Book" Button
            val btnBook = Button(this).apply {
                text = "Book"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    bookDevice(device, this) // Pass button reference to bookDevice
                }
            }
            deviceLayout.addView(textLayout)
            deviceLayout.addView(btnBook)

            llAllDevices.addView(deviceLayout)
        }
    }

    private fun bookDevice(device: Device, btnBook: Button) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Reference to the current user's Firestore document
        val currentUserDocRef = db.collection("users").document(currentUserId)

        // Convert Device object to a Map (Firestore expects a map for array operations)
        val deviceMap = mapOf(
            "deviceId" to device.deviceId,
            "deviceName" to device.deviceName,
            "devicePrice" to device.devicePrice,
            "deviceType" to device.deviceType,
            "deviceImage" to device.deviceImage,
            "deviceLocation" to device.deviceLocation
        )

        // Disable the button immediately to prevent multiple clicks
        btnBook.isEnabled = false
        btnBook.text = "Booked"

        // Add the device to the "reservations" array
        currentUserDocRef.update(
            "reservations",
            com.google.firebase.firestore.FieldValue.arrayUnion(deviceMap)
        )
            .addOnSuccessListener {
                Toast.makeText(this, "Device added to reservations!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                // Re-enable the button if the operation fails
                btnBook.isEnabled = true
                btnBook.text = "Book"
                Toast.makeText(
                    this,
                    "Failed to book device: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
