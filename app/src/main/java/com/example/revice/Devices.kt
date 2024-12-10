package com.example.revice

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.revice.databinding.ActivityDevicesBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class Devices : AppCompatActivity() {
    private lateinit var devicesBinding: ActivityDevicesBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()


        val btnAdd = devicesBinding.btnAdd
        btnAdd.setOnClickListener {
            val intent = Intent(this, DeviceCreation::class.java)
            startActivity(intent)
        }

        // Fetch devices from Firestore and display them
        fetchDevices()
    }

    private fun fetchDevices() {
        val user = auth.currentUser
        if (user != null) {
            val userDevicesPath = db.collection("users").document(user.uid)

            userDevicesPath.get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val devicesList = document.get("devices") as? List<Map<String, Any>>
                        devicesList?.let { displayDevices(it) }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this, "Failed to load devices: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun displayDevices(devicesList: List<Map<String, Any>>) {
        val linearLayout = devicesBinding.root.findViewById<LinearLayout>(R.id.llAllDevices)
        linearLayout.removeAllViews() // Clear the list before adding new items

        for (deviceMap in devicesList) {
            // Create a new LinearLayout for each device
            val deviceItemView = LinearLayout(this)
            deviceItemView.orientation = LinearLayout.HORIZONTAL
            deviceItemView.setPadding(16, 16, 16, 16)
            deviceItemView.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

            // Create ImageView for device image
            val ivDeviceImage = ImageView(this)
            ivDeviceImage.layoutParams = LinearLayout.LayoutParams(100, 100)
            ivDeviceImage.scaleType = ImageView.ScaleType.CENTER_CROP
            (ivDeviceImage.layoutParams as LinearLayout.LayoutParams).marginEnd = 16

            // Create LinearLayout for text fields (name, type, price)
            val textLayout = LinearLayout(this)
            textLayout.orientation = LinearLayout.VERTICAL
            textLayout.layoutParams = LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
            ) // Set weight to 1 to push the button to the right

            val tvDeviceName = TextView(this)
            tvDeviceName.textSize = 18f
            tvDeviceName.setTypeface(tvDeviceName.typeface, android.graphics.Typeface.BOLD)

            val tvDeviceType = TextView(this)
            tvDeviceType.textSize = 14f

            val tvDevicePrice = TextView(this)
            tvDevicePrice.textSize = 14f
            tvDevicePrice.setTextColor(resources.getColor(android.R.color.holo_green_dark))

            // Set the device details
            tvDeviceName.text = deviceMap["deviceName"] as? String
            tvDeviceType.text = deviceMap["deviceType"] as? String
            tvDevicePrice.text = "$${deviceMap["devicePrice"] as? Double}"

            // Decode the Base64 image and set to ImageView
            val base64Image = deviceMap["deviceImage"] as? String
            base64Image?.let {
                val decodedImage = decodeBase64(it)
                ivDeviceImage.setImageBitmap(decodedImage)
            }

            // Add the text views to the text layout
            textLayout.addView(tvDeviceName)
            textLayout.addView(tvDeviceType)
            textLayout.addView(tvDevicePrice)

            // Create a delete button
            val btnDelete = Button(this)
            btnDelete.text = "Delete"
            btnDelete.layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            btnDelete.setOnClickListener {
                deleteDevice(deviceMap)
            }

            // Add ImageView, TextLayout, and Button to the main device item view
            deviceItemView.addView(ivDeviceImage)
            deviceItemView.addView(textLayout)
            deviceItemView.addView(btnDelete) // Button is added last, aligning it to the right

            // Add the device item view to the parent layout
            linearLayout.addView(deviceItemView)
        }
    }


    private fun decodeBase64(base64String: String): Bitmap {
        val decodedByteArray = Base64.decode(base64String, Base64.DEFAULT)
        return BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size)
    }

    private fun deleteDevice(deviceMap: Map<String, Any>) {
        val user = auth.currentUser
        if (user != null) {
            val userDevicesPath = db.collection("users").document(user.uid)

            userDevicesPath.update("devices", FieldValue.arrayRemove(deviceMap))
                .addOnSuccessListener {
                    Toast.makeText(this, "Device deleted successfully.", Toast.LENGTH_SHORT).show()
                    fetchDevices() // Refresh the device list
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        this,
                        "Failed to delete device: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

}
