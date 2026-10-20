package com.example.pubstats

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/**
 * Ödüllü (rewarded) reklam akışı: kullanıcı reklamı sonuna kadar izlerse
 * ekstra günlük sorgu hakkı kazanır. "Ücretsiz hak bitince kilitli özelliği
 * reklamla aç" modeli — kullanıcıyı rahatsız eden otomatik/araya giren
 * (interstitial) reklam DEĞİL, kullanıcının kendi isteğiyle tetiklediği
 * bir reklamdır.
 *
 * ⚠️ Burada kullanılan Ad Unit ID'ler Google'ın RESMİ TEST ID'LERİDİR.
 * Gerçek yayına çıkmadan önce bunları kendi AdMob hesabındaki gerçek
 * ID'lerle değiştirmen ZORUNLU (aksi halde AdMob hesabın askıya alınabilir).
 * Aynı şekilde AndroidManifest.xml'deki test App ID'sini de değiştir.
 */
class RewardedAdManager(private val context: Context) {

    companion object {
        // Google'ın herkese açık, resmi test reklam birimi — bkz.
        // https://developers.google.com/admob/android/test-ads
        private const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
        private const val TAG = "RewardedAdManager"
    }

    private var rewardedAd: RewardedAd? = null
    private var isInitialized = false

    fun initialize() {
        if (isInitialized) return
        MobileAds.initialize(context) { isInitialized = true }
        loadAd()
    }

    private fun loadAd() {
        RewardedAd.load(
            context,
            TEST_REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Reklam yüklenemedi: ${error.message}")
                    rewardedAd = null
                }
            }
        )
    }

    fun isReady(): Boolean = rewardedAd != null

    /** Reklamı gösterir; kullanıcı sonuna kadar izlerse [onRewardEarned] çağrılır. */
    fun show(activity: Activity, onRewardEarned: () -> Unit, onNotReady: () -> Unit) {
        val ad = rewardedAd
        if (ad == null) {
            onNotReady()
            loadAd() // bir dahaki için tekrar dene
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                loadAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadAd()
                onNotReady()
            }
        }

        ad.show(activity) {
            // Reklam gerçekten tamamlandı -> ödülü ver
            onRewardEarned()
        }
    }
}
