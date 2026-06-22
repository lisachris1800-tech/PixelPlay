package com.pixelplay.app;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.ViewHolder> {

    public interface VideoClickListener {
        void onVideoClick(VideoItem item);
    }

    private List<VideoItem> items;
    private VideoClickListener listener;

    public VideoAdapter(List<VideoItem> items, VideoClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_video, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int i) {
        VideoItem item = items.get(i);
        h.title.setText(item.title);
        if (item.durationMs > 0) {
            long sec = item.durationMs / 1000;
            h.duration.setText(String.format("%d:%02d", sec / 60, sec % 60));
        } else {
            h.duration.setText("--:--");
        }
        if (item.fileSize > 0) {
            float mb = item.fileSize / (1024f * 1024f);
            h.size.setText(String.format("%.0f MB", mb));
        } else {
            h.size.setText("");
        }
        h.thumbnail.setImageResource(android.R.drawable.presence_video_busy);
        h.card.setOnClickListener(v -> listener.onVideoClick(item));
    }

    @Override
    public int getItemCount() { return items.size(); }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView card;
        ImageView thumbnail;
        TextView title, duration, size;
        ViewHolder(View v) {
            super(v);
            card = (CardView) v;
            thumbnail = v.findViewById(R.id.videoThumbnail);
            title = v.findViewById(R.id.videoTitle);
            duration = v.findViewById(R.id.videoDuration);
            size = v.findViewById(R.id.videoSize);
        }
    }
}
