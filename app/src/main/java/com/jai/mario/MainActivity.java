package com.jai.mario;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private VLCVideoLayout videoLayout;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;

    private EditText edtUrl;
    private Button btnPlay, btnSwitchAudio;

    private List<MediaPlayer.TrackDescription> audioTracks = new ArrayList<>();
    private int currentAudioTrackIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoLayout = findViewById(R.id.videoView);
        edtUrl = findViewById(R.id.edtUrl);
        btnPlay = findViewById(R.id.btnPlay);
        btnSwitchAudio = findViewById(R.id.btnSwitchAudio);

        libVLC = new LibVLC(this, new ArrayList<>());
        mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.attachViews(videoLayout, null, false, false);

        btnPlay.setOnClickListener(v -> {
            String url = edtUrl.getText().toString().trim();
            if (!url.isEmpty()) {
                playNetworkVideo(url);
            }
        });

        btnSwitchAudio.setOnClickListener(v -> {
            if (audioTracks.size() > 1) {
                currentAudioTrackIndex = (currentAudioTrackIndex + 1) % audioTracks.size();
                int trackId = audioTracks.get(currentAudioTrackIndex).id;
                mediaPlayer.setAudioTrack(trackId);
            }
        });
    }

    private void playNetworkVideo(String url) {
        try {
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }

            Media media = new Media(libVLC, url);
            media.addOption(":no-drop-late-frames");
            media.addOption(":no-skip-frames");

            mediaPlayer.setMedia(media);
            media.release();

            mediaPlayer.setEventListener(event -> {
                if (event.type == MediaPlayer.Event.Playing) {
                    audioTracks = Arrays.asList(mediaPlayer.getAudioTracks());
                    currentAudioTrackIndex = getCurrentAudioTrackIndex();
                    Log.d("AUDIO", "Available tracks: " + audioTracks.size());
                }
            });

            mediaPlayer.play();
        } catch (Exception e) {
            Log.e("MainActivity", "Failed to play video", e);
        }
    }

    private int getCurrentAudioTrackIndex() {
        int currentId = mediaPlayer.getAudioTrack();
        for (int i = 0; i < audioTracks.size(); i++) {
            if (audioTracks.get(i).id == currentId) return i;
        }
        return 0;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
        mediaPlayer.detachViews();
        mediaPlayer.release();
        libVLC.release();
    }
}