package group.alarm.groupalarm.data

import com.google.firebase.Timestamp

data class Messages(
    var sender: String = "",
    var text: String = "",
    var timestamp: Timestamp = Timestamp.now(),
    var type: String = "",
    var chatId: String = "",
)