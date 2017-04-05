package com.zsm.directTransfer.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import com.zsm.directTransfer.transfer.operation.BadPacketException;
import com.zsm.directTransfer.transfer.operation.ConnectionSyncException;
import com.zsm.directTransfer.transfer.operation.DirectFileOperation.FileInfo;
import com.zsm.directTransfer.transfer.operation.DirectMessager;
import com.zsm.directTransfer.transfer.operation.DirectOperation;
import com.zsm.directTransfer.transfer.operation.ReadFileOperation;

public class DataConnection implements AutoCloseable {
	
	private Socket mSocket;
	private InputStream mInputStream;
	private OutputStream mOutputStream;
	private DirectMessager mMessager;

	DataConnection( Socket socket ) throws IOException {
		mSocket = socket;
		mInputStream = mSocket.getInputStream();
		mOutputStream = mSocket.getOutputStream();
		mMessager = new DirectMessager( mInputStream, mOutputStream );
	}

	@Override
	public void close() throws IOException {
		if( mMessager != null && !mMessager.isClosed() ) {
			mMessager.close();
		}
		mMessager = null;
		
		if( mOutputStream != null ) {
			mOutputStream.flush();
			mOutputStream.close();
		}
		mOutputStream = null;
		
		if( mInputStream != null ) {
			mInputStream.close();
		}
		mInputStream = null;
		
		if( mSocket != null && !mSocket.isClosed() ) {
			mSocket.close();
		}
		mSocket = null;
	}

	public int read(byte[] buffer) throws IOException {
		return mInputStream.read(buffer);
	}

	public void write(byte[] buffer, int offset, int len) throws IOException {
		mOutputStream.write(buffer, offset, len);
	}
	
	public void sendRequestOperation(FileInfo fi) throws IOException {
		mMessager.sendOperation( new ReadFileOperation(fi) );
	}

	public DirectOperation receiveOperation(int timeoutInMs)
				throws UnsupportedOperationException, IOException,
					   InterruptedException, ConnectionSyncException,
					   BadPacketException, TimeoutException {
		
		return mMessager.receiveOperation(true, timeoutInMs);
	}

}
