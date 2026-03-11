package com.example.a5ypexe_asistan

import android.app.Service
import android.content.Intent
import android.os.IBinder

/**
 * Bu servis, sistem tarafından kullanıcı arayüzü olmadan SMS göndermek istendiğinde kullanılır.
 * (Örneğin: Android Auto veya kilit ekranı hızlı yanıtları)
 */
class HeadlessSmsSendService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Burada gelen intent verilerini alıp SMS gönderme mantığı eklenebilir.
        // Şimdilik asistanımız UI odaklı olduğu için servisi hemen durduruyoruz.
        stopSelf()
        return START_NOT_STICKY
    }
}
