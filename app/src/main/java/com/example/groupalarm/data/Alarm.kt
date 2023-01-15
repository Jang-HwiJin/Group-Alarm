package com.example.groupalarm.data

import com.google.firebase.Timestamp
import kotlin.collections.ArrayList

data class Alarm(
    var title: String = "",
    var owner: String = "",
    var time: Timestamp = Timestamp.now(),
    var dates: ArrayList<Timestamp> = ArrayList(),
    var invitedUsers: ArrayList<String>? = ArrayList(),
    var acceptedUsers: ArrayList<String>? = ArrayList(),
    var isActive: Boolean = true,
    var isRecurring: Boolean = false,
    var recurringDays: ArrayList<String> = ArrayList(),
    var chatId: String = "",
)