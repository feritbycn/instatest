package com.example.pubstats

import android.app.Activity
import android.content.Context
import android.util.Log
import com.android.billingclient.api.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Google Play Billing ile tek seferlik "Premium" satın alımını yönetir.
 *
 * ÖNEMLİ: Çalışması için Google Play Console'da bu applicationId altında
 * "premium_lifetime" adında bir "In-app product" (yönetilen ürün)
 * oluşturman gerekir. Test etmek için uygulamayı önce kapalı/dahili test
 * kanalına yüklemen ve bir test hesabıyla denemen gerekir — Play Billing
 * debug derlemede gerçek satın alma ekranı göstermez.
 *
 * Bu sınıf satın almayı SADECE cihaz üstünde doğrular (client-side).
 * Gerçek bir yayın için kendi sunucunda Play Developer API ile satın alma
 * jetonunu (purchase token) doğrulamanı ÖNERİRİM; bu basit sürümde o
 * sunucu tarafı yok.
 */
class BillingManager(private val context: Context) : PurchasesUpdatedListener {

    companion object {
        const val PREMIUM_PRODUCT_ID = "premium_lifetime"
        private const val TAG = "BillingManager"
    }

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium

    private var premiumProductDetails: ProductDetails? = null

    private val billingClient = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .build()

    fun startConnection(onReady: () -> Unit = {}) {
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    queryProductDetails()
                    restorePurchases()
                    onReady()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.debugMessage}")
                }
            }

            override fun onBillingServiceDisconnected() {
                Log.w(TAG, "Billing service disconnected")
            }
        })
    }

    private fun queryProductDetails() {
        val product = QueryProductDetailsParams.Product.newBuilder()
            .setProductId(PREMIUM_PRODUCT_ID)
            .setProductType(BillingClient.ProductType.INAPP)
            .build()

        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(listOf(product))
            .build()

        billingClient.queryProductDetailsAsync(params) { result, productDetailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                premiumProductDetails = productDetailsList.firstOrNull()
            }
        }
    }

    /** Mağazadan okunan gerçek fiyat metni (örn. "₺49,99"). Henüz yüklenmediyse null. */
    fun premiumPriceLabel(): String? =
        premiumProductDetails?.oneTimePurchaseOfferDetails?.formattedPrice

    fun launchPurchaseFlow(activity: Activity) {
        val details = premiumProductDetails
        if (details == null) {
            Log.w(TAG, "Ürün bilgisi henüz yüklenmedi")
            return
        }
        val productDetailsParamsList = listOf(
            BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(details)
                .build()
        )
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(productDetailsParamsList)
            .build()
        billingClient.launchBillingFlow(activity, flowParams)
    }

    fun restorePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { _, purchases ->
            val owned = purchases.any {
                it.products.contains(PREMIUM_PRODUCT_ID) &&
                    it.purchaseState == Purchase.PurchaseState.PURCHASED
            }
            _isPremium.value = owned
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (purchase in purchases) {
                if (purchase.products.contains(PREMIUM_PRODUCT_ID)) {
                    acknowledgeIfNeeded(purchase)
                }
            }
        }
    }

    private fun acknowledgeIfNeeded(purchase: Purchase) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return
        if (!purchase.isAcknowledged) {
            val params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.acknowledgePurchase(params) {
                _isPremium.value = true
            }
        } else {
            _isPremium.value = true
        }
    }

    fun endConnection() {
        billingClient.endConnection()
    }
}
