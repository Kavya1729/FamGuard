package com.example.kotlinf

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class DashboardAdapter(
    private val familyMembers: List<DashboardFragment.FamilyMemberDashboard>
) : RecyclerView.Adapter<DashboardAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_dashboard_member, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val member = familyMembers[position]

        holder.nameText.text = member.name
        holder.emailText.text = member.email
        holder.distanceText.text = member.distance
        holder.lastSeenText.text = getLastSeenText(member.lastUpdated)

        // Set status indicator
        if (member.isActive) {
            holder.statusIndicator.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, R.color.green)
            )
            holder.statusText.text = "Active"
            holder.statusText.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.green)
            )
        } else {
            holder.statusIndicator.setColorFilter(
                ContextCompat.getColor(holder.itemView.context, R.color.gray)
            )
            holder.statusText.text = "Inactive"
            holder.statusText.setTextColor(
                ContextCompat.getColor(holder.itemView.context, R.color.gray)
            )
        }

        // Highlight current user
        if (member.isCurrentUser) {
            holder.nameText.text = "${member.name} (You)"
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.light_blue)
            )
        } else {
            holder.itemView.setBackgroundColor(
                ContextCompat.getColor(holder.itemView.context, R.color.white)
            )
        }
    }

    override fun getItemCount(): Int = familyMembers.size

    private fun getLastSeenText(lastUpdated: Long): String {
        if (lastUpdated == 0L) return "Never"

        val now = System.currentTimeMillis()
        val diff = now - lastUpdated

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            else -> "${diff / 86400000}d ago"
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val nameText: TextView = itemView.findViewById(R.id.member_name)
        val emailText: TextView = itemView.findViewById(R.id.member_email)
        val distanceText: TextView = itemView.findViewById(R.id.member_distance)
        val lastSeenText: TextView = itemView.findViewById(R.id.member_last_seen)
        val statusIndicator: ImageView = itemView.findViewById(R.id.status_indicator)
        val statusText: TextView = itemView.findViewById(R.id.status_text)
    }
}