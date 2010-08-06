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
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class ListSelectorActivity extends Activity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
	
	private static final int REQUEST_CODE_IMPORT_NEW = 1;
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_selector);
        
        View v = findViewById(R.id.selector_btn_import);
        v.setOnClickListener(this);
        
        ListView list = (ListView)findViewById(R.id.selector_list);
        list.setOnItemSelectedListener(this);
        new Thread(new ListReadRunnable()).start();
    }
    
    private class ListReadRunnable implements Runnable {

		public ListReadRunnable() {
			// do nothing
		}

		@Override
		public void run() {
			DatabaseHelper helper = new DatabaseHelper(ListSelectorActivity.this);
			final String[] lists = helper.getLists();
			runOnUiThread(new Runnable() {

				@Override
				public void run() {
					ArrayAdapter adapter = new ArrayAdapter<String>(ListSelectorActivity.this, R.layout.list_line, lists);
					ListView view = (ListView) findViewById(R.id.selector_list);
					view.setAdapter(adapter);
					
					View progress = findViewById(R.id.selector_populating);
					progress.setVisibility(View.GONE);
				}
				
			});
		}
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
			intent.setType(Constants.MIME_TYPE_DIR);
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
		int dupBehave = prefs.getInt(Constants.PREF_DUPLICATE_BEHAVIOUR, Constants.DUP_REJECT_CONTINUE);
		switch (dupBehave) {
		case Constants.DUP_ACCEPT_CONTINUE:
			item = menu.findItem(R.id.menuitem_dupe_default_accept_continue); break;
		case Constants.DUP_ACCEPT_PAUSE:
			item = menu.findItem(R.id.menuitem_dupe_default_accept_pause); break;
		case Constants.DUP_REJECT_CONTINUE:
			item = menu.findItem(R.id.menuitem_dupe_default_reject_continue); break;
		case Constants.DUP_REJECT_PAUSE:
			item = menu.findItem(R.id.menuitem_dupe_default_reject_pause); break;
		}
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
		case R.id.menuitem_dupe_default_accept_pause:
			editor = prefs.edit();
			editor.putInt(Constants.PREF_DUPLICATE_BEHAVIOUR, Constants.DUP_ACCEPT_PAUSE);
			editor.commit();
			break;
		case R.id.menuitem_dupe_default_reject_pause:
			editor = prefs.edit();
			editor.putInt(Constants.PREF_DUPLICATE_BEHAVIOUR, Constants.DUP_REJECT_PAUSE);
			editor.commit();
			break;
		case R.id.menuitem_dupe_default_accept_continue:
			editor = prefs.edit();
			editor.putInt(Constants.PREF_DUPLICATE_BEHAVIOUR, Constants.DUP_ACCEPT_CONTINUE);
			editor.commit();
			break;
		case R.id.menuitem_dupe_default_reject_continue:
			editor = prefs.edit();
			editor.putInt(Constants.PREF_DUPLICATE_BEHAVIOUR, Constants.DUP_REJECT_CONTINUE);
			editor.commit();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onItemSelected(AdapterView<?> list, View text, int position,
			long id) {
		TextView t = (TextView) text;
		Intent intent = new Intent(Intent.ACTION_VIEW);
		Uri.Builder builder = new Uri.Builder();
		builder.path(t.getText().toString());
		intent.setDataAndType(builder.build(), Constants.MIME_TYPE_ITEM);
		startActivity(intent);
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		// Ok to do nothing.
	}
}
