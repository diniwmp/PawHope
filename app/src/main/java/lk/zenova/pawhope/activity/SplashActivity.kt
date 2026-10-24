package lk.zenova.pawhope.activity

import android.animation.Animator
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.appcompat.app.AppCompatActivity
import lk.zenova.pawhope.R

class SplashActivity : AppCompatActivity() {

    companion object {
        private const val SPLASH_DURATION = 5000L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val loopAnimators = mutableListOf<Animator>()
    private var navigated = false

    private lateinit var root: View
    private lateinit var logo: View
    private lateinit var title: View
    private lateinit var tagline: View
    private lateinit var pawRow: View
    private lateinit var ripple1: View
    private lateinit var ripple2: View
    private lateinit var paw1: View
    private lateinit var paw2: View
    private lateinit var paw3: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        root = findViewById(R.id.splashRoot)
        logo = findViewById(R.id.splashLogo)
        title = findViewById(R.id.splashTitle)
        tagline = findViewById(R.id.splashTagline)
        pawRow = findViewById(R.id.pawRow)
        ripple1 = findViewById(R.id.ripple1)
        ripple2 = findViewById(R.id.ripple2)
        paw1 = findViewById(R.id.paw1)
        paw2 = findViewById(R.id.paw2)
        paw3 = findViewById(R.id.paw3)

        playIntro()
        startBreathing()
        startRipple(ripple1, 900)
        startRipple(ripple2, 1800)
        startPawWalk(paw1, paw2, paw3)

        handler.postDelayed({ goNext() }, SPLASH_DURATION)
    }

    // Logo pop-in + text slide-up + paws fade-in
    private fun playIntro() {
        val logoIn = ObjectAnimator.ofPropertyValuesHolder(
            logo,
            PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_X, 0.5f, 1f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.5f, 1f)
        )
        logoIn.duration = 1000
        logoIn.interpolator = OvershootInterpolator(1.4f)
        logoIn.start()

        slideUp(title, 500)
        slideUp(tagline, 800)

        pawRow.animate().alpha(1f).setStartDelay(1100).setDuration(600).start()
    }

    private fun slideUp(v: View, delay: Long) {
        v.alpha = 0f
        v.translationY = dp(24f)
        v.animate()
            .alpha(1f)
            .translationY(0f)
            .setStartDelay(delay)
            .setDuration(700)
            .setInterpolator(DecelerateInterpolator())
            .start()
    }

    private fun startBreathing() {
        val breathe = ObjectAnimator.ofPropertyValuesHolder(
            logo,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.06f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.06f)
        ).apply {
            duration = 1400
            startDelay = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        loopAnimators.add(breathe)
        breathe.start()
    }

    private fun startRipple(ring: View, delay: Long) {
        val a = ObjectAnimator.ofPropertyValuesHolder(
            ring,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.8f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.8f),
            PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 0f)
        ).apply {
            duration = 2200
            startDelay = delay
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = DecelerateInterpolator()
        }
        loopAnimators.add(a)
        a.start()
    }

    private fun startPawWalk(vararg paws: View) {
        val lift = -dp(12f)
        paws.forEachIndexed { i, paw ->
            val a = ObjectAnimator.ofPropertyValuesHolder(
                paw,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.25f, 1f),
                PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, lift),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.2f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.2f)
            ).apply {
                duration = 600
                startDelay = 1100 + i * 250L
                repeatCount = ValueAnimator.INFINITE
                repeatMode = ValueAnimator.REVERSE
                interpolator = AccelerateDecelerateInterpolator()
            }
            loopAnimators.add(a)
            a.start()
        }
    }

    @Suppress("DEPRECATION")
    private fun goNext() {
        if (navigated) return
        navigated = true

        root.animate()
            .alpha(0f)
            .setDuration(700)
            .withEndAction {
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()
            }
            .start()
    }

    // dp -> pixels
    private fun dp(value: Float): Float = value * resources.displayMetrics.density

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        loopAnimators.forEach { it.cancel() }
        root.animate().cancel()
    }
}