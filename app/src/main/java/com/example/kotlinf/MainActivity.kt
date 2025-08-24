package com.example.kotlinf

import android.Manifest
import android.content.ContentValues.TAG
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

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)

        // Check if user is logged in
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


        fun isLocationEnabled(context: Context): Boolean {
            val locationManager: LocationManager =
                context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                    || locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

        /**
         * Function to show the "enable GPS" Dialog box
         */
        fun showGPSNotEnabledDialog(context: Context) {
            AlertDialog.Builder(context)
                .setTitle("GPS Enabled")
                .setMessage("required for this app")
                .setCancelable(false)
                .setPositiveButton("enable_now") { _, _ ->
                    context.startActivity(Intent(ACTION_LOCATION_SOURCE_SETTINGS))
                }
                .show()
        }

        fun isAllPermissionsGranted(): Boolean{

            for(item in permissions){

            if(ContextCompat
                .checkSelfPermission(
                    this,
                    item
                ) != PackageManager.PERMISSION_GRANTED){
                return false
            }

            }

            return true
        }

        if(isAllPermissionsGranted()){
            if(isLocationEnabled(this)){
                setUpLocationListener()
            }
            else{
                showGPSNotEnabledDialog(this)
            }
        }
        else{
            askForPermission()
        }



        val bottomBar = findViewById<BottomNavigationView>(R.id.bottom_bar)

        bottomBar.setOnItemSelectedListener {
            if(it.itemId == R.id.nav_guard){
                inflateFragment(GuardFragment.newInstance())
            }
            else if(it.itemId == R.id.nav_home){
                inflateFragment(HomeFragment.newInstance())
            }
            else if(it.itemId == R.id.nav_dashboard){
                inflateFragment(MapsFragment())
            }
            else{
                inflateFragment( ProfileFragment.newInstance())
            }
            true
        }

        bottomBar.selectedItemId = R.id.nav_home

        val currentUser = FirebaseAuth.getInstance().currentUser
        val name = currentUser?.displayName.toString()
        val email = currentUser?.email.toString()
        val phoneNumber = currentUser?.phoneNumber.toString()

        val db = Firebase.firestore

        val user = hashMapOf(
            "name" to name,
            "email" to email,
            "phoneNumber" to phoneNumber
        )

        db.collection("users").document(email).set(user).addOnSuccessListener {  }
            .addOnFailureListener {  }

    }

    private fun setUpLocationListener() {
        val fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        val locationRequest = LocationRequest().setInterval(2000).setFastestInterval(2000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            object : LocationCallback() {
                override fun onLocationResult(locationResult: LocationResult) {
                    super.onLocationResult(locationResult)
                    for (location in locationResult.locations) {
                        Log.d("location89","onLocationResult: latitude ${location.latitude}")
                        Log.d("location89","onLocationResult: longitude ${location.longitude}")
//                        longitide and latitude log
                        val currentUser = FirebaseAuth.getInstance().currentUser
                        val email = currentUser?.email.toString()

                        val db = Firebase.firestore

                        val locationData = mutableMapOf<String,Any>(
                            "lat" to location.latitude.toString(),
                            "long" to location.longitude.toString()
                        )

                        db.collection("users").document(email).update(locationData).addOnSuccessListener {  }
                            .addOnFailureListener {  }

                    }
                    // Few more things we can do here:
                    // For example: Update the location of user on server
                }
            },
            Looper.myLooper()
        )
    }

    private fun isUserLoggedIn(): Boolean {
        val sharedPrefs = getSharedPreferences("MyFamilyPrefs", Context.MODE_PRIVATE)
        return sharedPrefs.getBoolean("isLoggedIn", false)
    }

    private fun askForPermission() {
        ActivityCompat.requestPermissions(this,permissions,permissionCode)
    }

    private fun inflateFragment(newInstance: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.contain,newInstance )
        transaction.commit()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String?>,
        grantResults: IntArray,
        deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)

        if(requestCode == permissionCode){
            if(allPermissionGranted()){
                setUpLocationListener()
            }else{

            }
        }
    }

    private fun allPermissionGranted(): Boolean {
        for(item in permissions){
            if(ContextCompat.checkSelfPermission(this,item) != PackageManager.PERMISSION_GRANTED){
                return false
            }
        }

        return true
    }
}