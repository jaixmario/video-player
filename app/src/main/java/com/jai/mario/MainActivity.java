package com.jai.mario;

import android.net.Uri;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private EditText editTextUrl;
    private Button btnPlayUrl;
    private SurfaceView surfaceView;

    private LibVLC libVLC;
    private MediaPlayer mediaPlayer;
    private SurfaceHolder holder;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        editTextUrl = findViewById(R.id.editTextUrl);
        btnPlayUrl = findViewById(R.id.btnPlayUrl);
        surfaceView = findViewById(R.id.surfaceView);
        holder = surfaceView.getHolder();

        ArrayList<String> options = new ArrayList<>();
        options.add("--audio-time-stretch"); // better audio quality
        options.add("--network-caching=1000"); // add buffer
        libVLC = new LibVLC(this, options);
        mediaPlayer = new MediaPlayer(libVLC);

        mediaPlayer.getVLCVout().setVideoView(surfaceView);
        mediaPlayer.getVLCVout().attachViews();

        btnPlayUrl.setOnClickListener(v -> {
            String url = editTextUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
                return;
            }

            playMediaFromUrl(url);
        });
    }

    private void playMediaFromUrl(String url) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }

        Media media = new Media(libVLC, Uri.parse(url));
        media.setHWDecoderEnabled(true, false);
        media.addOption(":network-caching=1000");
        media.addOption(":audio-track=0");
        mediaPlayer.setMedia(media);
        media.release();

        mediaPlayer.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.getVLCVout().detachViews();
            mediaPlayer.release();
        }
        if (libVLC != null) {
            libVLC.release();
        }
    }
}