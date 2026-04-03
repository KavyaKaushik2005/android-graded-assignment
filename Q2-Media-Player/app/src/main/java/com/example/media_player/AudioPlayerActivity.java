package com.example.media_player;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import java.util.concurrent.TimeUnit;

public class AudioPlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private TextView tvTrackTitle, tvArtist, tvCurrentTime, tvTotalTime;
    private TextView ivPlayPauseIcon;
    private SeekBar seekBar, seekVolume;
    private Handler handler;
    private Runnable updateRunnable;
    private boolean isUserSeeking = false;

    // File picker launcher
    private final ActivityResultLauncher<Intent> filePicker =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) loadAudio(uri);
                }
            });

    // Permission launcher
    private final ActivityResultLauncher<String> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) openFilePicker();
                else Toast.makeText(this, "Storage permission denied", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_audio_player);

        // Views
        tvTrackTitle    = findViewById(R.id.tvTrackTitle);
        tvArtist        = findViewById(R.id.tvArtist);
        tvCurrentTime   = findViewById(R.id.tvCurrentTime);
        tvTotalTime     = findViewById(R.id.tvTotalTime);
        ivPlayPauseIcon = findViewById(R.id.ivPlayPauseIcon);
        seekBar         = findViewById(R.id.seekBar);
        seekVolume      = findViewById(R.id.seekVolume);

        // Init player
        player = new ExoPlayer.Builder(this).build();
        handler = new Handler(Looper.getMainLooper());

        // Back button
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // Play/Pause
        findViewById(R.id.btnPlayPause).setOnClickListener(v -> togglePlayPause());

        // Rewind 10s
        findViewById(R.id.btnRewind).setOnClickListener(v -> {
            if (player != null) {
                long pos = Math.max(0, player.getCurrentPosition() - 10_000);
                player.seekTo(pos);
            }
        });

        // Forward 10s
        findViewById(R.id.btnForward).setOnClickListener(v -> {
            if (player != null) {
                long pos = Math.min(player.getDuration(), player.getCurrentPosition() + 10_000);
                player.seekTo(pos);
            }
        });

        // Pick file
        findViewById(R.id.btnPickFile).setOnClickListener(v -> checkPermissionAndPick());

        // SeekBar
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (fromUser && player != null && player.getDuration() > 0) {
                    long newPos = (long) (progress / 100.0 * player.getDuration());
                    tvCurrentTime.setText(formatTime(newPos));
                }
            }
            @Override public void onStartTrackingTouch(SeekBar sb) { isUserSeeking = true; }
            @Override public void onStopTrackingTouch(SeekBar sb) {
                isUserSeeking = false;
                if (player != null && player.getDuration() > 0) {
                    long newPos = (long) (sb.getProgress() / 100.0 * player.getDuration());
                    player.seekTo(newPos);
                }
            }
        });

        // Volume
        seekVolume.setProgress(80);
        seekVolume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                if (player != null) player.setVolume(progress / 100f);
            }
            @Override public void onStartTrackingTouch(SeekBar sb) {}
            @Override public void onStopTrackingTouch(SeekBar sb) {}
        });

        // Player listener
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                updatePlayPauseIcon();
                if (state == Player.STATE_READY) {
                    tvTotalTime.setText(formatTime(player.getDuration()));
                }
            }
            @Override public void onIsPlayingChanged(boolean isPlaying) {
                updatePlayPauseIcon();
                if (isPlaying) startProgressUpdates(); else stopProgressUpdates();
            }
        });

        // Start progress loop
        updateRunnable = () -> {
            if (player != null && !isUserSeeking && player.getDuration() > 0) {
                long pos  = player.getCurrentPosition();
                long dur  = player.getDuration();
                int  prog = (int) (pos * 100L / dur);
                seekBar.setProgress(prog);
                tvCurrentTime.setText(formatTime(pos));
            }
            handler.postDelayed(updateRunnable, 500);
        };
    }

    private void checkPermissionAndPick() {
        String perm = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? Manifest.permission.READ_MEDIA_AUDIO
                : Manifest.permission.READ_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this, perm) == PackageManager.PERMISSION_GRANTED) {
            openFilePicker();
        } else {
            permissionLauncher.launch(perm);
        }
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        filePicker.launch(intent);
    }

    private void loadAudio(Uri uri) {
        // Resolve display name
        String name = resolveFileName(uri);
        tvTrackTitle.setText(name != null ? name : "Unknown Track");
        tvArtist.setText(resolveArtist(uri));

        player.stop();
        player.clearMediaItems();
        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();
        player.play();
    }

    private String resolveFileName(Uri uri) {
        try (Cursor c = getContentResolver().query(uri,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                String name = c.getString(0);
                // strip extension
                int dot = name.lastIndexOf('.');
                return dot > 0 ? name.substring(0, dot) : name;
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String resolveArtist(Uri uri) {
        try (Cursor c = getContentResolver().query(uri,
                new String[]{MediaStore.Audio.Media.ARTIST}, null, null, null)) {
            if (c != null && c.moveToFirst()) {
                String artist = c.getString(0);
                if (artist != null && !artist.equals("<unknown>")) return artist;
            }
        } catch (Exception ignored) {}
        return "Unknown Artist";
    }

    private void togglePlayPause() {
        if (player == null) return;
        if (player.isPlaying()) player.pause();
        else if (player.getPlaybackState() == Player.STATE_ENDED) {
            player.seekTo(0); player.play();
        } else {
            player.play();
        }
    }

    private void updatePlayPauseIcon() {
        ivPlayPauseIcon.setText(player != null && player.isPlaying() ? "⏸" : "▶");
    }

    private void startProgressUpdates() { handler.post(updateRunnable); }
    private void stopProgressUpdates()  { handler.removeCallbacks(updateRunnable); }

    private String formatTime(long ms) {
        if (ms < 0) return "0:00";
        long min = TimeUnit.MILLISECONDS.toMinutes(ms);
        long sec = TimeUnit.MILLISECONDS.toSeconds(ms) % 60;
        return String.format("%d:%02d", min, sec);
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopProgressUpdates();
        if (player != null) player.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopProgressUpdates();
        if (player != null) { player.release(); player = null; }
    }
}
