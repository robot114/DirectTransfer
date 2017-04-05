package com.zsm.directTransfer.ui;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import android.app.Fragment;
import android.content.Context;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zsm.directTransfer.R;
import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.directTransfer.transfer.operation.DirectMessager;

public class TransferFragment extends Fragment {

	enum OPERATION { WRITE, READ }
	
	final private class TransferItem {
		private WifiP2pPeer mPeer;
		private File mFile;
		private OPERATION mOperation;

		TransferItem( WifiP2pPeer peer, File file, OPERATION op ) {
			mPeer = peer;
			mFile = file;
			mOperation = op;
		}

		@Override
		public int hashCode() {
			return mFile.hashCode() ^ mPeer.hashCode() ^ mOperation.ordinal();
		}

		@Override
		public boolean equals(Object obj) {
			if( this == obj ) {
				return true;
			}
			
			if( obj == null || !( obj instanceof TransferItem ) ) {
				return false;
			}
			TransferItem item = (TransferItem)obj;
			
			return mOperation == item.mOperation && mPeer.equals( item.mPeer )
					&& mFile.equals( item.mFile );
		}
	}
	
	private Context mContext;
	private StatusBarOperator mStatusOperator;
	private WifiP2pManager mManager;
	private Channel mChannel;
	
	private Hashtable<WifiP2pPeer, DirectMessager> mMessagers;
	
	private List<TransferItem> mTransferItemList;

	public TransferFragment(Context context, StatusBarOperator statusOperator,
							WifiP2pManager manager, Channel channel) {
		mContext = context;
		mStatusOperator = statusOperator;
		mManager = manager;
		mChannel = channel;
		
		mTransferItemList = new LinkedList<TransferItem>( );
		mMessagers = new Hashtable<WifiP2pPeer, DirectMessager>();
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
							 Bundle savedInstanceState) {
		
		View view = inflater.inflate( R.layout.fragment_transfer, (ViewGroup)null );
		return view;
	}

	
	@Override
	public void onDestroy() {
		super.onDestroy();
	}

	public void addUploadEntry(File[] source, WifiP2pPeer peer) {
		addTransferItemToList( mTransferItemList, source, peer, OPERATION.WRITE );
	}

	synchronized private void addTransferItemToList( List<TransferItem> list,
													 File[] files,
													 WifiP2pPeer peer,
													 OPERATION op ) {
		
		for( File f : files ) {
			list.add( new TransferItem( peer, f, op ) );
		}
	}

	public void queueTransfer(final InetAddress groupOwnerAddress) {
		new Thread( "Thread-QueueTransfer" ) {
			@Override
			public void run() {
				while( mTransferItemList.isEmpty() ) {
					try {
						sleep(1);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
//				transfer(groupOwnerAddress);
			}
		}.start();
		
	}

//	private void transfer(InetAddress groupOwnerAddress) {
//		Socket socket = new Socket();
//		byte buf[]  = new byte[1024];
//		try {
//		    /**
//		     * Create a client socket with the host,
//		     * port, and timeout information.
//		     */
//		    socket.bind(null);
//		    InetSocketAddress serverAddress
//		    	= new InetSocketAddress(groupOwnerAddress, MainActivity.TRANSFER_PORT);
//			socket.connect(serverAddress, 500);
//
//		    OutputStream outputStream = socket.getOutputStream();
//		    InputStream inputStream = new FileInputStream( mSource[0] );
//		    int len;
//		    while ((len = inputStream.read(buf)) != -1) {
//		        outputStream.write(buf, 0, len);
//		    }
//		    outputStream.close();
//		    inputStream.close();
//		    // TODO: ResId
//		    mStatusOperator
//		    	.setStatus( "Finished to transfer file " + mSource[0].getAbsoluteFile().getPath(),
//	    				StatusBarOperator.STATUS_NORMAL );;
//		    
//		} catch (FileNotFoundException e) {
//			// TODO: Res id
//		    mStatusOperator
//		    	.setStatus( "File " + mSource[0].getAbsoluteFile().getPath() + " not found!",
//		    				StatusBarOperator.STATUS_WARNING );;
//		} catch (IOException e) {
//			// TODO: Res id
//		    mStatusOperator
//		    	.setStatus( "Transfer file " + mSource[0].getAbsoluteFile().getPath() + " failed!"
//		    				+ " The reason is " + e.getMessage(),
//		    				StatusBarOperator.STATUS_WARNING );;
//		}
//
//		/**
//		 * Clean up any open sockets when done
//		 * transferring or if an exception occurred.
//		 */
//		finally {
//		    if (socket != null) {
//		        if (socket.isConnected()) {
//		            try {
//		                socket.close();
//		            } catch (IOException e) {
//		                //catch logic
//		            }
//		        }
//		    }
//		}
//	}
}
