# Can-Asistan 🎙️📱

**Can-Asistan**, görme engelli ve kısmi görme engelli kullanıcılar için geliştirilen, Türkçe sesli komutlarla çalışan erişilebilir bir Android yardımcı uygulamasıdır.

---

## 📸 Ekran Görüntüsü

<p align="center">
  <img src="assets/images/can-asistan-ui.png" alt="Can-Asistan Ana Ekran" width="300"/>
</p>

---

## ✨ Özellikler

### 📞 Sesli Arama
- `"Annemi ara"` gibi komutlarla rehberden kişi bulup otomatik arama başlatma
- Arama öncesi sesli onay alma

### 📩 Akıllı SMS Yönetimi
- Sesli olarak mesaj içeriği oluşturma
- Rehberden kişi seçerek SMS gönderme
- Gelen okunmamış mesajları sesli okuma
- Mesajları okundu olarak işaretleme

### 🔔 Bildirim ve Duyuru Desteği
- Gelen çağrıları sesli duyurma
- Gelen SMS bildirimlerini algılama
- Bildirim erişimi ile daha akıllı yardımcı deneyimi sağlama

### 🔋 Cihaz Bilgisi
- Pil seviyesi sorgulama
- Saat ve tarih bilgisi verme

### 🔢 Sesli Hesap Makinesi
- `"15 artı 25 kaç eder?"` gibi temel matematik işlemlerini gerçekleştirme

### 🔇 Akıllı Ses Yönetimi
- Asistan konuşurken ses odak yönetimi ile daha kontrollü ses deneyimi
- Arka planda çalışan yardımcı servis desteği

---

## 🛠️ Teknik Detaylar

- **Dil:** Kotlin
- **Minimum SDK:** API 24 (Android 7.0)
- **Hedef SDK:** API 34 (Android 14)
- **Geliştirme Ortamı:** Android Studio
- **Mimari Yaklaşım:** Durum bazlı komut akışı (`AppState`)
- **Arka Plan Yapısı:** Foreground / background service desteği

### Kullanılan Android Bileşenleri
- `SpeechRecognizer` — Sesli komut algılama
- `TextToSpeech` — Sesli geri bildirim
- `NotificationListenerService` — Bildirim dinleme
- `ContentResolver` — SMS ve rehber erişimi
- `BatteryManager` — Pil durumu bilgisi
- `RecognizerIntent` — Sesli giriş başlatma

---

## 🧠 Desteklenen Komut Örnekleri

- `Annemi ara`
- `Ayşe'ye mesaj gönder`
- `Mesajları oku`
- `Pil durumu ne`
- `Saat kaç`
- `Bugün tarih ne`
- `15 artı 25`
- `Yardım`

---

## 🔐 Gerekli İzinler

Uygulamanın düzgün çalışabilmesi için aşağıdaki izinlere ihtiyaç duyulabilir:

- Mikrofon erişimi
- Rehber erişimi
- SMS okuma / gönderme izni
- Telefon arama izni
- Bildirim erişimi
- Arka planda çalışma izni

> Not: Bazı özellikler cihaz üreticisine ve Android sürümüne göre farklı davranabilir.

---

## 🚀 Kurulum ve Çalıştırma

## GÜNCELLENECEK

---

## 📝 Değişim Günlüğü

Projedeki güncellemeleri ve teknik iyileştirmeleri [CHANGELOG.md](CHANGELOG.md) dosyasından takip edebilirsiniz.

---

## 🎯 Proje Durumu

Can-Asistan aktif geliştirme aşamasındadır.  
Hedef, daha doğal konuşma akışı olan, daha güvenilir ve daha yetenekli bir Türkçe Android sesli asistan oluşturmaktır.

### Planlanan İyileştirmeler
- Daha güçlü komut ayrıştırma sistemi
- Daha güvenli kişi eşleştirme
- Gelişmiş hata yönetimi
- Daha iyi kullanıcı arayüzü
- Ayarlar ekranının geliştirilmesi
- Yeni komut kategorileri eklenmesi

---

*Bu proje eğitim, erişilebilirlik ve yardımcı teknoloji amaçlı geliştirilmiştir.*
