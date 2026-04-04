package com.example.media_player;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class VideoPlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;
    private TextView tvStatus;
    private EditText etUrl;

    private final ActivityResultLauncher<Intent> videoPicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) loadMedia(uri, "Local File");
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView = findViewById(R.id.playerView);
        tvStatus = findViewById(R.id.tvStatus);
        etUrl = findViewById(R.id.etUrl);

        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        // Back Button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Open File
        findViewById(R.id.btnPickVideo).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("video/*");
            videoPicker.launch(intent);
        });

        // Open URL
        findViewById(R.id.btnOpenUrl).setOnClickListener(v -> {
            String url = etUrl.getText().toString().trim();
            if (!TextUtils.isEmpty(url)) {
                loadMedia(Uri.parse(url), "Remote Stream");
            } else {
                Toast.makeText(this, "Please enter a URL", Toast.LENGTH_SHORT).show();
            }
        });

        // Controls
        findViewById(R.id.btnPlay).setOnClickListener(v -> player.play());
        findViewById(R.id.btnPause).setOnClickListener(v -> player.pause());
        findViewById(R.id.btnStop).setOnClickListener(v -> {
            player.stop();
            player.seekTo(0);
        });
        findViewById(R.id.btnRestart).setOnClickListener(v -> {
            player.seekTo(0);
            player.play();
        });

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                if (state == Player.STATE_BUFFERING) {
                    tvStatus.setText("Buffering...");
                } else if (state == Player.STATE_READY) {
                    tvStatus.setText("Playing");
                }
            }
        });
    }

    private void loadMedia(Uri uri, String type) {
        tvStatus.setText("Loading " + type + "...");
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
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
