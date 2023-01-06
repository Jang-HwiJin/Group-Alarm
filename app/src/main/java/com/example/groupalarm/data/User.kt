package com.example.groupalarm.data

data class User(
    var username: String = "",
    var email: String = "",
    var displayName: String ="",
    var profileImg: String = "",
    var friends: ArrayList<User> = ArrayList(),
): java.io.Serializable