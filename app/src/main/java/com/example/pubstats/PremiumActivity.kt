package com.example.pubstats

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.pubstats.databinding.ActivityPremiumBinding
import com.example.pubstats.databinding.ItemPremiumFeatureBinding
import kotlinx.coroutines.launch

class PremiumActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumBinding
    private lateinit var billingManager: BillingManager
    private lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        billingManager = BillingManager(this)

        val features = listOf(
            binding.feature1 to getString(R.string.premium_feature_unlimited),
            binding.feature2 to getString(R.string.premium_feature_noads),
            binding.feature3 to getString(R.string.premium_feature_history),
            binding.feature4 to getString(R.string.premium_feature_compare),
            binding.feature5 to getString(R.string.premium_feature_badge),
            binding.feature6 to getString(R.string.premium_feature_dark)
        )
        features.forEach { (featureBinding, text) ->
            featureBinding.featureText.text = text
        }

        binding.buyButton.setOnClickListener {
            billingManager.launchPurchaseFlow(this)
        }
        binding.restoreButton.setOnClickListener {
            billingManager.restorePurchases()
        }

        billingManager.startConnection {
            runOnUiThread {
                binding.priceText.text = billingManager.premiumPriceLabel() ?: ""
            }
        }

        lifecycleScope.launch {
            billingManager.isPremium.collect { isPremium ->
                if (isPremium) {
                    prefs.isPremium = true
                    binding.alreadyPremiumText.visibility = View.VISIBLE
                    binding.buyButton.visibility = View.GONE
                }
            }
        }
    }

    override fun onDestroy() {
        billingManager.endConnection()
        super.onDestroy()
    }
}
