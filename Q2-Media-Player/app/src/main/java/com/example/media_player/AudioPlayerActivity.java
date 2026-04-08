package com.example.media_player;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class AudioPlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private TextView tvTrackTitle;
    private SeekBar seekBar;
    private FloatingActionButton btnPlayPause;
    private Handler handler;
    private Runnable updateRunnable;

    private final ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) loadAudio(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        tvTrackTitle = findViewById(R.id.tvTrackTitle);
        seekBar = findViewById(R.id.seekBar);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        handler = new Handler(Looper.getMainLooper());

        player = new ExoPlayer.Builder(this).build();

        // Listeners
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        findViewById(R.id.btnPickFile).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("audio/mpeg"); // Standard MIME for MP3
            filePicker.launch(intent);
        });

        btnPlayPause.setOnClickListener(v -> {
            if (player.isPlaying()) {
                player.pause();
            } else {
                if (player.getPlaybackState() == Player.STATE_IDLE) player.prepare();
                player.play();
            }
        });

        findViewById(R.id.btnStop).setOnClickListener(v -> {
            player.stop();
            player.seekTo(0);
            seekBar.setProgress(0);
        });

        findViewById(R.id.btnRestart).setOnClickListener(v -> {
            player.seekTo(0);
            player.play();
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && player.getDuration() > 0) {
                    player.seekTo((long) (progress / 100.0 * player.getDuration()));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        player.addListener(new Player.Listener() {
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                btnPlayPause.setImageResource(isPlaying ? 
                        android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            }
        });

        updateRunnable = () -> {
            if (player != null && player.getDuration() > 0) {
                long pos = player.getCurrentPosition();
                long dur = player.getDuration();
                seekBar.setProgress((int) (pos * 100 / dur));
            }
            handler.postDelayed(updateRunnable, 1000);
        };
        handler.post(updateRunnable);
    }

    private void loadAudio(Uri uri) {
        tvTrackTitle.setText("Now Playing: " + uri.getLastPathSegment());
        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();
        player.play();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) player.release();
        handler.removeCallbacks(updateRunnable);
    }
}







