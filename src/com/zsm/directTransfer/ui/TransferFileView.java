package com.zsm.directTransfer.ui;

import com.zsm.directTransfer.R;
import com.zsm.directTransfer.R.id;
import com.zsm.directTransfer.transfer.TransferProgressor.OPERATION;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;

public class TransferFileView extends LinearLayout {

	private TextView mFileView;
	private TextView mTargetView;

	public TransferFileView(Context context) {
		super(context);
		init();
	}

	private void init() {
		String infService = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li
			= (LayoutInflater)getContext().getSystemService( infService );
		li.inflate( R.layout.item_transfer_one, this, true );
		
		mFileView = (TextView)findViewById( id.TextViewFile );
		mTargetView = (TextView)findViewById( id.textViewTarget );
	}
	
	void setFileAndTarget( final String file, final String target,
						   final OPERATION operation ) {
		
		new Handler( Looper.getMainLooper() ).post( new Runnable () {
			@Override
			public void run() {
				mFileView.setText(file);
				int targetId
					= operation == OPERATION.READ 
						? R.string.labelFrom : R.string.labelTo;
				mTargetView.setText(getContext().getText(targetId) + target);
			}
		});
	}
}
