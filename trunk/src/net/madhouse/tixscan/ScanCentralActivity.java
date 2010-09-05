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
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class ScanCentralActivity extends Activity implements View.OnClickListener {
	
	private TextView mLabelLastScanned;
	private TextView mLabelScanCount;
	private TextView mLabelTotalCount;
	private TextView mLabelDuplicateCount;
	
	private static final String KEY_LAST_SCANNED = "last_scanned";
	
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.scan_central);
        mHelper = new DatabaseHelper(this);
        
        Intent intent = getIntent();
        Uri data = intent.getData();
        mTableId = Integer.parseInt(data.getEncodedPath());
        
        mLabelScanCount = (TextView)findViewById(R.id.scan_text_count);
        mScannedCursor = mHelper.getScannedTicketCursor(mTableId);
        startManagingCursor(mScannedCursor);
        
        mLabelTotalCount = (TextView)findViewById(R.id.scan_text_total);
        mAllCursor = mHelper.getAllTicketCursor(mTableId);
        startManagingCursor(mAllCursor);
        
        mLabelDuplicateCount = (TextView)findViewById(R.id.scan_text_dupes);
        mDuplicateCursor = mHelper.getDuplicateTicketCursor(mTableId);
        startManagingCursor(mDuplicateCursor);
        
        refreshLabelCounts(false);
        
        mLabelLastScanned = (TextView)findViewById(R.id.scan_text_last);
        if (savedInstanceState != null) {
        	mLabelLastScanned.setText(savedInstanceState.getString(KEY_LAST_SCANNED));
        }
        
        Button b = (Button)findViewById(R.id.scan_btn_scan);
        b.setOnClickListener(this);
    }

	protected void refreshLabelCounts(boolean requery) {
		if (requery) {
			mScannedCursor.requery();
	        mAllCursor.requery();
	        mDuplicateCursor.requery();
		}
        mLabelScanCount.setText(Integer.toString(mScannedCursor.getCount()));
        mLabelTotalCount.setText(Integer.toString(mAllCursor.getCount()));
        mLabelDuplicateCount.setText(Integer.toString(mDuplicateCursor.getCount()));
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onSaveInstanceState(android.os.Bundle)
	 */
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putString(KEY_LAST_SCANNED, mLabelLastScanned.getText().toString());
	}

	/* (non-Javadoc)
	 * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.scanner_options, menu);
		return true;
	}
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onDestroy()
	 */
	@Override
	protected void onDestroy() {
		mHelper.close();
		super.onDestroy();
	}

	private int mTableId;
	private DatabaseHelper mHelper;
	
	private Cursor mScannedCursor;
	private Cursor mAllCursor;
	private Cursor mDuplicateCursor;
	
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
		if (resultCode == RESULT_CANCELED) {
			bg.setBackgroundColor(getResources().getColor(android.R.color.black));
			mLabelLastScanned.setText("");
			return;
		}
		String ticket = data.getStringExtra("SCAN_RESULT");
		mLabelLastScanned.setText(ticket);
		switch (mHelper.getScanResult(mTableId, ticket)) {
		case DatabaseHelper.RESULT_OK:
			bg.setBackgroundColor(getResources().getColor(android.R.color.black));
			// TODO: Delay and then restart scanner
			break;
		case DatabaseHelper.RESULT_DUPLICATE_TICKET:
			bg.setBackgroundColor(getResources().getColor(R.color.yellow_bg));
			// TODO: Check pref, delay and restart scanner
			
			break;
		case DatabaseHelper.RESULT_UNKNOWN_TICKET:
			bg.setBackgroundColor(getResources().getColor(R.color.red_bg));
			// TODO: Check pref, delay and restart scanner
			break;
		case DatabaseHelper.RESULT_SQL_FAIL:
			// TODO: Popup database error message
			break;
		}
		
		refreshLabelCounts(true);
	}
}
