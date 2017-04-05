package com.zsm.directTransfer.action.wifip2p;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;

import com.zsm.directTransfer.ui.StatusBarOperator;

public class WifiP2pPeerReceiverAsyTask
				extends FileTransferAsyncTask {

	private File mFile;

	public WifiP2pPeerReceiverAsyTask( Socket socket,
									   StatusBarOperator statusBarOperator ) {
		
		super(socket, statusBarOperator);
	}
	
	@Override
	protected Integer doInBackground(File ... files) {
		mFile = files[0];
        File dirs = new File(mFile.getParent());
        if (!dirs.exists())
            dirs.mkdirs();
        try {
			mFile.createNewFile();
	        InputStream inputstream = mSocket.getInputStream();
	        FileOutputStream outputStream = new FileOutputStream(mFile);
			copyFile(inputstream, outputStream);
			outputStream.close();
	        return SUCCEED;
		} catch (IOException e) {
			return FAILED;
		}
	}

	@Override
	protected void onPostExecute(Integer result) {
		if( result == SUCCEED ) {
			// TODO ResId
			mStatusBarOperator
				.setStatus( "Receive file: " + mFile.getName() + " successfully!",
							StatusBarOperator.STATUS_NORMAL );
		} else {
			// TODO ResId
			mStatusBarOperator
				.setStatus( "Receive file: " + mFile.getName() + " failed!",
							StatusBarOperator.STATUS_WARNING );
		}
	}

}
