package com.zsm.directTransfer.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;

import android.content.Context;

import com.zsm.directTransfer.connection.PeerMessageConnection.MessageConnectionListener;
import com.zsm.directTransfer.ui.StatusBarOperator;
import com.zsm.log.Log;

public class MessageConnectionManager implements AutoCloseable {

	private static final int MESSAGE_PORT = 8888;
	private static final int BIND_REQUEST_TIMEOUT = 3000;
	
	private static MessageConnectionManager mInstance;
	
	private Context mContext;
	private MessageConnectionListener mMessageConnectionListener;
	private StatusBarOperator mStatusBar;

	private boolean mStarted;
	private ServerSocket mServerSocket;
	
	private Map<InetAddress, PeerMessageConnection> mPeerMessageConnectionSet;

	private MessageConnectionManager() {
		mPeerMessageConnectionSet
			= new HashMap<InetAddress, PeerMessageConnection>();
	}
	
	static public MessageConnectionManager getInstance() {
		if( mInstance == null ) {
			mInstance = new MessageConnectionManager();
		}
		
		return mInstance;
	}
	
	public void initInstance(Context context, MessageConnectionListener mcl,
							 StatusBarOperator statusBar) {
		mContext = context;
		mMessageConnectionListener = mcl;
		mStatusBar = statusBar;
		mStarted = false;
	}
	
	public void startMessageServer() throws IOException {
		if( mServerSocket != null && !mServerSocket.isClosed() ) {
			throw new IllegalStateException(
					"Message port has been listened: " + mServerSocket );
		}
		startListening();
	}
	
	private void startListening() throws IOException {
		if( mStarted ) {
			return;
		}
		
		mServerSocket = new ServerSocket(MESSAGE_PORT);
		Log.d( "Start to listen the message port ", MESSAGE_PORT );
		
		new Thread( "MessageListenerThread" ) {
			@Override
			public void run() {
				
				mStarted = true;
				Log.d( "Message listening thread started" );
				do {
					try {
						acceptMessageConnection();
					} catch (IOException e) {
						Log.e( e, "Cannot start client socket" );
						// TODO ResId
						mStatusBar
							.setStatus( "Cannot start receiving",
										StatusBarOperator.STATUS_WARNING);
					}
				} while( mStarted );
			}
		}.start();
	}

	@Override
	synchronized public void close() {
		Log.d( "Connection manager is to be closed" );
		mStarted = false;
		
		if( mServerSocket != null && !mServerSocket.isClosed() ) {
			try {
				mServerSocket.close();
			} catch (Exception e ) {
				Log.e( e, "Close messageServer Failed" );
			}
		}
		mServerSocket = null;
		
		for( PeerMessageConnection pmc : mPeerMessageConnectionSet.values() ) {
			pmc.close();
		}
		mPeerMessageConnectionSet.clear();
	}

	public void connectToMessagePeer( final InetAddress peerAddress ) {
		new Thread( "Thread-ConnectToMsgPeer" ) {

			@Override
			public void run() {
				PeerMessageConnection peerConnection
					= new PeerMessageConnection( 
							peerAddress, MESSAGE_PORT,
							mMessageConnectionListener );
				
			    try {
			    	peerConnection.connect(BIND_REQUEST_TIMEOUT);
			    	Log.d( "Message connection connected", peerConnection );
					mPeerMessageConnectionSet.put(peerAddress, peerConnection);
					mMessageConnectionListener.connected( peerConnection );
				} catch (IOException e) {
					Log.e( e, "Cannot request connection to the peer", peerAddress );
					peerConnection.close();
				}
			}
		}.start();
	}

	private PeerMessageConnection acceptMessageConnection() throws IOException {
		Socket client;
		client = mServerSocket.accept();
		Log.d( "Connection from client connected", client );
		
		InetAddress remoteAddress = client.getInetAddress();
		PeerMessageConnection peerMessageConnection
			= mPeerMessageConnectionSet.get( remoteAddress );
		
		if( peerMessageConnection != null ) {
			Log.w( "A connection to the peer existed, it will be reconnected",
				   peerMessageConnection, client );
			peerMessageConnection.reconnect(client);
		} else {
			peerMessageConnection
				= new PeerMessageConnection( client, mMessageConnectionListener );
			mMessageConnectionListener.connected( peerMessageConnection );
			
			mPeerMessageConnectionSet.put( remoteAddress, peerMessageConnection );
		}
		
		return peerMessageConnection;
	}
	
	public PeerMessageConnection getPeerMessageConnection( InetAddress address ) {
		return mPeerMessageConnectionSet.get(address);
	}
	
	synchronized public void closePeerMessageConnection( InetAddress address ) {
		PeerMessageConnection pmc = mPeerMessageConnectionSet.remove(address);
		if( pmc != null ) {
			pmc.close();
		}
	}
	
	synchronized public void closeAll() {
		for( PeerMessageConnection pmc : mPeerMessageConnectionSet.values() ) {
			pmc.close();
		}
		mPeerMessageConnectionSet.clear();
		Log.d( "All the peer message connection closed!" );
	}
}
