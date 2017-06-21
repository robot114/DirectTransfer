package com.zsm.directTransfer.connection;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.directTransfer.preferences.Preferences;
import com.zsm.directTransfer.transfer.operation.BadPacketException;
import com.zsm.directTransfer.transfer.operation.ConnectionSyncException;
import com.zsm.directTransfer.transfer.operation.DirectMessager;
import com.zsm.directTransfer.transfer.operation.DirectOperation;
import com.zsm.directTransfer.transfer.operation.StatusOperation;
import com.zsm.directTransfer.wifip2p.WifiP2pGroupManager;
import com.zsm.log.Log;

public class PeerMessageConnection implements Closeable {

//	private static final int TIMEOUT_FOR_ANNOUNCE_PEER = 3000;

	private enum STATE { 
		INIT,
		REQUEST_RECEIVING, REQUEST_DOING,
		RESPONSE_INIT, RESPONSE_SENT,
		REQUEST_INIT, REQUEST_SENDING, REQUEST_SENT,
		RESPONSE_RECEIVING, RESPONSE_OK, RESPONSE_ERR };
	
	private static final int CONNECT_RETRIES = 3;

	public static final String NAME_THREAD_MESSAGE = "Thread-Message";
	
	public interface MessageConnectionListener {
		void onConnectionFatalError(PeerMessageConnection connection, Exception reason);
		void connected(PeerMessageConnection peerMessageConnection);
	}

	private static final int TIME_FOR_NETWORK_READY = 1000;

	private Socket mSocket;
	private boolean mFromSocket;
	private InetAddress mPeerAddress;
	private int mPort;

	private DirectMessager mMessager;
	private MessageConnectionListener mConnectionListener;
	
	private int mConnectionTimeout;

	private DirectOperation mRequestOperation;

	private STATE mState;
	private PeerMessageThread mMessageThread;

	public PeerMessageConnection( Socket socket,
								  MessageConnectionListener listener )
					throws IOException, TimeoutException {
		
		mSocket = socket;
		mFromSocket = true;
		mPeerAddress = mSocket.getInetAddress();
		mPort = mSocket.getPort();
		mConnectionListener = listener;
		
		mConnectionTimeout = Preferences.getInstance().getConnectionTimeout();
		
		start();
	}
	
	public PeerMessageConnection( InetAddress peerAddress, int port,
						   		  MessageConnectionListener listener ) {
		
		mPeerAddress = peerAddress;
		mPort = port;
		mFromSocket = false;
		mConnectionListener = listener;
		
		mConnectionTimeout = Preferences.getInstance().getConnectionTimeout();
	}
	
	public void connect( ) throws IOException, TimeoutException {
		if( mFromSocket ) {
			throw new IllegalStateException( 
						"Cannot connect a peer constructed from socket" );
		}
		connectInner(CONNECT_RETRIES);
	}

	public void reconnect( ) throws IOException, TimeoutException {
		if( mFromSocket ) {
			// Constructed with socket means the peer request the connection.
			// So when reconnecting, just do nothing, and wait for the peer's request.
			Log.d( "Nothing to do when the connection is created with socket");
			return;
		}
		
		close();
		connectInner(1);
	}

	synchronized private void connectInner(int retries)
					throws IOException, TimeoutException {
		
	    InetSocketAddress socketAddress
    		= new InetSocketAddress(mPeerAddress, mPort);
	    
		try {
			Thread.sleep( TIME_FOR_NETWORK_READY/3 );
		} catch (InterruptedException ee) {
			Log.w( ee, "Thread to connect to Messager is interrupted" );
			return;
		}
	    for( int i = 0; i < retries; i++ ) {
			mSocket = new Socket( );
    		mSocket.bind(null);
	    	try {
				mSocket.connect(socketAddress, mConnectionTimeout);
				break;
			} catch (IOException e) {
				if( i == retries - 1 ) {
					throw e;
				}
				Log.d( e, "Peer is not ready to accept the message "
								+ "connection, try again later",
						socketAddress, "Retried times", i );
				try {
					Thread.sleep( TIME_FOR_NETWORK_READY );
				} catch (InterruptedException ee) {
					Log.w( ee, "Thread to connect to Messager is interrupted" );
					return;
				}
			}
	    }
	    
	    start( );
	}
	
	private void start( ) throws IOException, TimeoutException {
		if( mMessager != null ) {
			throw new IllegalStateException( 
						"There already has been a messager: " + mMessager );
		}
		
		WifiP2pPeer peer
			= WifiP2pGroupManager.getInstance()
				.findPeerByAddress(mSocket.getInetAddress(), mConnectionTimeout );
	
		InputStream in = mSocket.getInputStream();
		OutputStream out = mSocket.getOutputStream();
	    mMessager = new DirectMessager( in, out, peer );

		mMessageThread = new PeerMessageThread();
		mMessageThread.start();
	}

