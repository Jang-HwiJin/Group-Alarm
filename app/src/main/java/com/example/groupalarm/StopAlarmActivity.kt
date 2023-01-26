package com.example.groupalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.example.groupalarm.data.Alarm
import com.example.groupalarm.databinding.ActivityStopAlarmBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*


class StopAlarmActivity : AppCompatActivity() {

    lateinit var binding: ActivityStopAlarmBinding

    val firestore = FirebaseFirestore.getInstance()

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStopAlarmBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val alarmId = intent.getStringExtra("alarmId")
        var alarmIdHash = alarmId.hashCode()

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

        val dayOfWeekMap = mapOf(
            "M" to java.util.Calendar.MONDAY,
            "T" to java.util.Calendar.TUESDAY,
            "W" to java.util.Calendar.WEDNESDAY,
            "Th" to java.util.Calendar.THURSDAY,
            "F" to java.util.Calendar.FRIDAY,
            "Sa" to java.util.Calendar.SATURDAY,
            "Su" to java.util.Calendar.SUNDAY
        )

        val newIntent = Intent(applicationContext, AlarmReceiver::class.java)

        if (alarmId != null) {
            firestore.collection("alarms").document(alarmId)
                .get().addOnSuccessListener { alarmDoc ->
                    val alarm = alarmDoc.toObject(Alarm::class.java)
                    if(alarm != null) {
                        binding.alarmTitle.text = alarm.title
//                        if(alarm.isRecurring) {
//                            val calendar = Calendar.getInstance()
//                            calendar.time = Date()
//                            for(day in alarm.recurringDays) {
//                                alarmIdHash = alarmId.hashCode()
//                                if(day == "M" && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
//                                    alarmIdHash += 1
//                                }
//                                else if(day == "T" && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY) {
//                                    alarmIdHash += 2
//                                }
//                                else if(day == "W" && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY) {
//                                    alarmIdHash += 3
//                                }
//                                else if(day == "Th" && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY) {
//                                    alarmIdHash += 4
//                                }
//                                else if(day == "F" && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
//                                    alarmIdHash += 5
//                                }
//                                else if(day == "Sa" && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
//                                    alarmIdHash += 6
//                                }
//                                else if(day == "Su" && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
//                                    alarmIdHash += 7
//                                }
//                            }
//                        }
                    }

                }
        }


        // Bring user back to app
        binding.btnBackToApp.setOnClickListener {
            if (alarmId != null) {
                firestore.collection("alarms").document(alarmId)
                    .get().addOnSuccessListener { alarmDoc ->
                        val alarm = alarmDoc.toObject(Alarm::class.java)
                        if(alarm != null) {
                            if(alarm.isRecurring) {
                                val calendar = Calendar.getInstance()
                                calendar.time = Date()
                                alarmIdHash = alarmId.hashCode()

                                if(alarm.recurringDays.contains("M") && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
                                    alarmIdHash += 1
                                }
                                else if(alarm.recurringDays.contains("T") && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY) {
                                    alarmIdHash += 2
                                }
                                else if(alarm.recurringDays.contains("W") && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY) {
                                    alarmIdHash += 3
                                }
                                else if(alarm.recurringDays.contains("Th") && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY) {
                                    alarmIdHash += 4
                                }
                                else if(alarm.recurringDays.contains("F") && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                                    alarmIdHash += 5
                                }
                                else if(alarm.recurringDays.contains("Sa") && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                                    alarmIdHash += 6
                                }
                                else if(alarm.recurringDays.contains("Su") && calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                                    alarmIdHash += 7
                                }

                                val pendingIntent = PendingIntent.getBroadcast(applicationContext, alarmIdHash, newIntent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
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
//                                newIntent.putExtra("alarmId", alarmId)
                                startActivity(Intent(intent))

                            } else {
                                val pendingIntent = PendingIntent.getBroadcast(applicationContext, alarmIdHash, newIntent, FLAG_IMMUTABLE or FLAG_UPDATE_CURRENT)
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
//                                newIntent.putExtra("alarmId", alarmId)
                                startActivity(Intent(intent))
                            }
                        }

                    }
            }

        }

    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getNextWeekAlarmTime(alarmId: String): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.WEEK_OF_YEAR, 1)
        return calendar.timeInMillis
    }
}