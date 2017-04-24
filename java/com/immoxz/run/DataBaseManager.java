package com.immoxz.run;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.File;

/**
 * Created by Immoxz on 2017-03-16.
 */

public class DataBaseManager extends AppCompatActivity {
    //class variables
    private SQLiteDatabase db;

    //define defoult Tables names
    //tables for STAND
    private static final String TAB_STAND_10 = "stand_values_10";
    private static final String TAB_STAND = "stand_values";
    //tables SIT
    private static final String TAB_SIT_10 = "sit_values_10";
    private static final String TAB_SIT = "sit_values";
    //tables WALK
    private static final String TAB_WALK_10 = "walk_values_10";
    private static final String TAB_WALK = "walk_values";
    //tables RUN
    private static final String TAB_RUN_10 = "run_values_10";
    private static final String TAB_RUN = "run_values";
    //tables CYCLE
    private static final String TAB_CYCLE_10 = "bicycle_values_10";
    private static final String TAB_CYCLE = "bicycle_values";
    //ALL tables
    public static final String[] ALL_TABLES_NAMES = {TAB_STAND_10, TAB_STAND, TAB_SIT_10, TAB_SIT,
            TAB_WALK_10, TAB_WALK, TAB_RUN_10, TAB_RUN, TAB_CYCLE_10, TAB_CYCLE};

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
        try {
            db = SQLiteDatabase.openDatabase(dbPath, null, SQLiteDatabase.CREATE_IF_NECESSARY);
        } catch (SQLiteException e) {
            Log.e("WARN", "not able to create db");
            System.out.println("WARN not able to create db");
        }
        return db;
    }

    public void SetDefaultAccTables() {
        boolean status = true;
        int i = 0;
        while (status) {
            try {
                db = getDb();
                while (i < ALL_TABLES_NAMES.length) {
                    db.execSQL("create table " + ALL_TABLES_NAMES[i] + " ("
                            + " recId integer Primary Key autoincrement, "
                            + " acc_x_axis DOUBLE, "
                            + " acc_y_axis DOUBLE, "
                            + " acc_z_axis DOUBLE, "
                            + " gyro_deltaRotationVector_x_axis DOUBLE, "
                            + " gyro_deltaRotationVector_y_axis DOUBLE, "
                            + " gyro_deltaRotationVector_z_axis DOUBLE, "
                            + " gyro_deltaRotationVector_t_axis DOUBLE, "
                            + " light_sensor DOUBLE, "
                            + " jack_plugged DOUBLE ); ");
                    Log.d("DONE", "create table " + ALL_TABLES_NAMES[i]);
                    i++;
                }
                db.close();
                status = false;
            } catch (SQLiteException e) {
                Log.e("ERROR", e.getMessage());
                if (e.getMessage().contains("Sqlite code 1")) {
                    status = true;
                    Log.e("ERROR", "table existed: " + ALL_TABLES_NAMES[i]);
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
                while (i < ALL_TABLES_NAMES.length) {
                    db.execSQL("DROP TABLE IF EXISTS " + ALL_TABLES_NAMES[i] + ";");
                    Log.d("done", "table " + ALL_TABLES_NAMES[i] + " dropped");
                    i++;
                }
                db.close();
                status = false;
            } catch (SQLiteException e) {
                Log.e("ERROR", e.getMessage());
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

    public void InsertToAccTable(String tableName, float[] datas) {
        boolean status = true;
        while (status) {
            try {
                db = getDb();
                db.execSQL("insert into " + tableName + " ("
                        + " acc_x_axis, "
                        + " acc_y_axis, "
                        + " acc_z_axis, "
                        + " gyro_deltaRotationVector_x_axis, "
                        + " gyro_deltaRotationVector_y_axis, "
                        + " gyro_deltaRotationVector_z_axis, "
                        + " gyro_deltaRotationVector_t_axis, "
                        + " light_sensor, "
                        + " jack_plugged ) "
                        + " values ( "
                        + datas[0] + " , "
                        + datas[1] + " , "
                        + datas[2] + " , "
                        + datas[3] + " , "
                        + datas[4] + " , "
                        + datas[5] + " , "
                        + datas[6] + " , "
                        + datas[7] + " , "
                        + datas[8] + " );");
                db.close();
                status = false;
            } catch (SQLiteException e) {
                Log.e("ERROR", e.getMessage());
            }
        }

    }

    public static String[] getAccTablesNames() {
        return ALL_TABLES_NAMES;
    }

    public String[] getAccMaxValues() {

        String[] maxValues=new String[9];

        for (int i = 0; i < ALL_TABLES_NAMES.length-1; i++) {

            String workingTableName = ALL_TABLES_NAMES[i];
//                startInserting_walk = startInserting_run = startInserting_cycle = false;
            String maxQuery = "SELECT MAX(acc_x_axis), "
                    + " MAX(acc_y_axis), "
                    + " MAX(acc_z_axis), "
                    + " MAX(gyro_deltaRotationVector_x_axis), "
                    + " MAX(gyro_deltaRotationVector_y_axis), "
                    + " MAX(gyro_deltaRotationVector_z_axis), "
                    + " MAX(gyro_deltaRotationVector_t_axis), "
                    + " MAX(light_sensor), "
                    + " MAX(jack_plugged) FROM " + workingTableName + ";";
//            String maxQuery = "SELECT * FROM " + workingTableName + ";";
            try {
                db = getDb();
                Cursor cursor = db.rawQuery(maxQuery, null);
                if (cursor != null)
                    cursor.moveToFirst();
                maxValues[i] = "\nTable: "+workingTableName+" With: "
                        +String.valueOf(cursor.getFloat(0))
                        +" | "+String.valueOf(cursor.getFloat(1))
                        +" | "+String.valueOf(cursor.getFloat(2))
                        +" | "+String.valueOf(cursor.getFloat(8));
                System.out.println(maxValues);
                cursor.close();
                db.close();
            } catch (SQLiteException e) {
                Log.e("ERROR", e.getMessage());
            }
        }
        return maxValues;
    }
}


