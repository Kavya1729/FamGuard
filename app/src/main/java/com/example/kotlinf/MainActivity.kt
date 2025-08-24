package com.example.kotlinf

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.Looper
import android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class MainActivity : AppCompatActivity() {

    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_CONTACTS
    )

    val permissionCode = 10
    private lateinit var bottomBar: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        if (!isUserLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        if (isAllPermissionsGranted()) {
            if (isLocationEnabled(this)) {
                setUpLocationListener()
            } else {
                showGPSNotEnabledDialog(this)
            }
        } else {
            askForPermission()
        }

        // Setup bottom navigation (removed profile)
        bottomBar = findViewById<BottomNavigationView>(R.id.bottom_bar)
        bottomBar.setOnItemSelectedListener {
            when (it.itemId) {
                R.id.nav_guard -> inflateFragment(GuardFragment.newInstance())
                R.id.nav_home -> inflateFragment(HomeFragment.newInstance())
                R.id.nav_dashboard -> inflateFragment(MapsFragment())
                else -> false
            }
            true
        }

        bottomBar.selectedItemId = R.id.nav_home
        saveUserToFirestore()
    }

    fun navigateToMaps() {
        bottomBar.selectedItemId = R.id.nav_dashboard
        inflateFragment(MapsFragment())
    }

    private fun isLocationEnabled(context: Context): Boolean {
        val locationManager: LocationManager =
            context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun showGPSNotEnabledDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("GPS Required")
            .setMessage("GPS is required for this app to work properly")
            .setCancelable(false)
            .setPositiveButton("Enable Now") { _, _ ->
                context.startActivity(Intent(ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .show()
    }

    private fun isAllPermissionsGranted(): Boolean {
        for (item in permissions) {
            if (ContextCompat.checkSelfPermission(this, item) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }

    private fun setUpLocationListener() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest.create().apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)

                locationResult.lastLocation?.let { location ->
                    Log.d("location89", "onLocationResult: latitude ${location.latitude}")
                    Log.d("location89", "onLocationResult: longitude ${location.longitude}")
                    updateLocationInFirestore(location.latitude, location.longitude)
                }
            }
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            askForPermission()
            return
        }

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private fun updateLocationInFirestore(latitude: Double, longitude: Double) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val email = currentUser?.email

        if (email != null) {
            val db = Firebase.firestore
            val locationData: Map<String, Any> = hashMapOf(
                "lat" to latitude,
                "long" to longitude,
                "lastUpdated" to System.currentTimeMillis()
            )

            db.collection("users")
                .document(email)
                .update(locationData)
                .addOnSuccessListener {
                    Log.d("location89", "Location updated successfully")
                }
                .addOnFailureListener { exception ->
                    Log.e("location89", "Failed to update location", exception)
                }
        }
    }

    private fun saveUserToFirestore() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val name = currentUser?.displayName ?: ""
        val email = currentUser?.email ?: ""
        val phoneNumber = currentUser?.phoneNumber ?: ""

        val db = Firebase.firestore
        val user = hashMapOf(
            "name" to name,
            "email" to email,
            "phoneNumber" to phoneNumber,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users")
            .document(email)
            .set(user)
            .addOnSuccessListener {
                Log.d("firestore89", "User data saved successfully")
            }
            .addOnFailureListener { exception ->
                Log.e("firestore89", "Failed to save user data", exception)
            }
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPrefs = getSharedPreferences("MyFamilyPrefs", Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean("isLoggedIn", false)
    }

    private fun askForPermission() {
        ActivityCompat.requestPermissions(this, permissions, permissionCode)
    }

    private fun inflateFragment(newInstance: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.contain, newInstance)
        transaction.commit()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == permissionCode) {
            if (allPermissionGranted()) {
                if (isLocationEnabled(this)) {
                    setUpLocationListener()
                } else {
                    showGPSNotEnabledDialog(this)
                }
            } else {
                Log.e("permission89", "Some permissions were denied")
            }
        }
    }

    private fun allPermissionGranted(): Boolean {
        for (item in permissions) {
            if (ContextCompat.checkSelfPermission(this, item) != PackageManager.PERMISSION_GRANTED) {
                return false
            }
        }
        return true
    }
}