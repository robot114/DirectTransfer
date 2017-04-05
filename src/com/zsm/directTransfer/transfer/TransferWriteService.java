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
import com.zsm.directTransfer.transfer.TransferProgressor.Generator;
import com.zsm.directTransfer.transfer.operation.BadPacketException;
import com.zsm.directTransfer.transfer.operation.ConnectionSyncException;
import com.zsm.directTransfer.transfer.operation.DirectFileOperation.FileInfo;
import com.zsm.directTransfer.transfer.operation.DirectMessager;
import com.zsm.directTransfer.transfer.operation.DirectOperation;
import com.zsm.directTransfer.transfer.operation.ReadFileOperation;
import com.zsm.log.Log;

public class TransferWriteService implements Runnable, AutoCloseable {
	
	private static TransferWriteService mInstance;
	private ExecutorService mPool;
	private Generator mProgressorGenerator;

	private TransferWriteService() throws IOException {
		DataConnectionManager.getInstance().startListening();
		mPool = Executors.newFixedThreadPool(
				Preferences.getInstance().getMaxTransferThreadNum() );
	}
	
	public static TransferWriteService getInstance() throws IOException {
		if( mInstance == null ) {
			mInstance = new TransferWriteService();
		}
		
		return mInstance;
	}

	public void setProgressorGenerator( TransferProgressor.Generator g  ) {
		mProgressorGenerator = g;
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
				WriteTask wt
					= new WriteTask( dcm.accept(),
									 mProgressorGenerator.newProgressor() );
				mPool.execute( wt );
			} catch (IOException e) {
				Log.e( e, "Failed to accept data connection" );
			}
		}
	}

	private class WriteTask implements Runnable, Closeable {

		private DataConnection mConnection;
		private TransferProgressor mProgressor;
		private FileInfo mFileInfo;

		private WriteTask( DataConnection dataConnection, TransferProgressor tp ) {
			mConnection = dataConnection;
			mProgressor = tp;
		}
		
		@Override
		public void close() throws IOException {
			if( mConnection != null ) {
				mConnection.close();
			}
			mConnection = null;
			
		}

		@Override
		public void run() {
			try {
				write();
			} catch ( FileNotFoundException  e ) {
				Log.e( e, "File not found", mFileInfo );
				mProgressor.failed(mFileInfo, TransferProgressor.REASON.FILE_NOT_FOUND);
			} catch (UnsupportedOperationException | ConnectionSyncException
					 | BadPacketException | IOException e) {
				Log.e( e, "Invalid packet, reset the data connection" );
				mProgressor.failed( mFileInfo, TransferProgressor.REASON.IO_ERROR );
			} catch (InterruptedException e) {
				Log.e( e, "Transfer interrupted", mFileInfo );
				mProgressor.failed( mFileInfo, TransferProgressor.REASON.CANCELLED );
			} catch (TimeoutException e) {
				Log.e( e, "Transfer timeout" );
				mProgressor.failed( mFileInfo, TransferProgressor.REASON.IO_ERROR );
			} finally {
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
			
			DirectOperation op = mConnection.receiveOperation( 3000 );
			byte opCode = op.getOpCode();
			if( opCode != DirectMessager.OPCODE_TYPE_READ_FILE ) {
				throw new UnsupportedOperationException(
						"Should be read file opcode. OpCode is " + opCode );
			}
			ReadFileOperation rfo = (ReadFileOperation)op;
			mFileInfo = rfo.getFileInfo();
			File file = new File( mFileInfo.mFilePathName );
			mProgressor.start(mFileInfo);
			FileInputStream fis = null;
			try {
				fis = new FileInputStream( file );
				long totalLen = 0;
				if( mFileInfo.mStartPosition > 0 ) {
					fis.skip(mFileInfo.mStartPosition);
					totalLen = mFileInfo.mStartPosition;
				}
				int len = 0;
				byte[] buffer = new byte[2048];
				while( ( len = fis.read( buffer  ) ) > 0 ) {
					mConnection.write( buffer, 0, len );
					Thread.sleep( 10 );
					totalLen += len;
					mProgressor.update( mFileInfo, totalLen, mFileInfo.mSize );
				}
				mProgressor.succeed( mFileInfo );
			} finally {
				if( fis != null ) {
					fis.close();
				}
			}
		}
	}
}
