package com.jai.mario;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.*;
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

    private Media currentMedia = null;

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

        spinnerTracks.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!audioTracks.isEmpty() && position < audioTracks.size()) {
                    int trackId = audioTracks.get(position).id;
                    mediaPlayer.setAudioTrack(trackId);
                    Toast.makeText(MainActivity.this, "Switched to: " + audioTracks.get(position).name, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void playVideo(String url) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        currentMedia = new Media(libVLC, Uri.parse(url));
        currentMedia.addOption(":network-caching=1000");

        currentMedia.setEventListener(event -> {
            if (event.type == Media.Event.ParsedChanged) {
                runOnUiThread(this::loadAudioTracks);
            }
        });

        mediaPlayer.setMedia(currentMedia);
        currentMedia.parseAsync(Media.Parse.FetchNetwork);
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

        runOnUiThread(() -> {
            if (!names.isEmpty()) {
                spinnerAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, names);
                spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinnerTracks.setAdapter(spinnerAdapter);
                textAudioInfo.setText("Audio Tracks: " + names.size());
            } else {
                textAudioInfo.setText("No audio tracks found.");
            }
        });
    }

    private void showMetadata(String url) {
        Media tempMedia = new Media(libVLC, Uri.parse(url));

        tempMedia.setEventListener(event -> {
            if (event.type == Media.Event.ParsedChanged) {
                runOnUiThread(() -> {
                    StringBuilder info = new StringBuilder();

                    if (tempMedia.getTrackCount() == 0) {
                        info.append("No tracks found.\n");
                    } else {
                        for (int i = 0; i < tempMedia.getTrackCount(); i++) {
                            Media.Track baseTrack = tempMedia.getTrack(i);
                            if (baseTrack == null) continue;

                            info.append("Track ").append(i).append(": ");

                            switch (baseTrack.type) {
                                case Media.Track.Type.Video:
                                    Media.VideoTrack videoTrack = (Media.VideoTrack) baseTrack;
                                    info.append("Video\n");
                                    info.append("  Resolution: ")
                                        .append(videoTrack.width)
                                        .append("x")
                                        .append(videoTrack.height)
                                        .append("\n");
                                    break;

                                case Media.Track.Type.Audio:
                                    Media.AudioTrack audioTrack = (Media.AudioTrack) baseTrack;
                                    info.append("Audio\n");
                                    info.append("  Channels: ").append(audioTrack.channels).append("\n");
                                    info.append("  Rate: ").append(audioTrack.rate).append(" Hz\n");
                                    break;

                                case Media.Track.Type.Text:
                                    Media.SubtitleTrack subtitleTrack = (Media.SubtitleTrack) baseTrack;
                                    info.append("Subtitle\n");
                                    info.append("  Language: ")
                                        .append(subtitleTrack.language != null ? subtitleTrack.language : "Unknown")
                                        .append("\n");
                                    break;

                                default:
                                    info.append("Other\n");
                            }
                        }
                    }

                    textMetadata.setText(info.toString());
                    tempMedia.release(); // Release only after done
                });
            }
        });

        tempMedia.parseAsync(Media.Parse.FetchNetwork);
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