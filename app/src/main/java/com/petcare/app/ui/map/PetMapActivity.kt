package com.petcare.app.ui.map

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.petcare.app.R
import com.petcare.app.data.model.LocationEntity
import com.petcare.app.data.repository.PetRepository
import com.petcare.app.databinding.ActivityPetMapBinding
import com.petcare.app.databinding.DialogTagPlaceBinding
import com.petcare.app.util.Constants
import com.petcare.app.util.PlaceType
import com.petcare.app.util.SessionManager
import com.petcare.app.util.TimeUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

/**
 * Geotagging: shows nearby vets, groomers, dog parks, pet stores and shelters from OpenStreetMap,
 * plus the user's own tagged places. Long-press the map (or tap "Save") to tag a place; appointments
 * and activities linked to a tagged place are listed on its card.
 */
class PetMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetMapBinding
    private lateinit var repository: PetRepository
    private var userId: Long = -1L

    private val defaultCenter = GeoPoint(27.7172, 85.3240) // Kathmandu
    private var locationOverlay: MyLocationNewOverlay? = null
    private var nearbyPlaces: List<MapPlace> = emptyList()
    private var savedPlaces: List<LocationEntity> = emptyList()
    private var activePlace: MapPlace? = null
    private var activeFilter: String = FILTER_ALL
    private var fetchJob: Job? = null

    /** A saved place to open on start (from "View on map" on an appointment). */
    private var focusLocationId: Long = -1L

    /** A marker on the map: either a nearby OpenStreetMap place or one of the user's saved places. */
    data class MapPlace(
        val name: String,
        val type: PlaceType,
        val point: GeoPoint,
        val address: String = "",
        val saved: LocationEntity? = null
    )

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) {
            enableMyLocation()
        } else {
            Toast.makeText(this, "Location permission denied. Showing Kathmandu area.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = packageName
        binding = ActivityPetMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        repository = PetRepository.from(this)
        userId = SessionManager(this).getUserId()
        focusLocationId = intent.getLongExtra(Constants.EXTRA_LOCATION_ID, -1L)

        setupToolbar()
        setupMapView()
        setupCategoryFilters()
        setupCardButtons()
        observeSavedPlaces()
        checkLocationPermissions()
        fetchNearbyPlaces(defaultCenter)
    }

    private fun setupToolbar() {
        binding.toolbarMap.setNavigationOnClickListener { finish() }
    }

    private fun setupMapView() {
        binding.mapView.setTileSource(TileSourceFactory.MAPNIK)
        binding.mapView.setMultiTouchControls(true)
        // Use our own styled zoom buttons instead of osmdroid's built-in ones.
        binding.mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        binding.mapView.controller.setZoom(15.0)
        binding.mapView.controller.setCenter(defaultCenter)

        binding.btnZoomIn.setOnClickListener { binding.mapView.controller.zoomIn() }
        binding.btnZoomOut.setOnClickListener { binding.mapView.controller.zoomOut() }
        binding.btnMyLocation.setOnClickListener {
            val myLocation = locationOverlay?.myLocation
            if (myLocation != null) {
                binding.mapView.controller.animateTo(myLocation, 16.0, 600L)
            } else {
                Toast.makeText(this, "Waiting for your location…", Toast.LENGTH_SHORT).show()
                checkLocationPermissions()
            }
        }

        // Tap empty map to close the card; long-press to tag a new place.
        val events = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                binding.cardPoiDetails.visibility = View.GONE
                return false
            }

            override fun longPressHelper(p: GeoPoint): Boolean {
                showTagPlaceDialog(p, suggestedName = "", suggestedType = PlaceType.VET, address = "")
                return true
            }
        }
        binding.mapView.overlays.add(0, MapEventsOverlay(events))
    }

    private fun setupCategoryFilters() {
        binding.chipGroupMapCategories.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            activeFilter = when (checkedIds.first()) {
                R.id.chipCategorySaved -> FILTER_SAVED
                R.id.chipCategoryVets -> PlaceType.VET.code
                R.id.chipCategoryGrooming -> PlaceType.GROOMING.code
                R.id.chipCategoryShops -> PlaceType.SHOP.code
                R.id.chipCategoryParks -> PlaceType.PARK.code
                R.id.chipCategoryShelters -> PlaceType.SHELTER.code
                else -> FILTER_ALL
            }
            renderMarkers()
            if (activeFilter == FILTER_SAVED) zoomToSavedPlaces()
        }
    }

    private fun zoomToSavedPlaces() {
        val points = savedPlaces.map { GeoPoint(it.latitude, it.longitude) }
        when {
            points.isEmpty() -> Toast.makeText(this, "No saved places yet – long-press the map to tag one", Toast.LENGTH_SHORT).show()
            points.size == 1 -> binding.mapView.controller.animateTo(points.first(), 17.0, 600L)
            else -> binding.mapView.zoomToBoundingBox(BoundingBox.fromGeoPoints(points).increaseByScale(1.4f), true, 80)
        }
    }

    private fun setupCardButtons() {
        binding.btnClosePoiCard.setOnClickListener { binding.cardPoiDetails.visibility = View.GONE }

        binding.btnPoiDirections.setOnClickListener {
            val place = activePlace ?: return@setOnClickListener
            val geoUri = Uri.parse("geo:${place.point.latitude},${place.point.longitude}?q=${place.point.latitude},${place.point.longitude}(${Uri.encode(place.name)})")
            try {
                startActivity(Intent(Intent.ACTION_VIEW, geoUri))
            } catch (e: ActivityNotFoundException) {
                Toast.makeText(this, "No map application available", Toast.LENGTH_SHORT).show()
            }
        }

        binding.btnPoiShare.setOnClickListener {
            val place = activePlace ?: return@setOnClickListener
            val shareText = "${place.name} (${place.type.label}) – https://www.openstreetmap.org/?mlat=${place.point.latitude}&mlon=${place.point.longitude}#map=17/${place.point.latitude}/${place.point.longitude}"
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, shareText)
                type = "text/plain"
            }
            startActivity(Intent.createChooser(sendIntent, "Share location"))
        }

        binding.btnPoiSaveToggle.setOnClickListener {
            val place = activePlace ?: return@setOnClickListener
            val saved = place.saved
            if (saved == null) {
                showTagPlaceDialog(place.point, place.name, place.type, place.address)
            } else {
                confirmRemovePlace(saved)
            }
        }
    }

    private fun observeSavedPlaces() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                repository.getLocationsForUser(userId).collect { places ->
                    savedPlaces = places
                    renderMarkers()

                    // Keep an open saved-place card in sync, or open the requested place once.
                    activePlace?.saved?.let { current ->
                        val updated = places.firstOrNull { it.id == current.id }
                        if (updated == null) binding.cardPoiDetails.visibility = View.GONE else showPlaceCard(toMapPlace(updated))
                    }
                    if (focusLocationId != -1L) {
                        places.firstOrNull { it.id == focusLocationId }?.let {
                            val place = toMapPlace(it)
                            binding.mapView.controller.setZoom(17.0)
                            binding.mapView.controller.setCenter(place.point)
                            showPlaceCard(place)
                        }
                        focusLocationId = -1L
                    }
                }
            }
        }
    }

    private fun toMapPlace(loc: LocationEntity) = MapPlace(
        name = loc.name,
        type = PlaceType.fromCode(loc.locationType),
        point = GeoPoint(loc.latitude, loc.longitude),
        address = loc.address,
        saved = loc
    )

    private fun checkLocationPermissions() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        ) {
            enableMyLocation()
        } else {
            requestPermissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun enableMyLocation() {
        val overlay = MyLocationNewOverlay(GpsMyLocationProvider(this), binding.mapView)
        overlay.enableMyLocation()
        overlay.runOnFirstFix {
            runOnUiThread {
                val myLoc = overlay.myLocation ?: return@runOnUiThread
                // Don't jump away from a place the user asked to see.
                if (activePlace?.saved == null) binding.mapView.controller.animateTo(myLoc)
                fetchNearbyPlaces(myLoc)
            }
        }
        locationOverlay = overlay
        binding.mapView.overlays.add(overlay)
    }

    private fun fetchNearbyPlaces(center: GeoPoint) {
        binding.pbMapLoading.visibility = View.VISIBLE
        fetchJob?.cancel() // a newer position replaces an older, still-running search
        fetchJob = lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { queryOverpass(center.latitude, center.longitude) }
            binding.pbMapLoading.visibility = View.GONE
            if (result != null) {
                nearbyPlaces = result
            } else if (nearbyPlaces.isEmpty()) {
                nearbyPlaces = sampleKathmanduPlaces()
                Toast.makeText(this@PetMapActivity, "Couldn't reach OpenStreetMap – showing sample places", Toast.LENGTH_SHORT).show()
            }
            renderMarkers()
        }
    }

    /**
     * Queries OpenStreetMap (Overpass API) for pet places within 5 km, trying each public
     * server in turn because they are often overloaded. Returns null if all of them fail.
     */
    private fun queryOverpass(lat: Double, lon: Double): List<MapPlace>? {
        val around = "around:5000,$lat,$lon"
        val query = "[out:json][timeout:20];(" +
            "nwr($around)[\"amenity\"~\"^(veterinary|animal_shelter)$\"];" +
            "nwr($around)[\"shop\"~\"^(pet|pet_grooming)$\"];" +
            "nwr($around)[\"leisure\"=\"dog_park\"];" +
            ");out center 80;"
        val encoded = URLEncoder.encode(query, "UTF-8")
        return OVERPASS_SERVERS.firstNotNullOfOrNull { server -> queryOverpassServer("$server?data=$encoded") }
    }

    private fun queryOverpassServer(urlString: String): List<MapPlace>? {
        return try {
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 8000
                readTimeout = 25000
                // Overpass rejects requests without an identifying User-Agent.
                setRequestProperty("User-Agent", "PetCare/1.0 (Android; $packageName)")
            }
            if (connection.responseCode != 200) return null
            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            val elements = json.optJSONArray("elements") ?: return null
            (0 until elements.length()).mapNotNull { i ->
                val obj = elements.getJSONObject(i)
                // Nodes carry lat/lon directly; ways and relations carry a "center".
                val pos = if (obj.has("lat")) obj else obj.optJSONObject("center") ?: return@mapNotNull null
                val tags = obj.optJSONObject("tags") ?: JSONObject()
                val type = when {
                    tags.optString("amenity") == "veterinary" -> PlaceType.VET
                    tags.optString("amenity") == "animal_shelter" -> PlaceType.SHELTER
                    tags.optString("shop") == "pet_grooming" -> PlaceType.GROOMING
                    tags.optString("shop") == "pet" -> PlaceType.SHOP
                    tags.optString("leisure") == "dog_park" -> PlaceType.PARK
                    else -> PlaceType.OTHER
                }
                val street = listOf(tags.optString("addr:street"), tags.optString("addr:city")).filter { it.isNotBlank() }
                MapPlace(
                    name = tags.optString("name").ifBlank { type.label },
                    type = type,
                    point = GeoPoint(pos.getDouble("lat"), pos.getDouble("lon")),
                    address = street.joinToString(", ")
                )
            }
        } catch (e: Exception) {
            null
        }
    }

    private fun sampleKathmanduPlaces(): List<MapPlace> = listOf(
        MapPlace("Kathmandu Vet Clinic & Animal Hospital", PlaceType.VET, GeoPoint(27.7180, 85.3250), "Lazimpat, Kathmandu"),
        MapPlace("Pawfect Pet Grooming & Spa", PlaceType.GROOMING, GeoPoint(27.7150, 85.3220), "Thamel, Kathmandu"),
        MapPlace("Central Pet Park & Play Zone", PlaceType.PARK, GeoPoint(27.7200, 85.3300), "Baluwatar, Kathmandu"),
        MapPlace("Himalayan Pet Supplies & Food Store", PlaceType.SHOP, GeoPoint(27.7130, 85.3200), "Durbar Marg, Kathmandu"),
        MapPlace("Valley Animal Rescue & Shelter", PlaceType.SHELTER, GeoPoint(27.7220, 85.3180), "Samakhusi, Kathmandu")
    )

    private fun renderMarkers() {
        val overlays = binding.mapView.overlays
        overlays.removeAll { it is Marker }

        val saved = savedPlaces.map(::toMapPlace)
        // Hide a nearby place once the user has saved it (same spot within ~20 m).
        val nearby = nearbyPlaces.filter { n ->
            saved.none { it.point.distanceToAsDouble(n.point) < 20.0 }
        }
        val visible = when (activeFilter) {
            FILTER_SAVED -> saved
            FILTER_ALL -> saved + nearby
            else -> (saved + nearby).filter { it.type.code == activeFilter }
        }

        val savedIcon = ContextCompat.getDrawable(this, R.drawable.ic_pin_saved)
        for (place in visible) {
            val marker = Marker(binding.mapView).apply {
                position = place.point
                title = place.name
                setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                if (place.saved != null) icon = savedIcon
                setOnMarkerClickListener { _, _ ->
                    showPlaceCard(place)
                    binding.mapView.controller.animateTo(place.point)
                    true
                }
            }
            overlays.add(marker)
        }
        binding.mapView.invalidate()
    }

    private fun showPlaceCard(place: MapPlace) {
        activePlace = place
        binding.tvPoiIcon.text = place.type.emoji
        binding.tvPoiName.text = place.name
        binding.tvPoiCategory.text = if (place.saved != null) "${place.type.label} · My place" else "${place.type.label} · Nearby"
        binding.tvPoiAddress.text = place.address.ifBlank {
            String.format(Locale.US, "%.5f, %.5f", place.point.latitude, place.point.longitude)
        }
        val isSaved = place.saved != null
        binding.btnPoiSaveToggle.setText(if (isSaved) R.string.remove_place else R.string.save_place)
        binding.btnPoiSaveToggle.setIconResource(if (isSaved) R.drawable.ic_delete else R.drawable.ic_bookmark)
        val toggleColor = getColor(if (isSaved) R.color.error else R.color.primary)
        binding.btnPoiSaveToggle.setTextColor(toggleColor)
        binding.btnPoiSaveToggle.iconTint = android.content.res.ColorStateList.valueOf(toggleColor)
        binding.tvPoiLinked.visibility = View.GONE
        binding.cardPoiDetails.visibility = View.VISIBLE

        val saved = place.saved ?: return
        lifecycleScope.launch {
            val visits = repository.getVisitsForLocation(saved.id)
            if (activePlace?.saved?.id != saved.id) return@launch
            binding.tvPoiLinked.text = if (visits.isEmpty()) {
                "Nothing linked yet. Pick this place when adding an appointment or logging an activity."
            } else {
                "Linked appointments & activities:\n" + visits.take(5).joinToString("\n") {
                    "• ${it.title} · ${it.petName} · ${TimeUtils.formatDateTime(it.whenMillis)}"
                }
            }
            binding.tvPoiLinked.visibility = View.VISIBLE
        }
    }

    private fun showTagPlaceDialog(point: GeoPoint, suggestedName: String, suggestedType: PlaceType, address: String) {
        val b = DialogTagPlaceBinding.inflate(layoutInflater)
        b.tvPlaceCoords.text = String.format(Locale.US, "%.5f, %.5f", point.latitude, point.longitude)
        b.etPlaceName.setText(suggestedName)
        b.etPlaceAddress.setText(address)
        b.actvPlaceType.setSimpleItems(PlaceType.entries.map { it.label }.toTypedArray())
        b.actvPlaceType.setText(suggestedType.label, false)

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(b.root)
            .setPositiveButton("Save Place", null)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = b.etPlaceName.text.toString().trim()
                if (name.isEmpty()) {
                    b.tilPlaceName.error = "Give the place a name"
                    return@setOnClickListener
                }
                val location = LocationEntity(
                    userId = userId,
                    name = name,
                    address = b.etPlaceAddress.text.toString().trim(),
                    latitude = point.latitude,
                    longitude = point.longitude,
                    locationType = PlaceType.fromLabel(b.actvPlaceType.text.toString()).code
                )
                lifecycleScope.launch {
                    val id = repository.insertLocation(location)
                    showPlaceCard(toMapPlace(location.copy(id = id)))
                    Toast.makeText(this@PetMapActivity, "$name saved to My Places", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            }
        }
        dialog.show()
    }

    private fun confirmRemovePlace(place: LocationEntity) {
        MaterialAlertDialogBuilder(this)
            .setTitle("Remove ${place.name}?")
            .setMessage("Appointments and activities linked to it are kept, just without a place.")
            .setPositiveButton("Remove") { _, _ ->
                lifecycleScope.launch {
                    repository.deleteLocation(place)
                    binding.cardPoiDetails.visibility = View.GONE
                    activePlace = null
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onResume() {
        super.onResume()
        binding.mapView.onResume()
        locationOverlay?.enableMyLocation()
    }

    override fun onPause() {
        super.onPause()
        binding.mapView.onPause()
        locationOverlay?.disableMyLocation()
    }

    companion object {
        private const val FILTER_ALL = "ALL"
        private const val FILTER_SAVED = "SAVED"
        private val OVERPASS_SERVERS = listOf(
            "https://overpass-api.de/api/interpreter",
            "https://maps.mail.ru/osm/tools/overpass/api/interpreter"
        )
    }
}
