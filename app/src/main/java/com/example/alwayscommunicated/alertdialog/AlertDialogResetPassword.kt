package com.example.alwayscommunicated.alertdialog

import android.app.AlertDialog
import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.alwayscommunicated.ChatLogActivity.Companion.TAG
import com.example.alwayscommunicated.R
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.dialog_send_password_reset_email.*

class AlertDialogResetPassword : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return activity?.let {
            val builder = AlertDialog.Builder(it)
            // Get the layout inflater
            val inflater = requireActivity().layoutInflater;
            // Inflate and set the layout for the dialog
            // Pass null as the parent view because its going in the dialog layout
            val view = inflater.inflate(R.layout.dialog_send_password_reset_email, null)
            val EmailResetPassword = view.findViewById<EditText>(R.id.email_address_reset_password)
            builder.setView(view)
                    builder
                // Add action buttons
                .setPositiveButton("Send",
                    DialogInterface.OnClickListener { dialog, id ->
                       if(EmailResetPassword != null){

                           var email = EmailResetPassword.text.toString()
                           if(!email.isEmpty()){
                               sendPasswordResetEmail(email)
                               Log.d("AlertDialog", email)
                           }
                       }
                    })
                .setNegativeButton("Cancel",
                    DialogInterface.OnClickListener { dialog, id ->
                        getDialog()!!.cancel()
                    })
            builder.create()
        } ?: throw IllegalStateException("Activity cannot be null")
    }

    private fun sendPasswordResetEmail(emailAddress : String){
        val auth = FirebaseAuth.getInstance()

        auth.sendPasswordResetEmail(emailAddress)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("AlertDialog", "Email sent.")
                }
            }
    }

}