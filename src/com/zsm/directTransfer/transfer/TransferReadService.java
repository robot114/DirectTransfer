package com.zsm.directTransfer.transfer;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import com.zsm.directTransfer.connection.DataConnection;
import com.zsm.directTransfer.connection.DataConnectionManager;
import com.zsm.directTransfer.preferences.Preferences;
import com.zsm.directTransfer.transfer.operation.DirectFileOperation.FileInfo;
import com.zsm.log.Log;

public class TransferReadService implements Runnable, AutoCloseable {

	private class TransferFile {
		private FileInfo mFileInfo;
		private InetAddress mPeer;
		private TransferProgressor mProgressor;
		
		TransferFile( FileInfo fi, InetAddress peer, TransferProgressor tp ) {
			mFileInfo = fi;
			mPeer = peer;
			mProgressor = tp;
		}
	}
	
	private static TransferReadService mInstance;
	private final BlockingQueue<TransferFile> mQueue;
	private final ExecutorService mPool;
	private boolean mClosed;

	private TransferReadService() {
		mQueue = new LinkedBlockingQueue<TransferFile>();
		mPool = Executors.newFixedThreadPool(
					Preferences.getInstance().getMaxTransferThreadNum() );
		
		mClosed = false;
	}
	
	public static TransferReadService getInstance() {
		if( mInstance == null ) {
			mInstance = new TransferReadService();
		}
		
		return mInstance;
	}
	
	public void add( FileInfo fi, InetAddress peer, TransferProgressor tp )
						throws InterruptedException {
		
		mQueue.put( new TransferFile( fi, peer, tp ) );
	}

	@Override
	public void run() {
		while( !mClosed ) {
			try {
				TransferFile tf = mQueue.take();
				mPool.execute( new ReadTask( tf.mFileInfo, tf.mPeer,
											 tf.mProgressor ) );
			} catch (InterruptedException e) {
			} finally {
				close();
			}
		}
	}

	@Override
	public void close() {
		mClosed = true;
		mQueue.clear();
		mPool.shutdown();
		List<Runnable> processingTask = mPool.shutdownNow();
		for( Runnable r : processingTask ) {
			ReadTask rt = (ReadTask)r;
			try {
				rt.close();
			} catch (IOException e) {
				Log.e( e, "Failed to close the read task, "
						  + "the other read tasks continue being closed" );
			}
		}
	}
	
	private class ReadTask implements Runnable, Closeable {

		private boolean mStop;
		private FileInfo mFileInfo;
		private InetAddress mPeer;
		private TransferProgressor mProgressor;
		private OutputStream mOutputStream;
		private DataConnection mConnection;

		private ReadTask( FileInfo fi, InetAddress peer,
						  TransferProgressor progressor ) {
			
			mFileInfo = fi;
			mPeer = peer;
			mProgressor = progressor;
		}
		
		@Override
		public void run() {
			try {
				read();
				mProgressor.succeed( mFileInfo );
			} catch ( FileNotFoundException e ) {
				Log.e( e, "Create file failed", mFileInfo.mFilePathName );
				mProgressor.failed( mFileInfo,
									TransferProgressor.REASON.CANNOT_CREATE_FILE );
			} catch (IOException e) {
				Log.e( e, "Error for I/O", mFileInfo );
				mProgressor.failed( mFileInfo, TransferProgressor.REASON.IO_ERROR );
			} catch (InterruptedException e) {
				Log.e( e, "Transfer interrupted", mFileInfo );
				mProgressor.failed( mFileInfo, TransferProgressor.REASON.CANCELLED );
			} finally {
				try {
					close();
				} catch (IOException e) {
					Log.e( e, "Close read task failed!" );
				}
			}
		}

		private void read() throws IOException, InterruptedException {
			mOutputStream = openOutputStream();
			mConnection
				= DataConnectionManager.getInstance().connectToServer( mPeer );
			mConnection.sendRequestOperation( mFileInfo );
			int len = 0;
			byte[] buffer = new byte[2048];
			long totalLen 
				= mFileInfo.mStartPosition > 0 ? mFileInfo.mStartPosition : 0;
			mProgressor.start( mFileInfo );
			while( !mStop && ( len = mConnection.read( buffer ) ) >= 0 ) {
				mOutputStream.write(buffer, 0, len);
				Thread.sleep( 10 );
				totalLen += len;
				mProgressor.update( mFileInfo, totalLen, mFileInfo.mSize );
			}
		}

		private OutputStream openOutputStream() throws FileNotFoundException {
			File file = new File( getLocalFileName() );
			boolean append
				= Preferences.getInstance().isAppend()
					&& file.exists() && file.length() < mFileInfo.mSize;
			
			if( append ) {
				mFileInfo.mStartPosition = mFileInfo.mStartPosition;
			}
			return new FileOutputStream( file, append );
		}

		private String getLocalFileName() {
			int separatorIndex
				= mFileInfo.mFilePathName.lastIndexOf( File.pathSeparator );
			String fn = mFileInfo.mFilePathName;
			if( separatorIndex > 0 ) {
				fn = fn.substring( separatorIndex + 1 );
			}
			return Preferences.getInstance().getWritePath() + File.pathSeparator + fn;
		}

		@Override
		public void close() throws IOException {
			mStop = true;
			if( mOutputStream != null ) {
				mOutputStream.close();
			}
			mOutputStream = null;
			
			if( mConnection != null ) {
				mConnection.close();
			}
			mConnection = null;
		}
		
	}
}
