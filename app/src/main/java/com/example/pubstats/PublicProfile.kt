package com.example.pubstats

/**
 * Yalnızca Instagram'ın HERKESE AÇIK (public, oturum açılmamış) profil
 * sayfasında zaten görünen bilgileri tutar. "Kim baktı" veya gizli hesap
 * içeriği gibi bir alan BİLEREK yoktur çünkü bu veri hiçbir kaynaktan
 * çekilemez.
 */
data class PublicProfile(
    val username: String,
    val displayName: String?,
    val bio: String?,
    val profileImageUrl: String?,
    val followers: String?,
    val following: String?,
    val posts: String?,
    val isLikelyPrivate: Boolean,
    val postThumbnails: List<String> // boş olabilir; bu normaldir, sahte veriyle doldurulmaz
)

sealed class ProfileResult {
    data class Success(val profile: PublicProfile) : ProfileResult()
    object NotFound : ProfileResult()
    object NetworkError : ProfileResult()
}
