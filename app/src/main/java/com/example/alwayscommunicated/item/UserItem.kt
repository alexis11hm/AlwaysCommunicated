package com.example.alwayscommunicated.item

import androidx.recyclerview.widget.RecyclerView
import com.example.alwayscommunicated.R
import com.example.alwayscommunicated.model.User
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupieViewHolder
import com.xwray.groupie.Item
import kotlinx.android.synthetic.main.user_row_new_message.view.*

class UserItem(val user: User): Item<GroupieViewHolder>() {

    override fun getLayout(): Int {
        return R.layout.user_row_new_message
    }

    override fun bind(viewHolder: GroupieViewHolder, position: Int) {
        //Will be called in our list for each user object later on...
        viewHolder.itemView.username_textview_new_message.text = user.username
        Picasso.get().load(user.profileImageUrl).into(viewHolder.itemView.photo_imageview_new_message)

    }
}