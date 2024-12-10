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
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.revice.databinding.ActivityReservationScreenBinding
import com.example.revice.models.Device
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.graphics.BitmapFactory
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import androidx.annotation.RequiresApi
import org.osmdroid.util.GeoPoint
import kotlin.math.*
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

class ReservationScreen : AppCompatActivity() {

    private lateinit var reservationBinding: ActivityReservationScreenBinding
    private var selectedLocation: GeoPoint? = null

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private val mapActivityLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.let { intent ->
                val geoPointStr = intent.getStringExtra("geopoint")
                if (!geoPointStr.isNullOrEmpty()) {
                    // Parse the GeoPoint string (format: "lat,lon")
                    val parts = geoPointStr.split(",")
                    if (parts.size == 2) {
                        try {
                            val latitude = parts[0].toDouble()
                            val longitude = parts[1].toDouble()
                            selectedLocation = GeoPoint(latitude, longitude)
                            loadDevices(reservationBinding.spnFilter.selectedItem.toString())
                        } catch (e: NumberFormatException) {
                            Toast.makeText(this, "Invalid location data received", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "No location data received", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "No data received from map", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Location selection cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onResume() {
        super.onResume()
        loadDevices("All")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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
            @RequiresApi(Build.VERSION_CODES.TIRAMISU)
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

        val btnBack = reservationBinding.ivBackReservation
        btnBack.setOnClickListener {
            val intent = Intent(this, HomeScreen::class.java)
            startActivity(intent)
        }

        val btnLocation = reservationBinding.btnLocation
        btnLocation.setOnClickListener {
            val intent = Intent(this, MapScreen::class.java)
            mapActivityLauncher.launch(intent)
        }

        // Load and display devices
        loadDevices("All")
    }

    private fun calculateDistance(point1: GeoPoint, point2: GeoPoint): Double {
        val r = 6371 // Earth's radius in kilometers

        val lat1 = Math.toRadians(point1.latitude)
        val lat2 = Math.toRadians(point2.latitude)
        val dLat = Math.toRadians(point2.latitude - point1.latitude)
        val dLon = Math.toRadians(point2.longitude - point1.longitude)

        val a = sin(dLat/2) * sin(dLat/2) +
                cos(lat1) * cos(lat2) *
                sin(dLon/2) * sin(dLon/2)

        val c = 2 * atan2(sqrt(a), sqrt(1-a))

        return r * c
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun addressToGeoPoint(address: String, callback: (GeoPoint?) -> Unit) {
        val geocoder = Geocoder(this)
        val fullAddress = "$address, Antwerp, Belgium"

        try {
            // Use the pre-Tiramisu synchronous method
            val addresses = geocoder.getFromLocationName(fullAddress, 1)

            if (addresses != null && addresses.isNotEmpty()) {
                val location = addresses[0]
                callback(GeoPoint(location.latitude, location.longitude))
            } else {
                // Try without house number as fallback
                val streetOnly = address.substringBefore(" ")
                val fallbackAddress = "$streetOnly, Antwerp, Belgium"
                val fallbackAddresses = geocoder.getFromLocationName(fallbackAddress, 1)

                if (fallbackAddresses != null && fallbackAddresses.isNotEmpty()) {
                    val fallbackLocation = fallbackAddresses[0]
                    callback(GeoPoint(fallbackLocation.latitude, fallbackLocation.longitude))
                } else {
                    callback(null)
                }
            }
        } catch (e: Exception) {
            Log.e("ReservationScreen", "Error converting address to GeoPoint: ${e.message}")
            callback(null)
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun loadDevices(selectedCategory: String) {
        try {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
                Log.e("ReservationScreen", "No user ID found")
                displayDevices(emptyList(), emptyList())
                return
            }

            val db = FirebaseFirestore.getInstance()
            Log.d("ReservationScreen", "Starting device load - Location: $selectedLocation, Category: $selectedCategory")

            db.collection("users").document(userId).get()
                .addOnSuccessListener { userDocument ->
                    val reservations = userDocument.get("reservations") as? List<Map<String, Any>> ?: emptyList()
                    val reservationIds = reservations.mapNotNull { it["deviceId"] as? String }

                    db.collection("users")
                        .get()
                        .addOnSuccessListener { querySnapshot ->
                            try {
                                val devicesList = mutableListOf<Device>()

                                if (selectedLocation != null) {
                                    val eligibleDevices = querySnapshot.documents
                                        .filter { it.id != userId }
                                        .flatMap { document ->
                                            (document.get("devices") as? List<Map<String, Any>> ?: emptyList())
                                                .filter { deviceMap ->
                                                    val deviceType = deviceMap["deviceType"] as? String
                                                    selectedCategory == "All" || deviceType == selectedCategory
                                                }
                                        }

                                    if (eligibleDevices.isEmpty()) {
                                        runOnUiThread { displayDevices(emptyList(), reservationIds) }
                                        return@addOnSuccessListener
                                    }

                                    var processedCount = 0
                                    eligibleDevices.forEach { deviceMap ->
                                        try {
                                            val deviceLocation = deviceMap["deviceLocation"] as? String ?: return@forEach
                                            addressToGeoPoint(deviceLocation) { deviceGeoPoint ->
                                                processedCount++
                                                if (deviceGeoPoint != null) {
                                                    val distance = calculateDistance(selectedLocation!!, deviceGeoPoint)
                                                    if (distance <= 1.0) {
                                                        val device = mapToDevice(deviceMap, deviceLocation)
                                                        devicesList.add(device)
                                                    }
                                                }

                                                if (processedCount == eligibleDevices.size) {
                                                    runOnUiThread { displayDevices(devicesList, reservationIds) }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            processedCount++
                                            if (processedCount == eligibleDevices.size) {
                                                runOnUiThread { displayDevices(devicesList, reservationIds) }
                                            }
                                        }
                                    }
                                } else {
                                    querySnapshot.documents.forEach { document ->
                                        if (document.id != userId) {
                                            val devices = document.get("devices") as? List<Map<String, Any>> ?: return@forEach

                                            devices.forEach { deviceMap ->
                                                val deviceType = deviceMap["deviceType"] as? String ?: return@forEach
                                                if (selectedCategory == "All" || deviceType == selectedCategory) {
                                                    val device = mapToDevice(deviceMap)
                                                    devicesList.add(device)
                                                }
                                            }
                                        }
                                    }
                                    runOnUiThread { displayDevices(devicesList, reservationIds) }
                                }
                            } catch (e: Exception) {
                                runOnUiThread { displayDevices(emptyList(), emptyList()) }
                            }
                        }
                        .addOnFailureListener { exception ->
                            runOnUiThread {
                                Toast.makeText(this, "Failed to load devices: ${exception.message}", Toast.LENGTH_SHORT).show()
                                displayDevices(emptyList(), emptyList())
                            }
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("ReservationScreen", "Failed to get user reservations: ${e.message}")
                    displayDevices(emptyList(), emptyList())
                }
        } catch (e: Exception) {
            displayDevices(emptyList(), emptyList())
        }
    }

    private fun mapToDevice(deviceMap: Map<String, Any>, location: String? = null): Device {
        return Device(
            deviceId = deviceMap["deviceId"] as? String ?: "",
            deviceName = deviceMap["deviceName"] as? String ?: "",
            devicePrice = (deviceMap["devicePrice"] as? Number)?.toDouble() ?: 0.0,
            deviceType = deviceMap["deviceType"] as? String ?: "",
            deviceImage = deviceMap["deviceImage"] as? String ?: "",
            deviceLocation = location ?: deviceMap["deviceLocation"] as? String ?: ""
        )
    }

    private fun displayDevices(devicesList: List<Device>, reservationIds: List<String>) {
        val llAllDevices = reservationBinding.llAllDevices
        llAllDevices.removeAllViews() // Clear existing views

        if (devicesList.isEmpty()) {
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

            val ivAllDeviceImage = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(100, 100).apply { marginEnd = 16 }
                val decodedString = Base64.decode(device.deviceImage, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(decodedString, 0, decodedString.size)
                Glide.with(this@ReservationScreen)
                    .load(decodedBitmap)
                    .into(this)
            }
            deviceLayout.addView(ivAllDeviceImage)

            val textLayout = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            textLayout.addView(TextView(this).apply { text = device.deviceName; textSize = 18f })
            textLayout.addView(TextView(this).apply { text = device.deviceType })
            textLayout.addView(TextView(this).apply {
                text = "$${device.devicePrice}"
                setTextColor(getColor(android.R.color.holo_green_dark))
            })
            textLayout.addView(TextView(this).apply { text = device.deviceLocation })
            deviceLayout.addView(textLayout)

            val btnBook = Button(this).apply {
                text = if (reservationIds.contains(device.deviceId)) "Booked" else "Book"
                isEnabled = !reservationIds.contains(device.deviceId)
                setOnClickListener {
                    bookDevice(device, this)
                }
            }
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
