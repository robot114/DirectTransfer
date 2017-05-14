package com.zsm.directTransfer.ui;

import com.zsm.directTransfer.transfer.TransferProgressor;
import com.zsm.directTransfer.transfer.TransferTask;
import com.zsm.directTransfer.transfer.TransferProgressor.OPERATION;
import com.zsm.directTransfer.transfer.TransferTask.STATE;
import com.zsm.directTransfer.transfer.operation.DirectFileOperation.FileTransferInfo;

public class TransferController implements TransferProgressor {

	private static final int UPDATE_INTERVAL = 1000;
	private TransferFileView mFileView;
	private TransferProgressorView mProgressorView;
	private TransferTask mTransferTask;
	
	private FileTransferInfo mFileInfo;
	private OPERATION mOperation;
	
	private long mLastTime = 0;
	private long mStartTime;
	private long mCurrent = 0;

	TransferController(FileTransferInfo f, OPERATION operation ) {
		mFileInfo = f;
		mOperation = operation;
	}
	
	void setFileView( TransferFileView fv ) {
		mFileView = fv;
		updateFileView();
	}
	
	void setProgressView( TransferProgressorView tpv ) {
		mProgressorView = tpv;
		updateProgressView(mFileInfo, mCurrent, System.currentTimeMillis() );
	}
	
	@Override
	public void setTransferTask( TransferTask tt ) {
		mTransferTask = tt;
	}
	
	@Override
	public void start(FileTransferInfo fi) {
		mFileInfo = fi;
		mStartTime = System.currentTimeMillis();
		
		if( isVisible() ) {
			updateFileView();
		}
	}

	private void updateFileView() {
		if( mFileInfo != null ) {
			mFileView.setFileAndTarget( mFileInfo.mFileName,
										mFileInfo.getPeer().getUserDefinedName() );
		} else {
			mFileView.setFileAndTarget( "", "" );
		}
	}

	private boolean isVisible() {
		// When the visible items changed on a ListView, the data
		// presented by the view will changed too.
		// For the items invisible, should not have a view
		return mProgressorView != null && mProgressorView.getController() == this;
	}

	@Override
	public void update( FileTransferInfo fi, long current ) {
		mCurrent = current;
		long currentTime = System.currentTimeMillis();
		if( isVisible() && currentTime - mLastTime > UPDATE_INTERVAL ) {
			updateProgressView(fi, current, currentTime);
		}
	}

	private void updateProgressView(FileTransferInfo fi, long current, long currentTime) {
		mLastTime  = currentTime;
		long leftTimeMs = -1;
		if( current > 0 ) {
			leftTimeMs
				= (long) ((fi.getSize() - current )*( currentTime - mStartTime )
							/((float)current));
		}
		mProgressorView.setProgress(current, fi.getSize(), leftTimeMs );
	}

	@Override
	public void succeed(FileTransferInfo fi) {
		if( isVisible() ) {
			mProgressorView.setProgress( fi.getSize(), fi.getSize(), 0);
			mProgressorView.setState( STATE.FINISHED );
		}
	}

	@Override
	public void failed(FileTransferInfo fi, REASON reason) {
		if( isVisible() ) {
			if( reason == REASON.CANCELLED ) {
				mProgressorView.setState( STATE.CANCELLED );
			} else {
				mProgressorView.setState( STATE.FAILED );
			}
		}
	}

	@Override
	public long getTransferId() {
		return mFileInfo.getId();
	}
	
	@Override
	public void updateState(STATE state) {
		if( isVisible() ) {
			mProgressorView.setState(state);
		}
	}

	public void startPauseForView() {
		if( mTransferTask.getState() == STATE.PAUSED ) {
			mTransferTask.pauseByUi();
		} else {
			mTransferTask.resumeByUi();
		}
	}

	public void stopForView() {
		mTransferTask.cancelByUi();
	}

	@Override
	public void resumeTransferByPeer() {
		mTransferTask.resumeByPeer();
	}

	@Override
	public void pauseTransferByPeer() {
		mTransferTask.pauseByPeer();
	}

	@Override
	public void cancelTransferByPeer() {
		mTransferTask.cancelByPeer();
	}
}
