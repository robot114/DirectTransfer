package com.zsm.directTransfer.transfer.operation;

/**
 * Exception for the format of the packet is wrong. For example, unsupported type, 
 * the length of value is incorrect.
 * 
 * @author zsm
 *
 */
public class BadPacketException extends Exception {

	public BadPacketException(String cause) {
		super( cause );
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 1498755954396970274L;

}
