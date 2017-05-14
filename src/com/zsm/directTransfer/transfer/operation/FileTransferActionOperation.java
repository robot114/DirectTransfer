package com.zsm.directTransfer.transfer.operation;

import java.io.DataOutputStream;
import java.io.IOException;

import com.zsm.directTransfer.connection.DataConnection;
import com.zsm.directTransfer.connection.DataConnectionManager;
import com.zsm.directTransfer.connection.PeerMessageConnection;
import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.directTransfer.transfer.TransferProgressor;
import com.zsm.directTransfer.transfer.TransferProgressorManager;

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

	private byte mOperation = VALUE_ACTION_CONTINUE;
	private long mTransferId;
	
	protected FileTransferActionOperation() {
		super(DirectMessager.OPCODE_TYPE_FILE_OPERATION);
	}
	
	public FileTransferActionOperation( byte operation ) {
		this();
		mOperation = operation;
	}

	@Override
	void addArgument(byte type, int dataLen, byte[] data, WifiP2pPeer peer )
			throws UnsupportedOperationException, BadPacketException {
		
		switch( type ) {
			case TYPE_TRANSFER_ACTION:
				mOperation = data[0];
				checkOperation( mOperation );
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
		if( tp == null ) {
			return new StatusOperation( 
							StatusOperation.VALUE_STATUS_NO_SUCH_FILE_OPERATION );
		}
		switch( mOperation ) {
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
							"Unsupport file transfer operation: " + mOperation );
		}
		return StatusOperation.STATUS_OK;
	}

	@Override
	int calcTotalArgumentsLength() throws IOException {
		return DirectMessager.LENGTH_OPCODE + 1 + LEN_ACTION
			   + DirectMessager.LENGTH_OPCODE + 1 + DirectMessager.LENGTH_SERIAL_NO;
	}

	@Override
	void outputOperation(DataOutputStream out) throws IOException {
		out.write( TYPE_TRANSFER_ACTION );
		out.write( LEN_ACTION );
		out.write( mOperation );
		out.write( TYPE_TRANSFER_ID );
		out.write( DirectMessager.LENGTH_SERIAL_NO );
		out.writeLong( mTransferId );
	}
}
