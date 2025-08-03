package com.example.myapplication

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
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
                inflateFragment( DashboardFragment.newInstance())
            }
            else{
                inflateFragment( ProfileFragment.newInstance())
            }
            true
        }

        bottomBar.selectedItemId = R.id.nav_home
    }

    private fun inflateFragment(newInstance: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.contain,newInstance )
        transaction.commit()
    }
}