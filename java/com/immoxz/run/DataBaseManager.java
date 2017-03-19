package com.immoxz.run;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.io.File;

/**
 * Created by Immoxz on 2017-03-16.
 */

public class DataBaseManager extends AppCompatActivity {
    //class variables
    private SQLiteDatabase db;

    //define defoult Tables names
    //tables for gather 10 sec informations
    private static final String ACC_VAL_WALK_10SEC = "acc_values_walk_10";
    private static final String ACC_VAL_RUN_10SEC = "acc_values_run_10";
    private static final String ACC_VAL_BICYCLE_10SEC = "acc_values_bicycle_10";
    //tables gathering whole data
    private static final String ACC_VAL_WALK_WALK = "acc_values_walk_full";
    private static final String ACC_VAL_RUN_FULL = "acc_values_run_full";
    private static final String ACC_VAL_BICYCLE_FULL = "acc_values_bicycle_full";
    //ALL tables
    public static final String[] ACC_TABLES_NAMES = {ACC_VAL_WALK_10SEC, ACC_VAL_RUN_10SEC, ACC_VAL_BICYCLE_10SEC, ACC_VAL_WALK_WALK, ACC_VAL_RUN_FULL, ACC_VAL_BICYCLE_FULL};

    //setting default values
    private String dbName = "RunDB";
    private File externalStorage = Environment.getExternalStorageDirectory().getAbsoluteFile();
    private File internalStorage = Environment.getDataDirectory().getAbsoluteFile();
    private String dbPath = externalStorage + "/" + dbName; //TODO: change default storage later on.

    //File Manager
    FileManager fileManager = new FileManager();

    public void setDbName(String dbName) {
        this.dbName = dbName;
    }

    public String getDbName() {
        return dbName;
    }

    //setting internal(false) or external(true) storage for application.
    public void setDbPath(boolean choice) {
        if (choice) {
            if (fileManager.isExternalStorageWritable() & fileManager.isExternalStorageWritable()) {
                this.dbPath = externalStorage + "/" + dbName;
            } else {
                this.dbPath = internalStorage + "/" + dbName;
            }
        } else {
            this.dbPath = internalStorage + "/" + dbName;
        }
    }

    public String getDbPath() {
        return dbPath;
    }

    public SQLiteDatabase getDb() {
        boolean status = true;
        while (status) {
            try {
                db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.CREATE_IF_NECESSARY);
                status = false;
            } catch (SQLiteException e) {
                Log.e("WARN", "not able to create db");
                System.out.println("WARN not able to create db");
                status = true;
            }
        }
        return db;
    }

    public void SetDefaultAccTables() {
        boolean status = true;
        int i = 0;
        while (status) {
            try {
                db = getDb();
                while (i < ACC_TABLES_NAMES.length) {
                    db.execSQL("create table " + ACC_TABLES_NAMES[i] + " ("
                            + " recId integer Primary Key autoincrement, "
                            + " x_axis DOUBLE, "
                            + " y_axis DOUBLE, "
                            + " z_axis DOUBLE ); ");
                    Log.d("DONE", "create table " + ACC_TABLES_NAMES[i]);
                    i++;
                }
                db.close();
                status = false;
            } catch (SQLiteException e) {
                Log.e("ERROR", e.getMessage());
                if (e.getMessage().contains("Sqlite code 1")) {
                    status = true;
                    Log.e("ERROR", "table existed: " + ACC_TABLES_NAMES[i]);
                    i++;
                } else {
                    status = false;
                    i--;
                }
            }
        }
    }
    public void DropAllAccTables() {
        boolean status = true;
        int i = 0;
        while (status) {
            try {
                db = getDb();
                while (i < ACC_TABLES_NAMES.length) {
                    db.execSQL("DROP TABLE IF EXISTS "+ACC_TABLES_NAMES[i]+";");
                    Log.d("done", "table " + ACC_TABLES_NAMES[i]+" droped");
                    i++;
                }
                db.close();
                status = false;
            } catch (SQLiteException e) {
                Log.d("ERROR", e.getMessage());
                if (e.getMessage().contains("Sqlite code 1")) {
                    status = true;
                    i++;
                } else {
                    status = false;
                    i--;
                }
            }
        }
    }
}

