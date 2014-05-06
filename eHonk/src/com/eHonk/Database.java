package com.eHonk;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database extends SQLiteOpenHelper {
	
	public class OffenseRecord {
		private int notification_id;
		private String offender_license;
		private String timestamp;
		private int status_code;
		private String other_details;
		private int retries_count;
		
		public String getOffenderLicense() {
	    return offender_license;
    }
		public void setOffenderLicense(String offender_license) {
	    this.offender_license = offender_license;
    }
		public String getTimestamp() {
	    return timestamp;
    }
		public void setTimestamp(String timestamp) {
	    this.timestamp = timestamp;
    }
		public int getStatusCode() {
	    return status_code;
    }
		public void setStatusCode(int status_code) {
	    this.status_code = status_code;
    }
		public String getOtherDetails() {
	    return other_details;
    }
		public void setOtherDetails(String other_details) {
	    this.other_details = other_details;
    }
		public int getNotificationId() {
	    return notification_id;
    }
		public void setNotificationId(int notification_id) {
	    this.notification_id = notification_id;
    }
		public int getRetriesCount() {
	    return retries_count;
    }
		public void setRetriesCount(int retries_count) {
	    this.retries_count = retries_count;
    }
	}
	
  public SimpleDateFormat iso8601Format = new SimpleDateFormat(
      "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());

	public static final String DATABASE_NAME = "eHonkLocalDB";
	public static final int DATABASE_VERSION = 1;
	
	public static final String NOTIFICATIONS_LOG_TABLE_NAME = "notifications_log";
	public static final String NOTIFICATIONS_LOG_ID = "notification_id";
	public static final String NOTIFICATIONS_OFFENDER_LICENSE = "offender_license";
	public static final String NOTIFICATIONS_TIMESTAMP = "timestamp";
	public static final String NOTIFICATIONS_STATUS_CODE = "status_code";
	public static final String NOTIFICATIONS_OTHER_DETAILS = "other_details";
	public static final String NOTIFICATIONS_RETRIES_COUNT = "retries_count";

	public static final int NOTIFICATION_STATUS_NACK = 1;
	public static final int NOTIFICATION_STATUS_NREG = 2;
	public static final int NOTIFICATION_STATUS_NOTIFIED = 3;
	public static final int NOTIFICATION_STATUS_TIMEOUT = 4;
	public static final int NOTIFICATION_STATUS_ACK_OK = 5;
	public static final int NOTIFICATION_STATUS_ACK_IGN = 6;
	
	private static Database instance = null;
	
	public static Database getInstance(Context context) {
		if (instance == null)
			instance = new Database(context.getApplicationContext());
		
		return instance;
	}

	private Database(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		String CREATE_NOTIFICATIONS_TABLE = "CREATE TABLE " + NOTIFICATIONS_LOG_TABLE_NAME + "("
				+ NOTIFICATIONS_LOG_ID + " INTEGER PRIMARY KEY, "
				+ NOTIFICATIONS_OFFENDER_LICENSE + " TEXT, "
				+ NOTIFICATIONS_TIMESTAMP + " TEXT, "
				+ NOTIFICATIONS_STATUS_CODE + " INTEGER, "
				+ NOTIFICATIONS_OTHER_DETAILS + " TEXT, "
				+ NOTIFICATIONS_RETRIES_COUNT + " INTEGER)";
		db.execSQL(CREATE_NOTIFICATIONS_TABLE);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + NOTIFICATIONS_LOG_TABLE_NAME);
		onCreate(db);
	}
	
	public void addOffense(OffenseRecord offense) {
		SQLiteDatabase db = this.getWritableDatabase();
		
		ContentValues contentValues = new ContentValues();
		contentValues.put(NOTIFICATIONS_OFFENDER_LICENSE, offense.getOffenderLicense());
		contentValues.put(NOTIFICATIONS_TIMESTAMP, offense.getTimestamp());
		contentValues.put(NOTIFICATIONS_STATUS_CODE, offense.getStatusCode());
		contentValues.put(NOTIFICATIONS_OTHER_DETAILS, offense.getOtherDetails());
		contentValues.put(NOTIFICATIONS_RETRIES_COUNT, offense.getRetriesCount());
		
		db.insert(NOTIFICATIONS_LOG_TABLE_NAME, null, contentValues);
		
		db.close();
	}
	
	public List<OffenseRecord> selectOffenses(String license) {
		SQLiteDatabase db = this.getReadableDatabase();
		String SELECT_NOTIFICATIONS_QUERY;

		if (license==null || license.isEmpty())
			SELECT_NOTIFICATIONS_QUERY = "SELECT * FROM " + NOTIFICATIONS_LOG_TABLE_NAME;
		else
			SELECT_NOTIFICATIONS_QUERY = "SELECT * FROM " + NOTIFICATIONS_LOG_TABLE_NAME + " WHERE " + NOTIFICATIONS_OFFENDER_LICENSE + "=" + license;
		
		Log.d("DB_TAG", SELECT_NOTIFICATIONS_QUERY);
		Cursor cursor = db.rawQuery(SELECT_NOTIFICATIONS_QUERY, null);
		ArrayList<OffenseRecord> result = new ArrayList<OffenseRecord>();
		if (cursor.moveToFirst()) {			
			do {
				OffenseRecord offense = new OffenseRecord();
				offense.setNotificationId(Integer.parseInt(cursor.getString(0)));
				offense.setOffenderLicense(cursor.getString(1));
				offense.setTimestamp(cursor.getString(2));
				offense.setStatusCode(Integer.parseInt(cursor.getString(3)));
				offense.setOtherDetails(cursor.getString(4));
				offense.setRetriesCount(Integer.parseInt(cursor.getString(5)));
				result.add(offense);
			} while (cursor.moveToNext());
		}
		
		db.close();
		return result;
	}
	
	public void removeOffense(String license) {
		SQLiteDatabase db = this.getWritableDatabase();
    db.delete(NOTIFICATIONS_LOG_TABLE_NAME, NOTIFICATIONS_OFFENDER_LICENSE + " = ?",
        new String[] { license });
		db.close();
	}

	public int updateOffense(OffenseRecord offense) {
		int ret;
		SQLiteDatabase db = this.getWritableDatabase();
		 
    ContentValues values = new ContentValues();
    values.put(NOTIFICATIONS_OFFENDER_LICENSE, offense.getOffenderLicense());
    values.put(NOTIFICATIONS_TIMESTAMP, offense.getTimestamp());
    values.put(NOTIFICATIONS_STATUS_CODE, offense.getStatusCode());
    values.put(NOTIFICATIONS_OTHER_DETAILS, offense.getOtherDetails());
    values.put(NOTIFICATIONS_RETRIES_COUNT, offense.getRetriesCount());

    // update linie
    ret = db.update(NOTIFICATIONS_LOG_TABLE_NAME, values, NOTIFICATIONS_LOG_ID + " = ?",
            new String[] { String.valueOf(offense.getNotificationId()) });
    
    db.close();
    return ret;
	}
	
	public int generateNextId(String tableName, String primaryKeyName) {
		SQLiteDatabase db = this.getReadableDatabase();
		String SELECT_MAX_ID_FROM_TABLE = "SELECT MAX(" + primaryKeyName + ") FROM " + tableName;
		Cursor cursor = db.rawQuery(SELECT_MAX_ID_FROM_TABLE, null);
		if (cursor != null && cursor.moveToFirst() && cursor.getString(0) != null)
			return Integer.parseInt(cursor.getString(0)) + 1;
		
		db.close();
		return 0;
	}
}
