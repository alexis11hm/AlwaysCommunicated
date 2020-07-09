package com.example.alwayscommunicated

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.SearchView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.alwayscommunicated.item.UserItem
import com.example.alwayscommunicated.model.User
import com.example.alwayscommunicated.systemservice.Network
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.GroupieViewHolder
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_new_message.*

class NewMessageActivity : AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    var client : GoogleApiClient? = null
    var users = ArrayList<User>()
    var currentUser: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        supportActionBar?.title = "Select User"

        if(Network.thereIsInternetConnection(this)){
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            client = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

            currentUser = FirebaseAuth.getInstance().uid

            newmessage_activity_main.visibility = View.GONE
            newmessage_activity_progress.visibility = View.VISIBLE

            fetchUsers()
        }else{
            Toast.makeText(this, "No internet conecction", Toast.LENGTH_SHORT).show()
            newmessage_activity_progress.visibility = View.GONE
            newmessage_activity_main.visibility = View.GONE
            newmessage_activity_noconnection.visibility = View.VISIBLE
        }
    }

    companion object{
        val USER_KEY = "USER_KEY"
    }

    private fun fetchUsers() {
        val reference = FirebaseDatabase.getInstance().getReference("/users").orderByChild("username")
        val currentUser = FirebaseAuth.getInstance().uid
        reference.addListenerForSingleValueEvent(object: ValueEventListener{

            override fun onDataChange(data: DataSnapshot) {

                data.children.forEach{
                    Log.d("NewMessage",it.toString())
                    val user = it.getValue(User::class.java)
                    users.add(user!!)
                }

                newmessage_activity_main.visibility = View.VISIBLE
                newmessage_activity_progress.visibility = View.GONE

                filterUsers("")

            }



            override fun onCancelled(data: DatabaseError) {

            }
        })

    }

    private fun filterUsers(filter:String){
        val adapter = GroupAdapter<GroupieViewHolder>()

        if(filter.equals("")){
            for(user in users){
                if(!user.uid.equals(currentUser)){
                    adapter.add(UserItem(user))
                }
            }
        }else{
            for (user in users){
                if(user.username.toLowerCase().contains(filter.toLowerCase())){
                    if(!user.uid.equals(currentUser)){
                        adapter.add(UserItem(user))
                    }
                }
            }
        }

        adapter.setOnItemClickListener{item, view ->
                    val userItem = item as UserItem
                    val intent = Intent(view.context,ChatLogActivity::class.java)
                    intent.putExtra(USER_KEY,item.user)
                    startActivity(intent)

                    finish()

                }

        recyclerview_newmessage.setAdapter(adapter)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)

        if(!Network.thereIsInternetConnection(applicationContext)){

            var search = menu!!.findItem(R.id.search)
            var signOut = menu!!.findItem(R.id.menu_sign_out)
            signOut.setVisible(false)
            search.setVisible(false)
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
                filterUsers(search)
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

    override fun onConnectionFailed(p0: ConnectionResult) {

    }
}
