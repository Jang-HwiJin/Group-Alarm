package com.example.groupalarm

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.groupalarm.data.User
import com.example.groupalarm.databinding.ActivityDetailsBinding
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList


class DetailsActivity : AppCompatActivity() {

    lateinit var binding: ActivityDetailsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val intentTitle = intent.getStringExtra("AlarmTitle")
        val intentTime = intent.getLongExtra("AlarmTime", 0)
        val intentOwner = intent.getStringExtra("AlarmOwner")
        val intentUserList = intent.getSerializableExtra("AlarmUserList") as ArrayList<User>


        binding.alarmTitle.text = intentTitle.toString()
        binding.alarmTime.text = convertTimeForDisplay(intentTime)
        binding.alarmOwner.text = intentOwner.toString()


        var userNames: ArrayList<String> = ArrayList()
        for (i in 0 until intentUserList.size) {
            var username = intentUserList.get(i).username

            userNames.add(username)
        }
        binding.alarmUserList.text = userNames.toString().replace("[","").replace("]","")


        binding.backBtn.setOnClickListener {
            finish()
        }

    }

    private fun convertTimeForDisplay(time: Long): String {
        val date = Date(time)
        val format = SimpleDateFormat("hh:mm a")
        return format.format(date)
    }

}