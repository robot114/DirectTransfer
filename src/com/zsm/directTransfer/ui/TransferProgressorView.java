package com.zsm.directTransfer.ui;

import java.util.Locale;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zsm.directTransfer.R;
import com.zsm.directTransfer.R.drawable;
import com.zsm.directTransfer.R.string;
import com.zsm.directTransfer.transfer.TransferTask.STATE;
import com.zsm.log.Log;
import com.zsm.util.TextUtil;

public class TransferProgressorView extends LinearLayout {

	private ProgressBar mProgressorView;
	private ImageView mStartPauseView;
	private ImageView mStopView;
	private TextView mProgressTextView;
	private TextView mTimeView;
	private Context mContext;
	private TransferController mController;

	public TransferProgressorView(Context context) {
		super(context);
		
		mContext = context;
		init();
	}

	private void init() {
		String infService = Context.LAYOUT_INFLATER_SERVICE;
		LayoutInflater li
			= (LayoutInflater)getContext().getSystemService( infService );
		li.inflate( R.layout.item_transfer_progressor, this, true );
		mProgressorView = (ProgressBar)findViewById( R.id.progressBar );
		mStartPauseView = (ImageView)findViewById( R.id.imageViewStartPause );
		mStopView = (ImageView)findViewById( R.id.imageViewCancel );
		mProgressTextView = (TextView)findViewById( R.id.textViewProgress );
		mTimeView = (TextView)findViewById( R.id.textViewTime );
		
		mStartPauseView.setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				mController.startPauseForView();
			}
		} );
		
		mStopView.setOnClickListener( new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				mController.stopForView();
			}
		} );
	}

	void setProgress( final long current, final long total, long leftTimeMs ) {
		int bits = 0;
		String u = "KB";
		float factor = 1024.0f;
		if( total > 10*1024*1024 ) {
			bits = 10;
			u = "MB";
			factor = 1024*1024.0f;
		}
		final String progress
			= String.format( (Locale)null, "%1$.2f%2$S/%3$.2f%2$S", current/factor, u,
							 total/factor );
		final StringBuilder builder = new StringBuilder();
		builder.append( mContext.getString( string.transferProgressLeftTime ) );
		if( leftTimeMs >= 0 ) {
			TextUtil.appendDurationText( builder, leftTimeMs*1000 );
		} else {
			builder.append( '~' );
		}
		
		updateProgressorInMainLooper(current, total, bits, progress, builder);
	}

	private void updateProgressorInMainLooper(final long current,
											  final long total, final int bits,
											  final String progress,
											  final StringBuilder builder) {
		
		new Handler( Looper.getMainLooper() ).post( new Runnable () {
			@Override
			public void run() {
				mProgressorView.setMax( (int) (total >> bits ) );
				mProgressorView.setProgress( (int) (current >> bits) );
				mProgressTextView.setText(progress);
				mTimeView.setText( builder.toString() );
			}
		});
	}
	
	void setState( final STATE state ) {
		new Handler( Looper.getMainLooper() ).post( new Runnable () {
			@Override
			public void run() {
				updateByState(state);
			}
		});
	}

	private void updateByState(STATE state) {
		switch( state ) {
			case PAUSED:
				mStartPauseView.setImageResource( drawable.start );
				break;
			case STARTED:
				mStartPauseView.setImageResource( drawable.pause );
				break;
			case CANCELLED:
				mProgressTextView.setText( string.promptTransferCancelled );
				showButtons(View.INVISIBLE);
				break;
			case FAILED:
				mProgressTextView.setText( string.promptTransferFailed );
				showButtons(View.INVISIBLE);
				break;
			case FINISHED:
				showButtons(View.INVISIBLE);
				break;
			default:
				Log.d( "Unsupported state", state );
				break;
		}
	}

	private void showButtons(int v) {
		mStartPauseView.setVisibility( v );
		mStopView.setVisibility( v );
		mTimeView.setVisibility( v );
	}
	
	void setController( TransferController tc ) {
		mController = tc;
	}

	public TransferController getController() {
		return mController;
	}
}
