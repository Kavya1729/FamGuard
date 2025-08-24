package com.example.kotlinf

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class GuardFragment : Fragment(), InviteMailAdapter.OnActionClick {

    private lateinit var inviteMailEditText: EditText
    private lateinit var inviteRecycler: RecyclerView
    private lateinit var adapter: InviteMailAdapter
    private val invitesList = ArrayList<String>()
    lateinit var mContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_guard, container, false)

        // Initialize views
        val sendInviteBtn = view.findViewById<Button>(R.id.send_invite)
        inviteMailEditText = view.findViewById(R.id.invite_mail)
        inviteRecycler = view.findViewById(R.id.invite_recycler)

        // Set up RecyclerView
        setupRecyclerView()

        // Set onClickListener for send invite button
        sendInviteBtn.setOnClickListener {
            sendInvite()
        }

        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mContext = context
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        getInvites()
    }

    private fun setupRecyclerView() {
        adapter = InviteMailAdapter(invitesList, this)
        inviteRecycler.layoutManager = LinearLayoutManager(mContext)
        inviteRecycler.adapter = adapter
    }

    private fun getInvites() {
        val firestore = Firebase.firestore
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        if (currentUserEmail == null) {
            Log.e("invite89", "User email is null")
            return
        }

        Log.d("invite89", "Getting invites for user: $currentUserEmail")

        firestore.collection("users")
            .document(currentUserEmail)
            .collection("invites")
            .whereEqualTo("invite_status", 0) // Only pending invites
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("invite89", "Error listening for invites", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    invitesList.clear()

                    for (document in snapshot.documents) {
                        invitesList.add(document.id)
                        Log.d("invite89", "Found pending invite from: ${document.id}")
                    }

                    Log.d("invite89", "Found ${invitesList.size} pending invites")
                    adapter.notifyDataSetChanged()
                } else {
                    Log.e("invite89", "Snapshot is null")
                }
            }
    }

    private fun sendInvite() {
        val mail = inviteMailEditText.text.toString().trim()

        if (mail.isEmpty()) {
            Toast.makeText(mContext, "Please enter an email address", Toast.LENGTH_SHORT).show()
            return
        }

        if (!isValidEmail(mail)) {
            Toast.makeText(mContext, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        if (currentUserEmail == null) {
            Toast.makeText(mContext, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (mail == currentUserEmail) {
            Toast.makeText(mContext, "You cannot invite yourself", Toast.LENGTH_SHORT).show()
            return
        }

        val firestore = Firebase.firestore

        // First check if the user exists
        firestore.collection("users")
            .document(mail)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // User exists, send the invite
                    sendInviteToUser(mail, currentUserEmail)
                } else {
                    Toast.makeText(mContext, "User with this email is not registered", Toast.LENGTH_LONG).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("invite89", "Error checking if user exists", exception)
                Toast.makeText(mContext, "Error checking user: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendInviteToUser(recipientEmail: String, senderEmail: String) {
        val firestore = Firebase.firestore

        // Check if invite already exists
        firestore.collection("users")
            .document(recipientEmail)
            .collection("invites")
            .document(senderEmail)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val status = document.getLong("invite_status")
                    when (status) {
                        0L -> Toast.makeText(mContext, "Invite already sent to this user", Toast.LENGTH_SHORT).show()
                        1L -> Toast.makeText(mContext, "You are already connected with this user", Toast.LENGTH_SHORT).show()
                        -1L -> {
                            // Previous invite was denied, send new one
                            createInvite(recipientEmail, senderEmail)
                        }
                    }
                } else {
                    // No existing invite, create new one
                    createInvite(recipientEmail, senderEmail)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("invite89", "Error checking existing invite", exception)
                Toast.makeText(mContext, "Error sending invite: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createInvite(recipientEmail: String, senderEmail: String) {
        val firestore = Firebase.firestore
        val data = hashMapOf(
            "invite_status" to 0,
            "sent_at" to System.currentTimeMillis(),
            "sender_name" to (FirebaseAuth.getInstance().currentUser?.displayName ?: "Unknown User")
        )

        Log.d("invite89", "Creating invite from $senderEmail to $recipientEmail")

        firestore.collection("users")
            .document(recipientEmail)
            .collection("invites")
            .document(senderEmail)
            .set(data)
            .addOnSuccessListener {
                Log.d("invite89", "Invite sent successfully to $recipientEmail")
                Toast.makeText(mContext, "Invite sent successfully!", Toast.LENGTH_SHORT).show()
                inviteMailEditText.text.clear()
            }
            .addOnFailureListener { exception ->
                Log.e("invite89", "Failed to send invite to $recipientEmail", exception)
                Toast.makeText(mContext, "Failed to send invite: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    override fun onAcceptClick(mail: String) {
        updateInviteStatus(mail, 1, "accepted")
    }

    override fun onDenyClick(mail: String) {
        updateInviteStatus(mail, -1, "denied")
    }

    private fun updateInviteStatus(senderEmail: String, status: Int, action: String) {
        val firestore = Firebase.firestore
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        if (currentUserEmail == null) {
            Log.e("invite89", "Current user email is null")
            return
        }

        val data = hashMapOf<String, Any>(
            "invite_status" to status,
            "responded_at" to System.currentTimeMillis()
        )

        Log.d("invite89", "Updating invite status: $senderEmail -> $status ($action)")

        firestore.collection("users")
            .document(currentUserEmail)
            .collection("invites")
            .document(senderEmail)
            .update(data)
            .addOnSuccessListener {
                Log.d("invite89", "Invite $action successfully for $senderEmail")
                Toast.makeText(mContext, "Invite $action!", Toast.LENGTH_SHORT).show()

                // Remove from local list and update adapter
                invitesList.remove(senderEmail)
                adapter.notifyDataSetChanged()

                // If accepted, create reverse connection for easy lookup
                if (status == 1) {
                    createReverseConnection(senderEmail, currentUserEmail)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("invite89", "Failed to $action invite for $senderEmail", exception)
                Toast.makeText(mContext, "Failed to $action invite", Toast.LENGTH_SHORT).show()
            }
    }

    private fun createReverseConnection(senderEmail: String, currentUserEmail: String) {
        val firestore = Firebase.firestore
        val data = hashMapOf(
            "invite_status" to 1,
            "connected_at" to System.currentTimeMillis(),
            "connection_type" to "reverse" // To identify this is a reverse connection
        )

        firestore.collection("users")
            .document(senderEmail)
            .collection("invites")
            .document(currentUserEmail)
            .set(data)
            .addOnSuccessListener {
                Log.d("invite89", "Reverse connection created successfully")
            }
            .addOnFailureListener { exception ->
                Log.e("invite89", "Failed to create reverse connection", exception)
            }
    }

    companion object {
        @JvmStatic
        fun newInstance() = GuardFragment()
    }
}