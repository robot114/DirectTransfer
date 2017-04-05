package com.zsm.directTransfer.transfer.operation;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import com.zsm.log.Log;

public abstract class DirectFileOperation extends DirectOperation {

	protected static final byte TYPE_FILE_LIST = 0;
	protected static final byte TYPE_ONE_FILE = 1;
	static final byte LENGTH_FILE_SIZE = 8;

	public static class FileInfo {
			
		FileInfo( String name, long size, long start ) {
			mFilePathName = name;
			mSize = size;
			mStartPosition = start;
		}
		
		FileInfo( File file ) {
			mFilePathName = file.getPath();
			mSize = file.length();
			mStartPosition = 0;
		}
		
		@Override
		public String toString() {
			StringBuffer buff = new StringBuffer();
			buff.append( mFilePathName )
				.append( ", size: " )
				.append( mSize )
				.append( ", startFrom: " )
				.append( mStartPosition );
			
			return buff.toString();
		}

		public String mFilePathName;
		public long mSize;
		public long mStartPosition;
	}

	protected List<FileInfo> mFileList;
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
		mFileList = new ArrayList<FileInfo>();
	}
	
	protected FileInfo parseFileInfo(byte[] data, int dataLen) {
		if( dataLen <= LENGTH_FILE_SIZE ) {
			Log.w( "Invalid file size", dataLen);
			return null;
		}
		
		String fileName = null;
		try {
			fileName
				= new String(data, LENGTH_FILE_SIZE, 
							 dataLen - LENGTH_FILE_SIZE,
							 DirectMessager.READABLE_ENCODE );
		} catch (UnsupportedEncodingException e) {
			Log.e( e, "Failed to get file name" );
			return null;
		}
		
		long size = bytesToLong(LENGTH_FILE_SIZE, data);
		FileInfo fileInfo = new FileInfo( fileName, size, 0 );
		return fileInfo;
	}

	@Override
	protected void outputOperation(DataOutputStream out) throws IOException {
		out.write( DirectMessager.OPCODE_TYPE_WRITE_FILE );
		
		if( mOutputOneByOne ) {
			for( FileInfo fi : mFileList ) {
				byte[] fileNameBytes
					= fi.mFilePathName.getBytes( DirectMessager.READABLE_ENCODE );
				out.write( TYPE_ONE_FILE );
				
				int len = LENGTH_FILE_SIZE + fileNameBytes.length;
				out.writeShort( (short)len );
				out.writeLong( fi.mSize );
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
			for( FileInfo fi : mFileList ) {
				len += DirectMessager.ARG_TYPE_LENGTH 
						+ DirectMessager.ARG_LENGTH_LENGTH;
				len += LENGTH_FILE_SIZE;
				byte[] fileNameBytes
					= fi.mFilePathName.getBytes( DirectMessager.READABLE_ENCODE );
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
		for( FileInfo fi : mFileList ) {
			buff.append( "    " )
				.append( fi.toString() )
				.append( "\r\n" );
		}
		return buff.toString();
	}

	@Override
	protected void addArgument(byte type, int dataLen, byte[] data)
			throws BadPacketException, UnsupportedOperationException {
				
				Log.d( "Argument to be added", "type", type, "len", dataLen );
				switch( type ) {
					case TYPE_FILE_LIST:
						break;
					case TYPE_ONE_FILE:
						FileInfo fileInfo = parseFileInfo(data, dataLen);
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

}