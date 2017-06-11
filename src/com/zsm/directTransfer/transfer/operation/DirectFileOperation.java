package com.zsm.directTransfer.transfer.operation;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.log.Log;

public abstract class DirectFileOperation extends DirectOperation {

	// For the write operation, there can be more than one file information.
	// For the read operation, only one file information can be included.
	// +------------------------------------------------------------------------------------------------------------------------------+
	// |OPCODE(1/2)|type(TYPE_ONE_FILE)|length(8+8+file_path_name_len)|transfer_id(8bytes)file_len/start_pos(8bytes)file_path_name|...|
	// +------------------------------------------------------------------------------------------------------------------------------+
	// +-----------------------------------------------------------------------------------------------------------------------------------+
	// |OPCODE(1/2)|type(TYPE_FILE_LIST)|length(VAR)|xml defined by http://www.wi-fi.org/specifications/wifidirectservices/filetransfer.xsd|
	// +-----------------------------------------------------------------------------------------------------------------------------------+
	protected static final byte TYPE_FILE_LIST = 0;
	protected static final byte TYPE_ONE_FILE = 1;
	static final byte LENGTH_FILE_SIZE = 8;
	static final byte LENGTH_TRANSFER_ID_SIZE = 8;

	private static final Random ID_RANDOM = new Random(System.currentTimeMillis());
	
	protected List<FileTransferInfo> mFileList;
	protected boolean mOutputOneByOne;
	
	/**
	 * Constructor for output operation to the peer.
	 * 
	 * @param outputOneByOne true, output the file list as TYPE_ONE_FILE format;
	 * 				false, as TYPE_FILE_LIST format
	 */
	protected DirectFileOperation( byte opCode, boolean outputOneByOne ) {
		super( opCode );
		if( !outputOneByOne ) {
			throw new UnsupportedOperationException(
					"output One By One is " + outputOneByOne );
		}
		mOutputOneByOne = outputOneByOne;
		mFileList = new ArrayList<FileTransferInfo>();
	}
	
	protected FileTransferInfo parseFileInfo(byte[] data, int dataLen,
											 WifiP2pPeer peer) {
		if( dataLen <= LENGTH_FILE_SIZE ) {
			Log.w( "Invalid file size", dataLen);
			return null;
		}
		
		String fileName = null;
		try {
			fileName
				= new String(data, LENGTH_FILE_SIZE + LENGTH_TRANSFER_ID_SIZE, 
							 dataLen - LENGTH_FILE_SIZE - LENGTH_TRANSFER_ID_SIZE,
							 DirectMessager.READABLE_ENCODE );
		} catch (UnsupportedEncodingException e) {
			Log.e( e, "Failed to get file name" );
			return null;
		}
		long transferId = bytesToLong(data, 0);
		long size = bytesToLong(data, LENGTH_TRANSFER_ID_SIZE);
		FileTransferInfo fileInfo
			= new FileTransferInfo( transferId, fileName, size, 0, peer );
		return fileInfo;
	}

	@Override
	protected void outputOperation(DataOutputStream out) throws IOException {
		if( mOutputOneByOne ) {
			for( FileTransferInfo fi : mFileList ) {
				byte[] fileNameBytes
					= fi.getFilePathName().getBytes( DirectMessager.READABLE_ENCODE );
				out.writeByte( TYPE_ONE_FILE );
				
				int len
					= LENGTH_TRANSFER_ID_SIZE + LENGTH_FILE_SIZE
						+ fileNameBytes.length;
				
				out.writeShort( (short)len );
				out.writeLong( fi.getId() );
				out.writeLong( fi.getSize() );
				out.write( fileNameBytes );
			}
		} else {
			throw new UnsupportedOperationException( 
						"Only TYPE_ONE_FILE supported" );
		}
		
	}

