package com.example.a5ypexe_asistan

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val swCallAnnouncer = findViewById<SwitchCompat>(R.id.sw_call_announcer)
        val llRepeatCount = findViewById<LinearLayout>(R.id.ll_repeat_count)
        val etRepeatCount = findViewById<EditText>(R.id.et_repeat_count)
        val swBackgroundRun = findViewById<SwitchCompat>(R.id.sw_background_run)
        val btnSave = findViewById<Button>(R.id.btn_save)

        val prefs = getSharedPreferences("CanAsistanPrefs", Context.MODE_PRIVATE)

        // Mevcut ayarları yükle
        val isCallAnnouncerEnabled = prefs.getBoolean("call_announcer_enabled", false)
        val repeatCount = prefs.getInt("call_announcer_repeat", 1)
        val isBackgroundEnabled = prefs.getBoolean("background_run_enabled", false)

        swCallAnnouncer.isChecked = isCallAnnouncerEnabled
        etRepeatCount.setText(repeatCount.toString())
        swBackgroundRun.isChecked = isBackgroundEnabled
        llRepeatCount.visibility = if (isCallAnnouncerEnabled) View.VISIBLE else View.GONE

        swCallAnnouncer.setOnCheckedChangeListener { _, isChecked ->
            llRepeatCount.visibility = if (isChecked) View.VISIBLE else View.GONE
        }

        btnSave.setOnClickListener {
            val countStr = etRepeatCount.text.toString()
            var count = countStr.toIntOrNull() ?: 1
            if (count < 1) count = 1
            if (count > 3) count = 3

            val backgroundChecked = swBackgroundRun.isChecked

            prefs.edit().apply {
                putBoolean("call_announcer_enabled", swCallAnnouncer.isChecked)
                putInt("call_announcer_repeat", count)
                putBoolean("background_run_enabled", backgroundChecked)
                apply()
            }

            // Servis yönetimini tetikle
            val serviceIntent = Intent(this, BackgroundService::class.java)
            if (backgroundChecked) {
                startForegroundService(serviceIntent)
            } else {
                stopService(serviceIntent)
            }

            Toast.makeText(this, "Ayarlar kaydedildi", Toast.LENGTH_SHORT).show()
            finish()
        }
    }
}
