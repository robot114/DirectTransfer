package com.zsm.directTransfer.transfer.operation;

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
	public ReadFileOperation( FileInfo fi, boolean outputOneByOne ) {
		this(outputOneByOne);
		
		mFileList.add( fi );
	}
	
	public ReadFileOperation( FileInfo fi ) {
		this(fi, true);
	}
	
	private ReadFileOperation(boolean outputOneByOne) {
		super(DirectMessager.OPCODE_TYPE_READ_FILE, outputOneByOne);
	}

	@Override
	public void addArgument(byte type, int dataLen, byte[] data)
					throws UnsupportedOperationException, BadPacketException {
		if( mFileList.size() >= 1 ) {
			throw new BadPacketException( 
						"Only one file is allowed in a read operation" );
		}
		super.addArgument(type, dataLen, data);
	}

	@Override
	public StatusOperation doOperation() {
		// TODO Auto-generated method stub
		return null;
	}

	public FileInfo getFileInfo() {
		return mFileList.get(0);
	}

}
