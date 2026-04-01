package com.example.currencyconverter; // ← Change this to your actual package name

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    // ── Exchange rates relative to INR ──
    private static final double INR_TO_USD = 0.012;
    private static final double INR_TO_JPY = 1.78;
    private static final double INR_TO_EUR = 0.011;

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_THEME  = "theme"; // "dark" or "light"

    private EditText etINR, etUSD, etJPY, etEUR;
    private boolean isUpdating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Apply saved theme BEFORE setContentView
        applyThemeFromPrefs();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ── Bind views ──
        etINR = findViewById(R.id.etINR);
        etUSD = findViewById(R.id.etUSD);
        etJPY = findViewById(R.id.etJPY);
        etEUR = findViewById(R.id.etEUR);

        ImageButton btnSettings = findViewById(R.id.btnSettings);
        Button btnClear     = findViewById(R.id.btnClear);
        Button btnBackspace = findViewById(R.id.btnBackspace);
        Button btnPercent   = findViewById(R.id.btnPercent);
        Button btnDivide    = findViewById(R.id.btnDivide);
        Button btnMultiply  = findViewById(R.id.btnMultiply);
        Button btnMinus     = findViewById(R.id.btnMinus);
        Button btnPlus      = findViewById(R.id.btnPlus);
        Button btnEquals    = findViewById(R.id.btnEquals);
        Button btn0  = findViewById(R.id.btn0);
        Button btn1  = findViewById(R.id.btn1);
        Button btn2  = findViewById(R.id.btn2);
        Button btn3  = findViewById(R.id.btn3);
        Button btn4  = findViewById(R.id.btn4);
        Button btn5  = findViewById(R.id.btn5);
        Button btn6  = findViewById(R.id.btn6);
        Button btn7  = findViewById(R.id.btn7);
        Button btn8  = findViewById(R.id.btn8);
        Button btn9  = findViewById(R.id.btn9);
        Button btnDot = findViewById(R.id.btnDot);

        // ── Active field tracking ──
        final EditText[] activeField = {etINR};
        etINR.setOnClickListener(v -> activeField[0] = etINR);
        etUSD.setOnClickListener(v -> activeField[0] = etUSD);
        etJPY.setOnClickListener(v -> activeField[0] = etJPY);
        etEUR.setOnClickListener(v -> activeField[0] = etEUR);

        // ── Settings button → show theme dialog ──
        btnSettings.setOnClickListener(v -> showThemeDialog());

        // ── Digit buttons ──
        btn0.setOnClickListener(v -> appendToField(activeField[0], "0"));
        btn1.setOnClickListener(v -> appendToField(activeField[0], "1"));
        btn2.setOnClickListener(v -> appendToField(activeField[0], "2"));
        btn3.setOnClickListener(v -> appendToField(activeField[0], "3"));
        btn4.setOnClickListener(v -> appendToField(activeField[0], "4"));
        btn5.setOnClickListener(v -> appendToField(activeField[0], "5"));
        btn6.setOnClickListener(v -> appendToField(activeField[0], "6"));
        btn7.setOnClickListener(v -> appendToField(activeField[0], "7"));
        btn8.setOnClickListener(v -> appendToField(activeField[0], "8"));
        btn9.setOnClickListener(v -> appendToField(activeField[0], "9"));

        btnDot.setOnClickListener(v -> {
            String current = activeField[0].getText().toString();
            if (!current.contains(".")) {
                appendToField(activeField[0], current.isEmpty() ? "0." : ".");
            }
        });

        btnClear.setOnClickListener(v -> {
            isUpdating = true;
            etINR.setText("");
            etUSD.setText("");
            etJPY.setText("");
            etEUR.setText("");
            isUpdating = false;
        });

        btnBackspace.setOnClickListener(v -> {
            String current = activeField[0].getText().toString();
            if (!current.isEmpty()) {
                String newVal = current.substring(0, current.length() - 1);
                activeField[0].setText(newVal);
                activeField[0].setSelection(newVal.length());
                convertFrom(activeField[0]);
            }
        });

        btnPercent.setOnClickListener(v -> {
            String current = activeField[0].getText().toString();
            if (!current.isEmpty()) {
                try {
                    double val = Double.parseDouble(current) / 100.0;
                    activeField[0].setText(formatNumber(val));
                    convertFrom(activeField[0]);
                } catch (NumberFormatException ignored) {}
            }
        });

        btnDivide.setOnClickListener(v -> {});
        btnMultiply.setOnClickListener(v -> {});
        btnMinus.setOnClickListener(v -> {});
        btnPlus.setOnClickListener(v -> {});
        btnEquals.setOnClickListener(v -> convertFrom(activeField[0]));

        // ── TextWatchers ──
        etINR.addTextChangedListener(makeWatcher(etINR));
        etUSD.addTextChangedListener(makeWatcher(etUSD));
        etJPY.addTextChangedListener(makeWatcher(etJPY));
        etEUR.addTextChangedListener(makeWatcher(etEUR));
    }

    // ────────────────────────────────────────────
    //  THEME
    // ────────────────────────────────────────────

    private void applyThemeFromPrefs() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String theme = prefs.getString(KEY_THEME, "dark");
        if (theme.equals("light")) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        }
    }

    private void showThemeDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentTheme = prefs.getString(KEY_THEME, "dark");
        int checkedItem = currentTheme.equals("light") ? 1 : 0;
        String[] options = {"🌙  Dark Mode", "☀️  Light Mode"};

        new AlertDialog.Builder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(options, checkedItem, (dialog, which) -> {
                    String selected = (which == 1) ? "light" : "dark";
                    // Save preference
                    prefs.edit().putString(KEY_THEME, selected).apply();
                    // Apply — this recreates the activity automatically
                    if (selected.equals("light")) {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                    } else {
                        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                    }
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ────────────────────────────────────────────
    //  CONVERTER
    // ────────────────────────────────────────────

    private void appendToField(EditText field, String character) {
        String current = field.getText().toString();
        if (current.equals("0") && !character.equals(".")) current = "";
        String newVal = current + character;
        field.setText(newVal);
        field.setSelection(newVal.length());
    }

    private TextWatcher makeWatcher(EditText source) {
        return new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                if (!isUpdating) convertFrom(source);
            }
        };
    }

    private void convertFrom(EditText source) {
        if (isUpdating) return;
        String text = source.getText().toString().trim();
        if (text.isEmpty() || text.equals(".")) {
            isUpdating = true;
            if (source != etINR) etINR.setText("");
            if (source != etUSD) etUSD.setText("");
            if (source != etJPY) etJPY.setText("");
            if (source != etEUR) etEUR.setText("");
            isUpdating = false;
            return;
        }
        double inputValue;
        try {
            inputValue = Double.parseDouble(text);
        } catch (NumberFormatException e) {
            return;
        }
        double inrValue;
        if      (source == etINR) inrValue = inputValue;
        else if (source == etUSD) inrValue = inputValue / INR_TO_USD;
        else if (source == etJPY) inrValue = inputValue / INR_TO_JPY;
        else                      inrValue = inputValue / INR_TO_EUR;

        isUpdating = true;
        if (source != etINR) etINR.setText(formatNumber(inrValue));
        if (source != etUSD) etUSD.setText(formatNumber(inrValue * INR_TO_USD));
        if (source != etJPY) etJPY.setText(formatNumber(inrValue * INR_TO_JPY));
        if (source != etEUR) etEUR.setText(formatNumber(inrValue * INR_TO_EUR));
        isUpdating = false;
    }

    private String formatNumber(double value) {
        if (value == 0) return "0";
        String result;
        if      (value >= 1000) result = String.format("%.2f", value);
        else if (value >= 1)    result = String.format("%.4f", value);
        else                    result = String.format("%.6f", value);
        if (result.contains(".")) {
            result = result.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return result;
    }
}