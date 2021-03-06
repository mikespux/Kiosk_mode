package com.sondreweb.kiosk_mode_alpha.storage;

import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

/**
 * Ansvar for å lage Tabellen og holde oversikt over radene for Geofencer.
 */

public class GeofenceTable{

    public static final String TAG = GeofenceTable.class.getName();

    public static final String TABLE_NAME ="Geofences";

    public static final String COLUMN_GEOFENCE_ID="geofence_id"; //requestId String

    public static final String COLUMN_LATITUDE = "latitude"; //latitude double

    public static final String COLUMN_LONGITUDE = "longitude"; //longitude double

    public static final String COLUMN_RADIUS = "radius"; //radius float

    public static final String DATABASE_CREATE_GEOFENCES =
            "create table " + TABLE_NAME + "(" +
                    COLUMN_GEOFENCE_ID + " integer primary key , " +
                    COLUMN_LATITUDE + " real not null, " +
                    COLUMN_LONGITUDE + " real not null, " +
                    COLUMN_RADIUS + " real not null," +
                    " UNIQUE(" + COLUMN_LATITUDE + ", " + COLUMN_LONGITUDE + ","+ COLUMN_RADIUS + ")"+
                    ");";

    /*
    *   create table GeofenceClass(geofence_id integer primary key autoincrement,
    *           latitude real not null,
    *           longitude real not null,
    *           radius real not null,
    *           UNIQUE(latitude, longitude, radius)
    *           );
    *
    *           Grunne til at latitude, longitude og radius må være en uniq kombinasjon er siden det skal være mulig å plassere
    *           Geofence som ligger på samme sentrum, men med forskjellig radius.
    *           Vi ønsker heller ikke at vi har geofence med samme radius, longitude og latitude flere ganger.
    * */

    public static void onCreate(SQLiteDatabase database){
        database.execSQL(DATABASE_CREATE_GEOFENCES);//execute sql setning
        Log.d(TAG, "onCreate ProfileTable");
    }


    //upgrade database strukturen.
    public static void onUpgrade(SQLiteDatabase database, int oldVersion, int newVersion){
        Log.v(TAG, "Removes all data from table:"+ TABLE_NAME +" Upgrade table from version: "+oldVersion+" To: "+newVersion);
        database.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME +";"); //drop table og bytter ut med no annet, viss vi forandrer versjonen
    }

}
