package com.zsm.directTransfer.transfer.operation;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Random;

import com.zsm.directTransfer.connection.PeerMessageConnection;
import com.zsm.directTransfer.data.WifiP2pPeer;


public abstract class DirectOperation {
	
	private byte mOperationCode;
	private long mSerialNo;

	protected DirectOperation( byte opCode ) {
		mOperationCode = opCode;
		mSerialNo = new Random().nextLong();
	}
	
	public static long bytesToLong( byte[] data, int start ) {
		return bytesToNumber( 8, data, start );
	}
	
	public static short bytesToShort( byte[] data, int start ) {
		return (short) bytesToNumber( 2, data, start );
	}
	
	public static int bytesToInt(byte[] data, int start) {
		return (int) bytesToNumber(4, data, start);
	}
	
	public static long bytesToNumber( int len, byte[] data, int start ) {
		long value = 0;
		int end = start + len;
		for (int i = start; i < end; i++) {
		   value = (value << 8) + (data[i] & 0xff);
		}
	    return value;
	}
	
	abstract void addArgument(byte type, int dataLen, byte[] data, WifiP2pPeer peer )
						throws UnsupportedOperationException, BadPacketException;

	public abstract StatusOperation doOperation( PeerMessageConnection connection );

	abstract int calcTotalArgumentsLength() throws IOException;
	
	abstract void outputOperation(DataOutputStream out) throws IOException;

	public byte getOpCode() {
		return mOperationCode;
	}

	public void setSerialNo(long sn) {
		mSerialNo = sn;
	}

	public long getSerialNo() {
		return mSerialNo;
	}

}
