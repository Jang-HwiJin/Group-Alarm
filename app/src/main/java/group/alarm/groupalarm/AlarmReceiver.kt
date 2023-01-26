package group.alarm.groupalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import androidx.annotation.RequiresApi


class AlarmReceiver: BroadcastReceiver() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarmId")

        val newIntent = Intent(context, StopAlarmActivity::class.java)
        newIntent.putExtra("alarmId", alarmId)
        newIntent.setFlags(FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(newIntent)
    }

}