package com.zsm.directTransfer.connection;

import java.net.InetAddress;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;

import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.directTransfer.wifip2p.WifiP2pGroupManager;
import com.zsm.log.Log;

public class PeerConnectionManager implements AutoCloseable {

	private static PeerConnectionManager mInstance;
	private WifiP2pManager mManager;
	private Context mContext;
	private WifiP2pConnectionBroadcastReceiver mWifiP2pConnectionReceiver;

	private PeerConnectionManager() {
		
	}
	
	public static PeerConnectionManager getInstance() {
		return mInstance;
	}

	public static void initInstance( WifiP2pManager manager, Context context ) {
		if( mInstance != null ) {
			throw new IllegalStateException(
						"PeerConnectionManager has been initialized!" );
		}
		mInstance = new PeerConnectionManager();
		mInstance.mManager = manager;
		mInstance.mContext = context;
		
		mInstance.registerBroadcastReceiver();
	}
	
	private void registerBroadcastReceiver() {
		mWifiP2pConnectionReceiver = new WifiP2pConnectionBroadcastReceiver();
		IntentFilter filter
			= new IntentFilter( WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION );
		mContext.registerReceiver( mWifiP2pConnectionReceiver, filter);
	}
	
	public void requestPeerP2pConnection(WifiP2pPeer peer, ActionListener l) {
		WifiP2pConfig wifiP2pConfig = new WifiP2pConfig();
		wifiP2pConfig.deviceAddress = peer.getMacAddress();
		mManager.connect(peer.getChannel(), wifiP2pConfig, l);
	}

	@Override
	public void close() throws Exception {
		if( mWifiP2pConnectionReceiver != null ) {
			mContext.unregisterReceiver(mWifiP2pConnectionReceiver);
			mWifiP2pConnectionReceiver = null;
		}
		
	}

	private class WifiP2pConnectionBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			NetworkInfo ni
				= intent.getParcelableExtra( WifiP2pManager.EXTRA_NETWORK_INFO );
			Log.d( "Wifi p2p connection broadcast received", ni );
			if( ni.getState() == NetworkInfo.State.DISCONNECTED ) {
				// This device is disconnected from the group
				MessageConnectionManager.getInstance().closeAll();
			}
			if( ni.isConnected() ) {
				WifiP2pGroup group
					= intent.getParcelableExtra( WifiP2pManager.EXTRA_WIFI_P2P_GROUP );
				WifiP2pInfo info
					= intent.getParcelableExtra( WifiP2pManager.EXTRA_WIFI_P2P_INFO );
				if( info.groupFormed ) {
					if( info.isGroupOwner ) {
						// Server socket started in {@code initServer}
					} else {
						MessageConnectionManager.getInstance()
							.connectToMessagePeer(info.groupOwnerAddress);
					}
				}
				Log.d( "P2p connected", group, info );
				WifiP2pGroupManager.getInstance()
					.updateGroup(group, info.groupOwnerAddress);
			}
		}
	}
	
	public PeerMessageConnection getPeerMessageConnection( WifiP2pPeer peer ) {
		InetAddress address
			= WifiP2pGroupManager.getInstance().getPeerInetAddress(peer);
		return MessageConnectionManager.getInstance()
					.getPeerMessageConnection(address);
	}

}
