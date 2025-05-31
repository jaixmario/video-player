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
    private Button btnPlayUrl, btnShowMetadata;
    private VLCVideoLayout videoLayout;
    private Spinner spinnerTracks;
    private TextView textAudioInfo, textMetadata;

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
        btnShowMetadata = findViewById(R.id.btnShowMetadata);
        videoLayout = findViewById(R.id.vlcVideoLayout);
        spinnerTracks = findViewById(R.id.spinnerTracks);
        textAudioInfo = findViewById(R.id.textAudioInfo);
        textMetadata = findViewById(R.id.textMetadata);

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

        btnShowMetadata.setOnClickListener(v -> {
            String url = editTextUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Enter a URL first", Toast.LENGTH_SHORT).show();
                return;
            }
            showMetadata(url);
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

        media.parseAsync(Media.Parse.FetchNetwork);
        media.setEventListener(event -> {
            if (event.type == Media.Event.ParsedChanged) {
                runOnUiThread(this::loadAudioTracks);
            }
        });

        mediaPlayer.setMedia(media);
        media.release();
        mediaPlayer.play();
    }

    private void loadAudioTracks() {
        MediaPlayer.TrackDescription[] tracks = mediaPlayer.getAudioTracks();
        audioTracks.clear();
        ArrayList<String> names = new ArrayList<>();

        if (tracks != null) {
            for (MediaPlayer.TrackDescription track : tracks) {
                if (track != null && track.name != null) {
                    audioTracks.add(track);
                    names.add(track.name);
                }
            }
        }

        spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTracks.setAdapter(spinnerAdapter);

        textAudioInfo.setText("Audio Tracks: " + names.size());
    }

    private void showMetadata(String url) {
        Media media = new Media(libVLC, Uri.parse(url));
        media.parseAsync(Media.Parse.FetchNetwork);

        media.setEventListener(event -> {
            if (event.type == Media.Event.ParsedChanged) {
                runOnUiThread(() -> {
                    StringBuilder info = new StringBuilder();

                    if (media.getTrackCount() == 0) {
                        info.append("No tracks found.\n");
                    } else {
                        for (int i = 0; i < media.getTrackCount(); i++) {
                            Media.Track track = media.getTrack(i);
                            info.append("Track ").append(i).append(": ").append(track.type.name()).append("\n");

                            if (track.type == Media.Track.Type.Video) {
                                info.append("  Resolution: ").append(track.video.width).append("x").append(track.video.height).append("\n");
                            } else if (track.type == Media.Track.Type.Audio) {
                                info.append("  Channels: ").append(track.audio.channels).append("\n");
                                info.append("  Rate: ").append(track.audio.rate).append(" Hz\n");
                            } else if (track.type == Media.Track.Type.Text) {
                                info.append("  Subtitle Language: ").append(track.language != null ? track.language : "Unknown").append("\n");
                            }
                        }
                    }

                    textMetadata.setText(info.toString());
                    media.release();
                });
            }
        });
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