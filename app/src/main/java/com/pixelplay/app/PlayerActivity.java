package com.pixelplay.app;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;

public class PlayerActivity extends AppCompatActivity {

    private PlayerView playerView;
    private ExoPlayer player;
    private ImageButton btnBack;
    private TextView titleText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        setContentView(R.layout.activity_player);

        playerView = findViewById(R.id.playerView);
        btnBack = findViewById(R.id.btnBack);
        titleText = findViewById(R.id.videoTitle);

        String uriStr = getIntent().getStringExtra("uri");
        String title = getIntent().getStringExtra("title");
        if (title != null) titleText.setText(title);

        btnBack.setOnClickListener(v -> finish());

        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        playerView.setKeepContentOnPlayerReset(true);

        if (uriStr != null) {
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
            MediaItem mediaItem = MediaItem.fromUri(Uri.parse(uriStr));
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
            player.addListener(new Player.Listener() {
                @Override
                public void onIsPlayingChanged(boolean isPlaying) {
                    if (isPlaying) {
                        View loader = findViewById(R.id.playerLoader);
                        if (loader != null) {
                            loader.setVisibility(View.GONE);
                            playerView.setVisibility(View.VISIBLE);
                        }
                    }
                }
            });
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}