package com.amazonaws.services.chime.sdk.meetings.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.amazonaws.services.chime.sdk.R
import com.amazonaws.services.chime.sdk.meetings.data.RosterAttendee

class UserJoinedAdapter(private val users: List<RosterAttendee>) :
    RecyclerView.Adapter<UserJoinedAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {

        return ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_user_joined, parent, false)
        )
    }

    override fun getItemCount() = users.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(users[position])
        holder.itemView.setOnClickListener {
            if (position != 0) userClickListener?.invoke(users[position])
        }
    }
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

         var imgAvatarUser: ImageView = itemView.findViewById(R.id.imgAvatarUser)
         var tvUserName: TextView = itemView.findViewById(R.id.tvUserName)

        fun bind(user: RosterAttendee) {
            tvUserName.text = user.attendeeName
        }
    }
    var userClickListener: ((RosterAttendee) -> Unit) ? = null
}
