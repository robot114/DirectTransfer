package com.zsm.directTransfer.preferences;

import java.io.File;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.zsm.directTransfer.R;
import com.zsm.directTransfer.app.ApplicationInterface;
import com.zsm.log.Log;

public class Preferences {
	private static final String DEFAULT_WRITE_PATH = "DirctTransfer";

	private static final String KEY_USE_SAF = "KEY_USE_SAF";
	private static final String KEY_READ_PATH = "KEY_READ_PATH";
	private static String KEY_MAX_TRANSFER_THREAD_NUM;
	private static String KEY_RESUME;
	static String KEY_WRITE_PATH;

	private static final String PREFERENCES_BASE_NAME = "DirectTransferPreferences";

	private static final int PREFRENCES_VERSION = 1;

	static private Preferences instance;
	final private SharedPreferences preferences;
	
	private StackTraceElement[] stackTrace;

	private Context mContext;

	private SQLiteOpenHelper mSQLiteOpenHelper;
	private PeerListDbOperator mPeerListOperator;
	private int mPromptPickPathCount;
	
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
		
		KEY_WRITE_PATH = c.getString( R.string.prefKey_DownloadTarget );
		KEY_MAX_TRANSFER_THREAD_NUM = c.getString( R.string.prefKey_MaxTransferThread );
		KEY_RESUME = c.getString( R.string.prefKey_Resume );
	}
	
	static public Preferences getInstance() {
//		instance.preferences.edit().clear().commit();
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

	public int getMaxTransferThreadNum() {
		String numStr = preferences.getString(KEY_MAX_TRANSFER_THREAD_NUM, "5" );
		return Integer.parseInt(numStr);
	}

	public String getWritePath() {
		if( isStorageAsscessFramework() ) {
			throw new UnsupportedOperationException(
				"Cannot invoke this method when SAF set, "
				+ "invoke getWriteUri instead" );
		}
		
		String pathStr = preferences.getString( KEY_WRITE_PATH, null );
		if( pathStr == null ) {
			promptPickPath();
			pathStr = defaultWritePath();
		}
		File dir = new File ( pathStr );
		
		boolean b = dir.mkdirs();
		Log.d( "Create dir for download result.", b, dir );
		
		return dir.getAbsolutePath();
	}

	private String defaultWritePath() {
		return Environment.getExternalStorageDirectory()
					+ File.separator + DEFAULT_WRITE_PATH;
	}

	public void setWritePath( String path ) {
		preferences
			.edit()
			.putString(KEY_WRITE_PATH, path)
			.putBoolean( KEY_USE_SAF, false )
			.commit();
		
		Log.d( "Download dir changed to ", getWritePath() );
	}

	public void setWriteUri(Uri uri) {
		preferences
			.edit()
			.putString(KEY_WRITE_PATH, uri.toString())
			.putBoolean( KEY_USE_SAF, true )
			.commit();
		
		Log.d( "Download uri changed to ", getWriteUri() );
	}
	
	public Uri getWriteUri() {
		if( !ApplicationInterface.isSafSystem() ) {
			throw new UnsupportedOperationException(
				"Version of the system does not support Storage Access Framework!" );
		}
		
		String uriStr = preferences.getString( KEY_WRITE_PATH, null );
		if( uriStr == null || !preferences.getBoolean( KEY_USE_SAF, false ) ) {
			throw new UnsupportedOperationException( "Write path has not picked by SAF!" );
		}
		Uri uri = Uri.parse(uriStr);
		return uri;
	}

	private void promptPickPath() {
		if( mPromptPickPathCount == 0 ) {
			String text
				= mContext.getString( R.string.promptPickWritePath,
									  mContext.getString( 
										R.string.prefTitle_DownloadTarget ),
									  defaultWritePath() );
			Toast.makeText(mContext, text, Toast.LENGTH_LONG ).show();
			mPromptPickPathCount = 10;
		}
		
		mPromptPickPathCount--;
	}

	public boolean isStorageAsscessFramework() {
		return preferences.getBoolean( KEY_USE_SAF, false );
	}
	
	public boolean isAppend() {
		return preferences.getBoolean( KEY_RESUME, false );
	}

	public int getConnectionTimeout() {
		return preferences.getInt( "KEY_CONNECT_TIMEOUT", 3000 );
	}

}
