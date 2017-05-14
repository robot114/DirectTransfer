package com.zsm.directTransfer.transfer;

import com.zsm.directTransfer.connection.DataConnection;
import com.zsm.directTransfer.transfer.operation.DirectFileOperation.FileTransferInfo;
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
	private STATE mTaskState;
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
		mProgressor.updateState( getState() );
	}

	public void pauseByPeer() {
		setState( STATE.PAUSED );
		mProgressor.updateState( getState() );
	}

	public void cancelByPeer() {
		setState( STATE.CANCELLED );
		mProgressor.updateState( getState() );
	}

	public void resumeByUi() {
		mConnection.notifyPeerOperation( 
			FileTransferActionOperation.VALUE_ACTION_CONTINUE );
		setState( STATE.NORMAL );
	}

	public void pauseByUi() {
		mConnection.notifyPeerOperation(
			FileTransferActionOperation.VALUE_ACTION_PAUSE );
		setState( STATE.PAUSED );
	}

	public void cancelByUi() {
		mConnection.notifyPeerOperation(
			FileTransferActionOperation.VALUE_ACTION_CANCEL );
		setState( STATE.CANCELLED );
	}
	
}
