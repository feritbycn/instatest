package com.example.pubstats

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Instagram'ın HERKESE AÇIK profil sayfasını (oturum açmadan, anonim olarak
 * herkesin tarayıcıdan gördüğü HTML) okur ve sadece orada zaten yayınlanmış
 * olan bilgileri çıkarır:
 *   - kullanıcı adı, görünen ad, profil fotoğrafı, bio
 *   - takipçi / takip / gönderi sayısı
 *   - (varsa) son gönderilerin küçük resim URL'leri
 *
 * Bu sınıf KASITLI olarak:
 *   - hiçbir kullanıcı adı/şifre göndermez (login yapmaz)
 *   - gizli hesapların içeriğini açmaya ÇALIŞMAZ
 *   - veri bulunamadığında UYDURMA veri üretmez; sadece boş/null döner
 *
 * Not: Instagram sayfa yapısını zaman zaman değiştirebilir ve anonim
 * isteklere giriş duvarı (login wall) gösterebilir. Böyle durumlarda bu
 * sınıf elinden geleni yapar, alamadığı alanları null/boş bırakır.
 */
class InstagramPublicScraper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun fetchProfile(rawUsername: String): ProfileResult = withContext(Dispatchers.IO) {
        val username = sanitize(rawUsername)
        if (username.isEmpty()) return@withContext ProfileResult.NotFound

        val request = Request.Builder()
            .url("https://www.instagram.com/$username/")
            // Desktop tarayıcı gibi görünmek için; Instagram mobil/bilinmeyen
            // ajanlara daha agresif bir şekilde giriş duvarı gösterebiliyor.
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
                    "(KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
            )
            .header("Accept-Language", "en-US,en;q=0.9")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.code == 404) {
                    return@withContext ProfileResult.NotFound
                }
                if (!response.isSuccessful) {
                    return@withContext ProfileResult.NetworkError
                }

                val html = response.body?.string().orEmpty()
                if (html.isBlank()) return@withContext ProfileResult.NetworkError

                parseProfile(username, html)
            }
        } catch (e: IOException) {
            ProfileResult.NetworkError
        }
    }

    private fun parseProfile(username: String, html: String): ProfileResult {
        val doc = Jsoup.parse(html)

        val ogTitle = doc.selectFirst("meta[property=og:title]")?.attr("content")
        val ogImage = doc.selectFirst("meta[property=og:image]")?.attr("content")
        val ogDescription = doc.selectFirst("meta[property=og:description]")?.attr("content")
            ?: doc.selectFirst("meta[name=description]")?.attr("content")

        // Sayfa gerçekten bulunamadıysa (kullanıcı adı yok) genelde og: etiketleri de yok olur.
        if (ogTitle.isNullOrBlank() && ogImage.isNullOrBlank() && ogDescription.isNullOrBlank()) {
            return ProfileResult.NotFound
        }

        val displayName = ogTitle
            ?.substringBefore(" (@")
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        // Örnek format: "1,234 Followers, 56 Following, 78 Posts - See Instagram..."
        val countsRegex = Regex(
            """([\d.,]+)\s+Followers,\s*([\d.,]+)\s+Following,\s*([\d.,]+)\s+Posts""",
            RegexOption.IGNORE_CASE
        )
        val countsMatch = ogDescription?.let { countsRegex.find(it) }
        val followers = countsMatch?.groupValues?.getOrNull(1)
        val following = countsMatch?.groupValues?.getOrNull(2)
        val posts = countsMatch?.groupValues?.getOrNull(3)

        val bio = ogDescription
            ?.substringAfter("Posts - ", missingDelimiterValue = "")
            ?.substringBefore(" (@")
            ?.takeIf { it.isNotBlank() }

        val isLikelyPrivate = html.contains("\"is_private\":true") ||
            html.contains("This Account is Private", ignoreCase = true)

        val thumbnails = extractThumbnails(html)

        val profile = PublicProfile(
            username = username,
            displayName = displayName,
            bio = bio,
            profileImageUrl = ogImage,
            followers = followers,
            following = following,
            posts = posts,
            isLikelyPrivate = isLikelyPrivate,
            postThumbnails = thumbnails
        )
        return ProfileResult.Success(profile)
    }

    /**
     * Instagram anonim ziyaretçilere genelde gönderi listesini vermiyor
     * (giriş duvarı). Bazı durumlarda sayfa hâlâ ilk birkaç gönderinin
     * önizleme URL'sini gömülü JSON içinde bırakabiliyor; varsa onları
     * çıkarırız. Yoksa boş liste döner — bu NORMAL bir durumdur ve
     * sahte/placeholder görsel ile DOLDURULMAZ.
     */
    private fun extractThumbnails(html: String): List<String> {
        val regex = Regex(""""display_url":"([^"]+)"""")
        return regex.findAll(html)
            .map { it.groupValues[1].replace("\\/", "/").replace("\\u0026", "&") }
            .distinct()
            .take(12)
            .toList()
    }

    private fun sanitize(input: String): String {
        return input.trim().removePrefix("@").filter { it.isLetterOrDigit() || it == '.' || it == '_' }
    }
}
