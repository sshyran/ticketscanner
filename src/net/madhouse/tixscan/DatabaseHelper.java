/*
 *  Copyright 2010 Brian C. Young and listed contributors
 *  
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may 
 *  not use this file except in compliance with the License. You may obtain a 
 *  copy of the License at 
 *  
 *      http://www.apache.org/licenses/LICENSE-2.0 
 *  
 *  Unless required by applicable law or agreed to in writing, software 
 *  distributed under the License is distributed on an "AS IS" BASIS, 
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 *  See the License for the specific language governing permissions and 
 *  limitations under the License. 
 */
package net.madhouse.tixscan;

import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	
	private final static String LOG_TAG = "DatabaseHelper";
	
	private static final String DATABASE_NAME = "ticket_lists.db";
	private static final int DATABASE_VERSION = 1;
	private static final class MetaTable implements BaseColumns {
		public static final String TABLE_NAME = "tables";
		public static final String NAME = "name";
		
		public static final String[] COLS_JUST_NAMES = { NAME };
		
		private MetaTable() {}
	}
	private static final class TicketTable implements BaseColumns {
		public static final String TEXT = "text";
		public static final String FIRST_SCAN = "first_scan";
		public static final String SCAN_COUNT = "scan_count";
		
		public static final String[] COLS_JUST_IDS = { _ID };
		public static final String[] COLS_JUST_COUNTS = { SCAN_COUNT };
		
		public static final String SELECT_SCANNED = SCAN_COUNT + ">0";
		public static final String SELECT_DUPE = SCAN_COUNT + ">1";
		public static final String SELECT_ONE = TEXT+"=?";

		private TicketTable() {}
	}
	
	public static final int RESULT_OK = 0;
	public static final int RESULT_DUPLICATE_TICKET = -1;
	public static final int RESULT_UNKNOWN_TICKET = -2;
	public static final int RESULT_SQL_FAIL = -3;
	public static final int RESULT_LIST_EXISTS = -4;
	public static final int RESULT_BAD_LIST_NAME = -5;
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public int getListCount() {
		return mTableNames.size();
	}
	
	public String[] getLists() {
		if (mTableNames == null)
			getReadableDatabase();
		
		String[] ret = new String[mTableNames.size()];
		mTableNames.toArray(ret);
		Arrays.sort(ret, String.CASE_INSENSITIVE_ORDER);
		return ret;
	}
	
	public int createTable(String name, String[] tickets) {
		if (mTableNames == null)
			getWritableDatabase();
		
		if (mTableNames.contains(name))
			return RESULT_LIST_EXISTS;
		if (!name.matches(".+"))
			return RESULT_BAD_LIST_NAME;
		
		String hashedName = encodeTableName(name);
		
		SQLiteDatabase db = getWritableDatabase();
		try {
			Log.d(LOG_TAG, "Trying to create table " + hashedName);
			db.execSQL("CREATE TABLE " + hashedName + " (" +
					TicketTable._ID + " INTEGER PRIMARY KEY," +
					TicketTable.TEXT + " TEXT," +
					TicketTable.FIRST_SCAN + " INTEGER," + 
					TicketTable.SCAN_COUNT + " INTEGER" + 
					");" );
			Log.d(LOG_TAG, "success; adding to metatable");
			db.execSQL("INSERT INTO " + MetaTable.TABLE_NAME + " (" +
					MetaTable.NAME + ") VALUES (?)",
					new Object[] { name } );
			Log.d(LOG_TAG, "success");
		} catch (SQLException e) {
			Log.e(LOG_TAG, "failed: ", e);
			return RESULT_SQL_FAIL;
		}
		mTableNames.add(name);
		
		for (String s : tickets) {
			try {
				Log.d(LOG_TAG, "Trying to insert ticket " + s);
				db.execSQL("INSERT INTO " + hashedName + " (" +
						TicketTable.TEXT + "," +
						TicketTable.FIRST_SCAN + "," +
						TicketTable.SCAN_COUNT +
						") VALUES (?,NULL,0)"
						, new Object[] { s } );
				Log.d(LOG_TAG, "success");
			} catch (SQLException e) {
				Log.e(LOG_TAG, "failed: ", e);
				continue;
			}
		}
		
		return RESULT_OK;
	}
	
	public int getTotalCount(String tableName) {
		int count;
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(encodeTableName(tableName), TicketTable.COLS_JUST_IDS, null, null, null, null, null);
			count = c.getCount();
			c.close();
		} catch (SQLException e) {
			e.printStackTrace();
			return 0;
		}
		return count;
	}
	
	public int getScannedCount(String tableName) {
		int count;
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(encodeTableName(tableName), TicketTable.COLS_JUST_IDS, TicketTable.SELECT_SCANNED, null, null, null, null);
			count = c.getCount();
			c.close();
		} catch(SQLException e) {
			e.printStackTrace();
			return 0;
		}
		return count;
	}
	
	public int getDuplicateCount(String tableName) {
		int count = 0;
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(encodeTableName(tableName), TicketTable.COLS_JUST_COUNTS, TicketTable.SELECT_DUPE, null, null, null, null);
			for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
				count += c.getInt(0) - 1;
			}
			c.close();
		} catch(SQLException e) {
			e.printStackTrace();
			return count;
		}
		return count;
	}
	
	public int getScanResult(String tableName, String key) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			Cursor c = db.query(encodeTableName(tableName), TicketTable.COLS_JUST_COUNTS, TicketTable.SELECT_ONE, new String[]{key}, null, null, null);
			if (c.getCount() == 0) {
				c.close();
				return RESULT_UNKNOWN_TICKET;
			}
			c.moveToFirst();
			int count = c.getInt(0);
			c.close();
			if (count == 0) {
				// TODO: Update row in table with single count and date
				return RESULT_OK;
			}
			// TODO: Update row in table with incremented count
			return RESULT_DUPLICATE_TICKET;
		} catch(SQLException e) {
			e.printStackTrace();
			return RESULT_SQL_FAIL;
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		try {
			Log.d(LOG_TAG, "Trying to create metatable" );
			db.execSQL("CREATE TABLE " + MetaTable.TABLE_NAME + " (" +
					MetaTable._ID + " INTEGER PRIMARY KEY," +
					MetaTable.NAME + " TEXT" +
					");" );
			Log.d(LOG_TAG, "success; adding to metatable");
		} catch (SQLException e) {
			Log.e(LOG_TAG, "failed: ", e);
		}
		mTableNames = new TreeSet<String>();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		mTableNames = new TreeSet<String>();
		try {
			Log.d(LOG_TAG, "Trying to read metatable");
			Cursor c = db.query(MetaTable.TABLE_NAME, MetaTable.COLS_JUST_NAMES, null, null, null, null, null);
			if (c.moveToFirst()) for (; !c.isAfterLast(); c.moveToNext()) {
				mTableNames.add(c.getString(0));
			}
			c.close();
			Log.d(LOG_TAG, "success, metatable read");
		} catch (SQLException e) {
			Log.e(LOG_TAG, "failed: ", e);
		}
	}
	
	private String encodeTableName(String name) {
		return "t" + Integer.toString(Math.abs(name.hashCode()));
	}
	
	private TreeSet<String> mTableNames;
}