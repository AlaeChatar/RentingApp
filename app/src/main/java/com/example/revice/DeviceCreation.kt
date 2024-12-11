package com.example.revice

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.revice.databinding.ActivityDeviceCreationBinding
import com.example.revice.models.Device
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.ByteArrayOutputStream

class DeviceCreation : AppCompatActivity() {
    private lateinit var creationBinding: ActivityDeviceCreationBinding
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var base64Image: String? = null // Holds the encoded image
    private lateinit var location: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        creationBinding = ActivityDeviceCreationBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(creationBinding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val ivPicture = creationBinding.ivPicture

        // Register the Activity Result Launcher
        pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data: Intent? = result.data
                val uri = data?.data
                uri?.let {
                    val resizedBitmap = resizeImage(uri)
                    ivPicture.setImageBitmap(resizedBitmap) // Load the resized bitmap into the ImageView
                    base64Image = encodeImageToBase64(resizedBitmap) // Encode the image to Base64
                }
            }
        }

        // Button to open gallery
        ivPicture.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            pickImageLauncher.launch(intent)
        }

        val spinner: Spinner = creationBinding.spnCategory
        // Create an ArrayAdapter using the string array and a default spinner layout.
        ArrayAdapter.createFromResource(
            this,
            R.array.category,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        val btnBack = creationBinding.ivBackDeviceCrea
        btnBack.setOnClickListener {
            val intent = Intent(this, Devices::class.java)
            startActivity(intent)
        }

        val btnCreate = creationBinding.btnCreate
        btnCreate.setOnClickListener {
            createDeviceAndStore()
        }
    }

    private fun createDeviceAndStore() {
        val deviceName = creationBinding.etDeviceName.text.toString()
        val devicePrice = creationBinding.etPrice.text.toString().toDoubleOrNull() ?: 0.0
        val deviceType = creationBinding.spnCategory.selectedItem.toString()

        if (deviceName.isBlank() || deviceType.isBlank()) {
            Toast.makeText(baseContext, "Device name and type are required.", Toast.LENGTH_SHORT).show()
            return
        }

        if (base64Image == null) {
            Toast.makeText(this, "Please select an image.", Toast.LENGTH_SHORT).show()
            return
        }

        val user = auth.currentUser
        if (user == null) {
            Toast.makeText(this, "User not authenticated.", Toast.LENGTH_SHORT).show()
            return
        }

        // Fetch the user's location from Firestore
        val userDocRef = db.collection("users").document(user.uid)
        userDocRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    location = document.getString("location") ?: "Unknown Location"
                }

                // Create a Device object
                val device = Device(
                    deviceId = System.currentTimeMillis().toString(),
                    deviceName = deviceName,
                    devicePrice = devicePrice,
                    deviceType = deviceType,
                    deviceImage = base64Image!!, // Add the encoded image
                    deviceLocation = location
                )

                val userDevicesPath = db.collection("users").document(user.uid)

                // Check if the user document exists
                userDevicesPath.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            // Document exists; update the devices array
                            userDevicesPath.update("devices", FieldValue.arrayUnion(device.toMap()))
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Device added successfully.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navigateToDevices()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Failed to add device: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        } else {
                            // Document does not exist; create it with the devices array
                            val deviceData = mapOf("devices" to listOf(device.toMap()))
                            userDevicesPath.set(deviceData)
                                .addOnSuccessListener {
                                    Toast.makeText(
                                        this,
                                        "Device created and added successfully.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    navigateToDevices()
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(
                                        this,
                                        "Failed to create document: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Failed to check document existence: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
    }

    private fun resizeImage(uri: Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        val targetWidth = 500
        val targetHeight = (originalBitmap.height * targetWidth) / originalBitmap.width
        return Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
    }

    private fun encodeImageToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        val byteArray = byteArrayOutputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun navigateToDevices() {
        val intent = Intent(this, Devices::class.java)
        startActivity(intent)
    }

    private fun Device.toMap(): Map<String, Any> {
        return mapOf(
            "deviceId" to deviceId,
            "deviceName" to deviceName,
            "devicePrice" to devicePrice,
            "deviceType" to deviceType,
            "deviceImage" to deviceImage, // Include the encoded image
            "deviceLocation" to deviceLocation
        )
    }
}
