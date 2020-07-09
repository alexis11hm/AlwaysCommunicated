package com.example.alwayscommunicated

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.example.alwayscommunicated.alertdialog.AlertDialogResetPassword
import com.example.alwayscommunicated.model.User
import com.example.alwayscommunicated.systemservice.Network
import com.google.android.gms.auth.api.Auth
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInResult
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.common.api.OptionalPendingResult
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FacebookAuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import kotlinx.android.synthetic.main.activity_new_message.*
import java.security.MessageDigest


class LoginActivity: AppCompatActivity(), GoogleApiClient.OnConnectionFailedListener {

    var client : GoogleApiClient? = null
    val SIGN_IN_GOOGLE_CODE = 1000
    var firebaseAuth : FirebaseAuth? = null
    var firebaseAuthListener : FirebaseAuth.AuthStateListener? = null

    companion object{
        var NORMAL_LOGIN = 1
        var GOOGLE_LOGIN = 2
        var FACEBOOK_LOGIN = 3
        var TYPE_LOGIN = 0
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        supportActionBar?.hide()

        if(Network.thereIsInternetConnection(this)){

            val sharedPref = this?.getPreferences(Context.MODE_PRIVATE) ?: return
            login_email.setText(sharedPref.getString(getString(R.string.email_login_remember), ""))
            login_password.setText(sharedPref.getString(getString(R.string.password_login_remember), ""))


            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build()

            client = GoogleApiClient.Builder(this)
                .enableAutoManage(this, this)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build()

            firebaseAuth = FirebaseAuth.getInstance()
            firebaseAuthListener = FirebaseAuth.AuthStateListener{

            }
            firebaseAuthListener = FirebaseAuth.AuthStateListener(object : FirebaseAuth.AuthStateListener, (FirebaseAuth) -> Unit {

                override fun onAuthStateChanged(firebaseAuth: FirebaseAuth) {
                    val user = firebaseAuth!!.currentUser
                    if(user != null){
                    }else{
                    }
                }
                override fun invoke(p1: FirebaseAuth) {}

            })

            login_button.setOnClickListener{
                var email = login_email.text.toString()
                var password = login_password.text.toString()

                if(!email.isEmpty() && !password.isEmpty()){
                    login()
                }else{
                    Toast.makeText(this, "Some fields are emptys.", Toast.LENGTH_SHORT).show()
                }
            }

            button_google_auth.setOnClickListener {
                val intent = Auth.GoogleSignInApi.getSignInIntent(client)
                startActivityForResult(intent, SIGN_IN_GOOGLE_CODE)

            }

            create_account_view.setOnClickListener {
                val intent = Intent(this,RegisterActivity::class.java)
                startActivity(intent)
            }

            forgot_password_login.setOnClickListener{
                val dialog = AlertDialogResetPassword()
                dialog.show(supportFragmentManager,"ResetPassword")
                Toast.makeText(this,"Me hiciste click", Toast.LENGTH_SHORT).show()
            }

        }else{
            login_activity_progress.visibility = View.GONE
            login_activity_main.visibility = View.GONE
            login_activity_noconnection.visibility = View.VISIBLE
        }

    }




    private fun goLatestMessagesScreen() {
        Toast.makeText(this, "Authentication Successfully from API.", Toast.LENGTH_SHORT).show()
        LoginActivity.TYPE_LOGIN = GOOGLE_LOGIN
        val intent = Intent(this, LatestMessagesActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK).or(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(intent)
    }


    override fun onStart() {
        super.onStart()
        if(Network.thereIsInternetConnection(this)){
            firebaseAuth!!.addAuthStateListener(firebaseAuthListener!!)
        }
    }

    override fun onStop() {
        super.onStop()
        if(firebaseAuthListener != null){
            firebaseAuth!!.removeAuthStateListener(firebaseAuthListener!!)
        }
    }
    override fun onConnectionFailed(p0: ConnectionResult) {

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode){
            SIGN_IN_GOOGLE_CODE ->{
                val result = Auth.GoogleSignInApi.getSignInResultFromIntent(data)
                handleSignInResult(result)
            }
        }
    }

    private fun handleSignInResult(result: GoogleSignInResult?) {
        if(result!!.isSuccess){
            result.signInAccount?.let { firebaseAuthWithGoogle(it) }
        }else{
            Toast.makeText(this, "You could not log in", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(signInAccount : GoogleSignInAccount) {
        login_activity_progress.visibility = View.VISIBLE
        login_activity_main.visibility = View.GONE

        val credential = GoogleAuthProvider.getCredential(signInAccount.idToken, null)
        firebaseAuth!!.signInWithCredential(credential).addOnCompleteListener{
            login_activity_progress.visibility = View.GONE
            login_activity_main.visibility = View.VISIBLE

            if(!it.isSuccessful) {
                Toast.makeText(this, "no autenticaso bro", Toast.LENGTH_SHORT).show()
            }else{
                Toast.makeText(this, "Autenticado con credenciales", Toast.LENGTH_SHORT).show()
                performRegister()
                goLatestMessagesScreen()
            }
        }
    }

    private fun performRegister(){
        lateinit var username:String
        lateinit var email:String
        lateinit var photo : String

            var opr = Auth.GoogleSignInApi.silentSignIn(client)
            if(opr.isDone()) {
                var result = opr.get()
                if(result.isSuccess) {
                    var account = result.signInAccount
                    username = account!!.displayName!!
                    photo = account!!.photoUrl!!.toString()
                    var reference = FirebaseDatabase.getInstance().getReference("/users/$username")
                    Toast.makeText(this, reference.toString(), Toast.LENGTH_SHORT).show()
                    if(reference != null) {
                        saveUserToFirebaseDatabase(photo, username)
                    }
                }
            }
    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String, username:String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val reference = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val user = User(uid, username , profileImageUrl )
        reference.setValue(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Finally we saved the user to Firebase Database", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, LatestMessagesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)

            }.addOnFailureListener{
                Toast.makeText(this, "It was not saved user in Firebase Database", Toast.LENGTH_SHORT).show()
            }


    }

    private fun login(){
        val email = login_email.text.toString()
        val password = login_password.text.toString()

        Log.d("login","Attempt login with email/pw: $email/***")
        login_activity_progress.visibility = View.VISIBLE
        login_activity_main.visibility = View.GONE
        var auth = FirebaseAuth.getInstance()
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                // Sign in success, update UI with the signed-in user's information
                Log.d("login", "signInWithEmail:success")
                val user = auth.currentUser
                Toast.makeText(this, "Authentication Successfully.", Toast.LENGTH_SHORT).show()

                login_activity_progress.visibility = View.GONE
                login_activity_main.visibility = View.VISIBLE

                LoginActivity.TYPE_LOGIN = NORMAL_LOGIN

                if(checkbox_rember_me_login.isChecked){
                    val sharedPref = this?.getPreferences(Context.MODE_PRIVATE)
                    with (sharedPref.edit()) {
                        putString(getString(R.string.email_login_remember), email)
                        putString(getString(R.string.password_login_remember), password)
                        commit()
                    }
                }

                val intent = Intent(this, LatestMessagesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
            } else {
                // If sign in fails, display a message to the user.
                Log.w("login", "signInWithEmail:failure", task.exception)
                Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                login_activity_progress.visibility = View.GONE
                login_activity_main.visibility = View.VISIBLE
                // ...
            }

        }.addOnFailureListener{
            Log.d("error","Failed to authenticate user: ${it.message}")
            Toast.makeText(this, "Failed to authenticate user: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }



}