package com.zsm.directTransfer.data;

import java.net.InetAddress;
import java.util.Random;

import com.zsm.directTransfer.transfer.TransferProgressor;
import com.zsm.directTransfer.transfer.operation.DirectFileOperation.FileTransferInfo;

public class FileTransferObject {
	
	private FileTransferInfo mFileInfo;
	private TransferProgressor mProgressor;
	
	public FileTransferObject( FileTransferInfo fi, TransferProgressor tp ) {
		
		mFileInfo = fi;
		mProgressor = tp;
	}
	
	public long getFileTransferId() {
		return mFileInfo.getId();
	}

	public FileTransferInfo getFileTransferInfo() {
		return mFileInfo;
	}

	public TransferProgressor getProgressor() {
		return mProgressor;
	}

	@Override
	public int hashCode() {
		return (int)(getFileTransferId() ^ (getFileTransferId() >>> 32));
	}

	@Override
	public String toString() {
		StringBuffer bu = new StringBuffer();
		bu.append( ", [ File: " ).append( mFileInfo )
		  .append( "]" );
		
		return bu.toString();
	}

	@Override
	public boolean equals(Object obj) {
		if( obj == null || !(obj instanceof FileTransferObject ) ) {
			return false;
		}
		return ((FileTransferObject)obj).getFileTransferId() == getFileTransferId();
	}
}
