package com.example.kotlinf

import androidx.fragment.app.Fragment
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class MapsFragment : Fragment() {

    private lateinit var mMap: GoogleMap
    private val familyMembers = mutableListOf<FamilyMember>()

    private val callback = OnMapReadyCallback { googleMap ->
        mMap = googleMap
        setupMap()
        loadFamilyMembersLocations()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_maps, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(callback)
    }

    private fun setupMap() {
        try {
            mMap.isMyLocationEnabled = true
            mMap.uiSettings.isMyLocationButtonEnabled = true
            mMap.uiSettings.isZoomControlsEnabled = true
        } catch (e: SecurityException) {
            Log.e("maps89", "Location permission not granted", e)
        }
    }

    private fun loadFamilyMembersLocations() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserEmail = currentUser?.email

        if (currentUserEmail == null) {
            Log.e("maps89", "Current user email is null")
            return
        }

        val db = Firebase.firestore

        // First, get all the family members (accepted invites)
        db.collection("users")
            .document(currentUserEmail)
            .collection("invites")
            .whereEqualTo("invite_status", 1) // Only accepted invites
            .get()
            .addOnSuccessListener { inviteDocuments ->
                val familyEmails = mutableListOf<String>()

                // Add current user
                familyEmails.add(currentUserEmail)

                // Add family members
                for (document in inviteDocuments) {
                    familyEmails.add(document.id)
                }

                // Also check for invites sent by current user that were accepted
                db.collection("users")
                    .whereArrayContains("invites.$currentUserEmail.invite_status", 1)
                    .get()
                    .addOnSuccessListener { sentInviteDocuments ->
                        for (document in sentInviteDocuments) {
                            val email = document.id
                            if (!familyEmails.contains(email)) {
                                familyEmails.add(email)
                            }
                        }

                        // Now fetch location data for all family members
                        fetchFamilyMembersData(familyEmails)
                    }
                    .addOnFailureListener { exception ->
                        Log.e("maps89", "Error getting sent invites", exception)
                        // Still fetch data for received invites
                        fetchFamilyMembersData(familyEmails)
                    }
            }
            .addOnFailureListener { exception ->
                Log.e("maps89", "Error getting family members", exception)
                // At least show current user
                fetchFamilyMembersData(listOf(currentUserEmail))
            }
    }

    private fun fetchFamilyMembersData(emails: List<String>) {
        val db = Firebase.firestore
        familyMembers.clear()

        for (email in emails) {
            db.collection("users")
                .document(email)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val name = document.getString("name") ?: "Unknown"
                        val lat = document.getDouble("lat")
                        val long = document.getDouble("long")
                        val lastUpdated = document.getLong("lastUpdated") ?: 0

                        if (lat != null && long != null) {
                            val member = FamilyMember(
                                name = name,
                                email = email,
                                latitude = lat,
                                longitude = long,
                                lastUpdated = lastUpdated
                            )

                            familyMembers.add(member)
                            addMarkerForMember(member)

                            Log.d("maps89", "Added family member: $name at ($lat, $long)")
                        }
                    }

                    // After loading all members, adjust camera
                    if (familyMembers.isNotEmpty()) {
                        adjustCameraToShowAllMembers()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("maps89", "Error getting user data for $email", exception)
                }
        }
    }

    private fun addMarkerForMember(member: FamilyMember) {
        val position = LatLng(member.latitude, member.longitude)
        val currentUser = FirebaseAuth.getInstance().currentUser

        val markerColor = if (member.email == currentUser?.email) {
            BitmapDescriptorFactory.HUE_BLUE // Current user in blue
        } else {
            BitmapDescriptorFactory.HUE_RED // Family members in red
        }

        val marker = mMap.addMarker(
            MarkerOptions()
                .position(position)
                .title(member.name)
                .snippet(getLocationUpdateTime(member.lastUpdated))
                .icon(BitmapDescriptorFactory.defaultMarker(markerColor))
        )

        Log.d("maps89", "Added marker for ${member.name} at ${member.latitude}, ${member.longitude}")
    }

    private fun adjustCameraToShowAllMembers() {
        if (familyMembers.isEmpty()) return

        val boundsBuilder = LatLngBounds.Builder()
        for (member in familyMembers) {
            boundsBuilder.include(LatLng(member.latitude, member.longitude))
        }

        val bounds = boundsBuilder.build()
        val padding = 100 // pixels

        try {
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        } catch (e: Exception) {
            Log.e("maps89", "Error adjusting camera", e)
            // Fallback: center on first member
            if (familyMembers.isNotEmpty()) {
                val firstMember = familyMembers[0]
                val center = LatLng(firstMember.latitude, firstMember.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(center, 12f))
            }
        }
    }

    private fun getLocationUpdateTime(lastUpdated: Long): String {
        if (lastUpdated == 0L) return "Location unknown"

        val now = System.currentTimeMillis()
        val diff = now - lastUpdated

        return when {
            diff < 60000 -> "Updated just now"
            diff < 3600000 -> "Updated ${diff / 60000} minutes ago"
            diff < 86400000 -> "Updated ${diff / 3600000} hours ago"
            else -> "Updated ${diff / 86400000} days ago"
        }
    }

    data class FamilyMember(
        val name: String,
        val email: String,
        val latitude: Double,
        val longitude: Double,
        val lastUpdated: Long
    )
}