package com.zsm.directTransfer.data;

import android.net.wifi.p2p.WifiP2pDevice;

public class FakeWifiP2pPeer extends WifiP2pPeer {

	private String mAddress;

	public FakeWifiP2pPeer(String address ) {
		super(address, new WifiP2pDevice() );
		mAddress = address;
	}

	@Override
	public String getMacAddress() {
		return mAddress;
	}

}
