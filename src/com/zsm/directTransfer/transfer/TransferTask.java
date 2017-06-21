package com.zsm.directTransfer.transfer;

import com.zsm.directTransfer.connection.DataConnection;
import com.zsm.directTransfer.transfer.operation.FileTransferInfo;
import com.zsm.directTransfer.transfer.operation.FileTransferActionOperation;

public abstract class TransferTask {
	
	public enum STATE { 
		/* These states transferred by the task itself */
		INIT, PREPARING, STARTED, FINISHED, FAILED,
		NORMAL, /* This state just presents the admin state, not the task's state */
		PAUSED, CANCELLED 	/* These two can only be set by admin */ }

	protected FileTransferInfo mFileTraqnsferInfo;
	protected TransferProgressor mProgressor;
	protected DataConnection mConnection;
	private STATE mTaskState = STATE.INIT;
	private STATE mAdminState = STATE.NORMAL;
	protected final Object mLock = new Object();
	
	protected TransferTask( ) {
	}
	
	protected TransferTask( FileTransferInfo fti ) {
		mFileTraqnsferInfo = fti;
	}
	
	protected void setState(STATE state) {
		synchronized( mLock ) {
			if( state.ordinal() <= STATE.FAILED.ordinal() ) {
				mTaskState = state;
			} else {
				mAdminState = state;
			}
			// Progressor's state should be updated by the REAL state of the task
			if( mProgressor != null ) {
				mProgressor.updateState( getState() );
			}
		}
	}

	public STATE getState() {
		synchronized( mLock ) {
			if( mAdminState == STATE.NORMAL ) {
				return mTaskState;
			} else {
				return mAdminState;
			}
		}
	}

	public void resumeByPeer() {
		setState( STATE.NORMAL );
	}

	public void pauseByPeer() {
		setState( STATE.PAUSED );
	}

	public void cancelByPeer() {
		setState( STATE.CANCELLED );
	}

	public void resumeByUi() {
		mConnection.notifyPeerOperation( 
				mFileTraqnsferInfo.getId(),
				FileTransferActionOperation.VALUE_ACTION_CONTINUE );
		setState( STATE.NORMAL );
	}

	public void pauseByUi() {
		mConnection.notifyPeerOperation(
				mFileTraqnsferInfo.getId(),
				FileTransferActionOperation.VALUE_ACTION_PAUSE );
		setState( STATE.PAUSED );
	}

	public void cancelByUi() {
		mConnection.notifyPeerOperation(
				mFileTraqnsferInfo.getId(),
				FileTransferActionOperation.VALUE_ACTION_CANCEL );
		setState( STATE.CANCELLED );
	}
	
}
