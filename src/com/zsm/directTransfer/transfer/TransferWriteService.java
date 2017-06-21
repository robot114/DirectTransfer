package com.zsm.directTransfer.transfer;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;

import com.zsm.directTransfer.connection.DataConnection;
import com.zsm.directTransfer.connection.DataConnectionManager;
import com.zsm.directTransfer.preferences.Preferences;
import com.zsm.directTransfer.transfer.TransferProgressor.REASON;
import com.zsm.directTransfer.transfer.operation.BadPacketException;
import com.zsm.directTransfer.transfer.operation.ConnectionSyncException;
import com.zsm.directTransfer.transfer.operation.DirectMessager;
import com.zsm.directTransfer.transfer.operation.DirectOperation;
import com.zsm.directTransfer.transfer.operation.ReadFileOperation;
import com.zsm.log.Log;

public class TransferWriteService implements Runnable, AutoCloseable {
	
	private static TransferWriteService mInstance;
	private ExecutorService mPool;

	private TransferWriteService() {
		mPool = Executors.newFixedThreadPool(
				Preferences.getInstance().getMaxTransferThreadNum() );
	}
	
	public static TransferWriteService getInstance() throws IOException {
		return mInstance;
	}

	public static void start( ) {
		if( mInstance != null ) {
			throw new IllegalStateException( 
						"TransferWriteService has been started!" );
		}
		mInstance = new TransferWriteService( );
		new Thread( mInstance, "WriteService" ).start();
	}
	
	@Override
	public void close() throws Exception {
		mPool.shutdown();
		List<Runnable> processingTask = mPool.shutdownNow();
		for( Runnable r : processingTask ) {
			WriteTask wt = (WriteTask)r;
			try {
				wt.close();
			} catch (IOException e) {
				Log.e( e, "Failed to close the write task, "
						  + "the other read tasks continue being closed" );
			}
		}
	}

	@Override
	public void run() {
		DataConnectionManager dcm = DataConnectionManager.getInstance();
		while( dcm.isServerStarted() ) {
			try {
				WriteTask wt = new WriteTask( dcm.accept() );
				mPool.execute( wt );
			} catch (IOException e) {
				Log.e( e, "Failed to accept data connection" );
			}
		}
	}

	class WriteTask extends TransferTask implements Runnable, Closeable {

		private WriteTask( DataConnection dataConnection ) {
			mConnection = dataConnection;
			mConnection.setTransferTask( this );
			setState( STATE.INIT );
		}
		
		@Override
		public void close() throws IOException {
			if( getState() != STATE.FINISHED ) {
				setState( STATE.CANCELLED );
			}
			
			if( mConnection != null ) {
				mConnection.close();
			}
			mConnection = null;
			unlock();
		}

		private void unlock() {
			synchronized( mLock ) {
				if( getState() == STATE.PAUSED ) {
					mLock.notifyAll();
				}
			}
		}
		
		@Override
		public void run() {
			mFileTraqnsferInfo = null;
			try {
				write();
			} catch ( FileNotFoundException  e ) {
				setState( STATE.FAILED );
				Log.e( e, "File not found", mFileTraqnsferInfo );
				if( mProgressor != null ) {
					mProgressor.failed(mFileTraqnsferInfo, REASON.FILE_NOT_FOUND);
				}
			} catch (UnsupportedOperationException | ConnectionSyncException
					 | BadPacketException | IOException e) {
				setState( STATE.FAILED );
				Log.e( e, "Invalid packet, reset the data connection" );
				if( mProgressor != null ) {
					mProgressor.failed( mFileTraqnsferInfo, REASON.IO_ERROR );
				}
			} catch (InterruptedException e) {
				Log.d( "Transfer interrupted", mFileTraqnsferInfo );
				if( mProgressor != null ) {
					mProgressor.failed( mFileTraqnsferInfo, REASON.CANCELLED );
				}
			} catch (TimeoutException e) {
				setState( STATE.FAILED );
				Log.e( e, "Transfer timeout" );
				if( mProgressor != null ) {
					mProgressor.failed( mFileTraqnsferInfo, REASON.IO_ERROR );
				}
			} finally {
				if( mFileTraqnsferInfo != null ) {
					TransferProgressorManager.getInstance()
						.removeByTransferId( mFileTraqnsferInfo.getId() );
				}
				try {
					close();
				} catch (IOException e) {
					Log.e( e, "Close the write task failed!" );
				}
			}
		}
		
		private void write() 
			throws IOException, UnsupportedOperationException,
				   InterruptedException, ConnectionSyncException,
				   BadPacketException, TimeoutException {
			
			setState( STATE.PREPARING );
			DirectOperation op = mConnection.receiveOperation( 3000 );
			byte opCode = op.getOpCode();
			if( opCode != DirectMessager.OPCODE_TYPE_READ_FILE ) {
				throw new UnsupportedOperationException(
						"Should be read file opcode. OpCode is " + opCode );
			}
			ReadFileOperation rfo = (ReadFileOperation)op;
			mFileTraqnsferInfo = rfo.getFileInfo();
			File file = new File( mFileTraqnsferInfo.getFilePathName() );
			mFileTraqnsferInfo.setSize( file.length() );
			mProgressor
				= TransferProgressorManager.getInstance()
					.getByTransferId( mFileTraqnsferInfo.getId() );
			
			mProgressor.setTransferTask( this );

			mProgressor.start(mFileTraqnsferInfo);
			FileInputStream fis = null;
			try {
				fis = new FileInputStream( file );
				long totalLen = 0;
				long startPosition = mFileTraqnsferInfo.getStartPosition();
				if( startPosition > 0 ) {
					fis.skip(startPosition);
					totalLen = startPosition;
				}
				int len = 0;
				byte[] buffer = new byte[4096];
				setState( STATE.STARTED );
				while( ( len = fis.read( buffer  ) ) > 0 ) {
					totalLen = doWrite(totalLen, len, buffer);
					if( getState() == STATE.CANCELLED ) {
						throw new InterruptedException( "Stopped by user!" );
					}
				}
				setState( STATE.FINISHED );
				mProgressor.succeed( mFileTraqnsferInfo );
			} finally {
				if( fis != null ) {
					fis.close();
				}
			}
		}

		private long doWrite(long totalLen, int len, byte[] buffer)
				throws IOException, InterruptedException {

			synchronized( mLock ) {
				mConnection.write( buffer, 0, len );
				Thread.sleep( 10 );
				totalLen += len;
				mProgressor.update( mFileTraqnsferInfo, totalLen );
				
				while( getState() == STATE.PAUSED ) {
					mLock.wait();
				}
				
				return totalLen;
			}
		}
		
		@Override
		public void resumeByPeer() {
			synchronized( mLock ) {
				unlock();
				super.resumeByPeer();
			}
		}
		
		@Override
		public void resumeByUi() {
			synchronized( mLock ) {
				unlock();
				super.resumeByUi();
			}
		}
	}
}
