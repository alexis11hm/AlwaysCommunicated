package com.example.alwayscommunicated

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import kotlinx.android.synthetic.main.activity_register.*
import java.util.*
import com.example.alwayscommunicated.model.User
import com.example.alwayscommunicated.systemservice.Network
import com.google.firebase.database.FirebaseDatabase
import kotlinx.android.synthetic.main.activity_login.*


class RegisterActivity : AppCompatActivity() {

    var REQUEST_CODE_PERMISSION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        supportActionBar?.hide()

        if(Network.thereIsInternetConnection(this)){
            var version = Build.VERSION.SDK_INT


            register_button.setOnClickListener{
                performRegister()
            }



            have_account_text.setOnClickListener{
                Log.d("Mensaje","Try to show login Activity")
                finish()
            }




            select_photo_register.setOnClickListener{
                if(version >= Build.VERSION_CODES.M){
                    if(verifyPermission()){
                        openGallery()
                    }else{
                        requestPermissionsUser()
                    }
                }else {
                    openGallery()
                }
            }

        }else{
            register_activity_progress.visibility = View.GONE
            register_activity_main.visibility = View.GONE
            register_activity_noconnection.visibility = View.VISIBLE
        }

    }

    private fun verifyPermission(): Boolean{
        return (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestPermissionsUser() {
        //Checking permissions
        if(ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            //Explain because we request the permissions
            if(ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                Toast.makeText(applicationContext,"We need you grant the permission for access your photo",Toast.LENGTH_LONG).show()
            }
            //Request Permission
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE_PERMISSION)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when(requestCode){
            REQUEST_CODE_PERMISSION -> {
                for((index, value) in (grantResults.withIndex())){
                    if(value==PackageManager.PERMISSION_GRANTED){
                        Log.d("Mensaje","The permission ${permissions[index]} was granted ")
                        openGallery()
                    }else{
                        Log.d("Mensaje","The permission ${permissions[index]} was refused ")
                    }
                }
            }
        }
    }

    private fun openGallery(){
        Log.d("RegisterActivity","Try show photo selector")
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 0)
    }

    var selectedPhotoUri: Uri? = null

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if(requestCode == 0 && resultCode == Activity.RESULT_OK && data != null){
            //proceed and check what selected image was...
            Log.d("RegisterActivity", "Photo was selected")
            selectedPhotoUri = data.data
            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, selectedPhotoUri);

            select_imageview_register.setImageBitmap(bitmap)
            select_photo_register.alpha = 0f
            //val bitmapDrawable = BitmapDrawable(bitmap)
            //select_photo_register.setBackgroundDrawable(bitmapDrawable)
        }

    }



    private fun performRegister(){

        register_activity_main.visibility = View.GONE
        register_activity_progress.visibility = View.VISIBLE

        val username = username_register.text.toString()
        val email = email_register.text.toString()
        val password = password_register.text.toString()

        Log.d("Mensaje","User: ${username}")
        Log.d("Mensaje","Email: ${email}")
        Log.d("Mensaje","Password: ${password}")

        if(!email.isEmpty() && !password.isEmpty() && !username.isEmpty() && selectedPhotoUri!=null){
            val auth = FirebaseAuth.getInstance()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("crear", "createUserWithEmail:success")
                        val user = auth.currentUser
                        Toast.makeText(this, "Authentication Successfully, curret user is ${task.result!!.user!!.uid}.", Toast.LENGTH_SHORT).show()

                        uploadImageToFirebaseStorage()

                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w("crear", "createUserWithEmail:failure", task.exception)
                        Toast.makeText(this, "Authentication failed.", Toast.LENGTH_SHORT).show()
                        register_activity_main.visibility = View.VISIBLE
                        register_activity_progress.visibility = View.GONE

                    }

                }.addOnFailureListener{
                    Log.d("error","Failed to create user: ${it.message}")
                Toast.makeText(this, "Failed to create user: ${it.message}", Toast.LENGTH_SHORT).show()
                    register_activity_main.visibility = View.VISIBLE
                    register_activity_progress.visibility = View.GONE
                }
        }else{
            Toast.makeText(this, "No puede haber campos vacios", Toast.LENGTH_SHORT).show()
            register_activity_main.visibility = View.VISIBLE
            register_activity_progress.visibility = View.GONE
        }
    }

    private fun uploadImageToFirebaseStorage() {
        if(selectedPhotoUri == null) return

        val filename = UUID.randomUUID().toString()
        val reference = FirebaseStorage.getInstance().getReference("/images/$filename")
        reference.putFile(selectedPhotoUri!!)
            .addOnFailureListener {
                Toast.makeText(this, "Failed to upload photo: ${it.message}", Toast.LENGTH_SHORT).show()
                //do some logging here
                register_activity_main.visibility = View.VISIBLE
                register_activity_progress.visibility = View.GONE
            }.addOnSuccessListener {
                Toast.makeText(this, "Success to upload photo: ${it.metadata?.path}", Toast.LENGTH_SHORT).show()

                reference.downloadUrl.addOnSuccessListener {
                    it.toString()
                    Toast.makeText(this, "File Location: ${it}", Toast.LENGTH_SHORT).show()
                    saveUserToFirebaseDatabase(it.toString())
                }
            }

    }

    private fun saveUserToFirebaseDatabase(profileImageUrl: String) {
        val uid = FirebaseAuth.getInstance().uid ?: ""
        val reference = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val user = User(uid, username_register.text.toString(), profileImageUrl )
        reference.setValue(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Finally we saved the user to Firebase Database", Toast.LENGTH_SHORT).show()

                register_activity_main.visibility = View.VISIBLE
                register_activity_progress.visibility = View.GONE

                val intent = Intent(this, LatestMessagesActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)

            }.addOnFailureListener{
                Toast.makeText(this, "It was not saved user in Firebase Database", Toast.LENGTH_SHORT).show()
                register_activity_main.visibility = View.VISIBLE
                register_activity_progress.visibility = View.GONE
            }


    }
}


