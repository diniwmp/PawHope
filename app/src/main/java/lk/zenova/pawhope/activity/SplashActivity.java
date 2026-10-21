package lk.zenova.pawhope.activity;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

import lk.zenova.pawhope.R;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 5000;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<Animator> loopAnimators = new ArrayList<>();
    private boolean navigated = false;

    private View root, logo, title, tagline, pawRow, ripple1, ripple2;
    private View paw1, paw2, paw3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        root = findViewById(R.id.splashRoot);
        logo = findViewById(R.id.splashLogo);
        title = findViewById(R.id.splashTitle);
        tagline = findViewById(R.id.splashTagline);
        pawRow = findViewById(R.id.pawRow);
        ripple1 = findViewById(R.id.ripple1);
        ripple2 = findViewById(R.id.ripple2);
        paw1 = findViewById(R.id.paw1);
        paw2 = findViewById(R.id.paw2);
        paw3 = findViewById(R.id.paw3);

        playIntro();
        startBreathing();
        startRipple(ripple1, 900);
        startRipple(ripple2, 1800);
        startPawWalk(paw1, paw2, paw3);

        handler.postDelayed(this::goNext, SPLASH_DURATION);
    }

    private void playIntro() {
        ObjectAnimator logoIn = ObjectAnimator.ofPropertyValuesHolder(logo,
                PropertyValuesHolder.ofFloat(View.ALPHA, 0f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_X, 0.5f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.5f, 1f));
        logoIn.setDuration(1000);
        logoIn.setInterpolator(new OvershootInterpolator(1.4f));
        logoIn.start();

        slideUp(title, 500);
        if (tagline != null) {
            slideUp(tagline, 800);
        }

        pawRow.animate().alpha(1f).setStartDelay(1100).setDuration(600).start();
    }

    private void slideUp(View v, long delay) {
        v.setAlpha(0f);
        v.setTranslationY(dp(24));
        v.animate()
                .alpha(1f)
                .translationY(0f)
                .setStartDelay(delay)
                .setDuration(700)
                .setInterpolator(new DecelerateInterpolator())
                .start();
    }

    private void startBreathing() {
        ObjectAnimator breathe = ObjectAnimator.ofPropertyValuesHolder(logo,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.06f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.06f));
        breathe.setDuration(1400);
        breathe.setStartDelay(1000);
        breathe.setRepeatCount(ValueAnimator.INFINITE);
        breathe.setRepeatMode(ValueAnimator.REVERSE);
        breathe.setInterpolator(new AccelerateDecelerateInterpolator());
        loopAnimators.add(breathe);
        breathe.start();
    }

    private void startRipple(View ring, long delay) {
        ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(ring,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.8f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.8f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.6f, 0f));
        a.setDuration(2200); 
        a.setStartDelay(delay);
        a.setRepeatCount(ValueAnimator.INFINITE);
        a.setRepeatMode(ValueAnimator.RESTART);
        a.setInterpolator(new DecelerateInterpolator());
        loopAnimators.add(a);
        a.start();
    }

    private void startPawWalk(View... paws) {
        float lift = -dp(12);
        for (int i = 0; i < paws.length; i++) {
            ObjectAnimator a = ObjectAnimator.ofPropertyValuesHolder(paws[i],
                    PropertyValuesHolder.ofFloat(View.ALPHA, 0.25f, 1f),
                    PropertyValuesHolder.ofFloat(View.TRANSLATION_Y, 0f, lift),
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.2f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.2f));
            a.setDuration(600);
            a.setStartDelay(1100 + i * 250L);
            a.setRepeatCount(ValueAnimator.INFINITE);
            a.setRepeatMode(ValueAnimator.REVERSE);
            a.setInterpolator(new AccelerateDecelerateInterpolator());
            loopAnimators.add(a);
            a.start();
        }
    }

    private void goNext() {
        if (navigated) return;
        navigated = true;

        root.animate()
                .alpha(0f)
                .setDuration(700)
                .withEndAction(() -> {
                    startActivity(new Intent(this, MainActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                })
                .start();
    }

    private float dp(float value) {
        return value * getResources().getDisplayMetrics().density;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
        for (Animator a : loopAnimators) a.cancel();
        root.animate().cancel();
    }
}