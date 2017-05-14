package com.zsm.directTransfer.connection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.TimeoutException;

import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.directTransfer.transfer.TransferTask;
import com.zsm.directTransfer.transfer.operation.BadPacketException;
import com.zsm.directTransfer.transfer.operation.ConnectionSyncException;
import com.zsm.directTransfer.transfer.operation.DirectFileOperation.FileTransferInfo;
import com.zsm.directTransfer.transfer.operation.DirectMessager;
import com.zsm.directTransfer.transfer.operation.DirectOperation;
import com.zsm.directTransfer.transfer.operation.FileTransferActionOperation;
import com.zsm.directTransfer.transfer.operation.ReadFileOperation;
import com.zsm.directTransfer.wifip2p.WifiP2pGroupManager;

public class DataConnection implements AutoCloseable {
	
	private Socket mSocket;
	private InputStream mInputStream;
	private OutputStream mOutputStream;
	private DirectMessager mMessager;
	private DirectOperation mOperation;
	private TransferTask mTransferTask;

	DataConnection( Socket socket ) throws IOException {
		mSocket = socket;
		mInputStream = mSocket.getInputStream();
		mOutputStream = mSocket.getOutputStream();
		WifiP2pPeer peer
			= WifiP2pGroupManager.getInstance()
				.findPeerByAddress(getRemoteAddress());
		
		mMessager = new DirectMessager( mInputStream, mOutputStream, peer );
	}

	@Override
	public void close() throws IOException {
		DataConnectionManager.getInstance().remove( mOperation.getSerialNo() );
		
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
	
	public long sendRequestOperation(FileTransferInfo fi) throws IOException {
		mOperation = new ReadFileOperation(fi);
		DataConnectionManager.getInstance().add( mOperation.getSerialNo(), this );
		return mMessager.sendOperation( mOperation );
	}

	public DirectOperation receiveOperation(int timeoutInMs)
				throws UnsupportedOperationException, IOException,
					   InterruptedException, ConnectionSyncException,
					   BadPacketException, TimeoutException {
		
		mOperation = mMessager.receiveOperation(true, timeoutInMs);
		DataConnectionManager.getInstance().add( mOperation.getSerialNo(), this );
		return mOperation;
	}

	/**
	 * Invoked by {@link FileTransferActionOperation}. Just notify the TransferTask.
	 * This should not be invoked in the message thread 
	 * {@link PeerMessageConnection.PeerMessageThread}.
	 */
	public void resumeTranfser( ) {
		checkThread();
		mTransferTask.resumeByPeer(); 
	}

	/**
	 * Invoked by {@link FileTransferActionOperation}. Just notify the TransferTask.
	 * This should not be invoked in the message thread 
	 * {@link PeerMessageConnection.PeerMessageThread}.
	 */
	public void pauseTransfer() {
		checkThread();
		mTransferTask.pauseByPeer();
	}

	/**
	 * Invoked by {@link FileTransferActionOperation}. Just notify the TransferTask.
	 * This should not be invoked in the message thread 
	 * {@link PeerMessageConnection.PeerMessageThread}.
	 */
	public void cancelTransfer() {
		checkThread();
		mTransferTask.cancelByPeer();
	}

	private void checkThread() {
		if( !PeerMessageConnection.NAME_THREAD_MESSAGE
				.equals( Thread.currentThread().getName() ) ) {
			
			throw new IllegalStateException( 
					"This method can only invoked in the thread "
						+ PeerMessageConnection.NAME_THREAD_MESSAGE );
		}
	}

	public void notifyPeerOperation(byte operation) {
		PeerMessageConnection mc
			= MessageConnectionManager.getInstance()
				.getPeerMessageConnection( mSocket.getInetAddress() );
		
		mc.sendOperation( new FileTransferActionOperation( operation ) );
	}

	public void setTransferTask(TransferTask task) {
		mTransferTask = task;
	}
	
	public InetAddress getRemoteAddress() {
		return mSocket.getInetAddress();
	}

}