	@Override
	protected int calcTotalArgumentsLength() throws IOException {
		int len = 0;
		
		if( mOutputOneByOne ) {
			for( FileTransferInfo fi : mFileList ) {
				len += DirectMessager.ARG_TYPE_LENGTH 
						+ DirectMessager.ARG_LENGTH_LENGTH;
				len += LENGTH_TRANSFER_ID_SIZE + LENGTH_FILE_SIZE;
				byte[] fileNameBytes
					= fi.getFilePathName().getBytes( DirectMessager.READABLE_ENCODE );
				len += fileNameBytes.length;
			}
		} else {
			throw new UnsupportedOperationException(
						"Only TYPE_ONE_FILE supported" );
		}
		
		return len;
	}

	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer();
		buff.append( getClass().getName() ).append( ": " );
		for( FileTransferInfo fi : mFileList ) {
			buff.append( "    " )
				.append( fi.toString() )
				.append( "\r\n" );
		}
		return buff.toString();
	}

	@Override
	protected void addArgument(byte type, int dataLen, byte[] data, WifiP2pPeer peer)
			throws BadPacketException, UnsupportedOperationException {
				
		Log.d( "Argument to be added", "type", type, "len", dataLen );
		switch( type ) {
			case TYPE_FILE_LIST:
				break;
			case TYPE_ONE_FILE:
				FileTransferInfo fileInfo = parseFileInfo(data, dataLen, peer);
				if( fileInfo != null ) {
					mFileList.add( fileInfo );
				}
				break;
			default:
				// Ignore the unsupported type for later compatibility
				Log.w( "Unsupported type", type );
				break;
		}
	}

	public List<FileTransferInfo> getFileTransferInfoList() {
		return mFileList;
	}

	public class FileTransferInfo {

		private long mId;
		
		private String mFilePathName;
		public String mFileName;
		private long mSize;
		private long mStartPosition;

		private WifiP2pPeer mPeer;
			
		/**
		 * Constructed from the request packet. This constructor should be only
		 * invoked by {@link DirectFileOperation.parseFileInfo}.
		 * 
		 * @param id id of the transfer session
		 * @param name file name
		 * @param size file size
		 * @param start the position from where to transfer
		 * @param peer peer of the transfer
		 */
		protected FileTransferInfo( long id, String name, long size, long start,
								 WifiP2pPeer peer ) {
			mId = id;
			
			mFilePathName = name;
			mFileName = getFileName();
			mSize = size;
			mStartPosition = start;
			mPeer = peer;
		}
		
		/**
		 * Construct with a file and generated an id for the transfer. This should
		 * only be invoked where a file is selected to be transfered
		 * 
		 * @param file file to transfer
		 * @param peer peer of the transfer
		 */
		protected FileTransferInfo( File file, WifiP2pPeer peer ) {
			synchronized( ID_RANDOM ) {
				mId = ID_RANDOM.nextLong();
			}
			
			mFilePathName = file.getPath();
			mFileName = file.getName();
			mSize = file.length();
			mStartPosition = 0;
			mPeer = peer;
		}
		
		@Override
		public String toString() {
			StringBuffer buff = new StringBuffer();
			buff.append( mFilePathName )
				.append( ", gtransferId: " )
				.append( mId )
				.append( ", size: " )
				.append( mSize )
				.append( ", startFrom: " )
				.append( mStartPosition )
				.append( ", peer: ")
				.append( mPeer );
			
			return buff.toString();
		}
		
		private String getFileName() {
			int index = mFilePathName.lastIndexOf( File.separatorChar );
			return index >= 0 ? mFilePathName.substring(index) : mFilePathName;
		}

		public long getId() {
			return mId;
		}

		public String getFilePathName() {
			return mFilePathName;
		}

		public long getSize() {
			return mSize;
		}

		public long getStartPosition() {
			return mStartPosition;
		}

		public void setStartPosition(long start) {
			mStartPosition = start;
		}

		public WifiP2pPeer getPeer() {
			return mPeer;
		}
	}
}