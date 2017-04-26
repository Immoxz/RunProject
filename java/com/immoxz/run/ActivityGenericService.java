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
import android.os.PowerManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ActivityGenericService extends Service implements SensorEventListener {

    private Sensor accSensor;
    private Sensor lightSensor;

    private int tableNum;

    //  database
    private String[] tableNames;

    SensorManager mSensorManager;

    //    music resive
    private MusicIntentReceiver myReceiver;

    //File Manager
    FileManager fileManager = new FileManager();

    //variables for acc change
    float[] raw_acceleration;
    float[] gyro_axis;
    float[] light_sensor;
    float[] headset_values;
    List<float[]> all_values;

    //keeping aplication alive
    PowerManager mgr;
    PowerManager.WakeLock wakeLock;

    public ActivityGenericService() {
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener(this);
        unregisterReceiver(myReceiver);
        wakeLock.release();
        stopSelf();
    }

    @Override
    public void onCreate() {
        super.onCreate();

        //inicializing variables
        raw_acceleration = new float[4];
        gyro_axis = new float[4];
        light_sensor = new float[2];
        headset_values = new float[1];
        all_values = new ArrayList<>();

        //setting audio reciver
        myReceiver = new MusicIntentReceiver();

        //creating sensor manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        //makeing power manager
        mgr = (PowerManager) this.getSystemService(POWER_SERVICE);
        wakeLock = mgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MyWakeLock");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        tableNum = intent.getIntExtra("tabNum", 0);
        String serviceName = intent.getStringExtra("serviceName");

        Intent bIntent = new Intent(ActivityGenericService.this, TrainingActivity.class);
        PendingIntent pbIntent = PendingIntent.getActivity(ActivityGenericService.this, 0, bIntent, 0);
        NotificationCompat.Builder bBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.run)
                        .setContentTitle(serviceName + "Sensor")
                        .setContentText("Service to gather human activity properties")
                        .setOngoing(true)
                        .setContentIntent(pbIntent);
        Notification barNotify = bBuilder.build();
        this.startForeground(1, barNotify);

        runSensorsScanning();
        checkAndFillTables();
        setAndRegisterMusicReciver();

        wakeLock.acquire();

        return super.onStartCommand(intent, flags, startId);
    }

    protected void setAndRegisterMusicReciver() {
        IntentFilter filter = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
        registerReceiver(myReceiver, filter);
    }

    protected void checkAndFillTables() {
        if (fileManager.isExternalStorageWritable() & fileManager.isExternalStorageWritable()) {
            tableNames = DataBaseManager.getAccTablesNames();
        } else {
            Log.e("ERROR", "db not available");
        }
    }

    protected void runSensorsScanning() {

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        } else {
            Log.e("ERROR", "there is no accelerometer");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null) {
            lightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
        } else {
            Log.e("ERROR", "there is no light sensor");
        }

        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null)
            mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT) != null)
            mSensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);

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
        // grab the values and timestamp -- off the main thread

        Sensor sensor = event.sensor;

        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            raw_acceleration[0] = event.values[0];
            raw_acceleration[1] = event.values[1];
            raw_acceleration[2] = event.values[2];
            raw_acceleration[3] = sensor.getMaximumRange();

            //gyro is never listened, no need to check
//        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
//            gyro_axis[0] = event.values[0];
//            gyro_axis[1] = event.values[1];
//            gyro_axis[2] = event.values[2];
//            gyro_axis[3] = event.timestamp;
        } else if (sensor.getType() == Sensor.TYPE_LIGHT) {
            light_sensor[0] = event.values[0];
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
        float timestamp;
        float[] gyro_axis = new float[4];
        //gyro
        static final float EPSILON = 0.000000001f;
        static final float NS2S = 1.0f / 1000000000.0f;
        private float[] final_values = new float[9];

        @SafeVarargs
        @Override
        protected final Void doInBackground(List<float[]>... events) {
            // In this example, alpha is calculated as t / (t + dT),
            // where t is the low-pass filter's time-constant and
            // dT is the event delivery rate.
            final float alpha = 0.8f;

            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * events[0].get(0)[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * events[0].get(0)[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * events[0].get(0)[2];

            // Remove the gravity contribution with the high-pass filter.
            linear_acceleration[0] = events[0].get(0)[0] + events[0].get(0)[3] - gravity[0];
            linear_acceleration[1] = events[0].get(0)[1] + events[0].get(0)[3] - gravity[1];
            linear_acceleration[2] = events[0].get(0)[2] + events[0].get(0)[3] - gravity[2];

            gyro_axis = events[0].get(1);
            // This timestep's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp != 0) {

                final float[] deltaRotationVector = new float[4];
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
            final_values[0] = normalizeVariables(linear_acceleration[0], events[0].get(0)[3]);
            final_values[1] = normalizeVariables(linear_acceleration[1], events[0].get(0)[3]);
            final_values[2] = normalizeVariables(linear_acceleration[2], events[0].get(0)[3]);
            final_values[7] = normalizeVariables(events[0].get(2)[0] + events[0].get(2)[1], events[0].get(2)[1]);
            final_values[8] = events[0].get(3)[0];

            dataBaseManager.InsertToAccTable(tableNames[tableNum], final_values);

            return null;
        }
    }

    public float normalizeVariables(float value, float maxval) {
        float normalizedResult;
        if (value != maxval || value != 0.0f || maxval != 0.0f) {

            if (value / (2 * maxval) < 0f) {
                normalizedResult = 0;
            } else if (value / (2 * maxval) > 1f) {
                normalizedResult = 1;
            } else {
                normalizedResult = value / (2 * maxval);
                normalizedResult = (float) ((int) (normalizedResult * 100000f)) / 100000f;
            }
        } else {
            normalizedResult = 0.5f;
        }
        return normalizedResult;
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
