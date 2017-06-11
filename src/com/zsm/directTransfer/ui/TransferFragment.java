package com.zsm.directTransfer.ui;

import java.util.List;

import android.app.Fragment;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.LongSparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.zsm.directTransfer.R;
import com.zsm.directTransfer.data.FileTransferObject;
import com.zsm.directTransfer.transfer.TransferProgressor;
import com.zsm.directTransfer.transfer.TransferProgressor.OPERATION;
import com.zsm.directTransfer.transfer.TransferProgressorManager;
import com.zsm.directTransfer.transfer.operation.DirectFileOperation.FileTransferInfo;
import com.zsm.log.Log;

public class TransferFragment extends Fragment implements TransferProgressor.Factory {

	private Context mContext;
	private StatusBarOperator mStatusOperator;
	
	private LongSparseArray<FileTransferObject> mFileTransferObjectList;
	
	private ExpandableListView mProgressList;
	private TransferProgressorListAdapter mProgressListAdapter;

	public TransferFragment(Context context, StatusBarOperator statusOperator,
							WifiP2pManager manager, Channel channel) {
		mContext = context;
		mStatusOperator = statusOperator;
		mProgressListAdapter = new TransferProgressorListAdapter( mContext );
		mFileTransferObjectList = new LongSparseArray<FileTransferObject>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		
		View view = inflater.inflate( R.layout.fragment_transfer, (ViewGroup)null );
		mProgressList = (ExpandableListView)view.findViewById( R.id.progressList );
		
		mProgressList.setAdapter(mProgressListAdapter);
		return view;
	}

	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public void addTransferOperation( final List<FileTransferInfo> fis ) {
		for( FileTransferInfo fi : fis ) {
			// Send a file write operation, so the progressor should be writing one
			final TransferProgressor p
				= TransferProgressorManager.getInstance()
					.newProgressor( fi, OPERATION.WRITE );
			
			FileTransferObject fto = new FileTransferObject( fi, p );
			
			mFileTransferObjectList.put( fto.getFileTransferId(), fto );
		}
	}

	public FileTransferObject getFileTransferObject( long id ) {
		return mFileTransferObjectList.get(id);
	}

	@Override
	public TransferProgressor newProgressor(final FileTransferInfo fti,
											final OPERATION operation) {
		
		final TransferController p = new TransferController( fti, operation );
		new Handler(Looper.getMainLooper()).post( new Runnable() {
			@Override
			public void run() {
				mProgressListAdapter.add(p);
				Log.d( "Transfer controller added: ", p, "FileTransferInfo", fti );
				mProgressList.expandGroup( mProgressListAdapter.getGroupCount() - 1 );
			}
		});
		return p;
	}

}
