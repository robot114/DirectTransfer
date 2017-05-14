package com.zsm.directTransfer.transfer;

import android.util.LongSparseArray;

import com.zsm.directTransfer.transfer.TransferProgressor.Factory;
import com.zsm.directTransfer.transfer.TransferProgressor.OPERATION;
import com.zsm.directTransfer.transfer.operation.DirectFileOperation.FileTransferInfo;
import com.zsm.log.Log;

public class TransferProgressorManager {

	static private TransferProgressorManager mInstance;
	
	private Factory mProgressorFacroty;
	private LongSparseArray<TransferProgressor> mProgressorArray;
	
	private TransferProgressorManager( TransferProgressor.Factory f ) {
		mProgressorFacroty = f;
		
		mProgressorArray = new LongSparseArray<TransferProgressor>();
	}
	
	static public void init( TransferProgressor.Factory f ) {
		if( mInstance != null ) {
			throw new IllegalStateException( "Instance has been initialized!" );
		}
		
		mInstance = new TransferProgressorManager( f );
	}
	
	static public TransferProgressorManager getInstance() {
		return mInstance;
	}
	
	public TransferProgressor newProgressor(FileTransferInfo fti,
											OPERATION operation) {
		TransferProgressor p = mProgressorFacroty.newProgressor(fti, operation);
		mProgressorArray.put(fti.getId(), p);
		
		Log.d( "New progressor created.", "FileTransferInfo", fti, "Progressor", p );
		return p;
	}

	public TransferProgressor getByTransferId(long transferId) {
		return mProgressorArray.get(transferId);
	}
	
	public TransferProgressor removeByTransferId(long transferId) {
		TransferProgressor p = mProgressorArray.get(transferId);
		if( p != null ) {
			mProgressorArray.remove( transferId );
		}
		
		Log.d( "New progressor removed.", "id", transferId, "Progressor", p );
		return p;
	}
	
	public void removeAll() {
		mProgressorArray.clear();
	}
}