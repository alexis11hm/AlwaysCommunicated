package com.example.alwayscommunicated.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.View
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.alwayscommunicated.ChatLogActivity
import com.example.alwayscommunicated.NewMessageActivity
import com.example.alwayscommunicated.R
import com.example.alwayscommunicated.item.LatestMessageItem
import com.example.alwayscommunicated.model.User
import com.squareup.picasso.Picasso
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL


class NewMessageNotification {

    private var CHANNEL_ID : String = "Message"
    private var TAG_NOTIFICATION : String = "Notification"
    private var bitmap: Bitmap? = null

    fun showNotification(view : View, fromUser: User, message : String, chatPartnerUser : User){
        val sonido = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) as Uri

        var idIcono:Int = when(view.tag){
            TAG_NOTIFICATION->{
                R.drawable.ic_notification
            }
            else->{
                R.mipmap.ic_launcher
            }
        }

        val intent = Intent(view.context, ChatLogActivity::class.java)
        intent.putExtra(NewMessageActivity.USER_KEY, chatPartnerUser)

        val pendingIntent: PendingIntent = PendingIntent.getActivity(view.context, 0,  intent,PendingIntent.FLAG_UPDATE_CURRENT)

        val thread = Thread(Runnable {
            try {
                bitmap = getBitmapFromURL(fromUser.profileImageUrl)
                var builder = NotificationCompat.Builder(view.context,CHANNEL_ID)
                    .setSmallIcon(idIcono)
                    .setContentTitle(fromUser.username)
                    .setContentText(message)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setSound(sonido)
                    .setVibrate(longArrayOf(100,200,100))
                    .setLargeIcon(bitmap)
                    .setLights(Color.BLUE,1,1)
                    .setContentIntent(pendingIntent)
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(message))



                if(Build.VERSION.SDK_INT>= Build.VERSION_CODES.LOLLIPOP){
                    builder.color = ContextCompat.getColor(view.context,R.color.colorPrimary)
                }

                createNotificationChannel(view)

                with(NotificationManagerCompat.from(view.context)){
                    notify(idIcono,builder.build())
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })

        thread.start()



    }

    private fun createNotificationChannel(view:View){
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O){
            val name:String = "Message"
            val descriptionN:String = "Notifications of Messages"
            val importance:Int = NotificationManager.IMPORTANCE_HIGH
            val channel: NotificationChannel = NotificationChannel(CHANNEL_ID,name,importance)
                .apply {
                    description = descriptionN
                }
            val notificationManager: NotificationManager = view.context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

    }

    private fun getBitmapFromURL(strURL: String?): Bitmap? {
        var bitmap : Bitmap? = null
        try {
            val url = URL(strURL)
            val connection: HttpURLConnection = url.openConnection() as HttpURLConnection
            connection.setDoInput(true)
            connection.connect()
            val input: InputStream = connection.getInputStream()
            bitmap = BitmapFactory.decodeStream(input)
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
        return bitmap
    }

}