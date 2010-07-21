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
import java.util.Set;
import java.util.TreeSet;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
	
	public DatabaseHelper(Context context) {
		super(context, DATABASE_NAME, null, DATABASE_VERSION);
	}

	public int getListCount() {
		return mTableNames.size();
	}
	
	public String[] getLists() {
		String[] ret = new String[mTableNames.size()];
		mTableNames.toArray(ret);
		Arrays.sort(ret, String.CASE_INSENSITIVE_ORDER);
		return ret;
	}
	
	public boolean createTable(String name, String[] tickets) {
		if (mTableNames.contains(name))
			return true;
		if (!name.matches("[a-zA-Z0-9_]+"))
			return false;
		
		SQLiteDatabase db = getWritableDatabase();
		try {
			db.execSQL("CREATE TABLE ? (" +
					Constants.TicketTable._ID + " INTEGER PRIMARY KEY," +
					Constants.TicketTable.TEXT + " TEXT," +
					Constants.TicketTable.FIRST_SCAN + " INTEGER," + 
					Constants.TicketTable.SCAN_COUNT + " INTEGER" + 
					");", new Object[] { name } );
		} catch (SQLException e) {
			return false;
		}
		mTableNames.add(name);
		
		for (String s : tickets) {
			try {
				db.execSQL("INSERT INTO ? (" +
						Constants.TicketTable.TEXT + "," +
						Constants.TicketTable.FIRST_SCAN + "," +
						Constants.TicketTable.SCAN_COUNT +
						") VALUES (?,NULL,0)"
						, new Object[] { name, s } );
			} catch (SQLException e) {
				continue;
			}
		}
		
		return true;
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
	
	private static final String DATABASE_NAME = "ticket_lists.db";
	private static final int DATABASE_VERSION = 1;
}