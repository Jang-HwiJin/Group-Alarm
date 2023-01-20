package com.example.groupalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Vibrator
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.example.groupalarm.DashboardActivity.Companion.ALARM_REQUEST_CODE


class AlarmReceiver: BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        // we will use vibrator first
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        vibrator.vibrate(4000)

        Toast.makeText(context, "Alarm! Wake up! Wake up!", Toast.LENGTH_LONG).show()

        val alarmRequestCode = intent.getStringExtra(ALARM_REQUEST_CODE)
        val newIntent = Intent(context, StopAlarmActivity::class.java)
        newIntent.putExtra(ALARM_REQUEST_CODE, alarmRequestCode)
        newIntent.setFlags(FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(newIntent)
    }

}