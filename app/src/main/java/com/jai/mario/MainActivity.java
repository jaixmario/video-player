package com.jai.mario;

import android.app.AlertDialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.C;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackGroupArray;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;
import androidx.media3.exoplayer.trackselection.MappingTrackSelector.MappedTrackInfo;
import androidx.media3.ui.PlayerView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;
    private EditText videoUrlInput;
    private Button playButton;
    private Button selectAudioButton;

    private DefaultTrackSelector trackSelector;
    private TrackGroupArray currentAudioGroups;
    private int audioRendererIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoUrlInput = findViewById(R.id.video_url_input);
        playButton = findViewById(R.id.play_button);
        selectAudioButton = findViewById(R.id.select_audio_button);
        playerView = findViewById(R.id.player_view);

        trackSelector = new DefaultTrackSelector(this);
        player = new ExoPlayer.Builder(this)
                .setTrackSelector(trackSelector)
                .build();

        playerView.setPlayer(player);

        playButton.setOnClickListener(v -> {
            String url = videoUrlInput.getText().toString().trim();
            if (!url.isEmpty()) {
                playVideo(url);
            } else {
                Toast.makeText(this, "Please enter a video URL", Toast.LENGTH_SHORT).show();
            }
        });

        selectAudioButton.setOnClickListener(v -> showAudioSelectionDialog());
    }

    private void playVideo(String videoUrl) {
        player.stop();

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(Uri.parse(videoUrl))
                .setMimeType(MimeTypes.APPLICATION_MATROSKA)
                .build();

        player.setMediaItem(mediaItem);
        player.prepare();

        player.addListener(new Player.Listener() {
            @Override
            public void onTracksChanged(Player player, Tracks tracks) {
                MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                if (mappedTrackInfo == null) return;

                for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
                    if (mappedTrackInfo.getRendererType(rendererIndex) == C.TRACK_TYPE_AUDIO) {
                        currentAudioGroups = mappedTrackInfo.getTrackGroups(rendererIndex);
                        audioRendererIndex = rendererIndex;
                    }
                }
            }
        });

        player.play();
    }

    private void showAudioSelectionDialog() {
        if (currentAudioGroups == null || currentAudioGroups.length == 0) {
            Toast.makeText(this, "No audio tracks available", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] items = new String[currentAudioGroups.length];
        for (int i = 0; i < currentAudioGroups.length; i++) {
            String lang = currentAudioGroups.get(i).getFormat(0).language;
            String mime = currentAudioGroups.get(i).getFormat(0).sampleMimeType;
            items[i] = (lang != null ? lang : "Track " + (i + 1)) + " (" + mime + ")";
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Audio Track")
                .setItems(items, (dialog, index) -> {
                    TrackGroup group = currentAudioGroups.get(index);

                    TrackSelectionOverride override = new TrackSelectionOverride(group, List.of(0));
                    TrackSelectionParameters parameters = trackSelector
                            .getParameters()
                            .buildUpon()
                            .clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .addOverride(override)
                            .build();

                    trackSelector.setParameters(parameters);

                    Toast.makeText(this, "Selected: " + items[index], Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}