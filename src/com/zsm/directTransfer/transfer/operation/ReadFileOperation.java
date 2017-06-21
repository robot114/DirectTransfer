package com.zsm.directTransfer.transfer.operation;

import com.zsm.directTransfer.connection.PeerMessageConnection;
import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.directTransfer.transfer.operation.FileTransferInfo;

public class ReadFileOperation extends DirectFileOperation {

	public ReadFileOperation() {
		this( true );
	}
	
	/**
	 * Constructor for output operation to the peer.
	 * 
	 * @param fi file information to output
	 * @param outputOneByOne true, output the file list as TYPE_ONE_FILE format;
	 * 				false, as TYPE_FILE_LIST format
	 */
	public ReadFileOperation( FileTransferInfo fi, boolean outputOneByOne ) {
		this(outputOneByOne);
		
		mFileList.add( fi );
	}
	
	public ReadFileOperation( FileTransferInfo fi ) {
		this(fi, true);
	}
	
	private ReadFileOperation(boolean outputOneByOne) {
		super(DirectMessager.OPCODE_TYPE_READ_FILE, outputOneByOne);
	}

	@Override
	protected long getOutputSizeOrStart( FileTransferInfo fi ) {
		// Reader only knows where to start, does not know how large is the file
		return fi.getStartPosition();
	}

	@Override
	public void addArgument(byte type, int dataLen, byte[] data, WifiP2pPeer peer )
					throws UnsupportedOperationException, BadPacketException {
		if( mFileList.size() >= 1 ) {
			throw new BadPacketException( 
						"Only one file is allowed in a read operation" );
		}
		super.addArgument(type, dataLen, data, peer);
	}

	@Override
	protected FileTransferInfo buildFileTransferInfo( long transferId,
									String fileName, long sizeOrStart,
									WifiP2pPeer peer) {
		// Reader only knows where to start, does not know how large is the file
		return new FileTransferInfo( transferId, fileName, 0, sizeOrStart, peer );
	}

	@Override
	public StatusOperation doOperation( PeerMessageConnection connection ) {
		// TODO Auto-generated method stub
		return null;
	}

	public FileTransferInfo getFileInfo() {
		return mFileList.get(0);
	}

}
