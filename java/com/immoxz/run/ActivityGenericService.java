package com.immoxz.run;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ActivityGenericService extends Service implements SensorEventListener {

    private Sensor accSensor;
    private boolean accBoolSensor = false;
    private Sensor gyroscopeSensor;
    private boolean gyroscopeBoolSensor = false;
    private Sensor lightSensor;
    private boolean lightBoolSensor = false;

    private int tableNum;

    //  database
    private String[] tableNames;

    SensorManager mSensorManager;

    //Music receiver
    private static final String TAG = "MainActivity";
    private MusicIntentReceiver myReceiver;

    //File Manager
    FileManager fileManager = new FileManager();

    //variables for final results
    float[] raw_acceleration;
    float[] gyro_axis;
    float[] light_sensor;
    float[] headset_values;
    List<float[]> all_values = null;

    public ActivityGenericService() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        unregisterReceiver(myReceiver);
        stopSelf();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        raw_acceleration = new float[4];
        gyro_axis = new float[5];
        light_sensor = new float[2];
        headset_values = new float[1];
        all_values = new ArrayList<>();

        tableNum = intent.getIntExtra("tabNum", 0);
        String serviceName = intent.getStringExtra("serviceName");

        //setting audio reciver
        myReceiver = new MusicIntentReceiver();

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            accBoolSensor = true;
        } else {
            Log.e("ERROR", "there is no accelerometer");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            gyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            gyroscopeBoolSensor = true;
        } else {
            Log.e("ERROR", "there is no gravity sensor");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            lightBoolSensor = true;
        } else {
            Log.e("ERROR", "there is no light sensor");
        }
        //register all sensors
        if (accBoolSensor)
            mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (gyroscopeBoolSensor)
            mSensorManager.registerListener(this, gyroscopeSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (lightBoolSensor)
            mSensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

        if (fileManager.isExternalStorageWritable() & fileManager.isExternalStorageWritable()) {
            tableNames = DataBaseManager.getAccTablesNames();
        } else {
            Log.e("ERROR", "db not available");
        }

        Intent bIntent = new Intent(ActivityGenericService.this, TrainingActivity.class);
        PendingIntent pbIntent = PendingIntent.getActivity(ActivityGenericService.this, 0, bIntent, 0);
        NotificationCompat.Builder bBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.run)
                        .setContentTitle(serviceName + "Sensor")
                        .setContentText("Service to gather human activity properties")
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .setContentIntent(pbIntent);
        Notification barNotify = bBuilder.build();
        this.startForeground(1, barNotify);

        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);

        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            raw_acceleration[0] = event.values[0];
            raw_acceleration[1] = event.values[1];
            raw_acceleration[2] = event.values[2];
            //adding max value of Sensors to range to process it later
            raw_acceleration[3] = sensor.getMaximumRange();
        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            gyro_axis[0] = event.values[0];
            gyro_axis[1] = event.values[1];
            gyro_axis[2] = event.values[2];
            gyro_axis[3] = event.timestamp;
        } else if (sensor.getType() == Sensor.TYPE_LIGHT) {
            light_sensor[0] = event.values[0];
            //adding max value of Sensors to range to process it later
            light_sensor[1] = sensor.getMaximumRange();
        }

        headset_values[0] = myReceiver.getHeadset_val();
        if (all_values.size() == 0) {
            all_values.add(raw_acceleration);
        } else {
            all_values.set(0, raw_acceleration);
        }
        if (all_values.size() == 1) {
            all_values.add(gyro_axis);
        } else {
            all_values.set(1, gyro_axis);
        }
        if (all_values.size() == 2) {
            all_values.add(light_sensor);
        } else {
            all_values.set(2, light_sensor);
        }
        if (all_values.size() == 3) {
            all_values.add(headset_values);
        } else {
            all_values.set(3, headset_values);
        }

        new SensorEventLoggerTask().execute(all_values);
    }

    private class SensorEventLoggerTask extends
            AsyncTask<List<float[]>, Void, Void> {

        private DataBaseManager dataBaseManager = new DataBaseManager();

        //creating values for accuracy
        private float[] gravity = new float[3];
        private float[] linear_acceleration = new float[3];
        private float[] raw_acceleration;
        private float max_acceleration;
        //gyro
        static final float EPSILON = 0.000000001f;
        static final float NS2S = 1.0f / 1000000000.0f;
        final float[] deltaRotationVector = new float[4];
        float timestamp;
        float[] gyro_axis = new float[4];

        private float[] final_values = new float[9];

        @SafeVarargs
        @Override
        protected final Void doInBackground(List<float[]>... events) {
            raw_acceleration = events[0].get(0);
            max_acceleration = events[0].get(0)[3];
            // In this example, alpha is calculated as t / (t + dT),
            // where t is the low-pass filter's time-constant and
            // dT is the event delivery rate.
            final float alpha = 0.8f;

            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * raw_acceleration[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * raw_acceleration[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * raw_acceleration[2];

            // Remove the gravity contribution with the high-pass filter.
            linear_acceleration[0] = raw_acceleration[0] + max_acceleration - gravity[0];
            linear_acceleration[1] = raw_acceleration[1] + max_acceleration - gravity[1];
            linear_acceleration[2] = raw_acceleration[2] + max_acceleration - gravity[2];

            gyro_axis = events[0].get(1);
            // This timestep's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp != 0) {
                final float dT = (gyro_axis[4] - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = gyro_axis[0];
                float axisY = gyro_axis[1];
                float axisZ = gyro_axis[2];

                // Calculate the angular speed of the sample
                float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

                // Normalize the rotation vector if it's big enough to get the axis
                // (that is, EPSILON should represent your maximum allowable margin of error)
                if (omegaMagnitude > EPSILON) {
                    axisX /= omegaMagnitude;
                    axisY /= omegaMagnitude;
                    axisZ /= omegaMagnitude;
                }

                // Integrate around this axis with the angular speed by the timestep
                // in order to get a delta rotation from this sample over the timestep
                // We will convert this axis-angle representation of the delta rotation
                // into a quaternion before turning it into the rotation matrix.
                float thetaOverTwo = omegaMagnitude * dT / 2.0f;
                float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
                float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
                deltaRotationVector[0] = sinThetaOverTwo * axisX;
                deltaRotationVector[1] = sinThetaOverTwo * axisY;
                deltaRotationVector[2] = sinThetaOverTwo * axisZ;
                deltaRotationVector[3] = cosThetaOverTwo;
                //TODO:SKONCZYC POMYSLEC O CI JAK i tak nie działa na moim urzadzeniu.
            }
            timestamp = gyro_axis[3];
            final_values[0] = normalizeVariables(linear_acceleration[0], max_acceleration);
            final_values[1] = normalizeVariables(linear_acceleration[1], max_acceleration);
            final_values[2] = normalizeVariables(linear_acceleration[2], max_acceleration);
            //mocking max result for gyro
            final_values[3] = normalizeVariables(deltaRotationVector[0],100);
            final_values[4] = normalizeVariables(deltaRotationVector[1],100);
            final_values[5] = normalizeVariables(deltaRotationVector[2],100);
            final_values[6] = normalizeVariables(deltaRotationVector[3],100);
            final_values[7] = normalizeVariables(events[0].get(2)[0],events[0].get(2)[1]);
            final_values[8] = events[0].get(3)[0];

            dataBaseManager.InsertToAccTable(tableNames[tableNum], final_values);

            return null;
        }

        public float normalizeVariables(float value, float maxval) {
            float normalizedResult;
            if (value / (2 * maxval) < 0) {
                normalizedResult = 0;
            } else if (value / (2 * maxval) > 1) {
                normalizedResult = 1;
            } else {
                normalizedResult = value / (2 * maxval);
            }
            return normalizedResult;
        }
    }

    private class MusicIntentReceiver extends BroadcastReceiver {

        private int headset_val = 0;

        public int getHeadset_val() {
            return headset_val;
        }

        public void setHeadset_val(int headset_val) {
            this.headset_val = headset_val;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_HEADSET_PLUG)) {
                int state = intent.getIntExtra("state", -1);
                switch (state) {
                    case 0:
                        setHeadset_val(0);
                        break;
                    case 1:
                        setHeadset_val(1);
                        break;
                    default:
                        Log.d("headsetLOG", "I have no idea what the headset state is");
                }
            }
        }
    }
}
