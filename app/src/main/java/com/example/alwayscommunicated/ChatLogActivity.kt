package com.example.alwayscommunicated

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.text.set
import com.example.alwayscommunicated.item.ChatFromItem
import com.example.alwayscommunicated.item.ChatToItem
import com.example.alwayscommunicated.model.ChatMessage
import com.example.alwayscommunicated.model.User
import com.example.alwayscommunicated.systemservice.Network
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_chat_log.*
import kotlinx.android.synthetic.main.activity_new_message.*
import java.text.SimpleDateFormat
import java.util.*

class ChatLogActivity : AppCompatActivity() {

    companion object{
        val TAG = "ChatLog"
    }

    val adapter = GroupAdapter<GroupieViewHolder>()
    var toUser:User? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_log)

        if(Network.thereIsInternetConnection(this)){
            recyclerview_chat_log.setAdapter(adapter)

            toUser = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
            supportActionBar?.title = toUser?.username
            supportActionBar?.setDisplayHomeAsUpEnabled(true)

            //setupDummyData()
            listenForMessage()

            send_button_chat_log.setOnClickListener{
                Log.d(TAG,"Attempt to send message...")
                performSendMessage()
            }
        }else{
            chat_activity_main.visibility = View.GONE
            chat_activity_noconnection.visibility = View.VISIBLE
        }


    }

    private fun listenForMessage() {
        val fromId = FirebaseAuth.getInstance().uid
        val toId = toUser?.uid
        val reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId")
        reference.addChildEventListener(object: ChildEventListener{

            override fun onChildAdded(data: DataSnapshot, p1: String?) {
                val chatMessage = data.getValue(ChatMessage::class.java)
                if(chatMessage != null){
                    Log.d(TAG, chatMessage.text.toString())

                    if(chatMessage.fromId == FirebaseAuth.getInstance().uid){
                        val currentUser = LatestMessagesActivity.currentUser ?: return
                        adapter.add(ChatFromItem(chatMessage, currentUser))
                    }else{
                        if(toUser != null){
                            adapter.add(ChatToItem(chatMessage, toUser!!))
                        }
                    }
                }
                recyclerview_chat_log.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {

            }

            override fun onChildRemoved(p0: DataSnapshot) {

            }
        })
    }

    private fun performSendMessage() {
        //how do we actually send a message to firebase
        val text = edittext_chat_log.text.toString()
        val fromId = FirebaseAuth.getInstance().uid
        val user = intent.getParcelableExtra<User>(NewMessageActivity.USER_KEY)
        val toId = user?.uid
        val date = System.currentTimeMillis()

        if(fromId == null || toId == null) return


        //var reference = FirebaseDatabase.getInstance().getReference("/messages").push()
        var reference = FirebaseDatabase.getInstance().getReference("/user-messages/$fromId/$toId").push()

        var toReference = FirebaseDatabase.getInstance().getReference("/user-messages/$toId/$fromId").push()

        val chatMessage = ChatMessage(reference.key!!, text, fromId, toId,date)
        reference.setValue(chatMessage)
            .addOnSuccessListener {
                Toast.makeText(this,"Saved our chat message: ${reference.key}",Toast.LENGTH_SHORT).show()
                edittext_chat_log.text.clear()
                recyclerview_chat_log.scrollToPosition(adapter.itemCount-1)

            }
            .addOnFailureListener{
                Toast.makeText(this,"Failed to saved our chat message",Toast.LENGTH_SHORT).show()
            }

        toReference.setValue(chatMessage)

        val latestMessageReference = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId/$toId")
        latestMessageReference.setValue(chatMessage)

        val latestMessageToReference = FirebaseDatabase.getInstance().getReference("/latest-messages/$toId/$fromId")
        latestMessageToReference.setValue(chatMessage)
    }


}
