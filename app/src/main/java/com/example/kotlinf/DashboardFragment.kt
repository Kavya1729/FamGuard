package com.example.kotlinf

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class DashboardFragment : Fragment() {

    private lateinit var mContext: Context
    private lateinit var familyMembersRecycler: RecyclerView
    private lateinit var dashboardAdapter: DashboardAdapter
    private lateinit var totalMembersText: TextView
    private lateinit var activeMembersText: TextView

    private val familyMembers = mutableListOf<FamilyMemberDashboard>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)

        // Initialize views
        totalMembersText = view.findViewById(R.id.total_members)
        activeMembersText = view.findViewById(R.id.active_members)
        familyMembersRecycler = view.findViewById(R.id.family_members_recycler)

        setupRecyclerView()
        loadFamilyData()

        return view
    }

    private fun setupRecyclerView() {
        dashboardAdapter = DashboardAdapter(familyMembers)
        familyMembersRecycler.layoutManager = LinearLayoutManager(mContext)
        familyMembersRecycler.adapter = dashboardAdapter
    }

    private fun loadFamilyData() {
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUserEmail = currentUser?.email

        if (currentUserEmail == null) {
            Log.e("dashboard89", "Current user email is null")
            return
        }

        val db = Firebase.firestore

        // Get all accepted invites (family members)
        db.collection("users")
            .document(currentUserEmail)
            .collection("invites")
            .whereEqualTo("invite_status", 1)
            .addSnapshotListener { inviteSnapshot, error ->
                if (error != null) {
                    Log.e("dashboard89", "Error listening for family members", error)
                    return@addSnapshotListener
                }

                if (inviteSnapshot != null) {
                    val familyEmails = mutableListOf<String>()

                    // Add current user
                    familyEmails.add(currentUserEmail)

                    // Add family members from invites
                    for (document in inviteSnapshot.documents) {
                        familyEmails.add(document.id)
                    }

                    // Fetch detailed data for all family members
                    fetchFamilyMembersDetails(familyEmails)
                }
            }
    }

    private fun fetchFamilyMembersDetails(emails: List<String>) {
        familyMembers.clear()
        val db = Firebase.firestore

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
                        val phoneNumber = document.getString("phoneNumber") ?: ""

                        val isActive = isUserActive(lastUpdated)
                        val distance = if (lat != null && long != null) {
                            calculateDistance(lat, long)
                        } else {
                            "Unknown"
                        }

                        val member = FamilyMemberDashboard(
                            name = name,
                            email = email,
                            phoneNumber = phoneNumber,
                            latitude = lat,
                            longitude = long,
                            lastUpdated = lastUpdated,
                            isActive = isActive,
                            distance = distance,
                            batteryLevel = "Unknown", // You can add battery level to user data if needed
                            isCurrentUser = email == FirebaseAuth.getInstance().currentUser?.email
                        )

                        familyMembers.add(member)
                    }

                    // Update UI after loading all members
                    updateDashboardStats()
                    dashboardAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { exception ->
                    Log.e("dashboard89", "Error getting user data for $email", exception)
                }
        }
    }

    private fun isUserActive(lastUpdated: Long): Boolean {
        if (lastUpdated == 0L) return false
        val now = System.currentTimeMillis()
        val fifteenMinutes = 15 * 60 * 1000
        return (now - lastUpdated) < fifteenMinutes
    }

    private fun calculateDistance(lat: Double, long: Double): String {
        // This is a simplified distance calculation
        // You can implement proper distance calculation using current user's location
        val currentUser = FirebaseAuth.getInstance().currentUser?.email
        // For now, return a placeholder
        return "Unknown"
    }

    private fun updateDashboardStats() {
        val totalMembers = familyMembers.size
        val activeMembers = familyMembers.count { it.isActive }

        totalMembersText.text = totalMembers.toString()
        activeMembersText.text = activeMembers.toString()
    }

    companion object {
        @JvmStatic
        fun newInstance() = DashboardFragment()
    }

    data class FamilyMemberDashboard(
        val name: String,
        val email: String,
        val phoneNumber: String,
        val latitude: Double?,
        val longitude: Double?,
        val lastUpdated: Long,
        val isActive: Boolean,
        val distance: String,
        val batteryLevel: String,
        val isCurrentUser: Boolean
    )
}