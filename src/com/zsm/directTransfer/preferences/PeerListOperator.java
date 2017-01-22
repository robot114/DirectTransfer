package com.zsm.directTransfer.preferences;

import java.util.ArrayList;

import com.zsm.directTransfer.data.WifiP2pPeer;

public interface PeerListOperator {

	abstract ArrayList<WifiP2pPeer> getPeers(String selection, String[] selectionArgs);

	abstract long addPeer(WifiP2pPeer peer);

	abstract boolean updatePeer(WifiP2pPeer peer);

	abstract boolean deletePeer(WifiP2pPeer peer);

	public abstract boolean peerExist(WifiP2pPeer peer);
}
