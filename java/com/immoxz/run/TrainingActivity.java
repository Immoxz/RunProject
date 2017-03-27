package com.immoxz.run;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class TrainingActivity extends AppCompatActivity {//implements SensorEventListener {
    // Get the Intent that started this activity and extract the string
//    Intent intent = getIntent();
//    String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);

//    private SensorManager mSensorManager;
//    private Sensor accSensor;
    private TriggerEventListener mTriggerEventListener;
    private TextView accValueView;
    private ImageButton btnWalk, btnRun, btnCycle;
    private Button btnStop;

    //creating values for accuracy
    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];
    private float[] max_acceleration = new float[3];

    //sqlLite database
    SQLiteDatabase db;
    private TextView dbPath;
    private String storagePath;
    private String[] tableNames;
    private DataBaseManager dataBaseManager = new DataBaseManager();
    private boolean startInserting_walk = false;
    private boolean startInserting_run = false;
    private boolean startInserting_cycle = false;

    //File Manager
    FileManager fileManager = new FileManager();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        //test
        final Intent runServiceIntent = new Intent(this, MyRunService.class);

        //sensors things
//        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
//        mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        //things on xml
        accValueView = (TextView) findViewById(R.id.accValues);
        btnWalk = (ImageButton) findViewById(R.id.btnWalk);
        btnRun = (ImageButton) findViewById(R.id.btnRun);
        btnCycle = (ImageButton) findViewById(R.id.btnCycle);
        btnStop = (Button) findViewById(R.id.btnStop);
        //DB
        dbPath = (TextView) findViewById(R.id.dbPath);

        mTriggerEventListener = new TriggerEventListener() {
            @Override
            public void onTrigger(TriggerEvent event) {
                System.out.println("trigger event??");
            }
        };

//        mSensorManager.requestTriggerSensor(mTriggerEventListener, accSensor);

        if (fileManager.isExternalStorageWritable() & fileManager.isExternalStorageWritable()) {
            dataBaseManager.SetDefaultAccTables();
            this.tableNames = dataBaseManager.getAccTablesNames();
            this.storagePath = dataBaseManager.getDbPath();
            dbPath.setText("\nDB path: " + storagePath);
            dbPath.append("\nAll Done");
        } else {
            dbPath.setText("Sorry couldn't get db");
        }

        //button work
        btnWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //startInserting_walk = true;
                //startInserting_run = startInserting_cycle = false;
                startService(runServiceIntent);
            }
        });
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startInserting_run = true;
                startInserting_walk = startInserting_cycle = false;
            }
        });
        btnCycle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startInserting_cycle = true;
                startInserting_walk = startInserting_run = false;
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String workingTableName = tableNames[0];
                startInserting_walk = startInserting_run = startInserting_cycle = false;
                String countQuery = "SELECT * FROM " + workingTableName + ";";
                try {
                    db = dataBaseManager.getDb();
                    Cursor cursor = db.rawQuery(countQuery, null);
                    int cnt = cursor.getCount();
                    cursor.close();
                    dbPath.setText("number of gatherd values: " + cnt);
                    db.close();
                } catch (SQLiteException e) {
                    dbPath.setText("\nERROR " + e.getMessage());
                }
                stopService(runServiceIntent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
//        mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        try {
            this.db = dataBaseManager.getDb();
        } catch (SQLiteException e) {
            dbPath.setText("\nERROR " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
//        mSensorManager.unregisterListener(this);
        try {
            db.close();
        } catch (SQLiteException e) {
            dbPath.setText("\nERROR " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        mSensorManager.unregisterListener(this);
        try {
            db.close();
        } catch (SQLiteException e) {
            Log.e("Error", "unable to close cb");
        }
    }


//    @Override
//    public final void onSensorChanged(SensorEvent event) {
//        // In this example, alpha is calculated as t / (t + dT),
//        // where t is the low-pass filter's time-constant and
//        // dT is the event delivery rate.
//        final float alpha = 0.8f;
//        String workingTableName = "";
//
//        // Isolate the force of gravity with the low-pass filter.
//        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
//        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
//        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];
//
//        // Remove the gravity contribution with the high-pass filter.
//        linear_acceleration[0] = event.values[0] - gravity[0];
//        linear_acceleration[1] = event.values[1] - gravity[1];
//        linear_acceleration[2] = event.values[2] - gravity[2];
//        accValueView.setText(linear_acceleration[0] + " " + linear_acceleration[1] + " " + linear_acceleration[2]);
//        if (startInserting_walk) {
//            dataBaseManager.InsertToAccTable(tableNames[0], linear_acceleration);
//            workingTableName = tableNames[0];
//        }
//        if (startInserting_run) {
//            dataBaseManager.InsertToAccTable(tableNames[1], linear_acceleration);
//            workingTableName = tableNames[1];
//        }
//        if (startInserting_cycle) {
//            dataBaseManager.InsertToAccTable(tableNames[2], linear_acceleration);
//            workingTableName = tableNames[2];
//        }
//
//        //updating info
//        if (!workingTableName.isEmpty()) {
//            String countQuery = "SELECT * FROM " + workingTableName + ";";
//            try {
//                db = dataBaseManager.getDb();
//                Cursor cursor = db.rawQuery(countQuery, null);
//                int cnt = cursor.getCount();
//                cursor.close();
//                dbPath.setText("number of gatherd values: " + cnt);
//                db.close();
//            } catch (SQLiteException e) {
//                dbPath.setText("\nERROR " + e.getMessage());
//            }
//        }
//        //set maxis
//        setAccMaxValue(linear_acceleration);
//    }
//
//    @Override
//    public void onAccuracyChanged(Sensor sensor, int accuracy) {
//
//    }

    private void setAccMaxValue(float[] values) {
        if (values.length != 0) {
            if (max_acceleration[0] <= values[0]) {
                max_acceleration[0] = values[0];
            }
            if (max_acceleration[1] <= values[1]) {
                max_acceleration[1] = values[1];
            }
            if (max_acceleration[2] <= values[2]) {
                max_acceleration[2] = values[2];
            }
            //accMaxValueView.setText(max_acceleration[0] + " " + max_acceleration[1] + " " + max_acceleration[2]);
        }
    }


}

