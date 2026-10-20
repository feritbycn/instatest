package com.example.pubstats

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.bumptech.glide.Glide
import com.example.pubstats.databinding.ActivityMainBinding
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var prefs: PreferencesManager
    private lateinit var billingManager: BillingManager
    private lateinit var rewardedAdManager: RewardedAdManager
    private val scraper = InstagramPublicScraper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        prefs = PreferencesManager(this)
        billingManager = BillingManager(this)
        rewardedAdManager = RewardedAdManager(this)

        billingManager.startConnection()
        rewardedAdManager.initialize()

        lifecycleScope.launch {
            billingManager.isPremium.collect { isPremium ->
                if (isPremium) prefs.isPremium = true
                updateQuotaUi()
            }
        }

        binding.searchButton.setOnClickListener { performSearch() }
        binding.swipeRefresh.setOnRefreshListener {
            binding.swipeRefresh.isRefreshing = false
            performSearch()
        }
        binding.watchAdButton.setOnClickListener { watchAdForBonus() }
        binding.premiumButton.setOnClickListener {
            startActivity(Intent(this, PremiumActivity::class.java))
        }
        binding.historyButton.setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        binding.compareButton.setOnClickListener {
            startActivity(Intent(this, CompareActivity::class.java))
        }

        updateQuotaUi()
    }

    override fun onResume() {
        super.onResume()
        updateQuotaUi() // Premium ekranından dönünce / gün değişiminde günceller
    }

    private fun updateQuotaUi() {
        if (prefs.isPremium) {
            binding.quotaText.text = getString(R.string.remaining_queries_unlimited)
            binding.watchAdButton.visibility = View.GONE
        } else {
            binding.quotaText.text = getString(R.string.remaining_queries, prefs.remainingQueries())
            binding.watchAdButton.visibility = View.VISIBLE
            binding.watchAdButton.isEnabled = prefs.canWatchAdForBonus()
        }
    }

    private fun watchAdForBonus() {
        if (!prefs.canWatchAdForBonus()) return
        rewardedAdManager.show(
            activity = this,
            onRewardEarned = {
                prefs.addBonusFromAd()
                updateQuotaUi()
                Toast.makeText(this, getString(R.string.ad_reward_earned), Toast.LENGTH_SHORT).show()
            },
            onNotReady = {
                Toast.makeText(this, getString(R.string.ad_not_ready), Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun performSearch() {
        val username = binding.usernameInput.text?.toString()?.trim().orEmpty()
        if (username.isEmpty()) return

        if (!prefs.isPremium && prefs.remainingQueries() <= 0) {
            showError(getString(R.string.limit_reached_message))
            return
        }

        showLoading()

        lifecycleScope.launch {
            val result = scraper.fetchProfile(username)
            when (result) {
                is ProfileResult.Success -> {
                    prefs.consumeOneQuery()
                    prefs.addHistoryEntry(result.profile.username, result.profile.followers, result.profile.posts)
                    updateQuotaUi()
                    showProfile(result.profile)
                }
                is ProfileResult.NotFound -> showError(getString(R.string.error_not_found))
                is ProfileResult.NetworkError -> showError(getString(R.string.error_network))
            }
        }
    }

    private fun showLoading() {
        binding.progressBar.visibility = View.VISIBLE
        binding.errorText.visibility = View.GONE
        binding.profileCard.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.progressBar.visibility = View.GONE
        binding.profileCard.visibility = View.GONE
        binding.errorText.visibility = View.VISIBLE
        binding.errorText.text = message
    }

    private fun showProfile(profile: PublicProfile) {
        binding.progressBar.visibility = View.GONE
        binding.errorText.visibility = View.GONE

        if (profile.isLikelyPrivate) {
            showError(getString(R.string.error_private))
            return
        }

        binding.profileCard.visibility = View.VISIBLE
        binding.usernameText.text = "@${profile.username}"
        binding.premiumBadge.visibility = if (prefs.isPremium) View.VISIBLE else View.GONE
        binding.bioText.text = profile.displayName?.let { "$it\n${profile.bio.orEmpty()}" }
            ?: profile.bio.orEmpty()
        binding.postsCount.text = profile.posts ?: "-"
        binding.followersCount.text = profile.followers ?: "-"
        binding.followingCount.text = profile.following ?: "-"

        Glide.with(this)
            .load(profile.profileImageUrl)
            .circleCrop()
            .into(binding.profileImage)

        if (profile.postThumbnails.isEmpty()) {
            binding.noPostsText.visibility = View.VISIBLE
            binding.postsRecyclerView.visibility = View.GONE
        } else {
            binding.noPostsText.visibility = View.GONE
            binding.postsRecyclerView.visibility = View.VISIBLE
            binding.postsRecyclerView.layoutManager = GridLayoutManager(this, 3)
            binding.postsRecyclerView.adapter = PostThumbnailAdapter(profile.postThumbnails)
        }
    }

    override fun onDestroy() {
        billingManager.endConnection()
        super.onDestroy()
    }
}
