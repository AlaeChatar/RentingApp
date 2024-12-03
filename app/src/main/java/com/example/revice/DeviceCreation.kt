package com.example.revice

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.revice.databinding.ActivityDeviceCreationBinding
import com.example.revice.models.Device
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class DeviceCreation : AppCompatActivity() {
    private lateinit var creationBinding: ActivityDeviceCreationBinding
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

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
            // Specify the layout to use when the list of choices appears.
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner.
            spinner.adapter = adapter
        }

        val btnCreate = creationBinding.btnCreate
        btnCreate.setOnClickListener{
            var intent = Intent(this, Devices::class.java)
            startActivity(intent)
        }
    }

    private fun createDeviceAndStore() {
        val deviceName = creationBinding.etDeviceName.text.toString()
        val devicePrice = creationBinding.etPrice.text.toString().toDoubleOrNull() ?: 0.0
        val deviceType = creationBinding.spnCategory.selectedItem.toString()

        if (deviceName.isBlank() || deviceType.isBlank()) {
            // Ensure required fields are not empty
            Toast.makeText(
                baseContext, "Device name and type are required.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Create a Device object
        val device = Device(
            deviceId = System.currentTimeMillis().toString(), // Unique ID
            deviceName = deviceName,
            devicePrice = devicePrice,
            deviceType = deviceType
        )

        // Add the device to Firestore under the user's devices array
        val user = auth.currentUser
        if (user != null) {
            val userDevicesPath = db.collection("users").document(user.uid)

            userDevicesPath.update("devices", FieldValue.arrayUnion(device))
                .addOnSuccessListener {
                    Toast.makeText(
                        baseContext, "Device added successfully.",
                        Toast.LENGTH_SHORT
                    ).show()
                    // Navigate to Devices activity
                    val intent = Intent(this, Devices::class.java)
                    startActivity(intent)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(
                        baseContext, "Failed to add device: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(
                baseContext, "User not authenticated.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun resizeImage(uri: Uri): Bitmap {
        val inputStream = contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        // Define the target size
        val targetWidth = 500
        val targetHeight = (originalBitmap.height * targetWidth) / originalBitmap.width

        return Bitmap.createScaledBitmap(originalBitmap, targetWidth, targetHeight, true)
    }
}