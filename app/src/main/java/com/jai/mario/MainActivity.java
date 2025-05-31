package com.jai.mario; 

import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackGroup;
import androidx.media3.common.TrackGroupArray;
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
    private DefaultTrackSelector trackSelector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        videoUrlInput = findViewById(R.id.video_url_input);
        playButton = findViewById(R.id.play_button);
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
    }

    private void playVideo(String videoUrl) {
        player.stop();

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(Uri.parse(videoUrl))
                .setMimeType(MimeTypes.APPLICATION_MATROSKA) // important for .mkv
                .build();

        player.setMediaItem(mediaItem);
        player.prepare();

        player.addListener(new androidx.media3.common.Player.Listener() {
            @Override
            public void onTracksChanged(androidx.media3.common.TrackGroupArray trackGroups, androidx.media3.common.TrackSelectionArray selections) {
                MappedTrackInfo mappedTrackInfo = trackSelector.getCurrentMappedTrackInfo();
                if (mappedTrackInfo == null) return;

                for (int rendererIndex = 0; rendererIndex < mappedTrackInfo.getRendererCount(); rendererIndex++) {
                    if (mappedTrackInfo.getRendererType(rendererIndex) == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                        TrackGroupArray audioGroups = mappedTrackInfo.getTrackGroups(rendererIndex);

                        if (audioGroups.length > 0) {
                            TrackGroup firstAudioGroup = audioGroups.get(0);

                            // Force selecting the first audio track (index 0)
                            TrackSelectionOverride override = new TrackSelectionOverride(firstAudioGroup, List.of(0));
                            DefaultTrackSelector.Parameters newParams = trackSelector.buildUponParameters()
                                    .setTrackSelectionOverrides(List.of(override))
                                    .build();
                            trackSelector.setParameters(newParams);
                        }
                    }
                }
            }
        });

        player.play();
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