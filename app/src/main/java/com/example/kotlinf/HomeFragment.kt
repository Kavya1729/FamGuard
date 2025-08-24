package com.example.kotlinf

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.ArrayList

class HomeFragment : Fragment() {

    lateinit var inviteAdapter: InviteAdapter
    private lateinit var auth: FirebaseAuth

    private val listContacts: ArrayList<ContactModel> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = Firebase.auth
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup logout functionality for 3 dots menu
        setupLogoutMenu(view)

        val listMembers = listOf<MemberModel>(
            MemberModel(
                "Kavya",
                "Mohalla Kot Delhi Wale Amroha-244221",
                "92%",
                "220 m"
            ),
            MemberModel(
                "Rahul",
                "Delhi sarogni nagar near new bus adda",
                "44%",
                "124 m"
            ),
            MemberModel(
                "Krish",
                "patal log near yamrAj AK-47 SECTOR",
                "67%",
                "1000 km"
            ),
            MemberModel(
                "Ben10",
                "game wordld not real, hard to find .",
                "10%",
                "24 m"
            ),
            MemberModel(
                "keven11",
                "jain-chok bhiwani near bda bazar haryana",
                "82%",
                "445 km"
            ),
        )

        val adapter = MemberAdapter(listMembers)

        val recycler = requireView().findViewById<RecyclerView>(R.id.recycler_member)
        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        inviteAdapter = InviteAdapter(listContacts)
        fetchDatabaseContacts()

        CoroutineScope(Dispatchers.IO).launch {
            insertDatabaseContacts(fetchContacts())
        }

        val inviteRecycler = requireView().findViewById<RecyclerView>(R.id.recycler_invite)
        inviteRecycler.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        inviteRecycler.adapter = inviteAdapter
    }

    private fun setupLogoutMenu(view: View) {
        // Using your existing menu_dots ImageView
        val menuDots = view.findViewById<ImageView>(R.id.menu_dots)

        menuDots?.setOnClickListener {
            showLogoutDialog()
        }
    }

    private fun showLogoutDialog() {
        // Get current user info for the dialog
        val currentUser = auth.currentUser
        val userName = currentUser?.displayName ?: "User"

        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout, $userName?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        // Sign out from Firebase
        auth.signOut()

        // Clear login state from SharedPreferences
        val sharedPrefs = requireActivity().getSharedPreferences("MyFamilyPrefs", Context.MODE_PRIVATE)
        val editor = sharedPrefs.edit()
        editor.putBoolean("isLoggedIn", false)
        editor.remove("loginTime")
        editor.remove("userName")
        editor.remove("userEmail")
        editor.apply()

        // Navigate to LoginActivity
        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun fetchDatabaseContacts() {
        val database = MyFamilyDatabase.getDatabase(requireContext())

        database.contactDao().getAllContacts().observe(viewLifecycleOwner) {
            listContacts.clear()
            listContacts.addAll(it)

            inviteAdapter.notifyDataSetChanged()
        }
    }

    private suspend fun insertDatabaseContacts(listContacts1: ArrayList<ContactModel>) {
        val database = MyFamilyDatabase.getDatabase(requireContext())

        database.contactDao().insertAll(listContacts1)
    }

    private fun fetchContacts(): ArrayList<ContactModel> {
        val cr = requireActivity().contentResolver
        val cursor = cr.query(
            ContactsContract.Contacts.CONTENT_URI,
            null,
            null,
            null,
            null
        )

        val listContacts = ArrayList<ContactModel>()

        cursor?.use { c ->
            val idIndex = c.getColumnIndexOrThrow(ContactsContract.Contacts._ID)
            val nameIndex = c.getColumnIndexOrThrow(ContactsContract.Contacts.DISPLAY_NAME)
            val hasPhoneIndex = c.getColumnIndexOrThrow(ContactsContract.Contacts.HAS_PHONE_NUMBER)

            while (c.moveToNext()) {
                val id = c.getString(idIndex)
                val name = c.getString(nameIndex)
                val hasPhoneNumber = c.getInt(hasPhoneIndex)

                if (hasPhoneNumber > 0) {
                    val pCur = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                        arrayOf(id),
                        null
                    )

                    pCur?.use { phoneCursor ->
                        val phoneIndex =
                            phoneCursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)

                        while (phoneCursor.moveToNext()) {
                            val phoneNum = phoneCursor.getString(phoneIndex)
                            listContacts.add(ContactModel(name, phoneNum))
                        }
                    }
                }
            }
        }

        return listContacts
    }

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragment()
    }
}