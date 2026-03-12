package com.example.a5ypexe_asistan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_DELIVER_ACTION || 
            intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            for (sms in messages) {
                val sender = sms.displayOriginatingAddress
                val body = sms.displayMessageBody
                
                val prefs = context.getSharedPreferences("CanAsistanPrefs", Context.MODE_PRIVATE)
                val isEnabled = prefs.getBoolean("sms_announcer_enabled", true)

                if (isEnabled) {
                    val announcement = "${Utils.metniInsanlastir(sender)} kişisinden yeni mesaj var. Mesaj şöyle: ${Utils.metniInsanlastir(body)}"
                    BackgroundService.getInstance()?.speak(announcement)
                    Log.d("SmsReceiver", "Mesaj duyuruluyor: $sender")
                }
            }
        }
    }
}
