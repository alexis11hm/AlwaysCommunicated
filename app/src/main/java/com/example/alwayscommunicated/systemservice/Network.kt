package com.example.alwayscommunicated.systemservice

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo

class Network {

    companion object{
        fun thereIsInternetConnection(context : Context) : Boolean{
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
            return activeNetwork != null && activeNetwork?.isConnectedOrConnecting!!
        }
    }
}