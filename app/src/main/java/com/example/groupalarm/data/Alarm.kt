package com.example.groupalarm.data

import kotlin.collections.ArrayList

data class Alarm(
    var title: String = "",
    var time: Long = 0,
    var isActive: Boolean = true,
    var users: ArrayList<User> = ArrayList(),
    var owner: String = "",

)