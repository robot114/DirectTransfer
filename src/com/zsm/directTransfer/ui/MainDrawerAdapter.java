package com.zsm.directTransfer.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.zsm.directTransfer.R;

public class MainDrawerAdapter extends BaseAdapter {
	
	public static final int TRANSFER_ITEM_POSITION = 0;
	public static final int PEER_ITEM_POSITION = 1;
	
	public static final int END_OF_POSITION = 2;
	
	private final static int[] ITEM_LAYOUT_ID = {
		R.layout.drawer_simple_item,	// TRANSFER_ITEM_POSITION
		R.layout.drawer_simple_item		// NEW_PEER_ITEM_POSITION
	};
	
	private final static int[] ITEM_DRAWABLE_ID = {
		R.drawable.transfer,
		R.drawable.discover
	};
	
	private final static int[] ITEM_TEXT_ID = {
		R.string.titleTransfer,
		R.string.titleDiscover
	};
	
	private View[] mItemViews;

	MainDrawerAdapter( Context context ) {
		mItemViews = new View[ITEM_LAYOUT_ID.length];
		
		LayoutInflater li
			= (LayoutInflater)context
				.getSystemService( Context.LAYOUT_INFLATER_SERVICE );
		for( int i = 0; i < ITEM_LAYOUT_ID.length; i++ ) {
			mItemViews[i] = li.inflate( ITEM_LAYOUT_ID[i], null);
			if( ITEM_DRAWABLE_ID[i] > 0 ) {
				TextView tv = (TextView)mItemViews[i];
				tv.setText( ITEM_TEXT_ID[i] );
				tv.setCompoundDrawablesWithIntrinsicBounds( ITEM_DRAWABLE_ID[i], 0, 0, 0);
			}
		}
	}
	
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		return ITEM_LAYOUT_ID.length;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		return "Add peer";
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		return mItemViews[position];
	}

}
