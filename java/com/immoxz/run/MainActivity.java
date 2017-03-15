package com.immoxz.run;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.Map;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class MainActivity extends AppCompatActivity implements SensorEventListener {
    private SensorManager mSensorManager;
    private Sensor accSensor;
    private TriggerEventListener mTriggerEventListener;
    private TextView accSensorNameView;
    private TextView accValueView;
    private TextView accMaxValueView;

    private Button moveBtn;
    private Button delBtn;
    private Button copyBtn;

    //creating values for accuracy
    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];
    private float[] max_acceleration = new float[3];

    //sqlLite database
    SQLiteDatabase db;
    private TextView dbPath;
    private File storagePath;
    private String myDBPath;
    private String myDBName;

    //File Manager
    FileManager fileManager = new FileManager();
    private String externalStoragePath;
    private String externalSDStoragePath;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //sensors things
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
        //List<Sensor> deviceSensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        //things on xml
        accSensorNameView = (TextView) findViewById(R.id.accSensorName);
        accValueView = (TextView) findViewById(R.id.accValues);
        accMaxValueView = (TextView) findViewById(R.id.accMaxValues);
        moveBtn = (Button) findViewById(R.id.moveFile);
        delBtn = (Button) findViewById(R.id.delFile);
        copyBtn = (Button) findViewById(R.id.copyFile);
        //DB
        dbPath = (TextView) findViewById(R.id.dbPath);

        mTriggerEventListener = new TriggerEventListener() {
            @Override
            public void onTrigger(TriggerEvent event) {
                System.out.println("trigger event??");
            }
        };

        mSensorManager.requestTriggerSensor(mTriggerEventListener, accSensor);
        accSensorNameView.setText(accSensor.getName());

        storagePath = getApplication().getFilesDir();
        if (fileManager.isExternalStorageWritable() & fileManager.isExternalStorageWritable()) {
            externalStoragePath = System.getenv("PRIMARY_STORAGE");
            externalSDStoragePath = System.getenv("SECONDARY_STORAGE");
            dbPath.setText(externalStoragePath + "\n" + externalSDStoragePath);
        }

        this.myDBName = "myAccValues";
        myDBPath = externalStoragePath + "/" + myDBName;
        dbPath.append("\nDB path: " + myDBPath);
        try {
            db = openOrCreateDatabase(myDBPath, MODE_PRIVATE, null);
            db.execSQL("DROP table IF EXISTS [tb_values];");
            db.execSQL("create table tb_values ("
                    + " recId integer Primary Key autoincrement, "
                    + " value_one text, "
                    + " value_two text, "
                    + " value_three text ); ");
            dbPath.append("\nINSERTING VALUES");
            db.execSQL("insert into tb_values ("
                    + " value_one, " + " value_two, " + " value_three) values ("
                    + "'TEST'" + "'TEST'" + "'TEST');");
            db.close();
            dbPath.append("\nAll Done");

        } catch (SQLiteException e) {
            dbPath.setText("\nERROR " + e.getMessage());
        }

        //button work
        moveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myDBPath = externalSDStoragePath + "/" + myDBName;
                dbPath.append("\nDB path: " + myDBPath);
                try {
                    db = openOrCreateDatabase(myDBPath, MODE_PRIVATE, null);
                    db.execSQL("DROP table IF EXISTS [tb_values];");
                    db.execSQL("create table tb_values ("
                            + " recId integer Primary Key autoincrement, "
                            + " value_one text, "
                            + " value_two text, "
                            + " value_three text ); ");
                    dbPath.append("\nINSERTING VALUES");
                    db.execSQL("insert into tb_values ("
                            + " value_one, " + " value_two, " + " value_three) values ("
                            + "'TEST'" + "'TEST'" + "'TEST');");
                    db.close();
                    dbPath.append("\nAll Done");

                } catch (SQLiteException e) {
                    dbPath.setText("\nERROR " + e.getMessage());
                }
            }
        });
        delBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                myDBPath = storagePath + "/" + myDBName;
                dbPath.append("\nDB path: " + myDBPath);
                try {
                    db = openOrCreateDatabase(myDBPath, MODE_PRIVATE, null);
                    db.execSQL("DROP table IF EXISTS [tb_values];");
                    db.execSQL("create table tb_values ("
                            + " recId integer Primary Key autoincrement, "
                            + " value_one text, "
                            + " value_two text, "
                            + " value_three text ); ");
                    dbPath.append("\nINSERTING VALUES");
                    db.execSQL("insert into tb_values ("
                            + " value_one, " + " value_two, " + " value_three) values ("
                            + "'TEST'" + "'TEST'" + "'TEST');");
                    db.close();
                    dbPath.append("\nAll Done");

                } catch (SQLiteException e) {
                    dbPath.setText("\nERROR " + e.getMessage());
                }
            }
        });
        copyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //fileManager.copyFile(dbSave[0], dbSave[1], externalStoragePath + "/" + "testDir");
            }
        });

    }

    @Override
    public final void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Do something here if sensor accuracy changes.
    }

    @Override
    public final void onSensorChanged(SensorEvent event) {
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
        accValueView.setText(linear_acceleration[0] + " " + linear_acceleration[1] + " " + linear_acceleration[2]);
        setAccMaxValue(linear_acceleration);
    }

    void setAccMaxValue(float[] values) {
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
            accMaxValueView.setText(max_acceleration[0] + " " + max_acceleration[1] + " " + max_acceleration[2]);
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}

