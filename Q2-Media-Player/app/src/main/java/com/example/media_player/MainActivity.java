package com.example.media_player;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btnOpenAudio).setOnClickListener(v ->
                startActivity(new Intent(this, AudioPlayerActivity.class)));

        findViewById(R.id.btnOpenVideo).setOnClickListener(v ->
                startActivity(new Intent(this, VideoPlayerActivity.class)));
    }
}
