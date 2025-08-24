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
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_guard, container, false)

        // Initialize views
        val sendInviteBtn = view.findViewById<Button>(R.id.send_invite)
        inviteMailEditText = view.findViewById(R.id.invite_mail)
        inviteRecycler = view.findViewById(R.id.invite_recycler)

        // Set up RecyclerView
        setupRecyclerView()

        // Set onClickListener directly here
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
        getInvites() // Call this here
    }

    private fun setupRecyclerView() {
        // Initialize adapter with empty list
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
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    invitesList.clear()

                    for (document in task.result) {
                        val inviteStatus = document.getLong("invite_status")
                        Log.d("invite89", "Document ID: ${document.id}, Status: $inviteStatus")

                        // Check for pending invites (status = 0)
                        if (inviteStatus == 0L) {
                            invitesList.add(document.id)
                        }
                    }

                    Log.d("invite89", "Found ${invitesList.size} pending invites: $invitesList")

                    // Notify adapter of data change
                    adapter.notifyDataSetChanged()

                    if (invitesList.isEmpty()) {
                        Log.d("invite89", "No pending invites found")
                    }
                } else {
                    Log.e("invite89", "Error getting invites", task.exception)
                }
            }
    }

    companion object {
        @JvmStatic
        fun newInstance() = GuardFragment()
    }

    private fun sendInvite() {
        val mail = inviteMailEditText.text.toString().trim()

        if (mail.isEmpty()) {
            Log.e("invite89", "Email is empty")
            return
        }

        val firestore = Firebase.firestore
        val data = hashMapOf(
            "invite_status" to 0
        )

        val senderMail = FirebaseAuth.getInstance().currentUser?.email.toString()

        Log.d("invite89", "Sending invite from $senderMail to $mail")

        firestore.collection("users")
            .document(mail)
            .collection("invites")
            .document(senderMail)
            .set(data)
            .addOnSuccessListener {
                Log.d("invite89", "Invite sent successfully to $mail")
                inviteMailEditText.text.clear()
            }
            .addOnFailureListener { exception ->
                Log.e("invite89", "Failed to send invite to $mail", exception)
            }
    }

    override fun onAcceptClick(mail: String) {
        updateInviteStatus(mail, 1, "accepted")
    }

    override fun onDenyClick(mail: String) {
        updateInviteStatus(mail, -1, "denied")
    }

    private fun updateInviteStatus(mail: String, status: Int, action: String) {
        val firestore = Firebase.firestore
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

        if (currentUserEmail == null) {
            Log.e("invite89", "Current user email is null")
            return
        }

        val data = hashMapOf(
            "invite_status" to status
        )

        Log.d("invite89", "Updating invite status: $mail -> $status ($action)")

        firestore.collection("users")
            .document(currentUserEmail)
            .collection("invites")
            .document(mail)
            .set(data)
            .addOnSuccessListener {
                Log.d("invite89", "Invite $action successfully for $mail")

                // Remove from local list and update adapter
                invitesList.remove(mail)
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { exception ->
                Log.e("invite89", "Failed to $action invite for $mail", exception)
            }
    }
}