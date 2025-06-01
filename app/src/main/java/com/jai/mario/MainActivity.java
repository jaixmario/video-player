package com.jai.mario;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    private SurfaceView surfaceView;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private EditText videoUrlInput;
    private Button playButton, selectAudioButton;

    private List<MediaPlayer.TrackDescription> audioTracks = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoUrlInput = findViewById(R.id.video_url_input);
        playButton = findViewById(R.id.play_button);
        selectAudioButton = findViewById(R.id.select_audio_button);
        surfaceView = findViewById(R.id.surface_view);

        ArrayList<String> options = new ArrayList<>();
        options.add("--no-drop-late-frames");
        options.add("--no-skip-frames");
        libVLC = new LibVLC(this, options);

        mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.getVLCVout().setVideoView(surfaceView);
        mediaPlayer.getVLCVout().attachViews();

        playButton.setOnClickListener(v -> {
            String url = videoUrlInput.getText().toString().trim();
            if (!url.isEmpty()) {
                playNetworkStream(url);
            } else {
                Toast.makeText(this, "Please enter a video URL", Toast.LENGTH_SHORT).show();
            }
        });

        selectAudioButton.setOnClickListener(v -> showAudioTrackDialog());
    }

    private void playNetworkStream(String url) {
        mediaPlayer.stop();

        Media media = new Media(libVLC, Uri.parse(url));
        media.setHWDecoderEnabled(true, false);
        media.addOption(":network-caching=200");
        mediaPlayer.setMedia(media);
        media.release();

        mediaPlayer.play();

        surfaceView.postDelayed(() -> {
            audioTracks = mediaPlayer.getAudioTracks();
            Toast.makeText(this, "Found " + audioTracks.size() + " audio tracks", Toast.LENGTH_SHORT).show();
        }, 1500);
    }

    private void showAudioTrackDialog() {
        if (audioTracks == null || audioTracks.isEmpty()) {
            Toast.makeText(this, "No audio tracks available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[audioTracks.size()];
        for (int i = 0; i < audioTracks.size(); i++) {
            MediaPlayer.TrackDescription desc = audioTracks.get(i);
            items[i] = desc.name != null ? desc.name : "Track " + desc.id;
        }

        new AlertDialog.Builder(this)
            .setTitle("Select Audio Track")
            .setItems(items, (dialog, which) -> {
                int trackId = audioTracks.get(which).id;
                mediaPlayer.setAudioTrack(trackId);
                Toast.makeText(this, "Switched to: " + items[which], Toast.LENGTH_SHORT).show();
            })
            .show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
        mediaPlayer.release();
        libVLC.release();
    }
}