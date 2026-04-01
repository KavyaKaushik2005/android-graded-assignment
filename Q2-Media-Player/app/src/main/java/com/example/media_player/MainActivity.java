package com.example.media_player;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private VideoView videoView;
    private TextView tvStatus, tvTitle, tvCurrentTime, tvTotalTime;
    private SeekBar seekBar;
    private ImageButton btnPlay, btnPause, btnStop, btnRestart;
    private Button btnOpenFile, btnOpenUrl;
    private View audioVisualizer;
    private ImageView ivMediaIcon;

    // Media
    private MediaPlayer audioPlayer;
    private boolean isAudioMode = false;
    private boolean isPrepared = false;
    private String currentTitle = "No media loaded";

    // SeekBar updater
    private final Handler handler = new Handler();
    private final Runnable updateSeekBar = new Runnable() {
        @Override
        public void run() {
            try {
                if (isAudioMode && audioPlayer != null && audioPlayer.isPlaying()) {
                    int current = audioPlayer.getCurrentPosition();
                    int total = audioPlayer.getDuration();
                    seekBar.setMax(total);
                    seekBar.setProgress(current);
                    tvCurrentTime.setText(formatTime(current));
                    tvTotalTime.setText(formatTime(total));
                } else if (!isAudioMode && videoView != null && videoView.isPlaying()) {
                    int current = videoView.getCurrentPosition();
                    int total = videoView.getDuration();
                    if (total > 0) {
                        seekBar.setMax(total);
                        seekBar.setProgress(current);
                        tvCurrentTime.setText(formatTime(current));
                        tvTotalTime.setText(formatTime(total));
                    }
                }
            } catch (Exception ignored) {}
            handler.postDelayed(this, 500);
        }
    };

    // File picker launcher
    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        String mimeType = getContentResolver().getType(uri);
                        String name = getFileName(uri);
                        if (mimeType != null && mimeType.startsWith("video/")) {
                            loadVideo(uri, name);
                        } else {
                            loadAudio(uri, name);
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        setupListeners();
        requestPermissions();
        handler.post(updateSeekBar);
    }

    private void initViews() {
        videoView     = findViewById(R.id.videoView);
        tvStatus      = findViewById(R.id.tvStatus);
        tvTitle       = findViewById(R.id.tvTitle);
        tvCurrentTime = findViewById(R.id.tvCurrentTime);
        tvTotalTime   = findViewById(R.id.tvTotalTime);
        seekBar       = findViewById(R.id.seekBar);
        btnPlay       = findViewById(R.id.btnPlay);
        btnPause      = findViewById(R.id.btnPause);
        btnStop       = findViewById(R.id.btnStop);
        btnRestart    = findViewById(R.id.btnRestart);
        btnOpenFile   = findViewById(R.id.btnOpenFile);
        btnOpenUrl    = findViewById(R.id.btnOpenUrl);
        audioVisualizer = findViewById(R.id.audioVisualizer);
        ivMediaIcon   = findViewById(R.id.ivMediaIcon);

        setStatus("Ready — open a file or URL");
        setControlsEnabled(false);
    }

    private void setupListeners() {
        btnOpenFile.setOnClickListener(v -> openFilePicker());
        btnOpenUrl.setOnClickListener(v -> showUrlDialog());

        btnPlay.setOnClickListener(v -> playMedia());
        btnPause.setOnClickListener(v -> pauseMedia());
        btnStop.setOnClickListener(v -> stopMedia());
        btnRestart.setOnClickListener(v -> restartMedia());

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser) {
                    if (isAudioMode && audioPlayer != null) audioPlayer.seekTo(progress);
                    else videoView.seekTo(progress);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });
    }

    // ── Open file picker ──────────────────────────────────────────────────────
    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "video/*"});
        filePickerLauncher.launch(intent);
    }

    // ── URL dialog ────────────────────────────────────────────────────────────
    private void showUrlDialog() {
        EditText input = new EditText(this);
        input.setHint("https://example.com/video.mp4");
        input.setPadding(40, 20, 40, 20);

        new AlertDialog.Builder(this)
                .setTitle("Enter Stream URL")
                .setView(input)
                .setPositiveButton("Load", (d, w) -> {
                    String url = input.getText().toString().trim();
                    if (!url.isEmpty()) loadVideoUrl(url);
                    else Toast.makeText(this, "URL cannot be empty", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ── Load audio from URI ───────────────────────────────────────────────────
    private void loadAudio(Uri uri, String name) {
        releaseAll();
        isAudioMode = true;
        isPrepared  = false;
        currentTitle = name;

        videoView.setVisibility(View.GONE);
        audioVisualizer.setVisibility(View.VISIBLE);
        ivMediaIcon.setVisibility(View.VISIBLE);

        setStatus("Loading audio…");
        tvTitle.setText(name);

        try {
            audioPlayer = new MediaPlayer();
            audioPlayer.setDataSource(getApplicationContext(), uri);
            audioPlayer.prepareAsync();
            audioPlayer.setOnPreparedListener(mp -> {
                isPrepared = true;
                setStatus("Audio ready — press Play");
                setControlsEnabled(true);
                seekBar.setMax(mp.getDuration());
                tvTotalTime.setText(formatTime(mp.getDuration()));
            });
            audioPlayer.setOnCompletionListener(mp -> setStatus("Playback complete"));
            audioPlayer.setOnErrorListener((mp, what, extra) -> {
                setStatus("Error: " + what);
                return true;
            });
        } catch (Exception e) {
            setStatus("Failed to load audio: " + e.getMessage());
        }
    }

    // ── Load video from URI ───────────────────────────────────────────────────
    private void loadVideo(Uri uri, String name) {
        releaseAll();
        isAudioMode = false;
        isPrepared  = false;
        currentTitle = name;

        videoView.setVisibility(View.VISIBLE);
        audioVisualizer.setVisibility(View.GONE);
        ivMediaIcon.setVisibility(View.GONE);

        setStatus("Loading video…");
        tvTitle.setText(name);
        prepareVideoView(uri);
    }

    // ── Load video from URL ───────────────────────────────────────────────────
    private void loadVideoUrl(String url) {
        releaseAll();
        isAudioMode = false;
        isPrepared  = false;
        currentTitle = url;

        videoView.setVisibility(View.VISIBLE);
        audioVisualizer.setVisibility(View.GONE);
        ivMediaIcon.setVisibility(View.GONE);

        setStatus("Buffering stream…");
        tvTitle.setText(shortenUrl(url));
        prepareVideoView(Uri.parse(url));
    }

    private void prepareVideoView(Uri uri) {
        videoView.setVideoURI(uri);
        videoView.setOnPreparedListener(mp -> {
            isPrepared = true;
            mp.setOnInfoListener((p, what, extra) -> false);
            setStatus("Ready — press Play");
            setControlsEnabled(true);
            seekBar.setMax(videoView.getDuration());
            tvTotalTime.setText(formatTime(videoView.getDuration()));
        });
        videoView.setOnCompletionListener(mp -> setStatus("Playback complete"));
        videoView.setOnErrorListener((mp, what, extra) -> {
            setStatus("Stream error (" + what + ") — check URL");
            return true;
        });
        videoView.requestFocus();
    }

    // ── Playback controls ─────────────────────────────────────────────────────
    private void playMedia() {
        if (!isPrepared) { Toast.makeText(this, "Nothing loaded", Toast.LENGTH_SHORT).show(); return; }
        if (isAudioMode && audioPlayer != null) {
            audioPlayer.start();
            setStatus("▶ Playing audio");
        } else {
            videoView.start();
            setStatus("▶ Playing video");
        }
    }

    private void pauseMedia() {
        if (isAudioMode && audioPlayer != null && audioPlayer.isPlaying()) {
            audioPlayer.pause();
            setStatus("⏸ Paused");
        } else if (!isAudioMode && videoView.isPlaying()) {
            videoView.pause();
            setStatus("⏸ Paused");
        }
    }

    private void stopMedia() {
        if (isAudioMode && audioPlayer != null) {
            audioPlayer.stop();
            try { audioPlayer.prepare(); } catch (Exception ignored) {}
            seekBar.setProgress(0);
            tvCurrentTime.setText("0:00");
            setStatus("⏹ Stopped");
        } else if (!isAudioMode) {
            videoView.stopPlayback();
            seekBar.setProgress(0);
            tvCurrentTime.setText("0:00");
            setStatus("⏹ Stopped");
            isPrepared = false;
            // Reload for restart capability
            videoView.resume();
        }
    }

    private void restartMedia() {
        if (isAudioMode && audioPlayer != null) {
            audioPlayer.seekTo(0);
            audioPlayer.start();
            setStatus("↺ Restarted");
        } else if (!isAudioMode) {
            videoView.seekTo(0);
            videoView.start();
            setStatus("↺ Restarted");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────
    private void releaseAll() {
        if (audioPlayer != null) {
            try { audioPlayer.stop(); } catch (Exception ignored) {}
            audioPlayer.release();
            audioPlayer = null;
        }
        videoView.stopPlayback();
        isPrepared = false;
        setControlsEnabled(false);
        seekBar.setProgress(0);
        tvCurrentTime.setText("0:00");
        tvTotalTime.setText("0:00");
    }

    private void setStatus(String msg) { tvStatus.setText(msg); }

    private void setControlsEnabled(boolean enabled) {
        btnPlay.setEnabled(enabled);
        btnPause.setEnabled(enabled);
        btnStop.setEnabled(enabled);
        btnRestart.setEnabled(enabled);
        seekBar.setEnabled(enabled);
    }

    private String formatTime(int ms) {
        int s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    private String getFileName(Uri uri) {
        String path = uri.getLastPathSegment();
        return (path != null) ? path : "media";
    }

    private String shortenUrl(String url) {
        return url.length() > 50 ? url.substring(0, 47) + "…" : url;
    }

    private void requestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.READ_MEDIA_AUDIO,
                            Manifest.permission.READ_MEDIA_VIDEO}, 100);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(updateSeekBar);
        releaseAll();
    }
}