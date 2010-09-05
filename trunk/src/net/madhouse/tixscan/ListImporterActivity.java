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
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ListImporterActivity extends Activity implements View.OnClickListener {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTheme(android.R.style.Theme_Dialog);
        setContentView(R.layout.list_importer);
        
        Button btn = (Button) findViewById(R.id.import_btn_ok);
        btn.setOnClickListener(this);
        
        btn = (Button) findViewById(R.id.import_btn_cancel);
        btn.setOnClickListener(this);
        
        btn = (Button) findViewById(R.id.import_dbg);
        btn.setOnClickListener(this);
    }

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.import_btn_ok:
			new Thread(new Downloader()).start();
			break;
		case R.id.import_btn_cancel:
			setResult(RESULT_CANCELED);
			finish();
			break;
		case R.id.import_dbg:
			new Thread(new Mischief()).start();
			break;
		}
	}

	private class Downloader implements Runnable {
		public Downloader() {
			// do nothing
		}

		@Override
		public void run() {
			// TODO todo oh gods todo
			
		}	
	}
	
	private class Mischief implements Runnable {
		public Mischief() {
			// do nothing
		}

		@Override
		public void run() {
			String[] titles = {
					"Ravens in the Library",
					"Cheshire Kitten (We're all mad here)",
					"Were-owl",
					"Love Lies",
					"Don't Get My Hopes Up",
					"Neptune",
					"Girl with the Lion's Tail (Lucia)",
					"September's Rhyme",
					"The Truth about Ninjas",
					"Salad of Doom",
					"Witchka",
					"To My Valentine"
			};
			TextView nameentry = (TextView)findViewById(R.id.import_edit_name);
			String name = nameentry.getText().toString().trim();
			DatabaseHelper helper = new DatabaseHelper(ListImporterActivity.this);
			
			setProgress(R.string.updating_db, true);
			int result = helper.createTable(name, titles);
			
			switch (result) {
			case DatabaseHelper.RESULT_LIST_EXISTS:
				setProgress(R.string.fail_list_exists, false);
				break;
			case DatabaseHelper.RESULT_BAD_LIST_NAME:
				setProgress(R.string.fail_bad_list_name, false);
				break;
			case DatabaseHelper.RESULT_SQL_FAIL:
				setProgress(R.string.fail_sql_error, false);
				break;
			default:
				// TODO: Does this need to be on the UI thread?
				Uri.Builder builder = new Uri.Builder();
				builder.path(Integer.toString(result));
				Intent ret = new Intent();
				ret.setDataAndType(builder.build(), Constants.MIME_TYPE_ITEM);
				setResult(RESULT_OK, ret);
				finish();
				break;
			}
			helper.close();
		}
	}
	
	void setProgress(final int message, final boolean stillRunning) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				findViewById(R.id.import_progress).setVisibility(stillRunning? View.VISIBLE: View.GONE);
				TextView proglabel = (TextView)findViewById(R.id.import_progress_label);
				proglabel.setVisibility(View.VISIBLE);
				proglabel.setText(message);
			}
		});
		
	}
}
