package com.zsm.directTransfer.app;

import android.os.Build;

public class ApplicationInterface {

	public static boolean isSafSystem() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
	}
}
