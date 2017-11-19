package mam.mam_project1.shaker;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;

import java.util.Random;

public class Shaker extends AppCompatActivity implements SensorEventListener {

    private static final float SHAKE_THRESHOLD = 10.0f;
    private static final int TIME_BETWEEN_SHAKES = 1000;

    SensorManager sensorManager;
    volatile long lastShakeDetectionTime;

    public int randomColor;
    public boolean shakeEventOccurred;

    public Shaker(Context applicationContext) {
        randomColor = Color.BLUE;
        lastShakeDetectionTime = System.currentTimeMillis();

        sensorManager = (SensorManager) applicationContext.getSystemService(SENSOR_SERVICE);
        Sensor accSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accSensor != null)
            sensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            long currentTime = System.currentTimeMillis();
            if ((currentTime - lastShakeDetectionTime) > TIME_BETWEEN_SHAKES) {

                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                if ((Math.sqrt(Math.pow(x, 2) + Math.pow(y, 2) + Math.pow(z, 2)) - SensorManager.GRAVITY_EARTH) > SHAKE_THRESHOLD) {
                    lastShakeDetectionTime = currentTime;
                    Random rnd = new Random();
                    randomColor = Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256));
                    shakeEventOccurred = true;

                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
