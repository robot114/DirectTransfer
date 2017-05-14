package com.zsm.directTransfer.ui;

import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;

import com.zsm.directTransfer.R;

public class ResourceUtility {

	private ResourceUtility() {
		
	}
	
	public static String getWifiP2pFailReason( Context context, int reason ) {
		
		int reasonId;
		switch( reason ) {
			case WifiP2pManager.P2P_UNSUPPORTED:
				reasonId = R.string.promptFailedToDiscoverPeerReasonUnsupport;
				break;
			case WifiP2pManager.BUSY:
				reasonId = R.string.promptFailedToDiscoverPeerReasonBusy;
				break;
			default:
				reasonId = R.string.promptFailedToDiscoverPeerReasonError;
				break;
		}
		
		return context.getString( reasonId );
	}

}
