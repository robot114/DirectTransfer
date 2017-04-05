package com.zsm.directTransfer.action.wifip2p;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.zsm.directTransfer.ui.StatusBarOperator;

public class WifiP2pPeerUploadAsyTask extends FileTransferAsyncTask {

	private File mFile;

	public WifiP2pPeerUploadAsyTask(Socket socket,
									StatusBarOperator statusBarOperator) {
		super(socket, statusBarOperator);
	}

	@Override
	protected Integer doInBackground(File... files) {
		mFile = files[0];
        try {
	        InputStream inputStream = new FileInputStream( mFile );
	        OutputStream outputStream = mSocket.getOutputStream();
			copyFile(inputStream, outputStream);
			outputStream.close();
			inputStream.close();
	        return SUCCEED;
		} catch (IOException e) {
			return FAILED;
		}
	}

}
