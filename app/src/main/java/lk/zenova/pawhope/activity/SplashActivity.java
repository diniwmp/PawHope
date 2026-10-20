package lk.zenova.pawhope.activity;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.TextView;
import lk.zenova.pawhope.R;
import androidx.appcompat.app.AppCompatActivity;

import lk.zenova.pawhope.R;

public class SplashActivity extends AppCompatActivity {

    private static final long SPLASH_DURATION = 2200;

    private ProgressBar progressBar;
    private TextView loadingText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        progressBar = findViewById(R.id.splashProgressBar);
        loadingText = findViewById(R.id.loadingText);

        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(SPLASH_DURATION);
        animator.setInterpolator(new DecelerateInterpolator());
        animator.addUpdateListener(a -> {
            int progress = (int) a.getAnimatedValue();
            progressBar.setProgress(progress);
            loadingText.setText(getString(R.string.splash_loading, progress));
            if (progress == 100) {
                goNext();
            }
        });
        animator.start();
    }

    private boolean navigated = false;

    private void goNext() {
        if (navigated) return;
        navigated = true;
        // පස්සේ: FirebaseAuth currentUser null නම් SignInActivity එකට යවමු
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
