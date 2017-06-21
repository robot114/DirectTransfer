package com.zsm.directTransfer.transfer.operation;

import java.io.DataOutputStream;
import java.io.IOException;

import com.zsm.directTransfer.connection.PeerMessageConnection;
import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.directTransfer.transfer.TransferProgressor;
import com.zsm.directTransfer.transfer.TransferProgressorManager;
import com.zsm.log.Log;

public class FileTransferActionOperation extends DirectOperation {
	// +---------------------------------------------------------------------------------------------+
	// |OPCODE(3)|type(TYPE_TRANSFER_ACTION)|length(1)|VALUE_ACTION_XXX|TYPE_TRANSFER_ID|length(8)|ID|
	// +---------------------------------------------------------------------------------------------+
	
	private static final byte TYPE_TRANSFER_ACTION = 0;
	private static final short LEN_ACTION = 1;
	public static final byte VALUE_ACTION_CONTINUE = 0;
	public static final byte VALUE_ACTION_PAUSE = 1;
	public static final byte VALUE_ACTION_CANCEL = 2;
	
	private static final byte TYPE_TRANSFER_ID = 1;

	private byte mAction = VALUE_ACTION_CONTINUE;
	private long mTransferId;
	
	protected FileTransferActionOperation() {
		super(DirectMessager.OPCODE_TYPE_FILE_OPERATION);
	}
	
	public FileTransferActionOperation( long transferId, byte operation ) {
		this();
		mTransferId = transferId;
		mAction = operation;
	}

	@Override
	void addArgument(byte type, int dataLen, byte[] data, WifiP2pPeer peer )
			throws UnsupportedOperationException, BadPacketException {
		
		switch( type ) {
			case TYPE_TRANSFER_ACTION:
				mAction = data[0];
				checkOperation( mAction );
				break;
			case TYPE_TRANSFER_ID:
				mTransferId = bytesToLong( data, 0 );
				break;
			default:
				throw new UnsupportedOperationException(
							"Unsupport operation type: " + type );
		}
	}

	private void checkOperation(byte operation) {
		if( operation < VALUE_ACTION_CONTINUE
			|| operation > VALUE_ACTION_CANCEL ) {
			
			throw new UnsupportedOperationException( 
						"Unsupported file operation: " + operation );
		}
	}

	@Override
	public StatusOperation doOperation(PeerMessageConnection connection) {
		TransferProgressor tp
			= TransferProgressorManager.getInstance()
				.getByTransferId(mTransferId);
		
		Log.d( "Find transferProgressor by id. ", "TansferId", mTransferId, tp );
		if( tp == null ) {
			return new StatusOperation( 
							StatusOperation.VALUE_STATUS_NO_SUCH_FILE_OPERATION );
		}
		switch( mAction ) {
			case VALUE_ACTION_CONTINUE:
				tp.resumeTransferByPeer();
				break;
			case VALUE_ACTION_PAUSE:
				tp.pauseTransferByPeer();
				break;
			case VALUE_ACTION_CANCEL:
				tp.cancelTransferByPeer();
				break;
			default:
				throw new UnsupportedOperationException(
							"Unsupport file transfer operation: " + mAction );
		}
		return StatusOperation.STATUS_OK;
	}

	@Override
	int calcTotalArgumentsLength() throws IOException {
		return DirectMessager.ARG_TYPE_LENGTH  + DirectMessager.ARG_LENGTH_LENGTH
				+ LEN_ACTION + DirectMessager.ARG_TYPE_LENGTH
				+ DirectMessager.ARG_LENGTH_LENGTH + DirectMessager.LENGTH_SERIAL_NO;
	}

	@Override
	void outputOperation(DataOutputStream out) throws IOException {
		out.writeByte( TYPE_TRANSFER_ACTION );
		out.writeShort( LEN_ACTION );
		out.writeByte( mAction );
		out.writeByte( TYPE_TRANSFER_ID );
		out.writeShort( DirectMessager.LENGTH_SERIAL_NO );
		out.writeLong( mTransferId );
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append( "TransferId: " )
			   .append( mTransferId )
			   .append( ", Action: " );
		switch( mAction ) {
			case VALUE_ACTION_CONTINUE:
				builder.append( "Continue" );
				break;
			case VALUE_ACTION_PAUSE:
				builder.append( "Pause" );
				break;
			case VALUE_ACTION_CANCEL:
				builder.append( "Cancel" );
				break;
			default:
				builder.append( "Unknown" );
				break;
		}
		builder.append( "(" )
			   .append( mAction )
			   .append( ")" );
		
		return builder.toString();
	}
}
