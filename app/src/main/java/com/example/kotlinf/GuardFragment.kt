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
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

class GuardFragment : Fragment(), InviteMailAdapter.OnActionClick {

    private lateinit var inviteMailEditText: EditText
    private lateinit var inviteRecycler: RecyclerView
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
        getInvites() // ✅ Only call this here
    }

    private fun getInvites() {
        val firestore = Firebase.firestore
        firestore.collection("users")
            .document(FirebaseAuth.getInstance().currentUser?.email.toString())
            .collection("invites")
            .get()
            .addOnCompleteListener {
                if (it.isSuccessful) {
                    val list = ArrayList<String>()
                    for (item in it.result) {
                        if (item.get("invite_status") == 0L) {
                            list.add(item.id)
                        }
                    }

                    Log.d("invite89", "getInvites: $list")

                    // Use the RecyclerView reference directly
                    val adapter = InviteMailAdapter(list, this)
                    inviteRecycler.adapter = adapter
                }
            }
    }

    companion object {
        @JvmStatic
        fun newInstance() = GuardFragment()
    }

    private fun sendInvite() {
        val mail = inviteMailEditText.text.toString()
        val firestore = Firebase.firestore

        val data = hashMapOf(
            "invite_status" to 0
        )

        val senderMail = FirebaseAuth.getInstance().currentUser?.email.toString()

        firestore.collection("users")
            .document(mail)
            .collection("invites")
            .document(senderMail).set(data)
            .addOnSuccessListener {
                Log.d("invite89", "Invite sent successfully")
            }.addOnFailureListener {
                Log.e("invite89", "Failed to send invite", it)
            }
    }

    override fun onAcceptClick(mail: String) {
        val firestore = Firebase.firestore

        val data = hashMapOf(
            "invite_status" to 1
        )

        val senderMail = FirebaseAuth.getInstance().currentUser?.email.toString()

        firestore.collection("users")
            .document(senderMail)
            .collection("invites")
            .document(mail).set(data)
            .addOnSuccessListener {
                Log.d("invite89", "Invite sent successfully")
            }.addOnFailureListener {
                Log.e("invite89", "Failed to send invite", it)
            }
    }

    override fun onDenyClick(mail: String) {
        val firestore = Firebase.firestore

        val data = hashMapOf(
            "invite_status" to -1
        )

        val senderMail = FirebaseAuth.getInstance().currentUser?.email.toString()

        firestore.collection("users")
            .document(senderMail)
            .collection("invites")
            .document(mail).set(data)
            .addOnSuccessListener {
                Log.d("invite89", "Invite sent successfully")
            }.addOnFailureListener {
                Log.e("invite89", "Failed to send invite", it)
            }

    }
}
