package com.example.pubstats

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Uygulamanın tüm yerel durumunu (günlük ücretsiz sorgu hakkı, reklamla
 * kazanılan bonus hak, premium durumu, arama geçmişi) SharedPreferences
 * üzerinde tutar. Sunucu yok; premium doğrulaması Play Billing'den gelen
 * sonuca göre yapılır (bkz. BillingManager).
 */
class PreferencesManager(context: Context) {

    private val prefs = context.getSharedPreferences("pubstats_prefs", Context.MODE_PRIVATE)
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    companion object {
        const val FREE_DAILY_LIMIT = 5
        const val BONUS_PER_AD = 3
        const val MAX_BONUS_PER_DAY = 9 // en fazla 3 reklam/gün -> spam reklam izletmeyi engeller
    }

    var isPremium: Boolean
        get() = prefs.getBoolean("is_premium", false)
        set(value) = prefs.edit().putBoolean("is_premium", value).apply()

    private fun today(): String = dayFormat.format(Date())

    private fun resetIfNewDay() {
        val lastDay = prefs.getString("last_day", null)
        if (lastDay != today()) {
            prefs.edit()
                .putString("last_day", today())
                .putInt("used_today", 0)
                .putInt("bonus_today", 0)
                .apply()
        }
    }

    fun remainingQueries(): Int {
        if (isPremium) return Int.MAX_VALUE
        resetIfNewDay()
        val used = prefs.getInt("used_today", 0)
        val bonus = prefs.getInt("bonus_today", 0)
        return (FREE_DAILY_LIMIT + bonus - used).coerceAtLeast(0)
    }

    fun canWatchAdForBonus(): Boolean {
        if (isPremium) return false
        resetIfNewDay()
        return prefs.getInt("bonus_today", 0) < MAX_BONUS_PER_DAY
    }

    fun addBonusFromAd() {
        resetIfNewDay()
        val bonus = prefs.getInt("bonus_today", 0)
        prefs.edit().putInt("bonus_today", (bonus + BONUS_PER_AD).coerceAtMost(MAX_BONUS_PER_DAY)).apply()
    }

    fun consumeOneQuery() {
        if (isPremium) return
        resetIfNewDay()
        val used = prefs.getInt("used_today", 0)
        prefs.edit().putInt("used_today", used + 1).apply()
    }

    // ---- Arama geçmişi (premium özellik) ----

    fun addHistoryEntry(username: String, followers: String?, posts: String?) {
        val arr = getHistoryRaw()
        val entry = JSONObject().apply {
            put("username", username)
            put("followers", followers ?: "")
            put("posts", posts ?: "")
            put("timestamp", System.currentTimeMillis())
        }
        arr.put(entry)
        // En fazla son 200 kayıt tut
        val trimmed = JSONArray()
        val start = (arr.length() - 200).coerceAtLeast(0)
        for (i in start until arr.length()) trimmed.put(arr.get(i))
        prefs.edit().putString("history", trimmed.toString()).apply()
    }

    fun getHistory(): List<SearchHistoryEntry> {
        val arr = getHistoryRaw()
        val list = mutableListOf<SearchHistoryEntry>()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            list.add(
                SearchHistoryEntry(
                    username = o.getString("username"),
                    followers = o.optString("followers").ifBlank { null },
                    posts = o.optString("posts").ifBlank { null },
                    timestamp = o.getLong("timestamp")
                )
            )
        }
        return list.reversed() // en yeni en üstte
    }

    private fun getHistoryRaw(): JSONArray {
        val raw = prefs.getString("history", null) ?: return JSONArray()
        return try { JSONArray(raw) } catch (e: Exception) { JSONArray() }
    }
}

data class SearchHistoryEntry(
    val username: String,
    val followers: String?,
    val posts: String?,
    val timestamp: Long
)
