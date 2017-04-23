package com.immoxz.run;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

public class TrainingActivity extends AppCompatActivity {


    //service references
    Intent serviceIntent = null;

    //sqlLite database
    SQLiteDatabase db;
    private TextView dbPath;
    private DataBaseManager dataBaseManager = new DataBaseManager();

    //File Manager
    FileManager fileManager = new FileManager();

    @SuppressLint("SetTextI18n")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_training);
        //initial intets for services
//        serviceIntent = new Intent(this, ActivityGenericIntentService.class);
        serviceIntent = new Intent(this, ActivityGenericService.class);

        //things on xml
        ImageButton btnWalk = (ImageButton) findViewById(R.id.btnWalk);
        ImageButton btnRun = (ImageButton) findViewById(R.id.btnRun);
        ImageButton btnCycle = (ImageButton) findViewById(R.id.btnCycle);
        ImageButton btnStand = (ImageButton) findViewById(R.id.btnStand);
        ImageButton btnSit = (ImageButton) findViewById(R.id.btnSit);
        Button btnStop = (Button) findViewById(R.id.btnStop);
        //DB
        dbPath = (TextView) findViewById(R.id.dbPath);

        if (fileManager.isExternalStorageWritable() & fileManager.isExternalStorageWritable()) {
            dataBaseManager.SetDefaultAccTables();
            String storagePath = dataBaseManager.getDbPath();
            dbPath.setText(R.string.db_path + storagePath);
            dbPath.append("\nAll Done");
        } else {
            dbPath.setText(R.string.db_not_found);
        }

        //button work
        btnSit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (serviceIntent != null) {
                    stopService(serviceIntent);
                }
                serviceIntent.putExtra("tabNum", 0);
                serviceIntent.putExtra("serviceName", "Sit");
                startService(serviceIntent);
            }
        });
        btnStand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (serviceIntent != null) {
                    stopService(serviceIntent);
                }
                serviceIntent.putExtra("tabNum", 2);
                serviceIntent.putExtra("serviceName", "Stand");
                startService(serviceIntent);
            }
        });
        btnWalk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceIntent != null) {
                    stopService(serviceIntent);
                }
                serviceIntent.putExtra("tabNum", 4);
                serviceIntent.putExtra("serviceName", "Walk");
                startService(serviceIntent);
            }
        });
        btnRun.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceIntent != null) {
                    stopService(serviceIntent);
                }
                serviceIntent.putExtra("tabNum", 6);
                serviceIntent.putExtra("serviceName", "Run");
                startService(serviceIntent);
            }
        });
        btnCycle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceIntent != null) {
                    stopService(serviceIntent);
                }
                serviceIntent.putExtra("tabNum", 8);
                serviceIntent.putExtra("serviceName", "Cycle");
                startService(serviceIntent);
            }
        });
        btnStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (serviceIntent != null){
                    stopService(serviceIntent);
                }
            }
        });

    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onResume() {
        super.onResume();
        try {
            this.db = dataBaseManager.getDb();
        } catch (SQLiteException e) {
            dbPath.setText(R.string.error + e.getMessage());
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void onPause() {
        super.onPause();
        try {
            db.close();
        } catch (SQLiteException e) {
            dbPath.setText(R.string.error + e.getMessage());
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

