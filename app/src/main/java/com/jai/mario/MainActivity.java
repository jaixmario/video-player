package com.jai.mario;

import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.libvlc.util.VLCVideoLayout;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText editTextUrl;
    private Button btnPlayUrl;
    private VLCVideoLayout videoLayout;
    private Spinner spinnerTracks;
    private TextView textAudioInfo;

    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;

    private ArrayList<MediaPlayer.TrackDescription> audioTracks = new ArrayList<>();
    private ArrayAdapter<String> spinnerAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextUrl = findViewById(R.id.editTextUrl);
        btnPlayUrl = findViewById(R.id.btnPlayUrl);
        videoLayout = findViewById(R.id.vlcVideoLayout);
        spinnerTracks = findViewById(R.id.spinnerTracks);
        textAudioInfo = findViewById(R.id.textAudioInfo);

        ArrayList<String> options = new ArrayList<>();
        options.add("--audio-time-stretch");
        options.add("--network-caching=1000");

        libVLC = new LibVLC(this, options);
        mediaPlayer = new MediaPlayer(libVLC);

        mediaPlayer.attachViews(videoLayout, null, false, false);

        btnPlayUrl.setOnClickListener(v -> {
            String url = editTextUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
                return;
            }
            playVideo(url);
        });

        spinnerTracks.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                if (!audioTracks.isEmpty() && position < audioTracks.size()) {
                    int trackId = audioTracks.get(position).id;
                    mediaPlayer.setAudioTrack(trackId);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void playVideo(String url) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        Media media = new Media(libVLC, Uri.parse(url));
        media.addOption(":network-caching=1000");
        mediaPlayer.setMedia(media);
        media.release();
        mediaPlayer.play();

        // Wait a second before reading tracks
        videoLayout.postDelayed(this::loadAudioTracks, 1200);
    }

    private void loadAudioTracks() {
        audioTracks.clear();
        MediaPlayer.TrackDescription[] tracks = mediaPlayer.getAudioTracks();
        ArrayList<String> trackNames = new ArrayList<>();

        if (tracks != null) {
            for (MediaPlayer.TrackDescription track : tracks) {
                if (track.name != null) {
                    audioTracks.add(track);
                    trackNames.add(track.name);
                }
            }
        }

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, trackNames);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTracks.setAdapter(spinnerAdapter);

        textAudioInfo.setText("Audio Tracks: " + trackNames.size());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.detachViews();
            mediaPlayer.release();
        }
        if (libVLC != null) {
            libVLC.release();
        }
    }
}