package com.immoxz.run;

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
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.widget.Toast;

public class MyCycleService extends Service implements SensorEventListener {

//    public MyRunService() {
//        super("MainService");
//    }

    private SensorManager mSensorManager;
    private Sensor accSensor;

    SQLiteDatabase db;
    private String[] tableNames;
    private DataBaseManager dataBaseManager = new DataBaseManager();

    //File Manager
    FileManager fileManager = new FileManager();


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Toast.makeText(this, "Cycle Service Started", Toast.LENGTH_LONG).show();
        //sensors things
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //here u should make your service foreground so it will keep working even if app closed
        if (fileManager.isExternalStorageWritable() & fileManager.isExternalStorageWritable()) {
            dataBaseManager.SetDefaultAccTables();
            this.tableNames = dataBaseManager.getAccTablesNames();
        } else {
            Log.e("ERROR", "db not available");
        }

        NotificationManager mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Intent bIntent = new Intent(MyCycleService.this, TrainingActivity.class);
        PendingIntent pbIntent = PendingIntent.getActivity(MyCycleService.this, 0, bIntent, 0);
        NotificationCompat.Builder bBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.run)
                        .setContentTitle("RunSensor")
                        .setContentText("Subtitle TEST")
                        .setAutoCancel(true)
                        .setOngoing(true)
                        .setContentIntent(pbIntent);
        Notification barNotify = bBuilder.build();
        this.startForeground(1, barNotify);
        //then you should return sticky
        return Service.START_STICKY;
    }

//
//    @Override
//    protected void onHandleIntent(@Nullable Intent intent) {
//        try {
//            Thread.sleep(5000);
//        } catch (InterruptedException e) {
//            Thread.currentThread().interrupt();
//        }
//    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        // grab the values and timestamp -- off the main thread
        new SensorEventLoggerTask().execute(event);
    }

    private class SensorEventLoggerTask extends
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
            dataBaseManager.InsertToAccTable(tableNames[5], linear_acceleration);
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
        Toast.makeText(this, "Service Stopped", Toast.LENGTH_LONG).show();
        mSensorManager.unregisterListener(this);
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
}
