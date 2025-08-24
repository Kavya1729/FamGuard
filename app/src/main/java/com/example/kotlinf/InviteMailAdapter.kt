package com.example.kotlinf

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class InviteMailAdapter(
    private val listInvites: List<String>,
    private val onActionClick: GuardFragment
) : RecyclerView.Adapter<InviteMailAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_invite_mail, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listInvites[position]
        holder.name.text = item

        holder.accept.setOnClickListener {
            onActionClick.onAcceptClick(item)
        }

        holder.deny.setOnClickListener {
            onActionClick.onDenyClick(item)
        }
    }

    override fun getItemCount(): Int {
        return listInvites.size
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.mail)
        val accept: Button = itemView.findViewById(R.id.accept)
        val deny: Button = itemView.findViewById(R.id.deny)
    }

    interface OnActionClick {
        fun onAcceptClick(mail: String)
        fun onDenyClick(mail: String)
    }
}
