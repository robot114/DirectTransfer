package com.zsm.directTransfer.connection;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.zsm.directTransfer.transfer.operation.DirectFileOperation;
import com.zsm.log.Log;

public class DataConnectionManager implements AutoCloseable {

	private static final int DATA_CONNECTION_PORT = 8889;
	private static DataConnectionManager mInstance;
	private ServerSocket mServerSocket;

	private DataConnectionManager() {
		
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

	private class DataServerThread extends Thread {
		
		DataServerThread() {
			super( "DataServerThread" );
		}
		
		@Override
		public void run() {
			while( isServerStarted() ) {
				try {
					Socket socket = mServerSocket.accept();
				} catch (IOException e) {
					Log.e( e, "Failed to accept the data connection" );
					try {
						mServerSocket.close();
					} catch (IOException e1) {
						Log.w( e1, "Failed to close the mServerSocket" );
					}
					mServerSocket = null;
					break;
				}
			}
		}
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

	public DataConnection accept() throws IOException {
		return new DataConnection( mServerSocket.accept() );
	}
}
