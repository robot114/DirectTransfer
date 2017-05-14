package com.zsm.directTransfer.transfer.operation;

import java.io.File;

import com.zsm.directTransfer.connection.PeerMessageConnection;
import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.directTransfer.transfer.TransferProgressor;
import com.zsm.directTransfer.transfer.TransferProgressorManager;
import com.zsm.directTransfer.transfer.TransferReadService;
import com.zsm.log.Log;

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
	 * @param peer peer to which the operation will be sent
	 * @param outputOneByOne true, output the file list as TYPE_ONE_FILE format;
	 * 				false, as TYPE_FILE_LIST format
	 */
	public WriteFileOperation( File[] files, WifiP2pPeer peer,
							   boolean outputOneByOne ) {
		
		this( outputOneByOne );
		
		for( File f : files ) {
			addFile( f, peer );
		}
	}
	
	/**
	 * Constructor for output operation to the peer.
	 * 
	 * @param file file to output
	 * @param peer peer to which the operation will be sent
	 * @param outputOneByOne true, output the file list as TYPE_ONE_FILE format;
	 * 				false, as TYPE_FILE_LIST format
	 */
	WriteFileOperation( File file, WifiP2pPeer peer, boolean outputOneByOne ) {
		
		this(outputOneByOne);
		
		addFile( file, peer );
	}
	
	private void addFile(File file, WifiP2pPeer peer) {
		mFileList.add( new FileTransferInfo(file, peer) );
	}

	private WriteFileOperation( boolean outputOneByOne ) {
		super( DirectMessager.OPCODE_TYPE_WRITE_FILE, outputOneByOne );
	}
	
	@Override
	public StatusOperation doOperation( PeerMessageConnection connection ) {
		for( FileTransferInfo fi : mFileList ) {
			try {
				TransferProgressor progressor
					= TransferProgressorManager.getInstance()
							// Received a write operation means I need to start a
							// READ task
							.newProgressor( fi, TransferProgressor.OPERATION.READ );
				TransferReadService.getInstance().add( fi, progressor );
			} catch (InterruptedException e) {
				Log.e( e, "Add new file to read service is interrupted!",
					   fi.getFilePathName() );
				// TODO prompt
			}
		}
		return StatusOperation.STATUS_OK;
	}
	
}
