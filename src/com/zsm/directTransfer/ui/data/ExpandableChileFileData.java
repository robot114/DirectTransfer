package com.zsm.directTransfer.ui.data;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;

import android.content.Context;

import com.zsm.android.ui.ExpandableTextListChildGroup;
import com.zsm.directTransfer.R;

public class ExpandableChileFileData implements ExpandableTextListChildGroup {

	private final static int LABEL_RES_ID[] = new int[]{
		R.string.labelDirectory, R.string.labelLastModified, R.string.labelSize
	};
	private static final int CHILD_NUM = LABEL_RES_ID.length;
	private static DateFormat DATE_FORMAT;
	private static DateFormat TIME_FORMAT;
	
	private File mFile;
	
	public ExpandableChileFileData( File f ) {
		mFile = f;
	}
	
	@Override
	public String getShowString(Context context) {
		return mFile.getName();
	}

	@Override
	public int getGroupStyleResId() {
		return 0;
	}

	@Override
	public int getChildrenCount() {
		return CHILD_NUM;
	}

	@Override
	public String getChildShowLabel(Context context, int childPosition) {
		if( childPosition < CHILD_NUM ) {
			return context.getString( LABEL_RES_ID[childPosition] );
		}
		return null;
	}

	@Override
	public String getChildShowString(Context context, int childPosition) {
		String text;
		switch( LABEL_RES_ID[childPosition] ) {
			case R.string.labelDirectory:
				text = mFile.getAbsoluteFile().getPath();
				break;
			case R.string.labelLastModified:
				if( DATE_FORMAT == null ) {
					DATE_FORMAT = android.text.format.DateFormat.getDateFormat(context);
					TIME_FORMAT = android.text.format.DateFormat.getTimeFormat(context);
				}
				Date d = new Date( mFile.lastModified() );
				text = DATE_FORMAT.format(d) + " " + TIME_FORMAT.format(d);
				break;
			case R.string.labelSize:
				text = String.format( "%1$,d bytes", mFile.length() );
				break;
			default:
				throw new IllegalArgumentException( "Invalid resource id( "
								+ LABEL_RES_ID[childPosition] + " ) for child "
								+ childPosition );
		}
		
		return text;
	}

	@Override
	public int getChildLabelStyleResId() {
		return 0;
	}

	@Override
	public int getChildTextStyleResId() {
		return 0;
	}

	@Override
	public int hashCode() {
		return mFile.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if( obj == null || !( obj instanceof ExpandableChileFileData ) ) {
			return false;
		}
		
		ExpandableChileFileData ins = (ExpandableChileFileData)obj;
		return mFile.equals( ins.mFile );
	}

	@Override
	public String toString() {
		return mFile.getAbsolutePath();
	}

	public File getFile() {
		return mFile;
	}
}
