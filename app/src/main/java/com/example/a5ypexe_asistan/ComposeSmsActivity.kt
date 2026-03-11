package com.example.a5ypexe_asistan

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class ComposeSmsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Bu aktivite sadece bir köprü görevi görecek.
        // Gelen intent'i alıp MainActivity'ye veya asistanın ana akışına yönlendirebiliriz.
        
        val intent = intent
        val action = intent.action
        val data: Uri? = intent.data
        
        // Gelen numarayı alalım
        val number = data?.schemeSpecificPart ?: ""
        
        // Asistanı başlatıp bu numaraya mesaj gönderme moduna sokabiliriz
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra("EXTRA_SMS_NUMBER", number)
            putExtra("EXTRA_START_SMS_FLOW", true)
        }
        
        startActivity(mainIntent)
        finish()
    }
}
