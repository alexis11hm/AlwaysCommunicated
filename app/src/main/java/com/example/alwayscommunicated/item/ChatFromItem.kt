package com.example.alwayscommunicated.item

import com.example.alwayscommunicated.R
import com.example.alwayscommunicated.model.ChatMessage
import com.example.alwayscommunicated.model.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.chat_from_row.view.*
import kotlinx.android.synthetic.main.chat_to_row.view.*
import kotlinx.android.synthetic.main.latest_messages_row.view.*
import java.text.SimpleDateFormat
import java.util.*

class ChatFromItem(val chatMessage: ChatMessage, val user: User) : Item<GroupieViewHolder>() {
    override fun getLayout(): Int {
        return R.layout.chat_from_row
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        viewHolder.itemView.textview_from_row.text = chatMessage.text

        val uri = user.profileImageUrl
        val targetImageView = viewHolder.itemView.imageview_chat_from_row
        Picasso.get().load(uri).into(targetImageView)

        var messageDate = Date(chatMessage.date);
        var formatter= SimpleDateFormat("dd/MM/yy HH:mm aa");
        viewHolder.itemView.date_message.text = formatter.format(messageDate).toString()
    }
}