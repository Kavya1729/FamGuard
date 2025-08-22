package com.example.kotlinf

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {

    val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.READ_CONTACTS
    )

    val permissionCode = 10

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        askForPermission()

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