package com.jai.mario;

import android.content.Intent;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.SelectionOverride;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.ui.PlayerView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PICK_VIDEO = 1;

    private PlayerView playerView;
    private ExoPlayer player;
    private DefaultTrackSelector trackSelector;
    private TextView textAudioInfo;
    private Spinner spinnerTracks;
    private List<Integer> audioTrackIndices = new ArrayList<>();
    private Uri selectedUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        playerView = findViewById(R.id.playerView);
        textAudioInfo = findViewById(R.id.textAudioInfo);
        spinnerTracks = findViewById(R.id.spinnerTracks);
        Button btnSelect = findViewById(R.id.btnSelect);

        trackSelector = new DefaultTrackSelector(this);
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build();
        playerView.setPlayer(player);

        btnSelect.setOnClickListener(v -> selectVideoFromStorage());

        spinnerTracks.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, android.view.View view, int position, long id) {
                MappedTrackInfo trackInfo = trackSelector.getCurrentMappedTrackInfo();
                if (trackInfo != null) {
                    int rendererIndex = getAudioRendererIndex(trackInfo);
                    if (rendererIndex != -1 && position < audioTrackIndices.size()) {
                        SelectionOverride override = new SelectionOverride(audioTrackIndices.get(position), 0);
                        DefaultTrackSelector.ParametersBuilder parametersBuilder = trackSelector.buildUponParameters();
                        parametersBuilder.setSelectionOverride(
                                rendererIndex,
                                trackInfo.getTrackGroups(rendererIndex),
                                override
                        );
                        trackSelector.setParameters(parametersBuilder);
                    }
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
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
            selectedUri = data.getData();
            playVideo(selectedUri);
            analyzeAudioTracks(selectedUri);
        }
    }

    private void playVideo(Uri videoUri) {
        player.setMediaItem(MediaItem.fromUri(videoUri));
        player.prepare();
        player.play();
    }

    private void analyzeAudioTracks(Uri videoUri) {
        MediaExtractor extractor = new MediaExtractor();
        StringBuilder infoBuilder = new StringBuilder();
        audioTrackIndices.clear();
        List<String> spinnerItems = new ArrayList<>();

        try {
            extractor.setDataSource(this, videoUri, null);
            int trackCount = extractor.getTrackCount();
            infoBuilder.append("Total Tracks: ").append(trackCount).append("\n");

            for (int i = 0; i < trackCount; i++) {
                MediaFormat format = extractor.getTrackFormat(i);
                String mime = format.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    audioTrackIndices.add(i);
                    infoBuilder.append("Audio Track ").append(audioTrackIndices.size() - 1).append(":\n");
                    infoBuilder.append("  MIME: ").append(mime).append("\n");
                    String lang = format.containsKey(MediaFormat.KEY_LANGUAGE)
                            ? format.getString(MediaFormat.KEY_LANGUAGE)
                            : "Unknown";
                    infoBuilder.append("  Language: ").append(lang).append("\n");
                    spinnerItems.add("Track " + (audioTrackIndices.size() - 1) + " (" + lang + ")");
                }
            }

        } catch (IOException e) {
            infoBuilder.append("Error reading tracks: ").append(e.getMessage());
        } finally {
            extractor.release();
        }

        textAudioInfo.setText(infoBuilder.toString());

        // Update Spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, spinnerItems);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerTracks.setAdapter(adapter);
    }

    private int getAudioRendererIndex(MappedTrackInfo trackInfo) {
        for (int i = 0; i < trackInfo.getRendererCount(); i++) {
            if (trackInfo.getRendererType(i) == com.google.android.exoplayer2.C.TRACK_TYPE_AUDIO) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        player.release();
    }
}