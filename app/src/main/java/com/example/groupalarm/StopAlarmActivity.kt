package com.example.groupalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.groupalarm.data.Alarm
import com.example.groupalarm.databinding.ActivityStopAlarmBinding
import com.google.firebase.firestore.FirebaseFirestore


class StopAlarmActivity : AppCompatActivity() {

    lateinit var binding: ActivityStopAlarmBinding

    val firestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStopAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val alarmId = intent.getStringExtra("alarmId")


        var alarmUri: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        if (alarmUri == null) {
            alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        // setting default ringtone
        val ringtone = RingtoneManager.getRingtone(this, alarmUri)

        // we will use vibrator first
        val vibrator = this.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        val pattern = longArrayOf(0, 500, 1000)
        // 0 meaning is repeat indefinitely
        vibrator.vibrate(pattern, 0)

        Toast.makeText(this, "Alarm! Wake up! Wake up!", Toast.LENGTH_LONG).show()

        // play ringtone
        ringtone.play()

        if (alarmId != null) {
            firestore.collection("alarms").document(alarmId)
                .get().addOnSuccessListener { alarmDoc ->
                    val alarm = alarmDoc.toObject(Alarm::class.java)
                    if(alarm != null) {
                        binding.alarmTitle.text = alarm.title
                    }

                }
        }


        // Bring user back to app
        binding.btnBackToApp.setOnClickListener {
            val newIntent = Intent(applicationContext, AlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(applicationContext, alarmId.hashCode(), newIntent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

            alarmManager.cancel(pendingIntent)
            Toast.makeText(this, getString(R.string.alarmTurnedOff), Toast.LENGTH_LONG).show()
            vibrator.cancel()
            ringtone.stop()

            var pendingIntentToBeRemoved = DashboardActivity.alarmIntents[alarmId]
            if (pendingIntentToBeRemoved != null) {
                alarmManager.cancel(pendingIntentToBeRemoved)
            }

            val intent = Intent(this, DashboardActivity::class.java)
            newIntent.putExtra("alarmId", alarmId)
            startActivity(Intent(intent))

        }

    }
}