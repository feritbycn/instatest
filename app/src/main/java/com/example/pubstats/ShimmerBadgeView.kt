package com.example.pubstats

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Shader
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatTextView

/**
 * "Siyah + Altın / Shimmer" Premium rozeti.
 *
 * Koyu (siyah) zemin üzerindeki altın metnin üstünden sürekli, yumuşak bir
 * ışık şeridi kayar. Harici bir animasyon kütüphanesi (örn. Facebook
 * Shimmer) GEREKMEZ: metnin kendi Paint'ine kayan bir LinearGradient
 * shader uygulanır — bu, metin rengini zaman içinde koyu altından parlak
 * krem sarısına kaydırır.
 *
 * Kullanımı: layout'ta normal bir TextView gibi, sadece
 * `com.example.pubstats.ShimmerBadgeView` olarak tanımla
 * (bkz. activity_main.xml / activity_premium.xml).
 */
class ShimmerBadgeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : AppCompatTextView(context, attrs) {

    companion object {
        private const val SHIMMER_DURATION_MS = 1800L
        private val COLOR_DARK_GOLD = 0xFFB8860B.toInt()
        private val COLOR_BRIGHT_GOLD = 0xFFFFE9A8.toInt()
    }

    private var shimmerAnimator: ValueAnimator? = null
    private var gradient: LinearGradient? = null
    private val gradientMatrix = Matrix()
    private var shimmerSpan = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w <= 0) return
        // Şeridin kayacağı mesafe; genişlik arttıkça daha yumuşak bir geçiş olur.
        shimmerSpan = w * 1.6f
        gradient = LinearGradient(
            0f, 0f, shimmerSpan, 0f,
            intArrayOf(COLOR_DARK_GOLD, COLOR_BRIGHT_GOLD, COLOR_DARK_GOLD),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        paint.shader = gradient
        startShimmer()
    }

    private fun startShimmer() {
        shimmerAnimator?.cancel()
        if (shimmerSpan <= 0f) return
        shimmerAnimator = ValueAnimator.ofFloat(-shimmerSpan, shimmerSpan).apply {
            duration = SHIMMER_DURATION_MS
            repeatCount = ValueAnimator.INFINITE
            addUpdateListener { animator ->
                val dx = animator.animatedValue as Float
                gradientMatrix.setTranslate(dx, 0f)
                gradient?.setLocalMatrix(gradientMatrix)
                invalidate()
            }
            start()
        }
    }

    override fun onVisibilityChanged(changedView: android.view.View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)
        // Görünür değilken animasyonu durdur (pil/performans için gereksiz yere çalışmasın).
        if (visibility == VISIBLE) startShimmer() else shimmerAnimator?.cancel()
    }

    override fun onDetachedFromWindow() {
        shimmerAnimator?.cancel()
        super.onDetachedFromWindow()
    }
}
