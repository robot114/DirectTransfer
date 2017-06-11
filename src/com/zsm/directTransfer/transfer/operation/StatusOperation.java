package com.zsm.directTransfer.transfer.operation;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import com.zsm.directTransfer.connection.PeerMessageConnection;
import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.log.Log;

public class StatusOperation extends DirectOperation {

	// +----------------------------------------------------+
	// |OPCODE(0)|type(TYPE_STATUS)|length(1)|value|...		|
	// +----------------------------------------------------+
	static final byte TYPE_STATUS = 0;
	static final short LENGTH_STATUS = 1;
	static final byte VALUE_STATUS_OK = 0;
	static final byte VALUE_STATUS_NO_RESPONSE = 1;
	static final byte VALUE_STATUS_NOT_SUPPORTED = 2;
	static final byte VLAUE_STATUS_NO_FILE = 10;
	static final byte VALUE_STATUS_NO_SUCH_FILE_OPERATION = 11;
	
	static final byte TYPE_REASON = 1;
	
	static final StatusOperation STATUS_OK
		= new StatusOperation( VALUE_STATUS_OK );
	public static final StatusOperation STATUS_NO_RESPONSE
		= new StatusOperation( VALUE_STATUS_NO_RESPONSE, "No response" );
	
	private byte mStatus;
	private String mReason;
	
	StatusOperation( ) {
		super( DirectMessager.OPCODE_TYPE_STATUS );
		mStatus = VALUE_STATUS_OK;
	}
	
	StatusOperation( byte status ) {
		this();
		mStatus = status;
	}
	
	StatusOperation( byte status, String reason ) {
		this( status );
		mReason = reason;
	}
	
	public byte getStatus() {
		return mStatus;
	}

	public String getReason() {
		return mReason;
	}

	@Override
	void addArgument(byte type, int dataLen, byte[] data, WifiP2pPeer peer)
				throws BadPacketException {
		
		switch( type ) {
			case TYPE_STATUS:
				if( dataLen != 1 ) {
					throw new BadPacketException( 
							"Data length of status MUST be 1. Data len is "
							+ dataLen );
				}
				mStatus = data[0];
				Log.d( "Status is ", mStatus );
				break;
			case TYPE_REASON:
				try {
					mReason
						= new String( data, 0, dataLen,
									  DirectMessager.READABLE_ENCODE );
				} catch (UnsupportedEncodingException e) {
					Log.w( e, "Unsupported encodeing",
							DirectMessager.READABLE_ENCODE );
				}
				Log.d( "Reason is ", mReason );
				break;
			default:
				Log.d( "Unsupported arg type", type );
		}
	}

	@Override
	public StatusOperation doOperation( PeerMessageConnection connection ) {
		// Should not be invoked
		throw new IllegalStateException( 
					"doOperation should not be invoked for StatusOperation!" );
	}

	@Override
	void outputOperation(DataOutputStream out) throws IOException {
		out.writeByte( TYPE_STATUS );
		out.writeShort( LENGTH_STATUS );
		out.writeByte( mStatus );
		
		if( mReason != null ) {
			out.writeByte( TYPE_REASON );
			byte[] b = mReason.getBytes( DirectMessager.READABLE_ENCODE );
			out.writeShort( b.length );
			out.write( b );
		}
	}

	@Override
	int calcTotalArgumentsLength() throws UnsupportedEncodingException {
		int len
			= DirectMessager.ARG_TYPE_LENGTH  + DirectMessager.ARG_LENGTH_LENGTH
			  + LENGTH_STATUS;
		
		if( mReason != null ) {
			len += DirectMessager.ARG_TYPE_LENGTH 
					+ DirectMessager.ARG_LENGTH_LENGTH;
			byte[] b = mReason.getBytes( DirectMessager.READABLE_ENCODE );
			len += b.length;
		}
		return len;
	}

	public boolean ok() {
		return mStatus == VALUE_STATUS_OK;
	}

	@Override
	public String toString() {
		StringBuffer buff = new StringBuffer( );
		buff.append( "Status: " ).append( mStatus );
		if( mReason != null ) {
			buff.append( ", Reason: " ).append( mReason );
		} else {
			buff.append( ", No reason" );
		}
		return buff.toString();
	}

}
