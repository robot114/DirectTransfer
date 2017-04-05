package com.zsm.directTransfer.action.wifip2p;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.zsm.directTransfer.ui.StatusBarOperator;

import android.os.AsyncTask;

public abstract class FileTransferAsyncTask extends
		AsyncTask<File, Integer, Integer> {

	protected static final int FAILED = 1;
	protected static final int SUCCEED = 0;
	protected Socket mSocket;
	protected StatusBarOperator mStatusBarOperator;

	public FileTransferAsyncTask(Socket socket, StatusBarOperator statusBarOperator) {
		super();
		
		mSocket = socket;
		mStatusBarOperator = statusBarOperator;
	}

	protected void copyFile(InputStream inputstream, OutputStream outputStream) throws IOException {
		
		byte[] buf = new byte[1024];
	    int len;
		while ((len = inputstream.read(buf)) != -1) {
	        outputStream.write(buf, 0, len);
	    }
	
	}

}