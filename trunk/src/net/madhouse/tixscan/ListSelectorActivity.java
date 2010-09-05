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

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ListSelectorActivity extends Activity implements View.OnClickListener, AdapterView.OnItemClickListener {
	
	private static final int REQUEST_CODE_IMPORT_NEW = 1;
	
	private DatabaseHelper mHelper;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_selector);
        
        View v = findViewById(R.id.selector_btn_import);
        v.setOnClickListener(this);
        
        ListView list = (ListView)findViewById(R.id.selector_list);
        list.setOnItemClickListener(this);
        registerForContextMenu(list);
        mHelper = new DatabaseHelper(this);
        Cursor c = mHelper.getTablesCursor();
        startManagingCursor(c);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, R.layout.list_line, c, DatabaseHelper.MetaTable.COLS_JUST_NAMES, new int[] { R.id.item_line_name });
        list.setAdapter(adapter);
    }

	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		mHelper.close();
		super.onDestroy();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_CANCELED)
			return;
		
		Intent intent;
		
		switch (requestCode) {
		case REQUEST_CODE_IMPORT_NEW:
			intent = new Intent(Intent.ACTION_EDIT);
			intent.setDataAndType(data.getData(), data.getType());
			startActivity(intent);
			break;
		default:
			Log.w(Constants.LOG_TAG, "Unhandled activity result: " + requestCode);
		}
	}

	@Override
	public void onClick(View v) {
		Intent intent;
		
		switch (v.getId()) {
		case R.id.selector_btn_import:
			intent = new Intent(Intent.ACTION_INSERT);
			intent.setType(Constants.MIME_TYPE_DIR);
			startActivityForResult(intent, REQUEST_CODE_IMPORT_NEW);
			break;
		default:
			Log.w(Constants.LOG_TAG, "Unhandled click from view: " + v);
		}
	}

    /* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.selector_options, menu);
		
		MenuItem item = menu.findItem(R.id.menuitem_about);
		Intent launchAbout = new Intent(Intent.ACTION_MAIN);
		launchAbout.setClass(this, AboutActivity.class);
		item.setIntent(launchAbout);
		
		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateContextMenu(android.view.ContextMenu, android.view.View, android.view.ContextMenu.ContextMenuInfo)
	 */
	@Override
	public void onCreateContextMenu(ContextMenu menu, View v,
			ContextMenuInfo menuInfo) {
		getMenuInflater().inflate(R.menu.scanner_options, menu);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		switch (id) {
		case R.id.menuitem_about:
			break; // Handled by setting an intent
		default:
			Log.w(Constants.LOG_TAG, "Unhandled options menu item: " + id);
		}
		return super.onOptionsItemSelected(item);
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onContextItemSelected(android.view.MenuItem)
	 */
	@Override
	public boolean onContextItemSelected(MenuItem item) {
		int id = item.getItemId();
		
		switch (id) {
		default:
			Log.w(Constants.LOG_TAG, "Unhandled context menu item: " + id);
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onItemClick(AdapterView<?> list, View text, int position, long id) {
		Intent intent = new Intent(Intent.ACTION_VIEW);
		Uri.Builder builder = new Uri.Builder();
		builder.path(Long.toString(id));
		intent.setDataAndType(builder.build(), Constants.MIME_TYPE_ITEM);
		startActivity(intent);
	}
}
