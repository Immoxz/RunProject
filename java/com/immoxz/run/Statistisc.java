package com.immoxz.run;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class Statistisc extends AppCompatActivity {
    SQLiteDatabase db;
    private String[] tableNames;
    private DataBaseManager dataBaseManager = new DataBaseManager();

    //File Manager
    FileManager fileManager = new FileManager();

    Button btnShowStats, btnDelTables;
    TextView statsResult;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistisc);

        btnShowStats = (Button) findViewById(R.id.showStats);
        btnDelTables = (Button) findViewById(R.id.delTables);
        statsResult = (TextView) findViewById(R.id.statsResult);

        if (fileManager.isExternalStorageWritable() & fileManager.isExternalStorageWritable()) {
            dataBaseManager.SetDefaultAccTables();
            tableNames = DataBaseManager.getAccTablesNames();
        } else {
            Log.e("ERROR", "db not available");
        }

        updateStatsRows();
        btnShowStats.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateStatsRows();
            }
        });
        btnDelTables.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                flushDatabase();
            }
        });
    }
    private void updateStatsRows(){
        for (int i = 0; i < tableNames.length; i++) {

            String workingTableName = tableNames[i];
//                startInserting_walk = startInserting_run = startInserting_cycle = false;
            String countQuery = "SELECT * FROM " + workingTableName + ";";
            try {
                db = dataBaseManager.getDb();
                Cursor cursor = db.rawQuery(countQuery, null);
                int cnt = cursor.getCount();
                cursor.close();
                if (i == 0) {
                    statsResult.setText("number of values in " + workingTableName + ": " + cnt);
                } else {
                    statsResult.append("\nnumber of values in " + workingTableName + ": " + cnt);
                }
                db.close();
            } catch (SQLiteException e) {
                statsResult.setText(R.string.error + e.getMessage());
            }
        }
    }

    private void flushDatabase() {
        if (tableNames.length != 0) {
            dataBaseManager.DropAllAccTables();
            dataBaseManager.SetDefaultAccTables();
            updateStatsRows();
        }
    }
}
