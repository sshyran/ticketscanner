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
import java.util.TreeSet;

import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	
	private static final String DATABASE_NAME = "ticket_lists.db";
	private static final int DATABASE_VERSION = 1;
	public static final class MetaTable implements BaseColumns {
		public static final String TABLE_NAME = "tables";
		
		public static final String NAME = "name";
		
		public static final String[] COLS_JUST_NAMES = { NAME };
		public static final String[] COLS_JUST_IDS = { _ID };
		public static final String[] COLS_IDS_NAMES = { _ID, NAME };
		public static final String SELECT_BY_NAME = NAME+"=?";
		
		private MetaTable() {}
	}
	public static final class TicketTable implements BaseColumns {
		public static final String TABLE_NAME = "tickets";
		
		public static final String TEXT = "text";
		public static final String FIRST_SCAN = "first_scan";
		public static final String SCAN_COUNT = "scan_count";
		public static final String TABLE_ID = "table_id";
		
		public static final String[] COLS_JUST_IDS = { _ID };
		public static final String[] COLS_JUST_COUNTS = { SCAN_COUNT };
		
		public static final String SELECT_BY_TID = TABLE_ID+"=?";
		public static final String SELECT_SCANNED_BY_TID = TABLE_ID+"=? AND " + SCAN_COUNT+">0";
		public static final String SELECT_DUPE_BY_TID = TABLE_ID+"=? AND " + SCAN_COUNT+">1";
		public static final String SELECT_BY_TID_AND_TEXT = TABLE_ID+"=? AND " + TEXT+"=?";


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

	public int createTable(String name, String[] tickets) {
		if (!name.matches(".+"))
			return RESULT_BAD_LIST_NAME;
		
		SQLiteDatabase db = getWritableDatabase();
		Cursor c = null;
		int tableId;
		try {
			c = db.query(MetaTable.TABLE_NAME, MetaTable.COLS_JUST_IDS, MetaTable.SELECT_BY_NAME , new String[] {name}, null, null, null);
			if (c.getCount() > 0)
				return RESULT_LIST_EXISTS;
			
			db.beginTransaction();
			db.execSQL("INSERT INTO " + MetaTable.TABLE_NAME + " (" +
					MetaTable.NAME + 
					") VALUES (?)",
					new Object[] { name } );
			c.requery();
			c.moveToFirst();
			tableId = c.getInt(0);
	
			for (String s : tickets) {
				db.execSQL("INSERT INTO " + TicketTable.TABLE_NAME + " (" +
						TicketTable.TEXT + "," +
						TicketTable.FIRST_SCAN + "," +
						TicketTable.SCAN_COUNT + "," +
						TicketTable.TABLE_ID + 
						") VALUES (?,NULL,0,?)"
						, new Object[] { s, tableId } );
			}
			db.setTransactionSuccessful();
		} catch (SQLException e) {
			Log.e(Constants.LOG_TAG, "failed to create new ticket list: ", e);
			return RESULT_SQL_FAIL;
		} finally {
			db.endTransaction();
			if (c != null) c.close();
		}
		
		return tableId;
	}

	public Cursor getTablesCursor() {
		SQLiteDatabase db = getReadableDatabase();
		try {
			return db.query(MetaTable.TABLE_NAME, MetaTable.COLS_IDS_NAMES, null, null, null, null, MetaTable.NAME);
		} catch (SQLException e) {
			Log.e(Constants.LOG_TAG, "Couldn't read list of tables", e);
			return null;
		}
	}
	
	public Cursor getAllTicketCursor(int tableId) {
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(TicketTable.TABLE_NAME, TicketTable.COLS_JUST_IDS, 
					TicketTable.SELECT_BY_TID, new String[] { Integer.toString(tableId) }, 
					null, null, null);
			return c;
		} catch (SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Cursor getScannedTicketCursor(int tableId) {
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(TicketTable.TABLE_NAME, TicketTable.COLS_JUST_IDS, 
				TicketTable.SELECT_SCANNED_BY_TID, new String[] { Integer.toString(tableId) }, 
				null, null, null);
			return c;
		} catch(SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Cursor getDuplicateTicketCursor(int tableId) {
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(TicketTable.TABLE_NAME, TicketTable.COLS_JUST_COUNTS, 
					TicketTable.SELECT_DUPE_BY_TID, new String[] { Integer.toString(tableId) }, 
					null, null, null);
			return c;
		} catch(SQLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public int getScanResult(int tableId, String key) {
		Cursor c = null;
		try {
			SQLiteDatabase db = getWritableDatabase();
			c = db.query(TicketTable.TABLE_NAME, TicketTable.COLS_JUST_COUNTS, 
					TicketTable.SELECT_BY_TID_AND_TEXT, new String[]{ Integer.toString(tableId), key }, 
					null, null, null);
			if (c.getCount() == 0) {
				return RESULT_UNKNOWN_TICKET;
			}
			c.moveToFirst();
			int count = c.getInt(0);
			if (count == 0) {
				// TODO: Update row in table with single count and date
				return RESULT_OK;
			}
			// TODO: Update row in table with incremented count
			return RESULT_DUPLICATE_TICKET;
		} catch(SQLException e) {
			e.printStackTrace();
			return RESULT_SQL_FAIL;
		} finally {
			if (c != null) c.close();
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.beginTransaction();
		try {
			Log.d(Constants.LOG_TAG, "Trying to create metatable" );
			db.execSQL("CREATE TABLE " + MetaTable.TABLE_NAME + " (" +
					MetaTable._ID + " INTEGER PRIMARY KEY," +
					MetaTable.NAME + " TEXT" +
					");" );
			Log.d(Constants.LOG_TAG, "success; creating ticket table");
			db.execSQL("CREATE TABLE " + TicketTable.TABLE_NAME + " (" +
					TicketTable._ID + " INTEGER PRIMARY KEY," +
					TicketTable.TEXT + " TEXT," +
					TicketTable.FIRST_SCAN + " INTEGER," +
					TicketTable.SCAN_COUNT + " INTEGER," +
					TicketTable.TABLE_ID + " INTEGER, " +
					"FOREIGN KEY(" + TicketTable.TABLE_ID + ") REFERENCES " + 
					MetaTable.TABLE_NAME + "(" + MetaTable._ID + ")" +
					");" );
			db.setTransactionSuccessful();

		} catch (SQLException e) {
			Log.e(Constants.LOG_TAG, "failed: ", e);
		} finally {
			db.endTransaction();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		// Not caching any data
	}
	
}