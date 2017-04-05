package com.zsm.directTransfer.transfer.operation;

import java.io.Closeable;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeoutException;

import com.zsm.log.Log;

public class DirectMessager implements Closeable {

	private static final int LENGTH_BUFFER = 2048;
	
	// The packet is with TLV format. Type of the value is defined here with prefix
	// "TYPE_", whose length in the packet is 1 byte.
	// Length of the data's value is 2 bytes, which is from 0 to 65535. The length 
	// is stored as big-endian format.
	static final int ARG_LENGTH_LENGTH = 2;
	static final int ARG_TYPE_LENGTH = 1;
	
	// The packet starts with 
	// +--------------------------------------------------------------------------------+
	// |Magic Cookie|TOTAL OPCODE length|Ver(1)|Serial No|OPCODE|Arguments  DATA ... 	|
	// +--------------------------------------------------------------------------------+
	private static final byte[] MAGIC_COOKIE = new byte[]{
		(byte) 0xAA, 'D', 'i', 'r', 'e', 'c', 't', 'T', 'r', 'a', 'n', 's', 'f', 'e', 'r',
		0x33
	};
	static final int LENGTH_MAGIC_COOKIE = MAGIC_COOKIE.length;
	
	// The length does not include itself and the parts before it
	static final int LENGTH_TOTAL_REQUEST_LENGTH = 2;

	static final int LENGTH_VER = 1;
	static final byte VALUE_VER = 0x10;
	
	static final int LENGTH_SERIAL_NO = 2;
	
	static final int LENGTH_OPCODE = 1;
	
	// Add new opcode here
	static final byte OPCODE_TYPE_STATUS = 0;
	static final byte OPCODE_TYPE_WRITE_FILE = 1;
	public static final byte OPCODE_TYPE_READ_FILE = 2;
	
	static final Class<?>[] OPCODE_ARRAY 
		= { StatusOperation.class, WriteFileOperation.class, 
			ReadFileOperation.class };
	
	private Thread mInputThread;

	private boolean mClosed;

	private InputStream mInputStream;
	private OutputStream mOutputStream;

	private byte[] mInputBuffer;

	static final String READABLE_ENCODE = "UTF-8";
	
	public DirectMessager(InputStream in, OutputStream out ) {
		mInputStream = in;
		mOutputStream = out;
		mInputBuffer = new byte[LENGTH_BUFFER];
	}

	public void start() {
		mClosed = false;
		mInputThread.start();
	}
	
	@Override
	public void close() throws IOException {
		Log.d( "DirectMessager is to be closed" );
		mClosed = true;
		
		if( mInputStream != null ) {
			mInputStream.close();
			mInputStream = null;
		}
		
		if( mOutputStream != null ) {
			mOutputStream.close();
			mOutputStream = null;
		}
	}

	public boolean isClosed() {
		return mClosed;
	}

	public DirectOperation receiveOperation(boolean blockToWait, int timeoutInMs)
					throws IOException, InterruptedException, 
						   ConnectionSyncException, BadPacketException, 
						   UnsupportedOperationException, TimeoutException {

		long timeoutSysTime = timeoutInMs > 0 ? System.currentTimeMillis()
				+ timeoutInMs : -1;

		int totalLen = -1;
		int len = readOneItem(MAGIC_COOKIE.length, mInputBuffer, blockToWait,
				timeoutSysTime);
		if (len < 0) {
			return null;
		}
		checkMagicCookie(mInputBuffer);

		len = readOneItem(LENGTH_TOTAL_REQUEST_LENGTH, mInputBuffer, true,
				timeoutSysTime);
		if (len < 0) {
			return null;
		}
		totalLen = (mInputBuffer[0] << 8) + mInputBuffer[1];
		if (totalLen < 0) {
			throw new ConnectionSyncException("Invalid message total length: "
					+ totalLen);
		}

		len = readOneItem(LENGTH_VER, mInputBuffer, true, timeoutSysTime);
		if (len < 0) {
			mInputStream.skip(totalLen);
			return null;
		}
		totalLen -= len;

		len = readOneItem(LENGTH_OPCODE, mInputBuffer, true, timeoutSysTime);
		if (len < 0) {
			mInputStream.skip(totalLen);
			return null;
		}
		totalLen -= len;

		byte opCode = mInputBuffer[0];
		DirectOperation op = null;
		try {
			op = (DirectOperation) OPCODE_ARRAY[opCode].newInstance();
			Log.d("Operation received, opcode: ", opCode);
		} catch (InstantiationException | IllegalAccessException e) {
			Log.w(e, "Cannot new instance operation", "opCode", opCode);
			mInputStream.skip(totalLen);
			return null;
		} catch (ArrayIndexOutOfBoundsException e) {
			mInputStream.skip(totalLen);
			throw new UnsupportedOperationException("Unsupported operation: "
					+ opCode);
		}

		while (totalLen > 0) {
			len = readOneItem(1, mInputBuffer, true, timeoutSysTime);
			if (len < 0) {
				break;
			}
			totalLen -= len;
			byte argType = mInputBuffer[0];

			len = readOneItem(ARG_LENGTH_LENGTH, mInputBuffer, true,
					timeoutSysTime);

			int argLen = (mInputBuffer[0] << 8) + mInputBuffer[1];
			totalLen -= len;

			if (argLen > totalLen) {
				mInputStream.skip(totalLen);
				BadPacketException e = new BadPacketException(
						"Argument length longer than the buffer left!");
				Log.e(e);
				throw e;
			}
			if (argLen > mInputBuffer.length) {
				mInputBuffer = new byte[argLen];
			}

			len = readOneItem(argLen, mInputBuffer, true, timeoutSysTime);
			totalLen -= len;

			op.addArgument(argType, argLen, mInputBuffer);
		}

		Log.d("Detail of the operation received", op);

		return op;
	}

	private int readOneItem(int lenToRead, byte[] buffer,
							boolean blockBeforeRead, long timeoutSysTime)
					throws IOException, InterruptedException, TimeoutException {

		int readSize = 0;
		while (lenToRead > 0) {
			if (timeoutSysTime > 0
					&& System.currentTimeMillis() > timeoutSysTime) {

				throw new TimeoutException("Time out when read from socket");
			}
			int readLength
					= java.lang.Math.min(mInputStream.available(), lenToRead);

			int len = mInputStream.read(buffer, readSize, readLength);
			if (len <= 0) {
				// All the bytes must be read once the first one read
				if (readSize == 0 && !blockBeforeRead) {
					return -1;
				} else {
					Thread.sleep(100);
					continue;
				}
			}
			lenToRead -= len;
			readSize += len;
		}

		return readSize;
	}

	private void checkMagicCookie(byte[] buffer) throws ConnectionSyncException {
		for (int i = 0; i < LENGTH_MAGIC_COOKIE; i++) {
			if (buffer[i] != MAGIC_COOKIE[i]) {
				throw new ConnectionSyncException("Invalid magic cookie!");
			}
		}

	}

	private void sendOperation( DirectOperation op, OutputStream os )
							throws IOException {
		
		DataOutputStream dos = new DataOutputStream(os);

		dos.write(MAGIC_COOKIE);
		
		// The length itself is not included
		int totalLen = LENGTH_VER + LENGTH_OPCODE + op.calcTotalArgumentsLength();
		dos.write( (byte)(totalLen>>8) );
		dos.write( (byte)totalLen );
		dos.write( VALUE_VER );
		op.outputOperation( dos );
		
		dos.flush();
		
		os.flush();
	}

	public void sendOperation( DirectOperation op ) throws IOException {
		sendOperation(op, mOutputStream );
	}
}
