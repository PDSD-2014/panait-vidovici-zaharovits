package com.eHonk;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class Database extends SQLiteOpenHelper {

	/* sometimes we need atomic multiple transactions */
	public final ReentrantLock lock = new ReentrantLock();
	
	public class OffenseRecord {
		private int notification_id;
		private String license;
		private String gcm_id;
		private String timestamp;
		private int type_code;
		private int status_code;
		private String other_details;
		private int retries_count;

		public String getLicense() {
			return license;
		}

		public void setLicense(String license) {
			this.license = license;
		}

		public String getTimestamp() {
			return timestamp;
		}

		public void setTimestamp(String timestamp) {
			this.timestamp = timestamp;
		}

		public int getTypeCode() {
			return type_code;
		}

		public void setTypeCode(int type_code) {
			this.type_code = type_code;
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

		public String getGcmId() {
			return gcm_id;
		}

		public void setGcmId(String gcm_id) {
			this.gcm_id = gcm_id;
		}
	}

	public static SimpleDateFormat iso8601Format = new SimpleDateFormat(
	    "yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());

	public static final String DATABASE_NAME = "eHonkLocalDB";
	public static final int DATABASE_VERSION = 1;

	public static final String NOTIFICATIONS_LOG_TABLE_NAME_SENT = "notifications_log_sent";
	public static final String NOTIFICATIONS_LOG_TABLE_NAME_RECV = "notifications_log_recv";

	public static final String NOTIFICATIONS_LOG_ID = "notification_id";
	public static final String NOTIFICATIONS_LICENSE = "license";
	public static final String NOTIFICATIONS_GCM_ID = "gcm_id";
	public static final String NOTIFICATIONS_TIMESTAMP = "timestamp";
	public static final String NOTIFICATIONS_STATUS_CODE = "status_code";
	public static final String NOTIFICATIONS_TYPE_CODE = "type_code";
	public static final String NOTIFICATIONS_OTHER_DETAILS = "other_details";
	public static final String NOTIFICATIONS_RETRIES_COUNT = "retries_count";

	public static final int NOTIFICATION_STATUS_NACK = 1;
	public static final int NOTIFICATION_STATUS_NREG = 2;
	public static final int NOTIFICATION_STATUS_NOTIFIED = 3;
	public static final int NOTIFICATION_STATUS_TIMEOUT = 4;
	public static final int NOTIFICATION_STATUS_ACK_OK = 5;
	public static final int NOTIFICATION_STATUS_ACK_IGN = 6;

	public static final int NOTIFICATION_TYPE_SENT = 1;
	public static final int NOTIFICATION_TYPE_RECV = 2;
	public static final int NOTIFICATION_TYPE_MULTISENT = 3;
	public static final int NOTIFICATION_TYPE_MULTIRECV = 4;

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
		String CREATE_NOTIFICATIONS_TABLE_SENT = "CREATE TABLE "
		    + NOTIFICATIONS_LOG_TABLE_NAME_SENT + "(" + NOTIFICATIONS_LOG_ID
		    + " INTEGER PRIMARY KEY, " + NOTIFICATIONS_LICENSE + " TEXT, "
		    + NOTIFICATIONS_GCM_ID + " TEXT UNIQUE, " + NOTIFICATIONS_TIMESTAMP
		    + " TEXT, " + NOTIFICATIONS_TYPE_CODE + " INTEGER, "
		    + NOTIFICATIONS_STATUS_CODE + " INTEGER, "
		    + NOTIFICATIONS_OTHER_DETAILS + " TEXT, " + NOTIFICATIONS_RETRIES_COUNT
		    + " INTEGER)";
		db.execSQL(CREATE_NOTIFICATIONS_TABLE_SENT);
		String CREATE_NOTIFICATIONS_TABLE_RECV = "CREATE TABLE "
		    + NOTIFICATIONS_LOG_TABLE_NAME_RECV + "(" + NOTIFICATIONS_LOG_ID
		    + " INTEGER PRIMARY KEY, " + NOTIFICATIONS_LICENSE + " TEXT, "
		    + NOTIFICATIONS_GCM_ID + " TEXT UNIQUE, " + NOTIFICATIONS_TIMESTAMP
		    + " TEXT, " + NOTIFICATIONS_TYPE_CODE + " INTEGER, "
		    + NOTIFICATIONS_STATUS_CODE + " INTEGER, "
		    + NOTIFICATIONS_OTHER_DETAILS + " TEXT, " + NOTIFICATIONS_RETRIES_COUNT
		    + " INTEGER)";
		db.execSQL(CREATE_NOTIFICATIONS_TABLE_RECV);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.execSQL("DROP TABLE IF EXISTS " + NOTIFICATIONS_LOG_TABLE_NAME_SENT);
		db.execSQL("DROP TABLE IF EXISTS " + NOTIFICATIONS_LOG_TABLE_NAME_RECV);
		onCreate(db);
	}

	private boolean check_table_name(String table_name) {
		if (table_name.equals(NOTIFICATIONS_LOG_TABLE_NAME_SENT)
		    || table_name.equals(NOTIFICATIONS_LOG_TABLE_NAME_RECV))
			return true;
		return false;
	}

	public void addOffense(String table_name, OffenseRecord offense) {

		if (!check_table_name(table_name))
			return;

		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues contentValues = new ContentValues();
		contentValues.put(NOTIFICATIONS_LICENSE, offense.getLicense());
		contentValues.put(NOTIFICATIONS_GCM_ID, offense.getGcmId());
		contentValues.put(NOTIFICATIONS_TIMESTAMP, offense.getTimestamp());
		contentValues.put(NOTIFICATIONS_TYPE_CODE, offense.getTypeCode());
		contentValues.put(NOTIFICATIONS_STATUS_CODE, offense.getStatusCode());
		contentValues.put(NOTIFICATIONS_OTHER_DETAILS, offense.getOtherDetails());
		contentValues.put(NOTIFICATIONS_RETRIES_COUNT, offense.getRetriesCount());

		db.insert(table_name, null, contentValues);

		db.close();
	}

	public List<OffenseRecord> selectOffenses(String table_name, String license) {

		if (!check_table_name(table_name))
			return null;

		SQLiteDatabase db = this.getReadableDatabase();
		String SELECT_NOTIFICATIONS_QUERY;

		if (license == null || license.isEmpty())
			SELECT_NOTIFICATIONS_QUERY = "SELECT * FROM " + table_name;
		else
			SELECT_NOTIFICATIONS_QUERY = "SELECT * FROM " + table_name + " WHERE "
			    + NOTIFICATIONS_LICENSE + "=" + license;

		Log.d("DB_TAG", SELECT_NOTIFICATIONS_QUERY);

		Cursor cursor = db.rawQuery(SELECT_NOTIFICATIONS_QUERY, null);
		ArrayList<OffenseRecord> result = new ArrayList<OffenseRecord>();
		if (cursor.moveToFirst()) {
			do {
				OffenseRecord offense = new OffenseRecord();
				offense.setNotificationId(Integer.parseInt(cursor.getString(0)));
				offense.setLicense(cursor.getString(1));
				offense.setGcmId(cursor.getString(2));
				offense.setTimestamp(cursor.getString(3));
				offense.setTypeCode(Integer.parseInt(cursor.getString(4)));
				offense.setStatusCode(Integer.parseInt(cursor.getString(5)));
				offense.setOtherDetails(cursor.getString(6));
				offense.setRetriesCount(Integer.parseInt(cursor.getString(7)));
				result.add(offense);
			} while (cursor.moveToNext());
		}

		db.close();
		return result;
	}

	public OffenseRecord selectOffense(String table_name, String gcmId) {

		if (gcmId == null || !check_table_name(table_name))
			return null;

		SQLiteDatabase db = this.getReadableDatabase();

		Cursor cursor = db.query(table_name, null, NOTIFICATIONS_GCM_ID + " = ?",
		    new String[] { gcmId }, null, null, null);

		if (!cursor.moveToFirst())
			return null;

		OffenseRecord offense = new OffenseRecord();

		offense.setNotificationId(Integer.parseInt(cursor.getString(0)));
		offense.setLicense(cursor.getString(1));
		offense.setGcmId(cursor.getString(2));
		offense.setTimestamp(cursor.getString(3));
		offense.setTypeCode(Integer.parseInt(cursor.getString(4)));
		offense.setStatusCode(Integer.parseInt(cursor.getString(5)));
		offense.setOtherDetails(cursor.getString(6));
		offense.setRetriesCount(Integer.parseInt(cursor.getString(7)));

		db.close();

		return offense;
	}

	public Cursor getNotificationsCursor(String table_name) {

		if (!check_table_name(table_name))
			return null;

		Cursor result;
		SQLiteDatabase db = this.getReadableDatabase();
		String SELECT_NOTIFICATIONS_QUERY = "SELECT * FROM " + table_name;

		result = db.rawQuery(SELECT_NOTIFICATIONS_QUERY, null);
		db.close();

		return result;
	}

	public void removeOffense(String table_name, String license) {

		if (!check_table_name(table_name))
			return;

		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(table_name, NOTIFICATIONS_LICENSE + " = ?",
		    new String[] { license });
		db.close();
	}
	
	public void removeOffense(String table_name, OffenseRecord offense) {

		if (!check_table_name(table_name))
			return;
		
		SQLiteDatabase db = this.getWritableDatabase();
		db.delete(table_name, NOTIFICATIONS_LOG_ID + " = ?",
		    new String[] { offense.getGcmId() });
		db.close();
	}

	public int updateOffense(String table_name, OffenseRecord offense) {

		int ret = 0;
		if (!check_table_name(table_name))
			return ret;

		SQLiteDatabase db = this.getWritableDatabase();

		ContentValues values = new ContentValues();
		values.put(NOTIFICATIONS_LICENSE, offense.getLicense());
		values.put(NOTIFICATIONS_GCM_ID, offense.getGcmId());
		values.put(NOTIFICATIONS_TIMESTAMP, offense.getTimestamp());
		values.put(NOTIFICATIONS_TYPE_CODE, offense.getTypeCode());
		values.put(NOTIFICATIONS_STATUS_CODE, offense.getStatusCode());
		values.put(NOTIFICATIONS_OTHER_DETAILS, offense.getOtherDetails());
		values.put(NOTIFICATIONS_RETRIES_COUNT, offense.getRetriesCount());

		// update linie
		ret = db.update(table_name, values, NOTIFICATIONS_LOG_ID + " = ?",
		    new String[] { String.valueOf(offense.getNotificationId()) });

		db.close();
		return ret;
	}

	public OffenseRecord getLastOffenses(String table_name, String timestamp) {

		if (!check_table_name(table_name))
			return null;

		OffenseRecord offense = null;

		SQLiteDatabase db = this.getReadableDatabase();

		String SELECT_LAST_NOTIFICATION = "SELECT * FROM " + table_name
		    + " WHERE " + NOTIFICATIONS_TIMESTAMP + " > '" + timestamp
		    + "' ORDER BY date(" + NOTIFICATIONS_TIMESTAMP + ") ASC Limit 1";
		/*
		String SELECT_LAST_NOTIFICATION = "SELECT * FROM " + table_name
		    + " WHERE date(" + NOTIFICATIONS_TIMESTAMP + ")>date(" + timestamp
		    + ") ORDER BY date(" + NOTIFICATIONS_TIMESTAMP + ") ASC Limit 1";
		*/

		Cursor cursor = db.rawQuery(SELECT_LAST_NOTIFICATION, null);

		if (cursor.moveToFirst()) {
			offense = new OffenseRecord();
			offense.setNotificationId(Integer.parseInt(cursor.getString(0)));
			offense.setLicense(cursor.getString(1));
			offense.setGcmId(cursor.getString(2));
			offense.setTimestamp(cursor.getString(3));
			offense.setTypeCode(Integer.parseInt(cursor.getString(4)));
			offense.setStatusCode(Integer.parseInt(cursor.getString(5)));
			offense.setOtherDetails(cursor.getString(6));
			offense.setRetriesCount(Integer.parseInt(cursor.getString(7)));
		}

		db.close();

		return offense;
	}

	public int generateNextId(String table_name, String primaryKeyName) {

		if (!check_table_name(table_name))
			return 0;

		SQLiteDatabase db = this.getReadableDatabase();
		String SELECT_MAX_ID_FROM_TABLE = "SELECT MAX(" + primaryKeyName
		    + ") FROM " + table_name;
		Cursor cursor = db.rawQuery(SELECT_MAX_ID_FROM_TABLE, null);
		if (cursor != null && cursor.moveToFirst() && cursor.getString(0) != null)
			return Integer.parseInt(cursor.getString(0)) + 1;

		db.close();
		return 0;
	}
}
