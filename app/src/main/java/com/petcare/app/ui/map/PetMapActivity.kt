package com.petcare.app.ui.map

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.petcare.app.databinding.ActivityPetMapBinding

/**
 * Placeholder for future Google Maps integration.
 * This activity currently displays a coming-soon UI as part of Phase 1.
 */
class PetMapActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPetMapBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPetMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarMap)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarMap.setNavigationOnClickListener {
            finish()
        }
    }

    /*
     * FUTURE INTEGRATION STEPS:
     * 1. Add 'com.google.android.gms:play-services-maps' to dependencies.
     * 2. Configure API Key in local.properties and AndroidManifest.xml.
     * 3. Replace placeholder UI with <fragment android:name="com.google.android.gms.maps.SupportMapFragment" ... />.
     * 4. Implement OnMapReadyCallback.
     * 5. Request ACCESS_FINE_LOCATION and ACCESS_COARSE_LOCATION permissions.
     * 6. Add markers for pet-related services (Veterinarians, Parks, etc.) using PetEntity data if applicable.
     */
}