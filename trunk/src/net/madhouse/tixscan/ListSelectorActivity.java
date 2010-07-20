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
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

public class ListSelectorActivity extends Activity implements View.OnClickListener {
	
	private static final int REQUEST_CODE_IMPORT_NEW = 1;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_selector);
        
        View v = findViewById(R.id.selector_btn_import);
        v.setOnClickListener(this);
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
			intent.setData(data.getData());
			startActivity(intent);
			break;
		}
	}

	@Override
	public void onClick(View v) {
		Intent intent;
		
		switch (v.getId()) {
		case R.id.selector_btn_import:
			intent = new Intent(Intent.ACTION_INSERT);
			intent.setType("vnd.android.cursor.dir/net.madhouse.tixscan.list");
			startActivityForResult(intent, REQUEST_CODE_IMPORT_NEW);
			break;
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
		
		SharedPreferences prefs = getSharedPreferences(Constants.PREFS_FILE, 0);
		boolean acceptByDef = prefs.getBoolean(Constants.PREF_ACCEPT_BY_DEFAULT, false);
		
		if (acceptByDef)
			item = menu.findItem(R.id.menuitem_dupe_default_accept);
		else 
			item = menu.findItem(R.id.menuitem_dupe_default_reject);
		item.setChecked(true);
		
		return true;
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onMenuItemSelected(int, android.view.MenuItem)
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		SharedPreferences prefs = getSharedPreferences(Constants.PREFS_FILE, 0);
		SharedPreferences.Editor editor;
		int id = item.getItemId();
		switch (id) {
		case R.id.menuitem_dupe_default_accept:
		case R.id.menuitem_dupe_default_reject:
			editor = prefs.edit();
			editor.putBoolean(Constants.PREF_ACCEPT_BY_DEFAULT, 
					id == R.id.menuitem_dupe_default_accept);
			editor.commit();
			item.setChecked(true);
			break;
		default:
			return false;
		}
		return false;
	}
}
