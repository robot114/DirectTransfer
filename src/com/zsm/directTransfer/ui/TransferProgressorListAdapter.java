package com.zsm.directTransfer.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

import com.zsm.android.ui.CheckableExpandableListAdapter;

public class TransferProgressorListAdapter extends
					CheckableExpandableListAdapter<TransferController> {

	TransferProgressorListAdapter(Context context) {
		super(context, false);
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return 1;
	}

	@Override
	public TransferController getChild(int groupPosition, int childPosition) {
		return getGroup( groupPosition );
	}

	@Override
	public long getGroupId(int groupPosition) {
		return getGroup( groupPosition ).getTransferId();
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return getGroup( groupPosition ).getTransferId();
	}

	@Override
	public boolean hasStableIds() {
		return true;
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
							 boolean isLastChild, View convertView,
							 ViewGroup parent) {
		
		TransferProgressorView tpv;
		if( convertView == null ) {
			tpv = new TransferProgressorView(mContext);
		} else {
			tpv = (TransferProgressorView)convertView;
		}
		
		TransferController controller = getGroup( groupPosition );
		controller.setProgressView(tpv);
		tpv.setController( controller );
		
		return tpv;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

	@Override
	protected View getGroupContentView() {
		return new TransferFileView(mContext);
	}

	@Override
	protected void updateGroupView(View view, int groupPosition) {
		TransferController controller = getGroup(groupPosition);
		controller.setFileView((TransferFileView) view);
	}

}
