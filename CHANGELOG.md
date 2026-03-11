# Değişim Günlüğü (Changelog) - Can Asistan

Tüm önemli değişiklikler bu dosyada kayıt altına alınacaktır.

## [1.0.0] - 2024-05-21 (Kararlı v1 Sürümü)

### ✨ Eklendi
- **Merkezi Ses Yönetimi:** `BackgroundService` içine entegre edilmiş singleton TTS yapısı.
- **Audio Focus:** Asistan konuşurken diğer medya seslerini otomatik kısma özelliği.
- **Utils Sınıfı:** Metinleri insanlaştırma ve telefon numaralarını okunabilir formata getirme mantığı.
- **İzin Yönetimi:** Android 13/14 (API 33/34) için runtime izin destekleri.
- **SmsReceiver & CallReceiver:** Gelen arama ve mesajların sesli duyurulması.

### 🛠️ Düzeltildi
- **ACTION_CALL Hatası:** `tel:` şeması eksikliği giderildi, aramalar kararlı hale getirildi.
- **String Parsing:** Regex tabanlı yeni komut ayrıştırma sistemi ile isim bozulmaları engellendi.
- **Retry Döngüsü:** `SpeechRecognizer` için 3 denemelik akıllı retry mekanizması eklendi.
- **Bellek Sızıntıları:** TTS ve SpeechRecognizer nesnelerinin `onDestroy` yönetimi düzeltildi.
- **State Machine:** Durumlar arası geçişler (bekleme modları) daha tutarlı hale getirildi.

### 🗑️ Kaldırıldı
- Kullanılmayan deprecated metodlar ve mükerrer fonksiyonlar temizlendi.
