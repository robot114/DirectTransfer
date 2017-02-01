package com.zsm.directTransfer.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.preference.PreferenceManager;

import com.zsm.log.Log;

public class Preferences {

	private static final String PREFERENCES_BASE_NAME = "DirectTransferPreferences";

	private static final int PREFRENCES_VERSION = 1;

	private static final String KEY_READ_PATH = "KEY_READ_PATH";
	
	static private Preferences instance;
	final private SharedPreferences preferences;
	
	private StackTraceElement[] stackTrace;

	private Context mContext;

	private SQLiteOpenHelper mSQLiteOpenHelper;
	private PeerListDbOperator mPeerListOperator;
	
	private Preferences( Context context ) {
		mContext = context;
		preferences
			= PreferenceManager
				.getDefaultSharedPreferences( context );
		
	}
	
	static public void init( Context c ) {
		if( instance != null ) {
			throw new IllegalStateException( "Preference has been initialized! "
											 + "Call getInitStackTrace() to get "
											 + "the initlization place." );
		}
		instance = new Preferences( c );
		instance.initInstance();
		instance.stackTrace = Thread.currentThread().getStackTrace();
	}
	
	static public Preferences getInstance() {
		return instance;
	}
	
	public void releaseResources() {
		if( mSQLiteOpenHelper != null ) {
			mSQLiteOpenHelper.close();
			mSQLiteOpenHelper = null;
		}
	}
	
	private void initInstance() {
		mPeerListOperator = new PeerListDbOperator( );
		mSQLiteOpenHelper
			= new SQLiteOpenHelper( mContext, PREFERENCES_BASE_NAME,
									null, PREFRENCES_VERSION ) {

			@Override
			public void onCreate(SQLiteDatabase db) {
				mPeerListOperator.createPeerTable(db);
			}

			@Override
			public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
				Log.w( "Upgrade from version " + oldVersion + " to " + newVersion
					   + ". All datas are destroyed!" );
				
				mPeerListOperator.updatePeerTable(db);
			}
		};
		
		mPeerListOperator.setSQLiteOpenHelper(mSQLiteOpenHelper);
	}

	public StackTraceElement[] getInitStackTrace() {
		return stackTrace;
	}
	
	public void registerOnSharedPreferenceChangeListener(
					OnSharedPreferenceChangeListener listener) {
		
		preferences.registerOnSharedPreferenceChangeListener(listener);
	}
	
	public PeerListOperator getPeerListOperator() {
		return mPeerListOperator;
	}

	public String getReadPath() {
		return preferences.getString( KEY_READ_PATH, null );
	}

	public void setReadPath(String absolutePath) {
		preferences.edit().putString(KEY_READ_PATH, absolutePath).commit();
	}
}
