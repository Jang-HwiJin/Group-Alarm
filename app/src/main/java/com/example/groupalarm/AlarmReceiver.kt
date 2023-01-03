package com.example.groupalarm

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.DialogFragment
import com.example.groupalarm.ScrollingActivity.Companion.ALARM_REQUEST_CODE


class AlarmReceiver: BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        // we will use vibrator first

        Toast.makeText(context, "Alarm! Wake up! Wake up!", Toast.LENGTH_LONG).show()

        val alarmRequestCode = intent.getStringExtra(ALARM_REQUEST_CODE)
        val newIntent = Intent(context, StopAlarmActivity::class.java)
        newIntent.putExtra(ALARM_REQUEST_CODE, alarmRequestCode)
        newIntent.setFlags(FLAG_ACTIVITY_NEW_TASK)

        context.startActivity(newIntent)
    }

}