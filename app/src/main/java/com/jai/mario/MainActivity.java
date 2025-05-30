package com.jai.mario;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.SeekBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.MediaPlayer.Event;
import org.videolan.libvlc.MediaPlayer.TrackDescription;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_VIDEO = 1;

    private VLCVideoLayout videoLayout;
    private MediaPlayer mediaPlayer;
    private LibVLC libVLC;

    private Button btnSelect, btnPlay, btnPause, btnSwitchAudio;
    private SeekBar seekBar;

    private List<TrackDescription> audioTracks = new ArrayList<>();
    private int currentAudioTrackIndex = 0;

    private Handler handler = new Handler();
    private Runnable updateSeekBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoLayout = findViewById(R.id.videoView);
        btnSelect = findViewById(R.id.btnSelect);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnSwitchAudio = findViewById(R.id.btnSwitchAudio);
        seekBar = findViewById(R.id.seekBar);

        libVLC = new LibVLC(this, new ArrayList<>());
        mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.attachViews(videoLayout, null, false, false);

        btnSelect.setOnClickListener(v -> selectVideoFromStorage());

        btnPlay.setOnClickListener(v -> {
            mediaPlayer.play();
            startSeekBarUpdater();
        });

        btnPause.setOnClickListener(v -> mediaPlayer.pause());

        btnSwitchAudio.setOnClickListener(v -> {
            if (audioTracks.size() > 1) {
                currentAudioTrackIndex = (currentAudioTrackIndex + 1) % audioTracks.size();
                int trackId = audioTracks.get(currentAudioTrackIndex).id;
                mediaPlayer.setAudioTrack(trackId);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            boolean userSeeking = false;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (userSeeking) {
                    mediaPlayer.setTime(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                userSeeking = true;
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                userSeeking = false;
            }
        });
    }

    private void selectVideoFromStorage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("video/*");
        startActivityForResult(Intent.createChooser(intent, "Select Video"), REQUEST_CODE_PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            Uri videoUri = data.getData();
            playVideo(videoUri);
        }
    }

    private void playVideo(Uri uri) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        Media media = new Media(libVLC, uri);
        mediaPlayer.setMedia(media);
        media.release();

        mediaPlayer.setEventListener(event -> {
            if (event.type == Event.Playing) {
                audioTracks = Arrays.asList(mediaPlayer.getAudioTracks());
                currentAudioTrackIndex = getCurrentAudioTrackIndex();

                seekBar.setMax((int) mediaPlayer.getLength());
                startSeekBarUpdater();
            }
        });

        mediaPlayer.play();
    }

    private int getCurrentAudioTrackIndex() {
        int currentId = mediaPlayer.getAudioTrack();
        for (int i = 0; i < audioTracks.size(); i++) {
            if (audioTracks.get(i).id == currentId) return i;
        }
        return 0;
    }

    private void startSeekBarUpdater() {
        updateSeekBar = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    seekBar.setProgress((int) mediaPlayer.getTime());
                    handler.postDelayed(this, 500);
                }
            }
        };
        handler.postDelayed(updateSeekBar, 0);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (updateSeekBar != null) handler.removeCallbacks(updateSeekBar);
        mediaPlayer.stop();
        mediaPlayer.detachViews();
        mediaPlayer.release();
        libVLC.release();
    }
}