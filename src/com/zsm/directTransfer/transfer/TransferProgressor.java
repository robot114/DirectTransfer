package com.zsm.directTransfer.transfer;

import com.zsm.directTransfer.transfer.TransferTask.STATE;
import com.zsm.directTransfer.transfer.operation.FileTransferInfo;

public interface TransferProgressor {
	
	public interface Factory {
		TransferProgressor newProgressor( FileTransferInfo fti,
										  OPERATION operation );
	}
	
	public enum REASON {
		FILE_NOT_FOUND, CANNOT_CREATE_FILE, IO_ERROR, CANCELLED,
	}

	public enum OPERATION { READ, WRITE };
	
	void start(FileTransferInfo fi);

	void update( FileTransferInfo fi, long current );
	
	void succeed(FileTransferInfo fi);

	void failed(FileTransferInfo fi, REASON reason);
	
	long getTransferId();

	void updateState(STATE state);

	void setTransferTask(TransferTask task);

	void resumeTransferByPeer();
	
	void pauseTransferByPeer();

	void cancelTransferByPeer();
}
