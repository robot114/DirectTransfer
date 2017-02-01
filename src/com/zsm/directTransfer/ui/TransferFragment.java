package com.zsm.directTransfer.ui;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zsm.directTransfer.R;

public class TransferFragment extends Fragment {

	private Context mContext;

	public TransferFragment(Context context, StatusBarOperator statusOperator) {
		mContext = context;
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = inflater.inflate( R.layout.fragment_transfer, (ViewGroup)null );
		return view;
	}

}
