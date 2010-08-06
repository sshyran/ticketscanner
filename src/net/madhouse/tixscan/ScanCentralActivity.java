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
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ScanCentralActivity extends Activity implements View.OnClickListener {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_central);
        mHelper = new DatabaseHelper(this);
        
        Intent intent = getIntent();
        Uri data = intent.getData();
        mTableName = data.getEncodedPath();
        
        // TODO: Push these out to a work thread
        TextView t = (TextView)findViewById(R.id.scan_text_count);
        mScanCount = mHelper.getScannedCount(mTableName);
        t.setText(Integer.toString(mScanCount));
        
        t = (TextView)findViewById(R.id.scan_text_total);
        t.setText(Integer.toString(mHelper.getTotalCount(mTableName)));
        
        t = (TextView)findViewById(R.id.scan_text_dupes);
        t.setText(Integer.toString(mHelper.getDuplicateCount(mTableName)));
        
        Button b = (Button)findViewById(R.id.scan_btn_scan);
        b.setOnClickListener(this);
    }

    /* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.scanner_options, menu);
		return true;
	}
	
	private String mTableName;
	private DatabaseHelper mHelper;
	private int mScanCount;
	private static final int REQUEST_CODE_SCAN = 0;
	
	private void triggerScan() {
		Intent intent = new Intent("com.google.zxing.client.android.SCAN");
        intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
        startActivityForResult(intent, REQUEST_CODE_SCAN);
	}

	@Override
	public void onClick(View v) {
		// Assuming there is only one thing listening for clicks
		triggerScan();
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// Assuming that the scanner is the only thing we start for results
		View bg = findViewById(R.id.scan_layout_root);
		TextView last = (TextView)findViewById(R.id.scan_text_last);
		if (resultCode == RESULT_CANCELED) {
			bg.setBackgroundColor(android.R.color.black);
			last.setText("");
			return;
		}
		String result = data.getDataString();
		last.setText(result);
		switch (mHelper.getScanResult(mTableName, result)) {
		case DatabaseHelper.RESULT_OK:
			bg.setBackgroundColor(android.R.color.black);
			TextView count = (TextView)findViewById(R.id.scan_text_count);
			count.setText(Integer.toString(++mScanCount));
			// TODO: Delay and then restart scanner
			break;
		case DatabaseHelper.RESULT_DUPLICATE_TICKET:
			bg.setBackgroundColor(R.color.yellow_bg);
			// TODO: Check pref, delay and restart scanner
			
			break;
		case DatabaseHelper.RESULT_UNKNOWN_TICKET:
			bg.setBackgroundColor(R.color.red_bg);
			// TODO: Check pref, delay and restart scanner
			break;
		case DatabaseHelper.RESULT_SQL_FAIL:
			// TODO: Popup database error message
			break;
		}
	}
}
