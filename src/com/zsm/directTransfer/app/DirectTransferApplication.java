package com.zsm.directTransfer.app;
import com.zsm.directTransfer.preferences.Preferences;
import com.zsm.driver.android.log.LogInstaller;
import com.zsm.driver.android.log.LogPreferences;
import com.zsm.log.Log;
import com.zsm.log.SystemOutLog;
import android.annotation.SuppressLint;
import android.app.Application;
import android.os.Build;


public class DirectTransferApplication extends Application {

	private static final String LOG_TAG = "DirectTransfer";

	public DirectTransferApplication() {
		super();
		LogInstaller.installAndroidLog( LOG_TAG );
	}

	@SuppressLint("DefaultLocale")
	@Override
	public void onCreate() {
		super.onCreate();
		LogPreferences.init( this );
		LogInstaller.installFileLog( this );
		if( Build.MANUFACTURER.toLowerCase().contains( "huawei" ) ) {
			// Just for Huawei pad
			Log.install( "SystemOut", new SystemOutLog( LOG_TAG ) );
			Log.setLevel( "SystemOut", Log.LEVEL.DEBUG );
		}
		
		Log.setGlobalLevel( Log.LEVEL.DEBUG );
		
		Preferences.init(getApplicationContext());
	}
}
