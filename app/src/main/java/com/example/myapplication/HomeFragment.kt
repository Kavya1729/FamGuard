package com.example.myapplication

import android.os.Bundle
import android.provider.ContactsContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.ArrayList

class HomeFragment : Fragment() {

    lateinit var  inviteAdapter : InviteAdapter

    private val listContacts: ArrayList<ContactModel> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            MemberModel("Ben10",
                "game wordld not real, hard to find .",
                "10%",
                "24 m"),
            MemberModel("keven11",
                "jain-chok bhiwani near bda bazar haryana",
                "82%",
                "445 km"),
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


    private fun fetchDatabaseContacts() {
        val database = MyFamilyDatabase.getDatabase(requireContext())

        database.contactDao().getAllContacts().observe(viewLifecycleOwner){
            listContacts.clear()
            listContacts.addAll(it)

            inviteAdapter.notifyDataSetChanged()
        }
    }

    private suspend fun insertDatabaseContacts(listContacts1: ArrayList<ContactModel>) {
        val database = MyFamilyDatabase.getDatabase(requireContext())

        database.contactDao().insertAll(listContacts)
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