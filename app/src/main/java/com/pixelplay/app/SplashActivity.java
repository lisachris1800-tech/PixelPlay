package com.pixelplay.app;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    private ImageView logo;
    private TextView title, subtitle, version;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        setContentView(R.layout.activity_splash);

        logo = findViewById(R.id.splashLogo);
        title = findViewById(R.id.splashTitle);
        subtitle = findViewById(R.id.splashSubtitle);
        version = findViewById(R.id.splashVersion);

        startAnimation();
    }

    private void startAnimation() {
        logo.setVisibility(View.VISIBLE);
        title.setVisibility(View.VISIBLE);
        subtitle.setVisibility(View.VISIBLE);
        if (version != null) version.setVisibility(View.VISIBLE);

        ObjectAnimator logoScaleX = ObjectAnimator.ofFloat(logo, "scaleX", 0.6f, 1f);
        ObjectAnimator logoScaleY = ObjectAnimator.ofFloat(logo, "scaleY", 0.6f, 1f);
        ObjectAnimator logoAlpha = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);
        logoScaleX.setDuration(500);
        logoScaleY.setDuration(500);
        logoAlpha.setDuration(500);

        ObjectAnimator titleAlpha = ObjectAnimator.ofFloat(title, "alpha", 0f, 1f);
        titleAlpha.setDuration(400);
        titleAlpha.setStartDelay(300);

        ObjectAnimator subtitleAlpha = ObjectAnimator.ofFloat(subtitle, "alpha", 0f, 1f);
        subtitleAlpha.setDuration(400);
        subtitleAlpha.setStartDelay(500);

        ObjectAnimator versionAlpha = null;
        if (version != null) {
            versionAlpha = ObjectAnimator.ofFloat(version, "alpha", 0f, 1f);
            versionAlpha.setDuration(400);
            versionAlpha.setStartDelay(700);
        }

        AnimatorSet set = new AnimatorSet();
        set.playTogether(logoScaleX, logoScaleY, logoAlpha, titleAlpha, subtitleAlpha);
        if (versionAlpha != null) set.play(versionAlpha).after(subtitleAlpha);
        set.setInterpolator(new AccelerateDecelerateInterpolator());
        set.addListener(new Animator.AnimatorListener() {
            @Override public void onAnimationStart(Animator animation) {}
            @Override public void onAnimationEnd(Animator animation) {
                logo.postDelayed(() -> {
                    Intent i = new Intent(SplashActivity.this, MainActivity.class);
                    startActivity(i);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }, 600);
            }
            @Override public void onAnimationCancel(Animator animation) {}
            @Override public void onAnimationRepeat(Animator animation) {}
        });
        set.start();
    }
}