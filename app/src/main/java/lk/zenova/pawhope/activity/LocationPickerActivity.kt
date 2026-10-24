package lk.zenova.pawhope.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import lk.zenova.pawhope.R
import java.io.IOException
import java.util.Locale
import kotlin.concurrent.thread

// සතා ඉන්න තැන map එකෙන් තෝරන screen එක.
// map එක move කරනකොට මැද pin එක ඉන්න තැන address එක ගන්නවා.
class LocationPickerActivity : AppCompatActivity(), OnMapReadyCallback {

    // මේ activity එකෙන් Report screen එකට දෙන දත්ත වල keys
    companion object {
        const val EXTRA_LAT = "lat"
        const val EXTRA_LNG = "lng"
        const val EXTRA_ADDRESS = "address"
        const val EXTRA_CITY = "city"
        const val EXTRA_DISTRICT = "district"

        // permission නැති නම්/GPS නැති නම් මුලින්ම පෙන්නන තැන: කොළඹ
        private val DEFAULT_LOCATION = LatLng(6.9271, 79.8612)
    }

    private var map: GoogleMap? = null
    private lateinit var fusedClient: FusedLocationProviderClient

    private lateinit var addressText: TextView
    private lateinit var coordsText: TextView
    private lateinit var searchInput: EditText
    private lateinit var btnConfirm: MaterialButton

    // දැනට pin එක ඉන්න තැන
    private var currentLatLng: LatLng = DEFAULT_LOCATION
    private var currentAddress = ""
    private var currentCity = ""
    private var currentDistrict = ""

    // address එක හොයන request එකක් පරණ වුණාම (map එක නැවත move වුණාම) එහි ප්‍රතිඵලය අත්හරින්න
    private var geocodeRequestId = 0

