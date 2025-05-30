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
            media.addOption(":network-caching=300");
            media.addOption(":no-drop-late-frames");
            media.addOption(":no-skip-frames");

            mediaPlayer.setMedia(media);
            media.release();

            mediaPlayer.setEventListener(event -> {
                switch (event.type) {
                    case MediaPlayer.Event.Playing:
                        Log.d("VLC", "Video is playing");
                        audioTracks = Arrays.asList(mediaPlayer.getAudioTracks());
                        currentAudioTrackIndex = getCurrentAudioTrackIndex();
                        break;
                    case MediaPlayer.Event.EncounteredError:
                        Log.e("VLC", "Playback encountered an error!");
                        break;
                    case MediaPlayer.Event.Stopped:
                        Log.d("VLC", "Playback stopped.");
                        break;
                    case MediaPlayer.Event.EndReached:
                        Log.d("VLC", "Playback ended.");
                        break;
                }
            });

            mediaPlayer.play();

        } catch (Exception e) {
            Log.e("MainActivity", "Error while preparing media: ", e);
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