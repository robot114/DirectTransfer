package com.zsm.directTransfer.action.wifip2p;

import java.io.File;
import java.io.IOException;

import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;

import com.zsm.directTransfer.data.WifiP2pPeer;

public class WifiP2pAction {

	private WifiP2pManager mManager;
	private WifiP2pPeer mPeer;
	private File mSource;
	private WifiP2pConfig mWifiP2pConfig;

	public WifiP2pAction( WifiP2pManager manager, WifiP2pPeer peer, File source ) {
		mManager = manager;
		mPeer = peer;
		mSource = source;
		
		mWifiP2pConfig = new WifiP2pConfig();
		mWifiP2pConfig.deviceAddress = mPeer.getMacAddress();
	}
	
	/**
	 * Prepare for transferring. Normally, create the connection to peer in this method.
	 * 
	 * @throws IOException
	 */
	public void prepare() throws IOException {
		mManager.connect(mPeer.getChannel(), mWifiP2pConfig, new ActionListener() {

		    @Override
		    public void onSuccess() {
		        //success logic
		    }

		    @Override
		    public void onFailure(int reason) {
		        //failure logic
		    }
		});
	}
	
	/**
	 * Start to transfer
	 */
	public void start() {
		
	}
	
	/**
	 * Transfer is finished
	 * 
	 */
	public void finished() {
		
	}
}