    // Location permission ඉල්ලන launcher එක. FINE හෝ COARSE එකක් ලැබුණත් ඇති
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.any { it }) {
            enableMyLocationLayer()
            moveToMyLocation()
        } else {
            toast("Location permission is needed to show your position")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_location_picker)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        addressText = findViewById(R.id.addressText)
        coordsText = findViewById(R.id.coordsText)
        searchInput = findViewById(R.id.searchInput)
        btnConfirm = findViewById(R.id.btnConfirm)

        findViewById<android.view.View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<android.view.View>(R.id.btnSearch).setOnClickListener {
            searchAddress(searchInput.text.toString())
        }
        findViewById<android.view.View>(R.id.fabMyLocation).setOnClickListener {
            moveToMyLocation()
        }

        // keyboard එකේ "search" බටන් එක එබුවාමත් හොයනවා
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchAddress(searchInput.text.toString())
                true
            } else {
                false
            }
        }

        btnConfirm.setOnClickListener { returnResult() }

        // Map fragment එක load කරනවා. ලෑස්ති වුණාම onMapReady() call වෙනවා
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        googleMap.uiSettings.isMyLocationButtonEnabled = false   // අපේම FAB එක තියෙන නිසා
        googleMap.uiSettings.isZoomControlsEnabled = false
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DEFAULT_LOCATION, 15f))

        // map එක move වෙන්න පටන් ගත්තාම "Finding address…" පෙන්නනවා
        googleMap.setOnCameraMoveStartedListener {
            addressText.setText(R.string.finding_address)
        }
        // map එක නැවතුණාම මැද තැනේ address එක හොයනවා
        googleMap.setOnCameraIdleListener {
            currentLatLng = googleMap.cameraPosition.target
            updateAddress(currentLatLng)
        }

        if (hasLocationPermission()) {
            enableMyLocationLayer()
            moveToMyLocation()
        } else {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    private fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED
    }

    // ඔයාගේ තැන නිල් තිතක් විදිහට map එකේ පෙන්නනවා
    @SuppressLint("MissingPermission")
    private fun enableMyLocationLayer() {
        map?.isMyLocationEnabled = true
    }

    // දැන් ඉන්න තැනට map එක අරන් යනවා
    @SuppressLint("MissingPermission")
    private fun moveToMyLocation() {
        if (!hasLocationPermission()) {
            permissionLauncher.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            return
        }
        fusedClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    val here = LatLng(location.latitude, location.longitude)
                    map?.animateCamera(CameraUpdateFactory.newLatLngZoom(here, 17f))
                } else {
                    toast("Couldn't get your location. Is GPS turned on?")
                }
            }
    }

    // lat/lng එකකින් address එක ගන්නවා (Geocoder = network call, ඒ නිසා background thread එකේ)
    @Suppress("DEPRECATION")
    private fun updateAddress(latLng: LatLng) {
        val requestId = ++geocodeRequestId
        coordsText.text = String.format(Locale.US, "%.5f, %.5f", latLng.latitude, latLng.longitude)

        thread {
            var line = ""
            var city = ""
            var district = ""
            try {
                if (Geocoder.isPresent()) {
                    val results = Geocoder(this, Locale.getDefault())
                        .getFromLocation(latLng.latitude, latLng.longitude, 1)
                    val a = results?.firstOrNull()
                    if (a != null) {
                        line = a.getAddressLine(0) ?: ""
                        city = a.locality ?: a.subLocality ?: ""
                        // Google දෙන්නේ "Colombo District" වගේ, ඒ නිසා " District" අයින් කරනවා
                        district = (a.subAdminArea ?: a.adminArea ?: "").removeSuffix(" District")
                    }
                }
            } catch (e: IOException) {
                // internet ප්‍රශ්නයක්. address හිස්ව තියෙනවා, coordinates තියෙනවා
            }

            // UI update කරන්න ඕනි main thread එකේ
            runOnUiThread {
                if (requestId != geocodeRequestId) return@runOnUiThread   // පරණ result එකක්, අත්හරිනවා
                currentAddress = line
                currentCity = city
                currentDistrict = district
                addressText.text = line.ifEmpty { "Pinned location (address not found)" }
            }
        }
    }

    // search bar එකේ ලියපු තැන හොයනවා
    @Suppress("DEPRECATION")
    private fun searchAddress(query: String) {
        if (query.isBlank()) return
        hideKeyboard()

        thread {
            val results: List<Address> = try {
                // ශ්‍රී ලංකාව ඇතුළත විතරක් හොයනවා (lat 5.8–9.95, lng 79.4–82.0)
                Geocoder(this, Locale.getDefault())
                    .getFromLocationName(query, 5, 5.8, 79.4, 9.95, 82.0) ?: emptyList()
            } catch (e: IOException) {
                emptyList()
            }
            runOnUiThread { showSearchResults(results) }
        }
    }

    private fun showSearchResults(results: List<Address>) {
        when (results.size) {
            0 -> toast("No places found")
            1 -> moveTo(results[0])
            else -> {
                // ප්‍රතිඵල කිහිපයක් නම් list එකකින් තෝරන්න දෙනවා
                val labels = results.map { it.getAddressLine(0) ?: it.featureName ?: "Unknown" }
                MaterialAlertDialogBuilder(this)
                    .setTitle("Select a place")
                    .setItems(labels.toTypedArray()) { _, index -> moveTo(results[index]) }
                    .show()
            }
        }
    }

    private fun moveTo(address: Address) {
        val target = LatLng(address.latitude, address.longitude)
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(target, 17f))
    }

    // තෝරපු තැන Report screen එකට යවනවා
    private fun returnResult() {
        val result = Intent().apply {
            putExtra(EXTRA_LAT, currentLatLng.latitude)
            putExtra(EXTRA_LNG, currentLatLng.longitude)
            putExtra(EXTRA_ADDRESS, currentAddress.ifEmpty {
                String.format(Locale.US, "%.5f, %.5f", currentLatLng.latitude, currentLatLng.longitude)
            })
            putExtra(EXTRA_CITY, currentCity)
            putExtra(EXTRA_DISTRICT, currentDistrict)
        }
        setResult(RESULT_OK, result)
        finish()
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(searchInput.windowToken, 0)
        searchInput.clearFocus()
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}