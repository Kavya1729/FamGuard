package com.example.kotlinf

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton

class InviteAdapter(
    private val listContacts: List<ContactModel>,
    private val onInviteClick: OnInviteClick
): RecyclerView.Adapter<InviteAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InviteAdapter.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val item = inflater.inflate(R.layout.item_invite, parent, false)
        return ViewHolder(item)
    }

    override fun onBindViewHolder(holder: InviteAdapter.ViewHolder, position: Int) {
        val item = listContacts[position]
        holder.name.text = item.name

        holder.inviteButton.setOnClickListener {
            onInviteClick.onInviteClick(item)
        }
    }

    override fun getItemCount(): Int {
        return listContacts.size
    }

    class ViewHolder(private val item: View) : RecyclerView.ViewHolder(item) {
        val name = item.findViewById<TextView>(R.id.name)
        val inviteButton = item.findViewById<MaterialButton>(R.id.invite_button)
    }

    interface OnInviteClick {
        fun onInviteClick(contact: ContactModel)
    }
}