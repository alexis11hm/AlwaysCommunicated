package com.example.alwayscommunicated

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import com.example.alwayscommunicated.item.LatestMessageItem
import com.example.alwayscommunicated.model.ChatMessage
import com.example.alwayscommunicated.model.User
import com.example.alwayscommunicated.notification.NewMessageNotification
import com.example.alwayscommunicated.systemservice.Network
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.squareup.picasso.Picasso
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_latest_messages.*
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.latest_messages_row.*
import kotlinx.android.synthetic.main.latest_messages_row.view.*
import kotlinx.android.synthetic.main.latest_messages_row.view.new_message_notify

class LatestMessagesActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    var client : GoogleApiClient? = null
    var chatsCounter = 0L
    var peopleCounter = 0L
    var view : View? = null
    var fromUser : User? = null
    var chatPartnerUser : User? = null
    var users = ArrayList<User>()

    companion object{
        var currentUser : User? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)

        supportActionBar?.title = "Chats"

        if(Network.thereIsInternetConnection(this)){

            view = findViewById<View>(android.R.id.content).rootView
            //findViewById(android.R.id.content).getRootView()


            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            client = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

            recyclerview_latest_messages.setAdapter(adapter)
            recyclerview_latest_messages.addItemDecoration(DividerItemDecoration(this, DividerItemDecoration.VERTICAL))

            //set item click listener on your adapter

            adapter.setOnItemClickListener{item, view ->
                Log.d("LatestMessagesActivity", "123")
                val intent = Intent(this, ChatLogActivity::class.java)
                //we are missing the chat partner user
                val row = item as LatestMessageItem
                new_message_notify.visibility = View.GONE
                intent.putExtra(NewMessageActivity.USER_KEY, row.chatPartnerUser)
                startActivity(intent)
            }


            fetchCurrentUser()
            verifyUserIsLoggedIn()



            val BottomNavigation: BottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_latest_messages)

            var badgeChats = BottomNavigation.getOrCreateBadge(R.id.chats_lastest_messages)
            badgeChats.isVisible = true
            // An icon only badge will be displayed unless a number is set:
            badgeChats.number = 0

            listenForLatestMessages(badgeChats)

            var badgePeople = BottomNavigation.getOrCreateBadge(R.id.people_lastest_messages)
            badgePeople.isVisible = true
            // An icon only badge will be displayed unless a number is set:
            badgePeople.number = 0
            fetchNumberUsers(badgePeople)


            var bmv = BottomNavigationView.OnNavigationItemSelectedListener() {item ->
                when(item.itemId){
                    R.id.chats_lastest_messages -> {
                        true
                    }
                    R.id.people_lastest_messages -> {
                        val intent = Intent(this,NewMessageActivity::class.java)
                        startActivity(intent)
                        true
                    }
                    else -> false
                }
            }

            BottomNavigation.setOnNavigationItemSelectedListener(bmv)

        }else{
            Toast.makeText(this, "No internet conecction", Toast.LENGTH_SHORT).show()
            latestm_activity_main.visibility = View.GONE
            latestm_activity_noconnection.visibility = View.VISIBLE
        }

    }

    val latestMessageMap = HashMap<String, ChatMessage>()
    val adapter = GroupAdapter<GroupieViewHolder>()

    private fun refreshRecyclerViewMessages(allow : Int, key : String){
        adapter.clear()
        var counter = 0
        latestMessageMap.values.forEach{
            if(latestMessageMap.get(key)!!.id.equals(it.id)) {
                adapter.add(LatestMessageItem(it, allow))
            }else{
                adapter.add(LatestMessageItem(it, 0))
            }
            counter++
        }
    }

    private fun listenForLatestMessages(badge : BadgeDrawable) {
        val fromId = FirebaseAuth.getInstance().uid
        val reference = FirebaseDatabase.getInstance().getReference("/latest-messages/$fromId")
        reference.addChildEventListener(object: ChildEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {
            }

            override fun onChildChanged(data: DataSnapshot, p1: String?) {
                val chatMessage = data.getValue(ChatMessage::class.java) ?: return
                latestMessageMap[data.key!!] = chatMessage
                chatsCounter++
                badge.number = chatsCounter.toInt()
                if(chatMessage.fromId != FirebaseAuth.getInstance().uid){
                    callNotification(chatMessage)
                }
                refreshRecyclerViewMessages(1, data.key.toString())


            }

            override fun onChildAdded(data: DataSnapshot, p1: String?) {
                val chatMessage = data.getValue(ChatMessage::class.java) ?: return
                var cUser = FirebaseAuth.getInstance().uid
                Log.d("EXIT",cUser+" == "+chatMessage.fromId)
                if(!cUser.equals(chatMessage.fromId)){
                    latestMessageMap[data.key!!] = chatMessage
                    chatsCounter++
                    badge.number = chatsCounter.toInt()
                    refreshRecyclerViewMessages(0, data.key.toString())
                }else{
                    if(!cUser.equals(chatMessage.toId)){
                        latestMessageMap[data.key!!] = chatMessage
                        chatsCounter++
                        badge.number = chatsCounter.toInt()
                        refreshRecyclerViewMessages(0, data.key.toString())
                    }
                }
            }

            override fun onChildRemoved(p0: DataSnapshot) {
            }

        })


    }

    private fun callNotification(chatMessage : ChatMessage){

        var referenceTo = FirebaseDatabase.getInstance().getReference("/users/${chatMessage.fromId}")
        referenceTo.addListenerForSingleValueEvent(object: ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {

            }

            override fun onDataChange(data: DataSnapshot) {
                chatPartnerUser = data.getValue(User::class.java)
                showNotification(chatMessage, chatPartnerUser!!)
            }

        })
    }

    private fun showNotification(chatMessage: ChatMessage, chatPartnerUser : User){
        var reference = FirebaseDatabase.getInstance().getReference("/users/${chatMessage.fromId}")
        reference.addValueEventListener(object : ValueEventListener{
            override fun onCancelled(p0: DatabaseError) {
            }

            override fun onDataChange(data: DataSnapshot) {
                fromUser = data.getValue(User::class.java)
                NewMessageNotification().showNotification(view!!, fromUser!!, chatMessage.text,
                    chatPartnerUser!!)

            }

        })
    }

    private fun fetchCurrentUser() {
        val uid = FirebaseAuth.getInstance().uid
        val reference = FirebaseDatabase.getInstance().getReference("/users/$uid")
        reference.addListenerForSingleValueEvent(object: ValueEventListener{

            override fun onDataChange(data: DataSnapshot) {
                currentUser = data.getValue(User::class.java)
                Log.d("LatestMessages", "Current User: ${currentUser?.profileImageUrl}")
            }

            override fun onCancelled(data: DatabaseError) {

            }
        })
    }

    private fun verifyUserIsLoggedIn() {
        val uid = FirebaseAuth.getInstance().uid
        if(uid == null){
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
    }

    override fun onConnectionFailed(p0: ConnectionResult) {

    }

    private fun fetchNumberUsers(badge: BadgeDrawable) {
        val reference = FirebaseDatabase.getInstance().getReference("/users")
        val currentUser = FirebaseAuth.getInstance().uid
        reference.addListenerForSingleValueEvent(object: ValueEventListener{

            override fun onDataChange(data: DataSnapshot) {
                data.children.forEach{
                    peopleCounter++
                    badge.number = peopleCounter.toInt()
                }
            }
            override fun onCancelled(p0: DatabaseError) {

            }
        })
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)

        var item = menu!!.findItem(R.id.search)
        item.setVisible(false)

        if(!Network.thereIsInternetConnection(applicationContext)){
            var signOut = menu!!.findItem(R.id.menu_sign_out)
            signOut.setVisible(false)
        }

        var manager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        var searchItem = menu?.findItem(R.id.search)
        var searchView = searchItem?.actionView as SearchView

        searchView.setSearchableInfo(manager.getSearchableInfo(componentName))

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(search: String?): Boolean {
                Log.d("Filtro", search!!)
                //filterUsers(search)
                return true
            }

        })

        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item?.itemId){
            R.id.menu_sign_out->{
                FirebaseAuth.getInstance().signOut()
                when(LoginActivity.TYPE_LOGIN){
                    LoginActivity.NORMAL_LOGIN -> {
                        left()
                    }
                    LoginActivity.GOOGLE_LOGIN -> {
                        Auth.GoogleSignInApi.revokeAccess(client).setResultCallback {
                            left()
                        }
                    }
                    else ->{
                        Auth.GoogleSignInApi.revokeAccess(client).setResultCallback {
                            left()
                        }
                    }
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    fun left(){
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK).or(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
        Log.d("LEFT", "Saliendo...")
    }


}
