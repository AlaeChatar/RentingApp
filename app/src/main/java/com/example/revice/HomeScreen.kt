package com.example.revice

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.revice.databinding.ActivityHomeScreenBinding
import com.example.revice.models.Device
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class HomeScreen : AppCompatActivity() {

    private lateinit var homeScreenBinding: ActivityHomeScreenBinding
    private lateinit var auth: FirebaseAuth

    override fun onResume() {
        super.onResume()

        // Reload the reservations whenever the screen comes back into view
        loadReservations()
    }

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
        val btnReserve = homeScreenBinding.btnReserve

        ivProfile.setOnClickListener{
            val intent = Intent(this, Profile::class.java)
            startActivity(intent)
        }

        btnReserve.setOnClickListener{
            val intent = Intent(this, ReservationScreen::class.java)
            startActivity(intent)
        }

        loadReservations()
    }

    private fun loadReservations() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        // Reference to the current user's Firestore document
        val currentUserDocRef = db.collection("users").document(userId)

        // Fetch the reservations array from the current user's document
        currentUserDocRef.get()
            .addOnSuccessListener { document ->
                val reservationsList = mutableListOf<Device>()

                // Get the "reservations" field (an array of devices)
                val reservations = document.get("reservations") as? List<Map<String, Any>> ?: return@addOnSuccessListener

                // Loop through each reservation in the array
                reservations.forEach { reservationMap ->
                    val deviceName = reservationMap["deviceName"] as? String ?: return@forEach
                    val devicePrice = (reservationMap["devicePrice"] as? Number)?.toDouble() ?: return@forEach
                    val deviceType = reservationMap["deviceType"] as? String ?: return@forEach
                    val deviceImage = reservationMap["deviceImage"] as? String ?: return@forEach
                    val deviceLocation = reservationMap["deviceLocation"] as? String ?: return@forEach

                    // Add the reservation to the list
                    val device = Device(
                        deviceId = reservationMap["deviceId"] as? String ?: return@forEach,
                        deviceName = deviceName,
                        devicePrice = devicePrice,
                        deviceType = deviceType,
                        deviceImage = deviceImage,
                        deviceLocation = deviceLocation
                    )
                    reservationsList.add(device)
                }

                // Log the size of the reservations list to see if reservations were added
                Log.d("HomeScreen", "Reservations List Size: ${reservationsList.size}")

                // Display the reservations in the UI
                displayReservations(reservationsList)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Failed to load reservations: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun displayReservations(reservationsList: List<Device>) {
        val llAllReservations = homeScreenBinding.llAllReservations
        llAllReservations.removeAllViews() // Clear existing views

        if (reservationsList.isEmpty()) {
            // Show a message if no reservations are available
            val noReservationsMessage = TextView(this).apply {
                text = "No reservations found"
                textSize = 16f
                setPadding(16, 16, 16, 16)
            }
            llAllReservations.addView(noReservationsMessage)
            return
        }

        reservationsList.forEach { device ->
            val reservationLayout = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(16, 16, 16, 16)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { bottomMargin = 8 }
            }

            // ImageView for the device image
            val ivAllReservationsImage = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                    marginEnd = 16
                }
                val decodedString = Base64.decode(device.deviceImage, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                Glide.with(this@HomeScreen)
                    .load(decodedBitmap)
                    .into(this)
            }
            reservationLayout.addView(ivAllReservationsImage)

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

            // Add "Cancel" Button
            val btnCancel = Button(this).apply {
                text = "Cancel"
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setOnClickListener {
                    cancelBooking(device) // Call the method to cancel the booking
                }
            }
            reservationLayout.addView(textLayout)
            reservationLayout.addView(btnCancel)

            llAllReservations.addView(reservationLayout)
        }
    }

    private fun cancelBooking(device: Device) {
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

        // Remove the device from the "reservations" array
        currentUserDocRef.update(
            "reservations",
            com.google.firebase.firestore.FieldValue.arrayRemove(deviceMap)
        )
            .addOnSuccessListener {
                Toast.makeText(this, "Reservation canceled successfully", Toast.LENGTH_SHORT).show()
                loadReservations() // Refresh the reservations list
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to cancel reservation: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}