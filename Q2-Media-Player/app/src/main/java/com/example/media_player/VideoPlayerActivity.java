package com.example.media_player;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class VideoPlayerActivity extends AppCompatActivity {

    // Public domain sample streams (Blender Foundation open movies)
    private static final String URL_BIG_BUCK_BUNNY =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4";
    private static final String URL_ELEPHANTS_DREAM =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4";
    private static final String URL_TEARS_OF_STEEL =
            "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/TearsOfSteel.mp4";

    private ExoPlayer player;
    private PlayerView playerView;
    private EditText etVideoUrl;
    private TextView tvStatus, tvQuality;
    private FrameLayout loadingOverlay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_player);

        playerView    = findViewById(R.id.playerView);
        etVideoUrl    = findViewById(R.id.etVideoUrl);
        tvStatus      = findViewById(R.id.tvStatus);
        tvQuality     = findViewById(R.id.tvQuality);
        loadingOverlay = findViewById(R.id.loadingOverlay);

        // Back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Play button
        findViewById(R.id.btnPlay).setOnClickListener(v -> {
            String url = etVideoUrl.getText().toString().trim();
            if (TextUtils.isEmpty(url)) {
                Toast.makeText(this, "Please enter a stream URL", Toast.LENGTH_SHORT).show();
                return;
            }
            hideKeyboard();
            loadStream(url);
        });

        // Stop
        findViewById(R.id.btnStop).setOnClickListener(v -> stopStream());

        // Sample chips
        findViewById(R.id.chipBigBuckBunny).setOnClickListener(v ->  setUrl(URL_BIG_BUCK_BUNNY));
        findViewById(R.id.chipElephants).setOnClickListener(v ->     setUrl(URL_ELEPHANTS_DREAM));
        findViewById(R.id.chipTearsOfSteel).setOnClickListener(v ->  setUrl(URL_TEARS_OF_STEEL));

        // IME action
        etVideoUrl.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                String url = etVideoUrl.getText().toString().trim();
                if (!TextUtils.isEmpty(url)) { hideKeyboard(); loadStream(url); }
                return true;
            }
            return false;
        });

        initPlayer();
    }

    private void initPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                switch (state) {
                    case Player.STATE_BUFFERING:
                        loadingOverlay.setVisibility(View.VISIBLE);
                        setStatus("Buffering…", "LIVE", false);
                        break;
                    case Player.STATE_READY:
                        loadingOverlay.setVisibility(View.GONE);
                        setStatus("Stream ready · playing", "LIVE", true);
                        break;
                    case Player.STATE_ENDED:
                        loadingOverlay.setVisibility(View.GONE);
                        setStatus("Playback ended", "END", false);
                        break;
                    case Player.STATE_IDLE:
                        loadingOverlay.setVisibility(View.GONE);
                        setStatus("No stream loaded", "IDLE", false);
                        break;
                }
            }

            @Override
            public void onPlayerError(@androidx.annotation.NonNull PlaybackException error) {
                loadingOverlay.setVisibility(View.GONE);
                setStatus("Error: " + error.getMessage(), "ERR", false);
                Toast.makeText(VideoPlayerActivity.this,
                        "Playback error: " + error.getMessage(), Toast.LENGTH_LONG).show();
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) setStatus("Playing", "LIVE", true);
            }
        });
    }

    private void loadStream(String url) {
        if (player == null) initPlayer();
        setStatus("Connecting to stream…", "CONN", false);
        loadingOverlay.setVisibility(View.VISIBLE);

        player.stop();
        player.clearMediaItems();
        player.setMediaItem(MediaItem.fromUri(url));
        player.prepare();
        player.play();
    }

    private void stopStream() {
        if (player != null) {
            player.stop();
            player.clearMediaItems();
        }
        loadingOverlay.setVisibility(View.GONE);
        setStatus("Stopped", "IDLE", false);
    }

    private void setUrl(String url) {
        etVideoUrl.setText(url);
        etVideoUrl.setSelection(url.length());
    }

    private void setStatus(String message, String badge, boolean isLive) {
        tvStatus.setText(message);
        tvQuality.setText(badge);
        tvQuality.setTextColor(isLive
                ? getResources().getColor(R.color.accent_cyan, getTheme())
                : getResources().getColor(R.color.text_hint, getTheme()));
    }

    private void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
        if (imm != null && getCurrentFocus() != null)
            imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (player != null) player.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null && player.getPlaybackState() == Player.STATE_READY)
            player.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) { player.release(); player = null; }
    }
}
