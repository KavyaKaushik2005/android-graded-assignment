package com.example.sensor_app;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    SensorManager sensorManager;
    Sensor accel, light, proximity;

    TextView tvX, tvY, tvZ;
    TextView tvLight, tvLightCond;
    TextView tvProx, tvProxState;

    ProgressBar pbX, pbY, pbZ, pbLight, pbProx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Accelerometer views
        tvX   = findViewById(R.id.tvAccelX);
        tvY   = findViewById(R.id.tvAccelY);
        tvZ   = findViewById(R.id.tvAccelZ);
        pbX   = findViewById(R.id.pbAccelX);
        pbY   = findViewById(R.id.pbAccelY);
        pbZ   = findViewById(R.id.pbAccelZ);

        // Light views
        tvLight     = findViewById(R.id.tvLightValue);
        tvLightCond = findViewById(R.id.tvLightCondition);
        pbLight     = findViewById(R.id.pbLight);

        // Proximity views
        tvProx      = findViewById(R.id.tvProxValue);
        tvProxState = findViewById(R.id.tvProxState);
        pbProx      = findViewById(R.id.pbProximity);

        // Sensor Manager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel     = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        light     = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        proximity = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Register all sensors
        sensorManager.registerListener(this, accel,     SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, light,     SensorManager.SENSOR_DELAY_UI);
        sensorManager.registerListener(this, proximity, SensorManager.SENSOR_DELAY_UI);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister to save battery
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            tvX.setText("X-Axis : " + String.format("%.3f", x) + " m/s²");
            tvY.setText("Y-Axis : " + String.format("%.3f", y) + " m/s²");
            tvZ.setText("Z-Axis : " + String.format("%.3f", z) + " m/s²");

            // Map range [-20, 20] to [0, 1000]
            pbX.setProgress((int) ((x + 20) * 25));
            pbY.setProgress((int) ((y + 20) * 25));
            pbZ.setProgress((int) ((z + 20) * 25));

        } else if (event.sensor.getType() == Sensor.TYPE_LIGHT) {

            float lux = event.values[0];

            tvLight.setText((int) lux + " lux");
            tvLightCond.setText("Condition : " + getLightLabel(lux));
            pbLight.setProgress(Math.min((int)(lux / 10), 1000));

        } else if (event.sensor.getType() == Sensor.TYPE_PROXIMITY) {

            float dist  = event.values[0];
            float range = proximity.getMaximumRange();

            tvProx.setText(String.format("%.1f", dist) + " cm");
            tvProxState.setText("Status : " + (dist < range ? "Object Nearby" : "No Object Nearby"));
            pbProx.setProgress((int)((dist / range) * 100));
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed for this assignment
    }

    // Returns a human-readable light label
    private String getLightLabel(float lux) {
        if (lux < 10)   return "Dark";
        if (lux < 200)  return "Dim";
        if (lux < 1000) return "Normal";
        if (lux < 5000) return "Bright";
        return "Very Bright";
    }
}