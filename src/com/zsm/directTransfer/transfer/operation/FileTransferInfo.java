package com.zsm.directTransfer.transfer.operation;

import java.io.File;
import java.util.Random;

import com.zsm.directTransfer.data.WifiP2pPeer;

public class FileTransferInfo {

	private static final Random ID_RANDOM = new Random(System.currentTimeMillis());
	
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

	public void setSize(long length) {
		mSize = length;
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
