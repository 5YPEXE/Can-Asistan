package com.example.a5ypexe_asistan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class MmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prefs = context.getSharedPreferences("CanAsistanPrefs", Context.MODE_PRIVATE)
        val isEnabled = prefs.getBoolean("sms_announcer_enabled", true)

        if (isEnabled) {
            val announcement = "Yeni bir multimedya mesajı aldınız."
            BackgroundService.getInstance()?.speak(announcement)
            Log.d("MmsReceiver", "MMS bildirimi yapıldı.")
        }
    }
}
