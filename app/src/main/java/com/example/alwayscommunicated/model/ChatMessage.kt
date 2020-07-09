package com.example.alwayscommunicated.model


class ChatMessage(val id:String,val text:String,val fromId:String, val toId:String, val date:Long) {

    constructor() : this("","","","",-1)


}