	@Override
	public void close() {
		Log.d( "The peer message connection is to be closed. ", this );
		closeMessager();
		
		if( mSocket != null && mSocket.isConnected() ) {
			try {
				mSocket.close();
			} catch( Exception e ) {
				Log.w( e, "Close socket failed!", mSocket );
			}
		}
		mSocket = null;
		
		if( mMessageThread != null ) {
			mMessageThread.mClosed = true;
			// In case is sleeping
			mMessageThread.interrupt();
		}
		mMessageThread = null;
	}

	public void reconnect( Socket socket ) throws IOException, TimeoutException {
		if( !mFromSocket ) {
			throw new IllegalStateException( 
				"Only can reconnect with a socket when it is constructed "
				+ "from a socket" );
		}
		
		close();
		
		mSocket = socket;
		mPeerAddress = mSocket.getInetAddress();
		mPort = mSocket.getPort();
		
		start();
	}

	private void closeMessager() {
		if( mMessager != null && !mMessager.isClosed() ) {
			try {
				mMessager.close();
			} catch (IOException e) {
				Log.w( e, "Close the message failed", mMessager );
			}
		}
		mMessager= null;
	}

	public boolean isConnected() {
		return mSocket != null && mSocket.isConnected();
	}

	public void sendOperation( DirectOperation op ) {
		synchronized( mState ) {
			mRequestOperation = op;
		}
	}

	public InetAddress getPeerInetAddress() {
		return mPeerAddress;
	}

	@Override
	public int hashCode() {
		return mPeerAddress.hashCode() ^ mPort;
	}

	@Override
	public boolean equals(Object obj) {
		if( obj == this ) {
			return true;
		}
		
		if( obj == null || !( obj instanceof PeerMessageConnection ) ) {
			return false;
		}
		
		PeerMessageConnection con = (PeerMessageConnection)obj;
		return con.mPeerAddress.equals( mPeerAddress ) && con.mPort == mPort;
	}

	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append( "Constructed from Socket: " ).append(mFromSocket)
			.append( ", Connected to peer: " ).append( isConnected() )
			.append( ", Peer address: " ).append( mPeerAddress )
			.append( ", Peer port: " ).append( mPort );
		if( mSocket != null ) {
			buff.append( ", Local address: ").append( mSocket.getLocalAddress() )
				.append( ", Local port: " ).append( mSocket.getLocalPort() );
		}
		
		return buff.toString();
	}
	
	private class PeerMessageThread extends Thread {
		private boolean mClosed;

		private PeerMessageThread( ) throws IOException {
			super( NAME_THREAD_MESSAGE );
		}
		
		@Override
		public void run() {
			while( !mClosed ) {
				mState = STATE.INIT;
				handleMessage();
			}
		}

		private void handleMessage() {
			synchronized( mState ) {
				try {
					mState = STATE.REQUEST_RECEIVING;
					DirectOperation op = mMessager.receiveOperation( false, 0 );
					if( op != null ) {
						Log.d( "Operation received", op );
						mState = STATE.REQUEST_DOING;
						StatusOperation status
							= op.doOperation( PeerMessageConnection.this );
						Log.d( "Response is to be sent: ", status );
						mState = STATE.RESPONSE_INIT;
						mMessager.sendOperation(status);
						Log.d( "Response sent: ", status );
						mState = STATE.RESPONSE_SENT;
					}
					sleep( 100 );
					if( mRequestOperation != null ) {
						sendRequest();
						StatusOperation status = waitForResponse( 300000 );
					}
					sleep( 100 );
				} catch ( ConnectionSyncException | InterruptedException
						  | IOException | TimeoutException | BadPacketException e) {
					
					close();
				
					Log.e(e, "The connection will be reset!" );
					mConnectionListener
						.onConnectionFatalError( PeerMessageConnection.this, e );
				}
			}
		}
		
		public void sendRequest() throws IOException {
			try {
				mState = STATE.REQUEST_SENDING;
				mMessager.sendOperation(mRequestOperation);
				Log.d( "Request operation sent", mRequestOperation );
				mState = STATE.REQUEST_SENT;
			} finally {
				mRequestOperation = null;
			}
		}

		public StatusOperation waitForResponse(int timeoutInMs)
						throws IOException, InterruptedException,
							   ConnectionSyncException, BadPacketException,
							   TimeoutException {
			
			mState = STATE.RESPONSE_RECEIVING;
			DirectOperation op = mMessager.receiveOperation( true, timeoutInMs );
			if( ! ( op instanceof StatusOperation ) ) {
				// Request must followed by response
				mMessager.sendOperation( StatusOperation.STATUS_NO_RESPONSE );
				mState = STATE.INIT;
				throw new IllegalStateException( "No response after request!" );
			}
			StatusOperation status = (StatusOperation)op;
			mState = status.ok() ? STATE.RESPONSE_OK : STATE.RESPONSE_ERR;
			Log.d( "Response received", status );
			
			return status;
		}

	}
}
