package com.zsm.directTransfer.ui;

public interface StatusBarOperator {

	public static final int STATUS_NORMAL = 1;
	public static final int STATUS_WARNING = 2;
	
	void setStatus( String text, int status );
	
	void setStatus( int resId, int status );
	
	void clearStatus();
}
