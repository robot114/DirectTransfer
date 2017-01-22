package com.zsm.directTransfer.app;
import com.zsm.directTransfer.preferences.Preferences;
import com.zsm.driver.android.log.LogInstaller;
import com.zsm.driver.android.log.LogPreferences;

import android.app.Application;


public class DirectTransferApplication extends Application {

	public DirectTransferApplication() {
		super();
		LogInstaller.installAndroidLog( "DirectTransfer" );
	}

	@Override
	public void onCreate() {
		super.onCreate();
		LogPreferences.init( this );
		LogInstaller.installFileLog( this );
		
		Preferences.init(getApplicationContext());
	}
}
