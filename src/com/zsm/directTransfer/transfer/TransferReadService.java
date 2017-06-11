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
import java.util.concurrent.Semaphore;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.DocumentsContract;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.zsm.directTransfer.connection.DataConnectionManager;
import com.zsm.directTransfer.data.FileTransferObject;
import com.zsm.directTransfer.preferences.Preferences;
import com.zsm.directTransfer.transfer.TransferProgressor.REASON;
import com.zsm.directTransfer.transfer.operation.DirectFileOperation.FileTransferInfo;
import com.zsm.log.Log;

public class TransferReadService implements Runnable, AutoCloseable {

	private static final String[] DOCUMENT_COLUMNS
		= new String[]{ DocumentsContract.Document.COLUMN_DISPLAY_NAME,
						DocumentsContract.Document.COLUMN_DOCUMENT_ID,
						DocumentsContract.Document.COLUMN_SIZE };
	
	private static TransferReadService mInstance;
	
	private final BlockingQueue<FileTransferObject> mQueue;
	private final ExecutorService mPool;
	private boolean mClosed;

	private Activity mActivity;

	private TransferReadService( Activity ui ) {
		mActivity = ui;
		mQueue = new LinkedBlockingQueue<FileTransferObject>();
		mPool = Executors.newFixedThreadPool(
					Preferences.getInstance().getMaxTransferThreadNum() );
		
		mClosed = false;
	}
	
	public static void start(Activity ui) {
		if( mInstance != null ) {
			throw new IllegalStateException( 
						"TransferReadService has been started!" );
		}
		mInstance = new TransferReadService( ui );
		new Thread( mInstance, "ReadService" ).start();
	}
	
	public static TransferReadService getInstance() {
		return mInstance;
	}
	
	public void add( FileTransferInfo fti, TransferProgressor p )
						throws InterruptedException {
		
		p.start(fti);
		FileTransferObject fto = new FileTransferObject(fti, p);
		mQueue.put( fto );
		Log.d( "FileTransferInfo put in the queue: ", fti );
	}

	@Override
	public void run() {
		doReadTransfer();
	}

	private void doReadTransfer() {
		while( !mClosed ) {
			try {
				FileTransferObject tf = mQueue.take();
				Log.d( "FileTransferInfo taken from the queue, and to be executed: ",
						tf.getFileTransferInfo() );
				
				mPool.execute( new ReadTask( tf.getFileTransferInfo(),
											 tf.getProgressor() ) );
				Thread.sleep( 300 );
			} catch (InterruptedException e) {
				Log.e( e, "Read service is interrupted" );
				close();
			}
		}
	}

	@Override
	public void close() {
		Log.d( "Read service is to be closed." );
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

		private Semaphore mGrantSemaphore = new Semaphore( 0 );
		private boolean mGranted = false;
		
		private OutputStream mOutputStream;
		
		private ReadTask( FileTransferInfo fti, TransferProgressor p ) {
			super( fti );
			
			mProgressor = p;
			mProgressor.setTransferTask( this );
		}
		
		@Override
		public void run() {
			PermissionsManager.getInstance().requestPermissionsIfNecessaryForResult( 
				mActivity,
				new String[]{ Manifest.permission.WRITE_EXTERNAL_STORAGE },
				new PermissionsResultAction() {
					@Override
					public void onGranted() {
						// This method will be invoked in the main thread.
						// So only the result is recorded, and the resume the read task
						mGranted = true;
						mGrantSemaphore.release();
					}

					@Override
					public void onDenied(String permission) {
						// TODO: resid
						Toast.makeText(mActivity,
									  "Permission denied for writing to file",
									  Toast.LENGTH_SHORT)
							 .show();
						mGranted = false;
						mGrantSemaphore.release();
					}
					
				});
			
			if( mGranted ) {
				try {
					mGrantSemaphore.acquire();
					readForGrant();
				} catch (InterruptedException e) {
					Log.e( e, "GrantSemaphore is interrupted!" );
				}
			}
		}

		private void readForGrant() {
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
			byte[] buffer = new byte[4096];
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
				if( getState() == STATE.CANCELLED ) {
					throw new InterruptedException( "Stopped by user!" );
				}
			}
		}

		private OutputStream openOutputStream() throws IOException {
			if( Preferences.getInstance().isStorageAsscessFramework() ) {
				return openOutputStreamSAF();
			} else {
				return openOutputStreamFileSystem();
			}
		}

		private OutputStream openOutputStreamSAF() throws FileNotFoundException {
			Uri fileUri = getLocalFileUri();
			ContentResolver cr = mActivity.getContentResolver();
			Cursor c = cr.query(fileUri, DOCUMENT_COLUMNS, null, null, null);
			OutputStream os;
			if( c != null && c.moveToFirst() ) {
				boolean append = Preferences.getInstance().isAppend();
				long size
					= c.getLong( c.getColumnIndex( 
									DocumentsContract.Document.COLUMN_SIZE ) );
				
				mFileTraqnsferInfo.setStartPosition( size );
				String mode = append ? "wa" : "w";
				os = cr.openOutputStream(fileUri, mode );
				
				Log.d( "File exists and get its OutputStream.",
					   "uri", fileUri, "append", append, "size", size );
			} else {
				Uri writeTreeUri = Preferences.getInstance().getWriteUri();
				Uri dirUri
					= DocumentsContract.buildDocumentUriUsingTree(
							writeTreeUri,
							DocumentsContract.getTreeDocumentId(writeTreeUri));
				
				Uri newFileUri
					= DocumentsContract.createDocument(
						cr, dirUri,
						"application/octet-stream", getFileName() );
				if( newFileUri == null ) {
					throw new FileNotFoundException( "Create the file failed: " + fileUri );
				}
				os = cr.openOutputStream(newFileUri );
				Log.d( "New file created", newFileUri );
			}
			return os;
		}

		private Uri getLocalFileUri() {
			String fn = getFileName();
			Uri treeUri = Preferences.getInstance().getWriteUri();
			return DocumentsContract.buildDocumentUriUsingTree(treeUri, fn);
		}

		private OutputStream openOutputStreamFileSystem() throws FileNotFoundException {
			
			File file = new File( getLocalFileName() );
			
			boolean append
				= Preferences.getInstance().isAppend()
					&& file.exists() && file.length() <= mFileTraqnsferInfo.getSize();
			
			if( append ) {
				mFileTraqnsferInfo.setStartPosition( file.length() );
			}
			
			Log.d( "File to store: ", file );
			return new FileOutputStream( file, append );
		}

		private String getLocalFileName() {
			String fn = getFileName();
			return Preferences.getInstance().getWritePath() + File.separator + fn;
		}

		private String getFileName() {
			int separatorIndex
				= mFileTraqnsferInfo.getFilePathName().lastIndexOf( File.separator );
			String fn = mFileTraqnsferInfo.getFilePathName();
			if( separatorIndex > 0 ) {
				fn = fn.substring( separatorIndex + 1 );
			}
			return fn;
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
			
			mGrantSemaphore.release();
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
