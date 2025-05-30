package com.jai.mario;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.MediaPlayer.TrackDescription;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_VIDEO = 1;

    private VLCVideoLayout videoLayout;
    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;

    private Button btnSelectFile, btnSwitchAudio;
    private List<TrackDescription> audioTracks = new ArrayList<>();
    private int currentAudioTrackIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoLayout = findViewById(R.id.videoView);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnSwitchAudio = findViewById(R.id.btnSwitchAudio);

        libVLC = new LibVLC(this, new ArrayList<>());
        mediaPlayer = new MediaPlayer(libVLC);
        mediaPlayer.attachViews(videoLayout, null, false, false);

        btnSelectFile.setOnClickListener(v -> openFilePicker());

        btnSwitchAudio.setOnClickListener(v -> {
            if (audioTracks.size() > 1) {
                currentAudioTrackIndex = (currentAudioTrackIndex + 1) % audioTracks.size();
                int trackId = audioTracks.get(currentAudioTrackIndex).id;
                mediaPlayer.setAudioTrack(trackId);
                Log.d("VLC", "Switched to audio track: " + trackId);
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(intent, REQUEST_CODE_PICK_VIDEO);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_PICK_VIDEO && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception e) {
                Log.w("MainActivity", "Persist permission failed", e);
            }
            playVideo(uri);
        }
    }

    private void playVideo(Uri uri) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        Media media = new Media(libVLC, uri);
        media.addOption(":no-drop-late-frames");
        media.addOption(":no-skip-frames");

        mediaPlayer.setMedia(media);
        media.release();

        mediaPlayer.setEventListener(event -> {
            if (event.type == MediaPlayer.Event.Playing) {
                TrackDescription[] tracks = mediaPlayer.getAudioTracks();
                if (tracks != null) {
                    audioTracks = Arrays.asList(tracks);
                    currentAudioTrackIndex = getCurrentAudioTrackIndex();
                    Log.d("VLC", "Audio track count: " + audioTracks.size());
                }
            } else if (event.type == MediaPlayer.Event.EncounteredError) {
                Log.e("VLC", "Playback error!");
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mediaPlayer.stop();
        mediaPlayer.detachViews();
        mediaPlayer.release();
        libVLC.release();
    }
}