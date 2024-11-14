package com.example.revice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker

class MapScreen : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var btnArea: Button // Button to send GeoPoint
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private var currentMarker: Marker? = null // Variable to hold the current marker
    private var currentGeoPoint: GeoPoint? = null // Variable to store the current GeoPoint

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContentView(R.layout.activity_map_screen)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Request permissions if necessary
        requestPermissionsIfNecessary()

        // Initialize osmdroid configuration
        Configuration.getInstance().setUserAgentValue(getPackageName());

        // Initialize the MapView
        mapView = findViewById(R.id.mapView)
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)

        // Set initial view properties
        val startPoint = GeoPoint(51.229853, 4.415380)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(startPoint)

        // Initialize button
        btnArea = findViewById(R.id.btnArea)

        // Set up map events overlay for single tap detection
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                addMarkerAtLocation(p, "New Marker")
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                // Handle long press if needed
                return false
            }
        })
        mapView.overlays.add(mapEventsOverlay)

        // Button to send the location to the next screen
        btnArea.setOnClickListener {
            currentMarker?.let { geoPoint ->
                // Send the GeoPoint to ReservationScreen
                val intent = Intent(this, ReservationScreen::class.java)
                intent.putExtra("geopoint", geoPoint.position.toString())
                startActivity(intent)
            }
        }
    }

    // Function to add a marker at a specified location with a title
    private fun addMarkerAtLocation(location: GeoPoint, title: String) {
        // Remove the previous marker if it exists
        currentMarker?.let { mapView.overlays.remove(it) }

        // Create and add a new marker
        val marker = Marker(mapView).apply {
            position = location
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
        }
        mapView.overlays.add(marker)
        currentMarker = marker // Update the reference to the current marker
        mapView.invalidate() // Refresh the map to display the new marker
    }

    override fun onResume() {
        super.onResume()
        mapView.onResume() // Needed for osmdroid lifecycle
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause() // Needed for osmdroid lifecycle
    }

    private fun requestPermissionsIfNecessary() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toTypedArray(), REQUEST_PERMISSIONS_REQUEST_CODE)
        }
    }
}