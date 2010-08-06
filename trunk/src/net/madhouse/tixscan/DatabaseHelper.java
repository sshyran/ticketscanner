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

public class DatabaseHelper extends SQLiteOpenHelper {
	
	public static final class TicketTable implements BaseColumns {
		private static final String DATABASE_NAME = "ticket_lists.db";
		private static final int DATABASE_VERSION = 1;
		public static final String TEXT = "text";
		public static final String FIRST_SCAN = "first_scan";
		public static final String SCAN_COUNT = "scan_count";
		
		private TicketTable() {}
	}
	
	public static final int RESULT_OK = 0;
	public static final int RESULT_DUPLICATE_TICKET = -1;
	public static final int RESULT_UNKNOWN_TICKET = -2;
	public static final int RESULT_SQL_FAIL = -3;
	public static final int RESULT_LIST_EXISTS = -4;
	public static final int RESULT_BAD_LIST_NAME = -5;
	
	public DatabaseHelper(Context context) {
		super(context, TicketTable.DATABASE_NAME, null, TicketTable.DATABASE_VERSION);
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
		if (!name.matches("[a-zA-Z0-9_]+"))
			return RESULT_BAD_LIST_NAME;
		
		SQLiteDatabase db = getWritableDatabase();
		try {
			db.execSQL("CREATE TABLE ? (" +
					TicketTable._ID + " INTEGER PRIMARY KEY," +
					TicketTable.TEXT + " TEXT," +
					TicketTable.FIRST_SCAN + " INTEGER," + 
					TicketTable.SCAN_COUNT + " INTEGER" + 
					");", new Object[] { name } );
		} catch (SQLException e) {
			return RESULT_SQL_FAIL;
		}
		mTableNames.add(name);
		
		for (String s : tickets) {
			try {
				db.execSQL("INSERT INTO ? (" +
						TicketTable.TEXT + "," +
						TicketTable.FIRST_SCAN + "," +
						TicketTable.SCAN_COUNT +
						") VALUES (?,NULL,0)"
						, new Object[] { name, s } );
			} catch (SQLException e) {
				continue;
			}
		}
		
		return RESULT_OK;
	}
	
	private static final String[] COLS_JUST_IDS = { TicketTable._ID };
	private static final String[] COLS_JUST_COUNTS = { TicketTable.SCAN_COUNT };
	private static final String SELECT_SCANNED = TicketTable.SCAN_COUNT + ">0";
	private static final String SELECT_DUPE = TicketTable.SCAN_COUNT + ">1";
	private static final String SELECT_ONE = TicketTable.TEXT+"=?";
	
	public int getTotalCount(String tableName) {
		int count;
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(tableName, COLS_JUST_IDS, null, null, null, null, null);
			count = c.getCount();
			c.close();
		} catch (SQLException e) {
			return 0;
		}
		return count;
	}
	
	public int getScannedCount(String tableName) {
		int count;
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(tableName, COLS_JUST_IDS, SELECT_SCANNED, null, null, null, null);
			count = c.getCount();
			c.close();
		} catch(SQLException e) {
			return 0;
		}
		return count;
	}
	
	public int getDuplicateCount(String tableName) {
		int count = 0;
		try {
			SQLiteDatabase db = getReadableDatabase();
			Cursor c = db.query(tableName, COLS_JUST_COUNTS, SELECT_DUPE, null, null, null, null);
			for (c.moveToFirst(); !c.isAfterLast(); c.moveToNext()) {
				count += c.getInt(0) - 1;
			}
			c.close();
		} catch(SQLException e) {
			return count;
		}
		return count;
	}
	
	public int getScanResult(String tableName, String key) {
		try {
			SQLiteDatabase db = getWritableDatabase();
			Cursor c = db.query(tableName, COLS_JUST_COUNTS, SELECT_ONE, new String[]{tableName}, null, null, null);
			if (c.getCount() == 0) {
				c.close();
				return RESULT_UNKNOWN_TICKET;
			}
			int count = c.getInt(0);
			c.close();
			if (count == 0) {
				// TODO: Update row in table with single count and date
				return RESULT_OK;
			}
			// TODO: Update row in table with incremented count
			return RESULT_DUPLICATE_TICKET;
		} catch(SQLException e) {
			return RESULT_SQL_FAIL;
		}
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		mTableNames = new TreeSet<String>();
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		// TODO Auto-generated method stub

	}

	@Override
	public void onOpen(SQLiteDatabase db) {
		Map<String, String> tables = db.getSyncedTables();
		mTableNames = new TreeSet<String>();
		mTableNames.addAll(tables.keySet());
	}
	
	private TreeSet<String> mTableNames;
}