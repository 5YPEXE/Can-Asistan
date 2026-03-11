package com.example.a5ypexe_asistan

import java.util.*

object Utils {
    private val turkishLocale = Locale.forLanguageTag("tr-TR")

    fun metniInsanlastir(metin: String): String {
        var sonuc = metin
        if (metin.equals("TEB", ignoreCase = true)) return "Türk Ekonomi Bankası"
        
        // Eğer sadece rakam ve sembollerden oluşuyorsa (Telefon numarası ise)
        if (metin.matches(Regex("[0-9+\\s()-]+"))) {
            return formatlaTurkUsulu(metin)
        }

        sonuc = sonuc.lowercase(turkishLocale).replaceFirstChar { it.uppercase() }
        sonuc = sonuc.replace("teb", "Teb", ignoreCase = true).replace("iban", "İban", ignoreCase = true)
        
        // Telefon numaralarını tespit et ve formatla
        val telRegex = Regex("(05|5)\\d{9}")
        sonuc = telRegex.replace(sonuc) { formatlaTurkUsulu(it.value) }
        
        // Kodları (OTP vb.) rakam rakam okutmak için
        val kodRegex = Regex("\\d{3,6}")
        sonuc = kodRegex.replace(sonuc) { it.value.chunked(1).joinToString(", ") }
        
        return sonuc
    }

    private fun formatlaTurkUsulu(numara: String): String {
        val ham = numara.replace(Regex("[^0-9+]"), "")
        val rakam = ham.replace("+", "")
        return when {
            ham.startsWith("+90") && rakam.length == 12 -> 
                "Artı 90, ${rakam.substring(2,5)}, ${rakam.substring(5,8)}, ${rakam.substring(8,10)}, ${rakam.substring(10,12)}"
            rakam.startsWith("0") && rakam.length == 11 -> 
                "0, ${rakam.substring(1,4)}, ${rakam.substring(4,7)}, ${rakam.substring(7,9)}, ${rakam.substring(9,11)}"
            rakam.length == 10 -> 
                "${rakam.substring(0,3)}, ${rakam.substring(3,6)}, ${rakam.substring(6,8)}, ${rakam.substring(8,10)}"
            else -> ham.chunked(1).joinToString(", ")
        }
    }
}
