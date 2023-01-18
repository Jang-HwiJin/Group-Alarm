package com.example.groupalarm.data

import com.google.firebase.Timestamp
import java.util.*
import kotlin.collections.ArrayList

data class Messages(
    var sender: String = "",
    var text: String = "",
    var timestamp: Timestamp = Timestamp.now(),
    var type: String = "",
    var chatId: String = "",
)