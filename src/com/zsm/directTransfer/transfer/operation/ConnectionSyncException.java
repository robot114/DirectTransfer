package com.zsm.directTransfer.transfer.operation;

/**
 * Exception for the connection is not synchronized correctly. For example
 * the magic cookie does not match
 *  
 * @author zsm
 *
 */
public class ConnectionSyncException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5083538594526745802L;

	public ConnectionSyncException(String message) {
		super(message);
	}

}
