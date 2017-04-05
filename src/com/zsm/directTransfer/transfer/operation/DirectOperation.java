package com.zsm.directTransfer.transfer.operation;

import java.io.DataOutputStream;
import java.io.IOException;


public abstract class DirectOperation {
	
	private byte mOperationCode;

	protected DirectOperation( byte opCode ) {
		mOperationCode = opCode;
	}
	
	long bytesToLong( int len, byte[] data ) {
		long value = 0;
		for (int i = 0; i < len; i++) {
		   value = (value << 8) + (data[i] & 0xff);
		}
	    return value;
	}
	
	abstract void addArgument(byte type, int dataLen, byte[] data)
						throws UnsupportedOperationException, BadPacketException;

	public abstract StatusOperation doOperation();

	abstract int calcTotalArgumentsLength() throws IOException;
	
	abstract void outputOperation(DataOutputStream out) throws IOException;

	public byte getOpCode() {
		return mOperationCode;
	}

}
