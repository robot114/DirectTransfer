package com.zsm.directTransfer.transfer;

import com.zsm.directTransfer.transfer.operation.DirectFileOperation.FileInfo;

public interface TransferProgressor {
	
	interface Generator {

		TransferProgressor newProgressor();
		
	}
	
	public enum REASON {
		FILE_NOT_FOUND, CANNOT_CREATE_FILE, IO_ERROR, CANCELLED,
	};

	void start(FileInfo mFileInfo);

	void update(FileInfo fi, long current, long totalSize);

	void succeed(FileInfo fi);

	void failed(FileInfo fi, REASON reason);

}
