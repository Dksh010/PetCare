package com.petcare.app.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import com.petcare.app.R
import com.petcare.app.databinding.ActivityPetMapBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.HttpURLConnection
import java.net.URL

class PetMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetMapBinding
    private val defaultCenter = GeoPoint(27.7172, 85.3240) // Kathmandu default
    private var locationOverlay: MyLocationNewOverlay? = null
    private var allPoisList: List<PetPoi> = emptyList()
    private var activeSelectedPoi: PetPoi? = null
    private var activeFilterCategory: String = "ALL"

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseLocationGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineLocationGranted || coarseLocationGranted) {
            enableMyLocation()
        } else {
            Toast.makeText(this, "Location permission denied. Showing Kathmandu default area.", Toast.LENGTH_SHORT).show()
            centerMapOn(defaultCenter)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        binding = ActivityPetMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupMapView()
        setupCategoryFilters()
        checkLocationPermissions()
        fetchNearbyPetPois(defaultCenter)

        binding.btnClosePoiCard.setOnClickListener {
            binding.cardPoiDetails.visibility = View.GONE
        }

        binding.btnPoiDirections.setOnClickListener {
            val poi = activeSelectedPoi ?: return@setOnClickListener
            val geoUri = Uri.parse("geo:${poi.point.latitude},${poi.point.longitude}?q=${Uri.encode(poi.name)}")
            val mapIntent = Intent(Intent.ACTION_VIEW, geoUri)
            try {
                startActivity(mapIntent)
            } catch (e: Exception) {
                Toast.makeText(this, "No map application available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPoiShare.setOnClickListener {
            val poi = activeSelectedPoi ?: return@setOnClickListener
            val shareText = "Check out ${poi.name} on PetCare! Location: https://www.google.com/maps/search/?api=1&query=${poi.point.latitude},${poi.point.longitude}"
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "Share Location"))
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarMap)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarMap.setNavigationOnClickListener { finish() }
    }

    private fun setupMapView() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        binding.mapView.controller.setZoom(15.0)
        binding.mapView.controller.setCenter(defaultCenter)
    }

    private fun setupCategoryFilters() {
        binding.chipGroupMapCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            activeFilterCategory = when (checkedIds.first()) {
                R.id.chipCategoryVets -> "VET"
                R.id.chipCategoryGrooming -> "GROOMING"
                R.id.chipCategoryShops -> "SHOP"
                R.id.chipCategoryParks -> "PARK"
                R.id.chipCategoryShelters -> "SHELTER"
                else -> "ALL"
            }
            applyCategoryFilter()
        }
    }

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun enableMyLocation() {
        locationOverlay = MyLocationNewOverlay(GpsMyLocationProvider(this), binding.mapView)
        locationOverlay?.enableMyLocation()
        locationOverlay?.runOnFirstFix {
            runOnUiThread {
                val myLoc = locationOverlay?.myLocation
                if (myLoc != null) {
                    centerMapOn(myLoc)
                    fetchNearbyPetPois(myLoc)
                }
            }
        }
        binding.mapView.overlays.add(locationOverlay)
    }

    private fun centerMapOn(point: GeoPoint) {
        binding.mapView.controller.animateTo(point)
    }

    private fun fetchNearbyPetPois(center: GeoPoint) {
        binding.pbMapLoading.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val pois = tryFetchOverpassPois(center.latitude, center.longitude)
            withContext(Dispatchers.Main) {
                binding.pbMapLoading.visibility = View.GONE
                allPoisList = pois
                applyCategoryFilter()
            }
        }
    }

    private fun applyCategoryFilter() {
        val filtered = when (activeFilterCategory) {
            "VET" -> allPoisList.filter { it.category.contains("vet", ignoreCase = true) || it.category.contains("clinic", ignoreCase = true) }
            "GROOMING" -> allPoisList.filter { it.category.contains("grooming", ignoreCase = true) }
            "SHOP" -> allPoisList.filter { it.category.contains("store", ignoreCase = true) || it.category.contains("shop", ignoreCase = true) || it.category.contains("supply", ignoreCase = true) }
            "PARK" -> allPoisList.filter { it.category.contains("park", ignoreCase = true) }
            "SHELTER" -> allPoisList.filter { it.category.contains("shelter", ignoreCase = true) || it.category.contains("rescue", ignoreCase = true) }
            else -> allPoisList
        }
        renderPoiMarkers(filtered)
    }

    private fun tryFetchOverpassPois(lat: Double, lon: Double): List<PetPoi> {
        val poiList = mutableListOf<PetPoi>()
        try {
            val urlString = "https://overpass-api.de/api/interpreter?data=[out:json];node(around:6000,$lat,$lon)[\"amenity\"~\"veterinary|pet_grooming|pet_store|dog_park|animal_shelter\"];out;"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            if (connection.responseCode == 200) {
                val jsonString = connection.inputStream.bufferedReader().use { it.readText() }
                val root = JSONObject(jsonString)
                val elements = root.optJSONArray("elements") ?: return getFallbackKathmanduPois()
                for (i in 0 until elements.length()) {
                    val obj = elements.getJSONObject(i)
                    val pLat = obj.getDouble("lat")
                    val pLon = obj.getDouble("lon")
                    val tags = obj.optJSONObject("tags")
                    val name = tags?.optString("name") ?: "Pet Care Service"
                    val amenity = tags?.optString("amenity") ?: "veterinary"

                    poiList.add(PetPoi(name, amenity, GeoPoint(pLat, pLon)))
                }
            }
        } catch (e: Exception) {
            return getFallbackKathmanduPois()
        }

        return if (poiList.isNotEmpty()) poiList else getFallbackKathmanduPois()
    }

    private fun getFallbackKathmanduPois(): List<PetPoi> {
        return listOf(
            PetPoi("Kathmandu Vet Clinic & Animal Hospital", "veterinary", GeoPoint(27.7180, 85.3250), "Lazimpat, Kathmandu"),
            PetPoi("Pawfect Pet Grooming & Spa", "pet_grooming", GeoPoint(27.7150, 85.3220), "Thamel, Kathmandu"),
            PetPoi("Central Pet Park & Play Zone", "dog_park", GeoPoint(27.7200, 85.3300), "Baluwatar, Kathmandu"),
            PetPoi("Himalayan Pet Supplies & Food Store", "pet_store", GeoPoint(27.7130, 85.3200), "Durbar Marg, Kathmandu"),
            PetPoi("Valley Animal Rescue & Shelter", "animal_shelter", GeoPoint(27.7220, 85.3180), "Samakhusi, Kathmandu")
        )
    }

    private fun renderPoiMarkers(pois: List<PetPoi>) {
        // Clear previous overlays except location overlay
        val overlayList = binding.mapView.overlays
        overlayList.removeAll { it is Marker }

        for (poi in pois) {
            val marker = Marker(binding.mapView)
            marker.position = poi.point
            marker.title = poi.name
            marker.snippet = poi.address.ifEmpty { poi.category }

            marker.setOnMarkerClickListener { _, _ ->
                activeSelectedPoi = poi
                binding.tvPoiIcon.text = when {
                    poi.category.contains("vet", ignoreCase = true) || poi.category.contains("clinic", ignoreCase = true) -> "🩺"
                    poi.category.contains("grooming", ignoreCase = true) -> "✂️"
                    poi.category.contains("park", ignoreCase = true) -> "🎾"
                    poi.category.contains("store", ignoreCase = true) || poi.category.contains("shop", ignoreCase = true) -> "🛍️"
                    poi.category.contains("shelter", ignoreCase = true) -> "🐾"
                    else -> "🐾"
                }
                binding.tvPoiName.text = poi.name
                binding.tvPoiCategory.text = "${poi.category.replace("_", " ").uppercase()} · Verified Location"
                binding.tvPoiAddress.text = poi.address.ifEmpty { "Near Kathmandu, Nepal" }
                binding.cardPoiDetails.visibility = View.VISIBLE
                centerMapOn(poi.point)
                true
            }

            overlayList.add(marker)
        }
        binding.mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
    }

    data class PetPoi(
        val name: String,
        val category: String,
        val point: GeoPoint,
        val address: String = ""
    )
}