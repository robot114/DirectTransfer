package com.zsm.directTransfer.connection;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;

import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.log.Log;

public class DataConnectionManager implements AutoCloseable {

	private static final int DATA_CONNECTION_PORT = 8889;
	private static DataConnectionManager mInstance;
	private ServerSocket mServerSocket;
	private ConcurrentHashMap<Long, DataConnection> mDataConnectionTable;

	private DataConnectionManager() {
		mDataConnectionTable = new ConcurrentHashMap<Long, DataConnection>();
	}
	
	public static DataConnectionManager getInstance() {
		if( mInstance == null ) {
			mInstance = new DataConnectionManager();
		}
		
		return mInstance;
	}
	
	public void startListening() throws IOException {
		if( isServerStarted() ) {
			Log.d( "The port is being listened.", mServerSocket );
			return;
		}
		
		mServerSocket = new ServerSocket( DATA_CONNECTION_PORT );
	}

	public boolean isServerStarted() {
		return mServerSocket != null && !mServerSocket.isClosed();
	}
	
	@Override
	public void close() throws Exception {
		if( isServerStarted() ) {
			mServerSocket.close();
		}
		mServerSocket = null;
	}

	public DataConnection connectToServer( InetAddress server )
				throws IOException {
		
		Socket socket = new Socket( );
		InetSocketAddress soa
			= new InetSocketAddress( server, DATA_CONNECTION_PORT );
		socket.connect( soa );
		Log.d( "Data connection established. ", soa );
		
		return new DataConnection( socket );
	}

	public DataConnection connectToServer( WifiP2pPeer server )
				throws IOException {
		
		InetAddress address = server.getInetAddress();
		return connectToServer(address);
	}

	public DataConnection accept() throws IOException {
		return new DataConnection( mServerSocket.accept() );
	}

	public void add(long serialNo, DataConnection dataConnection) {
		mDataConnectionTable.put(serialNo, dataConnection);
	}

	public DataConnection remove(long serialNo) {
		return mDataConnectionTable.remove(serialNo);
	}

	public DataConnection getConnection(long serialNo) {
		return mDataConnectionTable.get(serialNo);
	}
}
