package com.example.pubstats

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.example.pubstats.databinding.ActivityCompareBinding
import com.example.pubstats.databinding.ItemCompareColumnBinding
import kotlinx.coroutines.launch

class CompareActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCompareBinding
    private val scraper = InstagramPublicScraper()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCompareBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = PreferencesManager(this)
        if (!prefs.isPremium) {
            binding.lockedOverlay.visibility = View.VISIBLE
            binding.goPremiumButton.setOnClickListener {
                startActivity(android.content.Intent(this, PremiumActivity::class.java))
            }
            return
        }

        binding.contentLayout.visibility = View.VISIBLE
        binding.compareNowButton.setOnClickListener { performCompare() }
    }

    private fun performCompare() {
        val userA = binding.usernameA.text?.toString()?.trim().orEmpty()
        val userB = binding.usernameB.text?.toString()?.trim().orEmpty()
        if (userA.isEmpty() || userB.isEmpty()) return

        binding.progressBar.visibility = View.VISIBLE
        binding.resultsRow.visibility = View.GONE

        lifecycleScope.launch {
            val resultA = scraper.fetchProfile(userA)
            val resultB = scraper.fetchProfile(userB)
            binding.progressBar.visibility = View.GONE

            bindColumn(binding.columnA, resultA)
            bindColumn(binding.columnB, resultB)
            binding.resultsRow.visibility = View.VISIBLE
        }
    }

    private fun bindColumn(column: ItemCompareColumnBinding, result: ProfileResult) {
        when (result) {
            is ProfileResult.Success -> {
                val p = result.profile
                column.colUsername.text = "@${p.username}"
                column.colFollowers.text = "${p.followers ?: "-"} takipçi"
                column.colFollowing.text = "${p.following ?: "-"} takip"
                column.colPosts.text = "${p.posts ?: "-"} gönderi"
                Glide.with(this).load(p.profileImageUrl).circleCrop().into(column.colImage)
            }
            is ProfileResult.NotFound -> {
                column.colUsername.text = getString(R.string.error_not_found)
            }
            is ProfileResult.NetworkError -> {
                column.colUsername.text = getString(R.string.error_network)
            }
        }
    }
}
