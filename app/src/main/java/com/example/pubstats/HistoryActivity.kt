package com.example.pubstats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.pubstats.databinding.ActivityHistoryBinding
import com.example.pubstats.databinding.ItemHistoryBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = PreferencesManager(this)

        if (!prefs.isPremium) {
            binding.lockedOverlay.visibility = View.VISIBLE
            binding.goPremiumButton.setOnClickListener {
                startActivity(android.content.Intent(this, PremiumActivity::class.java))
            }
            return
        }

        val history = prefs.getHistory()
        if (history.isEmpty()) {
            binding.emptyText.visibility = View.VISIBLE
            return
        }

        binding.historyRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.historyRecyclerView.adapter = HistoryAdapter(history)
    }
}

private class HistoryAdapter(private val items: List<SearchHistoryEntry>) :
    RecyclerView.Adapter<HistoryAdapter.VH>() {

    private val dateFormat = SimpleDateFormat("dd MMM, HH:mm", Locale("tr"))

    class VH(val binding: ItemHistoryBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, position: Int): VH {
        val binding = ItemHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.binding.historyUsername.text = "@${item.username}"
        val parts = mutableListOf<String>()
        item.followers?.let { parts.add("$it takipçi") }
        item.posts?.let { parts.add("$it gönderi") }
        parts.add(dateFormat.format(Date(item.timestamp)))
        holder.binding.historyStats.text = parts.joinToString(" • ")
    }

    override fun getItemCount(): Int = items.size
}
