package com.example.a5ypexe_asistan

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class SmsNotificationListener : NotificationListenerService() {

    companion object {
        private var instance: SmsNotificationListener? = null
        fun getInstance() = instance
    }

    override fun onListenerConnected() {
        instance = this
        Log.d("AsistanBildirim", "Servis başarıyla bağlandı.")
    }

    override fun onListenerDisconnected() {
        instance = null
    }

    fun okunduOlarakIsaretle() {
        try {
            val activeNotifications = getActiveNotifications()
            if (activeNotifications.isNullOrEmpty()) {
                Log.d("AsistanBildirim", "Aktif bildirim bulunamadı.")
                return
            }

            for (sbn in activeNotifications) {
                val pkg = sbn.packageName.lowercase()
                if (pkg.contains("messaging") || pkg.contains("sms") || pkg.contains("mms") || pkg.contains("telephony")) {
                    val actions = sbn.notification.actions ?: continue
                    for (action in actions) {
                        val title = action.title.toString().lowercase()
                        if (title.contains("okun") || title.contains("read") || 
                            title.contains("görül") || title.contains("done")) {
                            
                            action.actionIntent.send() 
                            Log.d("AsistanBildirim", "${sbn.packageName} için Okundu butonuna basıldı.")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AsistanBildirim", "Okundu işlemi sırasında hata: ${e.message}")
        }
    }
}
