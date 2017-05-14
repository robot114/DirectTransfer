package com.zsm.directTransfer.ui;

import com.zsm.android.ui.Utility;
import com.zsm.directTransfer.R;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StatusBarFragment extends Fragment implements StatusBarOperator {

	private TextView mTextView;
	private View mLayout;
	private Context mContext;

	public StatusBarFragment(Context context) {
		mContext = context;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		
		View view = inflater.inflate( R.layout.fragment_statusbar, null );
		mTextView = (TextView)view.findViewById( R.id.textViewStatus );
		mLayout = view.findViewById( R.id.statusBarLayout );
		
		int height = Utility.getViewHeight(mLayout);
		mLayout.setMinimumHeight(height);
		clearStatus();
		
		return view;
	}

	@Override
	public void setStatus(final String text, final int status) {
		if( mTextView != null ) {
			new Handler( Looper.getMainLooper() ).post( new Runnable() {
				@Override
				public void run() {
					mTextView.setText( text );
					setAppearance(status);
				}
			} );
		}
	}

	@Override
	public void setStatus(int resId, int status) {
		setStatus( mContext.getString(resId), status );
	}

	@Override
	public void clearStatus() {
		setStatus("", STATUS_NORMAL);
	}

	@SuppressWarnings("deprecation")
	private void setAppearance( int status ) {
		switch( status ) {
			case STATUS_WARNING:
				mLayout.setBackground( 
						Utility.getDrawableFromAttr( 
							mContext, R.attr.colorStatusBarWarning ) );
				mTextView.setTextAppearance( mContext,
								R.style.StautsAppearance_Warning_Text );
				break;
			case STATUS_NORMAL:
			default:
				mLayout.setBackground( 
					Utility.getDrawableFromAttr( 
						mContext, R.attr.colorStatusBarNormal ) );
				mTextView.setTextAppearance( mContext,
								R.style.StautsAppearance_Normal_Text );
				break;
		}
	}

	@Override
	public void setNormalStatus(String text) {
		setStatus(text, STATUS_NORMAL);
	}

	@Override
	public void setNormalStatus(int resId) {
		setStatus(resId, STATUS_NORMAL);
	}

	@Override
	public void setErrorStatus(String text) {
		setStatus(text, STATUS_WARNING);
	}

	@Override
	public void setErrorStatus(int resId) {
		setStatus(resId, STATUS_WARNING);
	}
}
