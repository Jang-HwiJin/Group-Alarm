package com.example.groupalarm.data

data class User(
    var username: String = "",
    var email: String = "",
    var displayName: String ="",
    var profileImg: String = "",
    var isVerified: Boolean = false,
    var alarms: ArrayList<String> = ArrayList(),
    var activeAlarms: ArrayList<String> = ArrayList(),
    )