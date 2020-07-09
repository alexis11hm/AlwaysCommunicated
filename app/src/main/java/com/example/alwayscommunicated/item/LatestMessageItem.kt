package com.example.alwayscommunicated.item

import android.view.View
import com.example.alwayscommunicated.R
import com.example.alwayscommunicated.model.ChatMessage
import com.example.alwayscommunicated.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.latest_messages_row.view.*
import java.text.SimpleDateFormat
import java.util.*

class LatestMessageItem(val chatMessage: ChatMessage, var allow : Int) : Item<GroupieViewHolder>() {

    var chatPartnerUser:User? = null

    override fun getLayout(): Int {
        return R.layout.latest_messages_row
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        if(FirebaseAuth.getInstance().uid == chatMessage.fromId) {
            viewHolder.itemView.latest_message_content.text = "You: " + chatMessage.text
        }else{
            viewHolder.itemView.latest_message_content.text = chatMessage.text
        }

        var messageDate = Date(chatMessage.date);
        var currentDate = Date(System.currentTimeMillis());
        var showDate = ""
        if(messageDate.day == currentDate.day){
            var formatter= SimpleDateFormat("HH:mm aa");
            var date = Date(chatMessage.date)
            viewHolder.itemView.latest_messages_date.text = formatter.format(date).toString()
        }else if(messageDate.day == currentDate.day -1  ){
            showDate = "Yesterday"
            viewHolder.itemView.latest_messages_date.text = showDate
        }else if(messageDate.day < currentDate.day -1 || messageDate.day>currentDate.day){
            var formatter= SimpleDateFormat("dd/MM/yy");
            var date = Date(chatMessage.date)
            viewHolder.itemView.latest_messages_date.text = formatter.format(date).toString()
        }

        val chatPartnerId:String
        if(chatMessage.fromId == FirebaseAuth.getInstance().uid){
            chatPartnerId = chatMessage.toId
        }else{
            chatPartnerId = chatMessage.fromId
        }


        when(allow){
            1 -> {
                viewHolder.itemView.new_message_notify.visibility = View.VISIBLE
            }
            0 -> {
                if(viewHolder.itemView.new_message_notify.visibility === View.VISIBLE){
                    viewHolder.itemView.new_message_notify.visibility = View.VISIBLE
                }else{
                    viewHolder.itemView.new_message_notify.visibility = View.GONE
                }
            }
        }

        val reference = FirebaseDatabase.getInstance().getReference("/users/$chatPartnerId")
        reference.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(data: DataSnapshot) {
                chatPartnerUser = data.getValue(User::class.java)
                viewHolder.itemView.latest_message_username.text = chatPartnerUser?.username

                val targetImageView = viewHolder.itemView.latest_message_image_profile
                Picasso.get().load(chatPartnerUser?.profileImageUrl).into(targetImageView)

            }

        })


    }
}