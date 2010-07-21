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

import android.provider.BaseColumns;

public final class Constants {
	public static final String PREFS_FILE = "TixScanPrefs";
	public static final String PREF_ACCEPT_BY_DEFAULT = "AcceptByDefault";
	
	public static final class TicketTable implements BaseColumns {
		public static final String TEXT = "text";
		public static final String FIRST_SCAN = "first_scan";
		public static final String SCAN_COUNT = "scan_count";
		
		private TicketTable() {}
	}
	
	private Constants() {}
}
