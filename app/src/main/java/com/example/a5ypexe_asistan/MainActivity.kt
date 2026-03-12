package com.example.a5ypexe_asistan

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.provider.Settings
import android.provider.Telephony
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import java.text.SimpleDateFormat
import java.util.*


sealed class AppState {
    object Idle : AppState()
    object WaitingSmsRecipient : AppState()
    object WaitingSmsBody : AppState()
    object WaitingSmsSendConfirmation : AppState()
    object WaitingCallRecipient : AppState()
    object WaitingCallConfirmation : AppState()
    object WaitingMarkAsReadConfirmation : AppState()
    object WaitingSmsContentConfirmation : AppState()
}

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "CanAsistan"
        private const val MAX_RETRY_COUNT = 3
        
        // Utterance ID Sabitleri
        private const val UTTERANCE_READY = "ID_READY"
        private const val UTTERANCE_INFO = "ID_INFO"
        private const val UTTERANCE_ERROR = "ID_ERROR"
        private const val UTTERANCE_CANCEL = "ID_CANCEL"
        private const val UTTERANCE_RETRY = "ID_RETRY_QUESTION"
        private const val QUESTION_SUFFIX = "_QUESTION"
    }

    private var tts: TextToSpeech? = null
    private var isTtsInitialized = false
    private var speechRecognizer: SpeechRecognizer? = null
    private var currentState: AppState = AppState.Idle
    private var isListening = false
    private var retryCount = 0

    private var pendingName = ""
    private var pendingNumber = ""
    private var pendingSmsBody = ""
    
    private val lastReadSmsIds = mutableListOf<Long>()
    private val unreadSmsMap = mutableMapOf<String, MutableList<String>>()

    private lateinit var tvStatus: TextView
    private lateinit var mainLayout: View

    private val turkishLocale = Locale.forLanguageTag("tr-TR")

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordGranted) {
            bildirimIzniKontrolEt()
        } else {
            showStatus("Ses kaydı izni gerekli", Color.GRAY)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initUI()
        tts = TextToSpeech(this, this)
        initSpeechRecognizer()
        temelIzinleriIste()
        checkAndStartBackgroundService()
    }

    private fun initUI() {
        tvStatus = findViewById(R.id.tv_status)
        mainLayout = findViewById(R.id.main)
        val viewTouchArea = findViewById<View>(R.id.view_touch_area)
        val btnSettings = findViewById<ImageButton>(R.id.btn_settings)
        
        viewTouchArea.setOnClickListener { 
            if (isListening) stopListeningManually() else dinlemeyeBasla()
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun checkAndStartBackgroundService() {
        val prefs = getSharedPreferences("CanAsistanPrefs", MODE_PRIVATE)
        if (prefs.getBoolean("background_run_enabled", true)) {
            val serviceIntent = Intent(this, BackgroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        }
    }

    private fun showStatus(text: String, color: Int) {
        runOnUiThread { 
            tvStatus.text = text
            mainLayout.setBackgroundColor(color)
        }
    }

    private fun goToIdle() {
        currentState = AppState.Idle
        isListening = false
        retryCount = 0
        showStatus("Hazır", Color.BLACK)
    }

    private fun speak(text: String, utteranceId: String = UTTERANCE_INFO) {
        if (!isTtsInitialized || tts == null) {
            Log.w(TAG, "TTS not ready. Text: $text")
            return
        }
        val params = Bundle().apply { 
            putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId) 
        }
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        isListening = true
                        showStatus("Dinliyorum...", Color.RED)
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() { isListening = false }
                    
                    override fun onError(error: Int) {
                        Log.e(TAG, "Speech Error: $error")
                        isListening = false
                        
                        if (currentState != AppState.Idle && retryCount < MAX_RETRY_COUNT) {
                            retryCount++
                            speak("Sizi tam duyamadım, tekrar söyler misiniz?", UTTERANCE_RETRY)
                        } else {
                            goToIdle()
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val data = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val result = data?.get(0) ?: ""
                        komutuIsle(result)
                    }

                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    private fun stopListeningManually() {
        speechRecognizer?.stopListening()
        speechRecognizer?.cancel()
        goToIdle()
    }

    private fun dinlemeyeBasla() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            temelIzinleriIste()
            return
        }
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
        }
        try {
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Start listening failed", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            tts?.apply {
                language = turkishLocale
                setSpeechRate(0.85f)
                setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        runOnUiThread { tvStatus.text = "Konuşuyor..." }
                    }
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId?.endsWith(QUESTION_SUFFIX) == true || utteranceId == UTTERANCE_RETRY) {
                            runOnUiThread { dinlemeyeBasla() }
                        } else if (utteranceId == UTTERANCE_READY || utteranceId == UTTERANCE_CANCEL || utteranceId == UTTERANCE_ERROR) {
                            goToIdle()
                        }
                    }
                    @Suppress("OVERRIDE_DEPRECATION")
                    override fun onError(utteranceId: String?) { goToIdle() }
                })
                speak("Can asistan hazır.", UTTERANCE_READY)
            }
        }
    }

    private fun komutuIsle(soz: String) {
        val rawInput = soz.lowercase(turkishLocale).trim()
        if (rawInput.isEmpty()) {
            goToIdle()
            return
        }
        showStatus("İşleniyor...", Color.BLACK)
        
        when (currentState) {
            AppState.WaitingSmsRecipient -> handleSmsRecipient(rawInput)
            AppState.WaitingSmsBody -> handleSmsBody(soz)
            AppState.WaitingSmsSendConfirmation -> handleSmsConfirmation(rawInput)
            AppState.WaitingCallRecipient -> handleCallRecipient(rawInput)
            AppState.WaitingCallConfirmation -> handleCallConfirmation(rawInput)
            AppState.WaitingMarkAsReadConfirmation -> handleMarkReadConfirmation(rawInput)
            AppState.WaitingSmsContentConfirmation -> handleSmsContentConfirmation(rawInput)
            AppState.Idle -> anaKomutlariIsle(rawInput)
        }
    }

    private fun anaKomutlariIsle(komut: String) {
        when {
            komut.contains("ayarlar") -> {
                speak("Ayarlar açılıyor.", UTTERANCE_READY)
                startActivity(Intent(this, SettingsActivity::class.java))
            }

            komut.contains(Regex("(yardım|yapabilirsin)")) -> {
                speak("Mesaj gönderebilirim, gelen mesajları okuyabilirim, arama yapabilirim, saat, tarih ve pil bilgisini söyleyebilirim.", UTTERANCE_READY)
            }
            
            komut.contains("mesaj") && (komut.contains("gönder") || komut.contains("yaz")) -> {
                val cleanName = komut.replace(Regex(".*?(mesaj|yaz|gönder).*? "), "").trim()
                if (cleanName.isEmpty() || cleanName == "mesaj" || cleanName == "yaz" || cleanName == "gönder") {
                    currentState = AppState.WaitingSmsRecipient
                    speak("Kime mesaj yazılacak?", "SMS_RECIPIENT$QUESTION_SUFFIX")
                } else {
                    handleSmsRecipient(cleanName)
                }
            }
            
            komut.contains("mesaj") && komut.contains("oku") -> {
                okunmamisSmsleriBul()
                if (unreadSmsMap.isEmpty()) {
                    speak("Okunmamış mesajınız yok.", UTTERANCE_READY)
                } else {
                    val count = unreadSmsMap.values.sumOf { it.size }
                    currentState = AppState.WaitingSmsContentConfirmation
                    speak("$count yeni mesajınız var. Okumamı ister misiniz?", "SMS_CONFIRM$QUESTION_SUFFIX")
                }
            }
            
            komut.contains("ara") -> {
                val cleanName = komut.replace(Regex(".*?ara.*? "), "").trim()
                if (cleanName.isEmpty() || cleanName == "ara") {
                    currentState = AppState.WaitingCallRecipient
                    speak("Kimi aramamı istersiniz?", "CALL_RECIPIENT$QUESTION_SUFFIX")
                } else {
                    handleCallRecipient(cleanName)
                }
            }

            komut.contains(Regex("(batarya|pil|şarj|yüzde)")) -> bataryaDurumunuSoyle()
            komut.contains("saat") -> speak("Saat şu an ${SimpleDateFormat("HH:mm", turkishLocale).format(Date())}.", UTTERANCE_READY)
            komut.contains(Regex("(tarih|bugün|günlerden)")) -> speak("Bugün ${SimpleDateFormat("d MMMM EEEE", turkishLocale).format(Date())}.", UTTERANCE_READY)
            komut.contains(Regex("(artı|eksi|çarpı|bölü)")) -> hesapla(komut)

            else -> speak("Anlayamadım, lütfen tekrar dener misiniz?", UTTERANCE_RETRY)
        }
    }

    // DURUM YÖNETİCİLERİ

    private fun handleSmsRecipient(isim: String) {
        val numara = rehberdeNumaraBul(isim)
        if (numara != null) {
            pendingName = isim.replaceFirstChar { it.uppercase() }
            pendingNumber = numara
            currentState = AppState.WaitingSmsBody
            speak("$pendingName kişisine ne yazmamı istersiniz?", "SMS_BODY$QUESTION_SUFFIX")
        } else {
            speak("$isim ismini bulamadım. Kime mesaj yazayım?", "SMS_RECIPIENT$QUESTION_SUFFIX")
        }
    }

    private fun handleSmsBody(body: String) {
        pendingSmsBody = body
        currentState = AppState.WaitingSmsSendConfirmation
        speak("Mesajınız şöyle: $pendingSmsBody. Göndermemi onaylıyor musunuz?", "SMS_SEND_CONF$QUESTION_SUFFIX")
    }

    private fun handleSmsConfirmation(input: String) {
        if (input.contains("evet") || input.contains("onay")) {
            smsGonder(pendingNumber, pendingSmsBody)
        } else {
            speak("Mesaj gönderimi iptal edildi.", UTTERANCE_CANCEL)
        }
    }

    private fun handleCallRecipient(isim: String) {
        val numara = rehberdeNumaraBul(isim)
        if (numara != null) {
            pendingName = isim.replaceFirstChar { it.uppercase() }
            pendingNumber = numara
            currentState = AppState.WaitingCallConfirmation
            speak("$pendingName kişisini arıyorum, onaylıyor musunuz?", "CALL_CONF$QUESTION_SUFFIX")
        } else {
            speak("$isim rehberde yok. Kimi aramalıyım?", "CALL_RECIPIENT$QUESTION_SUFFIX")
        }
    }

    private fun handleCallConfirmation(input: String) {
        if (input.contains("evet") || input.contains("onay")) {
            aramaYap(pendingNumber)
        } else {
            speak("Arama iptal edildi.", UTTERANCE_CANCEL)
        }
    }

    private fun handleMarkReadConfirmation(input: String) {
        if (input.contains("evet")) {
            SmsNotificationListener.getInstance()?.okunduOlarakIsaretle()
            speak("Tamam, mesajlar okundu yapıldı.", UTTERANCE_READY)
        } else {
            speak("Peki, okunmamış olarak bıraktım.", UTTERANCE_READY)
        }
    }

    private fun handleSmsContentConfirmation(input: String) {
        if (input.contains("evet") || input.contains("oku")) {
            mesajlariOku()
        } else {
            goToIdle()
        }
    }

    // Sistem İşlemleri

    private fun hesapla(komut: String) {
        try {
            val words = komut.split(" ")
            var operasyon = ""
            val values = mutableListOf<Double>()
            for (w in words) {
                val num = w.toDoubleOrNull() ?: kelimeyiSaiyaCevir(w)
                if (num != null) values.add(num)
                if (w in listOf("artı", "eksi", "çarpı", "bölü")) operasyon = w
            }
            if (values.size >= 2 && operasyon.isNotEmpty()) {
                val v1 = values[0]; val v2 = values[1]
                val res = when (operasyon) {
                    "artı" -> v1 + v2
                    "eksi" -> v1 - v2
                    "çarpı" -> v1 * v2
                    "bölü" -> if (v2 != 0.0) v1 / v2 else Double.NaN
                    else -> 0.0
                }
                if (res.isNaN()) speak("Bir sayıyı sıfıra bölemezsiniz.", UTTERANCE_READY)
                else {
                    val formatted = if (res % 1.0 == 0.0) res.toInt().toString() else String.format(turkishLocale, "%.2f", res)
                    speak("Sonuç: $formatted", UTTERANCE_READY)
                }
            } else speak("Hesaplama yapılamadı.", UTTERANCE_ERROR)
        } catch (e: Exception) { speak("İşlem sırasında hata oldu.", UTTERANCE_ERROR) }
    }

    private fun kelimeyiSaiyaCevir(k: String): Double? = when(k) {
        "sıfır" -> 0.0; "bir" -> 1.0; "iki" -> 2.0; "üç" -> 3.0; "dört" -> 4.0
        "beş" -> 5.0; "altı" -> 6.0; "yedi" -> 7.0; "sekiz" -> 8.0; "dokuz" -> 9.0; "on" -> 10.0; else -> null
    }

    private fun smsGonder(numara: String, mesaj: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            speak("Mesaj göndermek için iznim yok.", UTTERANCE_ERROR)
            return
        }
        try {
            val smsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService(android.telephony.SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                android.telephony.SmsManager.getDefault()
            }
            val parts = smsManager.divideMessage(mesaj)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(numara, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(numara, null, mesaj, null, null)
            }
            speak("Mesaj başarıyla gönderildi.", UTTERANCE_READY)
        } catch (e: Exception) {
            Log.e(TAG, "SMS failed", e)
            speak("Mesaj gönderilirken hata oluştu.", UTTERANCE_ERROR)
        }
    }

    private fun aramaYap(numara: String) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            speak("Arama yapmak için iznim yok.", UTTERANCE_ERROR)
            return
        }
        try {
            val intent = Intent(Intent.ACTION_CALL, "tel:$numara".toUri())
            startActivity(intent)
            goToIdle()
        } catch (e: Exception) {
            Log.e(TAG, "Call failed", e)
            speak("Arama başlatılamadı.", UTTERANCE_ERROR)
        }
    }

    private fun bataryaDurumunuSoyle() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val statusIntent = registerReceiver(null, filter)
        val level = statusIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = statusIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val pct = if (level != -1 && scale != -1) (level * 100 / scale.toFloat()).toInt() else -1
        if (pct != -1) speak("Pil seviyesi yüzde $pct.", UTTERANCE_READY)
        else speak("Batarya bilgisi alınamadı.", UTTERANCE_ERROR)
    }

    private fun temelIzinleriIste() {
        val permissions = mutableListOf(
            Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO, Manifest.permission.READ_SMS, 
            Manifest.permission.SEND_SMS, Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_CALL_LOG
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissions.toTypedArray())
    }

    private fun bildirimIzniKontrolEt() {
        val cn = ComponentName(this, SmsNotificationListener::class.java)
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (flat == null || !flat.contains(cn.flattenToString())) {
            speak("Lütfen bildirim erişimini aktif edin.", UTTERANCE_ERROR)
            startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
        }
    }

    private fun okunmamisSmsleriBul() {
        unreadSmsMap.clear()
        lastReadSmsIds.clear()
        val projection = arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY)
        contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, projection, "${Telephony.Sms.READ} = 0", null, Telephony.Sms.DEFAULT_SORT_ORDER)?.use { cursor ->
            val idIdx = cursor.getColumnIndex(Telephony.Sms._ID)
            val addrIdx = cursor.getColumnIndex(Telephony.Sms.ADDRESS)
            val bodyIdx = cursor.getColumnIndex(Telephony.Sms.BODY)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIdx)
                val address = cursor.getString(addrIdx) ?: "Bilinmeyen"
                val body = cursor.getString(bodyIdx) ?: ""
                lastReadSmsIds.add(id)
                val cleanBody = body.replace(Regex("B[0-9]{3}$"), "").trim()
                val name = rehberdeIsimBul(address) ?: Utils.metniInsanlastir(address)
                if (!unreadSmsMap.containsKey(name)) unreadSmsMap[name] = mutableListOf()
                unreadSmsMap[name]?.add(cleanBody)
            }
        }
    }

    private fun mesajlariOku() {
        val builder = StringBuilder()
        for ((sender, messages) in unreadSmsMap) {
            builder.append("$sender kişisinden gelen mesajlar şöyle: ")
            messages.forEach { body -> builder.append("${Utils.metniInsanlastir(body)}. ") }
        }
        builder.append("Mesajlar okundu olarak işaretlensin mi?")
        currentState = AppState.WaitingMarkAsReadConfirmation
        speak(builder.toString(), "READ_CONFIRM$QUESTION_SUFFIX")
    }

    private fun rehberdeNumaraBul(isim: String): String? {
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
        val input = isim.lowercase(turkishLocale)
        var partialMatch: String? = null
        
        contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
            val nIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val vIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (cursor.moveToNext()) {
                val displayName = cursor.getString(nIdx).lowercase(turkishLocale)
                val number = cursor.getString(vIdx)
                if (displayName == input) return number // Tam eşleşme önceliği
                if (displayName.contains(input)) partialMatch = number
            }
        }
        return partialMatch
    }

    private fun rehberdeIsimBul(numara: String): String? {
        val uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(numara))
        contentResolver.query(uri, arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME), null, null, null)?.use {
            if (it.moveToFirst()) return it.getString(0)
        }
        return null
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
        super.onDestroy()
    }
}
