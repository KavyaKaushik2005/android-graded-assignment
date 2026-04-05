package com.example.currencyconverter;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_THEME  = "theme";

    private Button btnDarkMode;
    private Button btnLightMode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeFromPrefs();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        btnDarkMode  = findViewById(R.id.btnDarkMode);
        btnLightMode = findViewById(R.id.btnLightMode);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String current = prefs.getString(KEY_THEME, "dark");

        // Highlight whichever is currently active
        updateButtonStates(current);

        btnDarkMode.setOnClickListener(v -> applyTheme("dark", prefs));
        btnLightMode.setOnClickListener(v -> applyTheme("light", prefs));
    }

    private void applyTheme(String theme, SharedPreferences prefs) {
        prefs.edit().putString(KEY_THEME, theme).apply();
        if (theme.equals("dark")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
        recreate();
    }

    private void updateButtonStates(String activeTheme) {
        // Active button: accent border + full opacity
        // Inactive button: dimmed opacity so the active choice is obvious
        if (activeTheme.equals("dark")) {
            btnDarkMode.setAlpha(1.0f);
            btnLightMode.setAlpha(0.4f);
        } else {
            btnDarkMode.setAlpha(0.4f);
            btnLightMode.setAlpha(1.0f);
        }
    }

    private void applyThemeFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String theme = prefs.getString(KEY_THEME, "dark");
        if (theme.equals("light")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }
}