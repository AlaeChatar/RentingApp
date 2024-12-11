package com.example.revice

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
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
import java.util.*

class MapScreen : AppCompatActivity() {

    private lateinit var mapView: MapView
    private lateinit var btnArea: Button // Button to send GeoPoint
    private lateinit var searchLocation: AutoCompleteTextView
    private val REQUEST_PERMISSIONS_REQUEST_CODE = 1
    private var currentMarker: Marker? = null // Variable to hold the current marker
    private var currentGeoPoint: GeoPoint? = null // Variable to store the current GeoPoint
    private val geocoder by lazy { Geocoder(this) }

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
        searchLocation = findViewById(R.id.searchLocation)

        setupMap()
        setupSearchAutocomplete()
        setupMapEvents()
        setupButtonListener()
    }

    private fun setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK)
        mapView.setMultiTouchControls(true)
        
        // Set initial view to Antwerp
        val startPoint = GeoPoint(51.229853, 4.415380)
        mapView.controller.setZoom(15.0)
        mapView.controller.setCenter(startPoint)
    }

    private fun setupSearchAutocomplete() {
        searchLocation.threshold = 3 // Start suggesting after 3 characters
        searchLocation.setOnItemClickListener { parent, _, position, _ ->
            val address = parent.getItemAtPosition(position) as String
            searchAndMoveToLocation(address)
        }

        searchLocation.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                if (s?.length ?: 0 >= 3) {
                    searchSuggestions(s.toString())
                }
            }
        })
    }

    private fun searchSuggestions(query: String) {
        Thread {
            try {
                val fullQuery = "$query, Antwerp, Belgium"
                val addresses = geocoder.getFromLocationName(fullQuery, 5)
                
                val suggestions = addresses?.map { address ->
                    "${address.thoroughfare ?: ""} ${address.subThoroughfare ?: ""}, ${address.locality ?: ""}"
                }?.filter { it.isNotBlank() }

                runOnUiThread {
                    val adapter = ArrayAdapter(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        suggestions ?: emptyList()
                    )
                    searchLocation.setAdapter(adapter)
                    adapter.notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e("MapScreen", "Error getting suggestions: ${e.message}")
            }
        }.start()
    }

    private fun searchAndMoveToLocation(addressString: String) {
        Thread {
            try {
                val fullAddress = "$addressString, Antwerp, Belgium"
                val addresses = geocoder.getFromLocationName(fullAddress, 1)
                
                addresses?.firstOrNull()?.let { address ->
                    val location = GeoPoint(address.latitude, address.longitude)
                    runOnUiThread {
                        mapView.controller.animateTo(location)
                        addMarkerAtLocation(location, addressString)
                    }
                }
            } catch (e: Exception) {
                Log.e("MapScreen", "Error searching location: ${e.message}")
            }
        }.start()
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

    private fun setupMapEvents() {
        // Set up map events overlay for single tap detection
        val mapEventsOverlay = MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                addMarkerAtLocation(p, "Selected Location")
                currentGeoPoint = p
                return true
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                // Handle long press if needed
                return false
            }
        })
        mapView.overlays.add(mapEventsOverlay)
    }

    private fun setupButtonListener() {
        btnArea.setOnClickListener {
            currentMarker?.let { marker ->
                val intent = Intent()
                intent.putExtra("geopoint", "${marker.position.latitude},${marker.position.longitude}")
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }
}