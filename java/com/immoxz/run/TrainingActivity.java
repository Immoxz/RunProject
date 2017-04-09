package com.immoxz.run;

import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.hardware.TriggerEvent;
import android.hardware.TriggerEventListener;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class TrainingActivity extends AppCompatActivity {
    //     Get the Intent that started this activity and extract the string
//    Intent intent = getIntent();
//    String message = intent.getStringExtra(MainActivity.EXTRA_MESSAGE);
//
    private TriggerEventListener mTriggerEventListener;
    private TextView accValueView;
    private ImageButton btnWalk, btnRun, btnCycle;
    private Button btnStop;

    //service references
    Intent serviceIntent = null;
    Intent runServiceIntent = null;
    Intent walkServiceIntent = null;
    Intent cycleServiceIntent = null;

    //sqlLite database
    SQLiteDatabase db;
    private TextView dbPath;
    private String storagePath;
    private String[] tableNames;
    private DataBaseManager dataBaseManager = new DataBaseManager();

    //File Manager
    FileManager fileManager = new FileManager();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        //initial intets for services
        //runServiceIntent = new Intent(this, MyRunService.class);
        serviceIntent = new Intent(this, ActivityGenericService.class);
        walkServiceIntent = new Intent(this, MyWalkService.class);
        cycleServiceIntent = new Intent(this, MyCycleService.class);

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


        if (fileManager.isExternalStorageWritable() & fileManager.isExternalStorageWritable()) {
            dataBaseManager.SetDefaultAccTables();
            this.tableNames = DataBaseManager.getAccTablesNames();
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
                stopService(serviceIntent);
                serviceIntent.putExtra("tabNum", 4);
                serviceIntent.putExtra("serviceName", "Walk");
                startService(serviceIntent);
                //startService(walkServiceIntent);
            }
        });
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(serviceIntent);
                serviceIntent.putExtra("tabNum", 6);
                serviceIntent.putExtra("serviceName", "Run");
                startService(serviceIntent);
            }
        });
        btnCycle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(serviceIntent);
                serviceIntent.putExtra("tabNum", 8);
                serviceIntent.putExtra("serviceName", "Cycle");
                startService(serviceIntent);
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopService(serviceIntent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        try {
            this.db = dataBaseManager.getDb();
        } catch (SQLiteException e) {
            dbPath.setText("\nERROR " + e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        try {
            db.close();
        } catch (SQLiteException e) {
            dbPath.setText("\nERROR " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            db.close();
        } catch (SQLiteException e) {
            Log.e("Error", "unable to close cb");
        }
    }
}

