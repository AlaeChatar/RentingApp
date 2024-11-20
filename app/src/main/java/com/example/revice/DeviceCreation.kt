package com.example.revice

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.MediaStore
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.revice.databinding.ActivityDeviceCreationBinding

class DeviceCreation : AppCompatActivity() {
    private lateinit var creationBinding: ActivityDeviceCreationBinding
    private lateinit var pickImageLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
                ivPicture.setImageURI(uri) // Display the selected image
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

    private fun requestPermissionsIfNecessary() {
        val permissions = arrayOf(
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
        )
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), 1)
        }
    }
}