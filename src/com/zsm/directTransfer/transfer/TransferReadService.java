package com.zsm.directTransfer.transfer;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import com.zsm.directTransfer.connection.DataConnectionManager;
import com.zsm.directTransfer.data.FileTransferObject;
import com.zsm.directTransfer.preferences.Preferences;
import com.zsm.directTransfer.transfer.TransferProgressor.REASON;
import com.zsm.directTransfer.transfer.operation.DirectFileOperation.FileTransferInfo;
import com.zsm.log.Log;

public class TransferReadService implements Runnable, AutoCloseable {

	private static TransferReadService mInstance;
	
	private final BlockingQueue<FileTransferObject> mQueue;
	private final ExecutorService mPool;
	private boolean mClosed;

	private TransferReadService() {
		mQueue = new LinkedBlockingQueue<FileTransferObject>();
		mPool = Executors.newFixedThreadPool(
					Preferences.getInstance().getMaxTransferThreadNum() );
		
		mClosed = false;
	}
	
	public static void start() {
		if( mInstance != null ) {
			throw new IllegalStateException( 
						"TransferReadService has been started!" );
		}
		mInstance = new TransferReadService( );
		new Thread( mInstance ).start();
	}
	
	public static TransferReadService getInstance() {
		return mInstance;
	}
	
	public void add( FileTransferInfo fti, TransferProgressor p )
						throws InterruptedException {
		
		p.start(fti);
		FileTransferObject fto = new FileTransferObject(fti, p);
		mQueue.put( fto );
	}

	@Override
	public void run() {
		while( !mClosed ) {
			try {
				FileTransferObject tf = mQueue.take();
				mPool.execute( new ReadTask( tf.getFileTransferInfo(),
											 tf.getProgressor() ) );
			} catch (InterruptedException e) {
			} finally {
				close();
			}
		}
	}

	@Override
	public void close() {
		Log.d( "Read is to be closed." );
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
	
	class ReadTask extends TransferTask implements Runnable, Closeable {

		private OutputStream mOutputStream;
		
		private ReadTask( FileTransferInfo fti, TransferProgressor p ) {
			super( fti );
			
			mProgressor = p;
			mProgressor.setTransferTask( this );
		}
		
		@Override
		public void run() {
			try {
				read();
				mProgressor.succeed( mFileTraqnsferInfo );
			} catch ( FileNotFoundException e ) {
				Log.e( e, "Create file failed", mFileTraqnsferInfo.getFilePathName() );
				mProgressor.failed( mFileTraqnsferInfo, REASON.CANNOT_CREATE_FILE );
			} catch (IOException e) {
				Log.e( e, "Error for I/O", mFileTraqnsferInfo );
				mProgressor.failed( mFileTraqnsferInfo, REASON.IO_ERROR );
			} catch (InterruptedException e) {
				Log.e( e, "Transfer interrupted", mFileTraqnsferInfo );
				mProgressor.failed( mFileTraqnsferInfo, REASON.CANCELLED );
			} finally {
				if( mFileTraqnsferInfo != null ) {
					TransferProgressorManager.getInstance()
						.removeByTransferId( mFileTraqnsferInfo.getId() );
				}

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
				= DataConnectionManager.getInstance()
					.connectToServer( mFileTraqnsferInfo.getPeer() );
			
			mConnection.setTransferTask( this );
			mConnection.sendRequestOperation( mFileTraqnsferInfo );
			int len = 0;
			byte[] buffer = new byte[2048];
			long totalLen 
				= mFileTraqnsferInfo.getStartPosition() > 0
					? mFileTraqnsferInfo.getStartPosition() : 0;
					
			mProgressor.start( mFileTraqnsferInfo );
			// The read task will not pause. When the peer's write task paused, the read
			// task will be paused as not data received
			while( ( len = mConnection.read( buffer ) ) >= 0 ) {
				
				mOutputStream.write(buffer, 0, len);
				totalLen += len;
				mProgressor.update( mFileTraqnsferInfo, totalLen );
				// Do not know who interrupts and why. So do not invoke sleep
//				Thread.sleep( 100 );
				if( getState() == STATE.CANCELLED ) {
					throw new InterruptedException( "Stopped by user!" );
				}
			}
		}

		private OutputStream openOutputStream() throws FileNotFoundException {
			File file = new File( getLocalFileName() );
			
			boolean append
				= Preferences.getInstance().isAppend()
					&& file.exists() && file.length() <= mFileTraqnsferInfo.getSize();
			
			if( append ) {
				mFileTraqnsferInfo.setStartPosition( file.length() );
			}
			
			Log.d( "File to store.", file );
			return new FileOutputStream( file, append );
		}

		private String getLocalFileName() {
			int separatorIndex
				= mFileTraqnsferInfo.getFilePathName().lastIndexOf( File.separator );
			String fn = mFileTraqnsferInfo.getFilePathName();
			if( separatorIndex > 0 ) {
				fn = fn.substring( separatorIndex + 1 );
			}
			return Preferences.getInstance().getWritePath() + File.separator + fn;
		}

		@Override
		public void close() throws IOException {
			Log.d( "Read task is to be closed." );
			
			if( getState() != STATE.FINISHED ) {
				setState( STATE.CANCELLED );
			}
			
			if( mOutputStream != null ) {
				mOutputStream.close();
			}
			mOutputStream = null;
			
			if( mConnection != null ) {
				mConnection.close();
			}
			mConnection = null;
		}

		@Override
		public void resumeByPeer() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void pauseByPeer() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void cancelByPeer() {
			// TODO Auto-generated method stub
			
		}
	}
}
