package com.immoxz.run;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class MyRunService extends IntentService implements SensorEventListener, SensorListener {

    public MyRunService() {
        super("MyRunService");
    }

    private SensorManager mSensorManager;
    private Sensor accSensor;
    private boolean accBoolSensor = false;
    private Sensor gyroscopSensor;
    private boolean gyroscopeBoolSensor = false;
    private Sensor lightSensor;
    private boolean lightBoolSensor = false;


    SQLiteDatabase db;
    private String[] tableNames;
    private DataBaseManager dataBaseManager = new DataBaseManager();

    //File Manager
    FileManager fileManager = new FileManager();


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Run Service Started", Toast.LENGTH_LONG).show();
        //sensors things
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null) {
            accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            accBoolSensor = true;
        } else {
            Log.e("ERROR", "there is no accelerometer");
        }
        if (mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) != null) {
            gyroscopSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
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
            mSensorManager.registerListener(this, gyroscopSensor, SensorManager.SENSOR_DELAY_NORMAL);
        if (lightBoolSensor)
            mSensorManager.registerListener(this, lightSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //here u should make your service foreground so it will keep working even if app closed
        if (fileManager.isExternalStorageWritable() & fileManager.isExternalStorageWritable()) {
            dataBaseManager.SetDefaultAccTables();
            this.tableNames = DataBaseManager.getAccTablesNames();
        } else {
            Log.e("ERROR", "db not available");
        }

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent bIntent = new Intent(MyRunService.this, TrainingActivity.class);
        PendingIntent pbIntent = PendingIntent.getActivity(MyRunService.this, 0, bIntent, 0);
        NotificationCompat.Builder bBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.run)
                        .setContentTitle("RunSensor")
                        .setContentText("Service to gather your run properties")
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .setContentIntent(pbIntent);
        Notification barNotify = bBuilder.build();
        this.startForeground(1, barNotify);
        //then you should return sticky
        return Service.START_STICKY;
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        Sensor sensor = event.sensor;
        if (sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            new SensorEventLoggerTaskAccelerometer().execute(event);
        } else if (sensor.getType() == Sensor.TYPE_GYROSCOPE) {
            new SensorEventLoggerTaskGyroscope().execute(event);
        } else if (sensor.getType() == Sensor.TYPE_LIGHT) {
            //TODO: get values
        }
        // stop the service
    }

    @Override
    public void onSensorChanged(int i, float[] floats) {

    }

    @Override
    public void onAccuracyChanged(int i, int i1) {

    }

    private class SensorEventLoggerTaskAccelerometer extends
            AsyncTask<SensorEvent, Object, Void> {

        private DataBaseManager dataBaseManager = new DataBaseManager();
        //creating values for accuracy
        private float[] gravity = new float[3];
        private float[] linear_acceleration = new float[3];

        @Override
        protected Void doInBackground(SensorEvent... events) {
            SensorEvent event = events[0];
            // In this example, alpha is calculated as t / (t + dT),
            // where t is the low-pass filter's time-constant and
            // dT is the event delivery rate.
            final float alpha = 0.8f;

            // Isolate the force of gravity with the low-pass filter.
            gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
            gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
            gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

            // Remove the gravity contribution with the high-pass filter.
            linear_acceleration[0] = event.values[0] - gravity[0];
            linear_acceleration[1] = event.values[1] - gravity[1];
            linear_acceleration[2] = event.values[2] - gravity[2];
            dataBaseManager.InsertToAccTable(tableNames[4], linear_acceleration);
            //setAccMaxValue(linear_acceleration);
            return null;
        }
    }


    private class SensorEventLoggerTaskGyroscope extends
            AsyncTask<SensorEvent, Object, Void> {

        private DataBaseManager dataBaseManager = new DataBaseManager();
        public static final float EPSILON = 0.000000001f;
        private static final float NS2S = 1.0f / 1000000000.0f;
        private final float[] deltaRotationVector = new float[4];
        private float timestamp;


        @Override
        protected Void doInBackground(SensorEvent... events) {
            SensorEvent event = events[0];
            // This timestep's delta rotation to be multiplied by the current rotation
            // after computing it from the gyro sample data.
            if (timestamp != 0) {
                final float dT = (event.timestamp - timestamp) * NS2S;
                // Axis of the rotation sample, not normalized yet.
                float axisX = event.values[0];
                float axisY = event.values[1];
                float axisZ = event.values[2];

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
                //deltaRotationVector[3] = cosThetaOverTwo;
                //TODO:SKONCZYC POMYSLEC O CI JAK i tak nie działa na moim urzadzeniu.
            }
            timestamp = event.timestamp;
            dataBaseManager.InsertToAccTable(tableNames[1], deltaRotationVector);
            //setAccMaxValue(linear_acceleration);
            return null;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        mSensorManager.unregisterListener((SensorEventListener) this);
        try {
            db.close();
        } catch (SQLiteException e) {
            Log.e("Error", "unable to close cb or DB closed");
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

    }
}
