package com.example.groupalarm.data

data class AlarmInvitations(
    var id: String = "",
    var userId: String = "",
    var alarmId: Boolean = true,
    var status: String = "pending",
    var owner: String = "",
)