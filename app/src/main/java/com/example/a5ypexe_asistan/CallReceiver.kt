package com.example.a5ypexe_asistan

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
            val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
            
            if (state == TelephonyManager.EXTRA_STATE_RINGING) {
                // Android 10+ cihazlarda numara almak için READ_CALL_LOG izni gerekli
                val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)
                
                val prefs = context.getSharedPreferences("CanAsistanPrefs", Context.MODE_PRIVATE)
                val isEnabled = prefs.getBoolean("call_announcer_enabled", true) // Varsayılan true yaptık

                if (isEnabled && incomingNumber != null) {
                    val callerName = findCallerName(context, incomingNumber)
                    val announcement = "Arama geliyor: ${Utils.metniInsanlastir(callerName)}"
                    
                    BackgroundService.getInstance()?.speak(announcement)
                    Log.d("CallReceiver", "Arama duyuruluyor: $callerName")
                }
            }
        }
    }

    private fun findCallerName(context: Context, number: String): String {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number))
        val projection = arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME)
        return try {
            context.contentResolver.query(uri, projection, null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else number
            } ?: number
        } catch (e: Exception) {
            number
        }
    }
}
