package com.zsm.directTransfer.transfer.operation;

import java.io.File;

public class WriteFileOperation extends DirectFileOperation{

	/**
	 * Default cConstructor for new instance from the message connection
	 */
	public WriteFileOperation() {
		this( true );
	}
	
	/**
	 * Constructor for output operation to the peer.
	 * 
	 * @param files array of the files to output
	 * @param outputOneByOne true, output the file list as TYPE_ONE_FILE format;
	 * 				false, as TYPE_FILE_LIST format
	 */
	public WriteFileOperation( File[] files, boolean outputOneByOne ) {
		
		this( outputOneByOne );
		
		for( File file : files ) {
			mFileList.add( new FileInfo( file ) );
		}
	}
	
	/**
	 * Constructor for output operation to the peer.
	 * 
	 * @param file file to output
	 * @param outputOneByOne true, output the file list as TYPE_ONE_FILE format;
	 * 				false, as TYPE_FILE_LIST format
	 */
	WriteFileOperation( File file, boolean outputOneByOne ) {
		
		this(outputOneByOne);
		
		mFileList.add( new FileInfo(file) );
	}
	
	private WriteFileOperation( boolean outputOneByOne ) {
		super( DirectMessager.OPCODE_TYPE_WRITE_FILE, outputOneByOne );
	}
	
	@Override
	public StatusOperation doOperation() {
		// TODO Auto-generated method stub
		return StatusOperation.STATUS_OK;
	}
	
}
