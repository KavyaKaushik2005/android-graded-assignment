package com.example.currencyconverter;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

public class MainActivity extends AppCompatActivity {

    private static final double INR_TO_USD = 0.012;
    private static final double INR_TO_JPY = 1.78;
    private static final double INR_TO_EUR = 0.011;

    private static final String PREFS_NAME = "AppSettings";
    private static final String KEY_THEME  = "theme";

    private Spinner spinnerFrom, spinnerTo;
    private EditText etSpinnerInput;
    private TextView tvSpinnerResult;
    private TextView tvRateHint;

    private final String[] CURRENCIES = {"INR", "USD", "JPY", "EUR"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeFromPrefs();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        spinnerFrom     = findViewById(R.id.spinnerFrom);
        spinnerTo       = findViewById(R.id.spinnerTo);
        etSpinnerInput  = findViewById(R.id.etSpinnerInput);
        tvSpinnerResult = findViewById(R.id.tvSpinnerResult);
        tvRateHint      = findViewById(R.id.tvRateHint);

        ImageButton btnSettings = findViewById(R.id.btnSettings);
        Button btnSwap      = findViewById(R.id.btnSwap);
        Button btnClear     = findViewById(R.id.btnClear);
        Button btnBackspace = findViewById(R.id.btnBackspace);
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

        // Custom adapter so spinner text is visible on dark/pastel background
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, R.layout.spinner_item, CURRENCIES);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        spinnerFrom.setAdapter(adapter);
        spinnerTo.setAdapter(adapter);
        spinnerTo.setSelection(1);

        AdapterView.OnItemSelectedListener spinnerListener = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> p, View v, int pos, long id) {
                updateSpinnerResult();
                updateRateHint();
            }
            @Override public void onNothingSelected(AdapterView<?> p) {}
        };
        spinnerFrom.setOnItemSelectedListener(spinnerListener);
        spinnerTo.setOnItemSelectedListener(spinnerListener);

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(this, SettingsActivity.class)));

        btnSwap.setOnClickListener(v -> {
            int fromPos = spinnerFrom.getSelectedItemPosition();
            int toPos   = spinnerTo.getSelectedItemPosition();
            spinnerFrom.setSelection(toPos);
            spinnerTo.setSelection(fromPos);
        });

        btn0.setOnClickListener(v -> appendToInput("0"));
        btn1.setOnClickListener(v -> appendToInput("1"));
        btn2.setOnClickListener(v -> appendToInput("2"));
        btn3.setOnClickListener(v -> appendToInput("3"));
        btn4.setOnClickListener(v -> appendToInput("4"));
        btn5.setOnClickListener(v -> appendToInput("5"));
        btn6.setOnClickListener(v -> appendToInput("6"));
        btn7.setOnClickListener(v -> appendToInput("7"));
        btn8.setOnClickListener(v -> appendToInput("8"));
        btn9.setOnClickListener(v -> appendToInput("9"));

        btnDot.setOnClickListener(v -> {
            String current = etSpinnerInput.getText().toString();
            if (!current.contains(".")) {
                appendToInput(current.isEmpty() ? "0." : ".");
            }
        });

        btnClear.setOnClickListener(v -> {
            etSpinnerInput.setText("");
            tvSpinnerResult.setText("0");
        });

        btnBackspace.setOnClickListener(v -> {
            String current = etSpinnerInput.getText().toString();
            if (!current.isEmpty()) {
                String newVal = current.substring(0, current.length() - 1);
                etSpinnerInput.setText(newVal);
                etSpinnerInput.setSelection(newVal.length());
                updateSpinnerResult();
            }
        });

        etSpinnerInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) { updateSpinnerResult(); }
        });

        updateRateHint();
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

    private void appendToInput(String character) {
        String current = etSpinnerInput.getText().toString();
        if (current.equals("0") && !character.equals(".")) current = "";
        String newVal = current + character;
        etSpinnerInput.setText(newVal);
        etSpinnerInput.setSelection(newVal.length());
    }

    private void updateSpinnerResult() {
        String text = etSpinnerInput.getText().toString().trim();
        if (text.isEmpty() || text.equals(".")) {
            tvSpinnerResult.setText("0");
            return;
        }
        double inputVal;
        try { inputVal = Double.parseDouble(text); }
        catch (NumberFormatException e) { return; }

        int fromPos = spinnerFrom.getSelectedItemPosition();
        int toPos   = spinnerTo.getSelectedItemPosition();
        double inrVal = toInr(inputVal, fromPos);
        double result = fromInr(inrVal, toPos);
        tvSpinnerResult.setText(formatNumber(result));
    }

    private void updateRateHint() {
        int fromPos = spinnerFrom.getSelectedItemPosition();
        int toPos   = spinnerTo.getSelectedItemPosition();
        double rate = fromInr(toInr(1.0, fromPos), toPos);
        tvRateHint.setText("1 " + CURRENCIES[fromPos] + " = " + formatNumber(rate) + " " + CURRENCIES[toPos]);
    }

    private double toInr(double value, int pos) {
        switch (pos) {
            case 0: return value;
            case 1: return value / INR_TO_USD;
            case 2: return value / INR_TO_JPY;
            case 3: return value / INR_TO_EUR;
            default: return value;
        }
    }

    private double fromInr(double inrValue, int pos) {
        switch (pos) {
            case 0: return inrValue;
            case 1: return inrValue * INR_TO_USD;
            case 2: return inrValue * INR_TO_JPY;
            case 3: return inrValue * INR_TO_EUR;
            default: return inrValue;
        }
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