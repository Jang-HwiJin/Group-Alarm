package com.example.groupalarm.data

import com.google.firebase.Timestamp
import java.util.*
import kotlin.collections.ArrayList

data class Chats(
    var users: ArrayList<String> = ArrayList(),
    var messages: ArrayList<Objects> = ArrayList(),
    var lastMessageTimestamp: Timestamp,
    var alarmId: String,
    )