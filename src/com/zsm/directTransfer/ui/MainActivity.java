package com.zsm.directTransfer.ui;

import java.io.File;
import java.io.IOException;
import java.util.Observable;
import java.util.Observer;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.ActionListener;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.zsm.directTransfer.R;
import com.zsm.directTransfer.connection.DataConnectionManager;
import com.zsm.directTransfer.connection.MessageConnectionManager;
import com.zsm.directTransfer.connection.PeerConnectionManager;
import com.zsm.directTransfer.connection.PeerMessageConnection;
import com.zsm.directTransfer.connection.PeerMessageConnection.MessageConnectionListener;
import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.directTransfer.transfer.TransferProgressorManager;
import com.zsm.directTransfer.transfer.TransferReadService;
import com.zsm.directTransfer.transfer.TransferWriteService;
import com.zsm.directTransfer.transfer.operation.DirectFileOperation;
import com.zsm.directTransfer.transfer.operation.WriteFileOperation;
import com.zsm.directTransfer.ui.FileFragment.UploadOperator;
import com.zsm.directTransfer.wifip2p.WifiP2pGroupManager;
import com.zsm.log.Log;

public class MainActivity extends Activity implements
		NavigationDrawerFragment.NavigationDrawerCallbacks, UploadOperator,
		MessageConnectionListener {

	final static int FRAGMENT_FILE_POSITION = MainDrawerAdapter.END_OF_POSITION;
	
	static final private int[] TITLE_RES_ID
		= new int[]{ R.string.titleTransfer, R.string.titleDiscover,
					 R.string.titleFile };
	/**
	 * Fragment managing the behaviors, interactions and presentation of the
	 * navigation drawer.
	 */
	private NavigationDrawerFragment mNavigationDrawerFragment;

	private StatusBarFragment mStatusBarFragment;
	
	/**
	 * Used to store the last screen title. For use in
	 * {@link #restoreActionBar()}.
	 */
	private CharSequence mTitle;
	
	private Observer mPeerSelectionObserver;

	private Fragment[] mFragment = new Fragment[FRAGMENT_FILE_POSITION+1];
	private WifiP2pManager mManager;
	private Channel mChannel;

	private BroadcastReceiver mMyselfWifiP2pReceiver;
	private WifiP2pDevice mMyselfDevice;

	private Thread mWaitPeerConnectionThread;

	private AlertDialog mWaitingForPeerConnectionDialog;

	
	public MainActivity() {
		super();
	}

	private void intiFragments() {
		mStatusBarFragment = new StatusBarFragment( this );
		
		mFragment[MainDrawerAdapter.TRANSFER_ITEM_POSITION]
				= new TransferFragment( this, mStatusBarFragment,
										mManager, mChannel );
		
		PeerFragment pf
			= new PeerFragment( this, mStatusBarFragment, mManager, mChannel );
		mPeerSelectionObserver = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				WifiP2pPeer peer = (WifiP2pPeer)arg;
				onPeerSelected(peer);
			}
		};
		mFragment[MainDrawerAdapter.PEER_ITEM_POSITION] = pf;
		pf.registerPeerSelectionObserver(mPeerSelectionObserver);
		
		FileFragment ff = new FileFragment(this, this, mStatusBarFragment);
		mFragment[FRAGMENT_FILE_POSITION] = ff;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	    mManager
	    	= (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
	    mChannel = mManager.initialize(this, getMainLooper(), null);
    
		intiFragments();
		TransferProgressorManager.init( getTransferFragment() );
		
		setContentView(R.layout.main);
		
		initConnectionManagers();
		initTransferService();
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager
				.beginTransaction()
				.replace(R.id.statusBarContainer, mStatusBarFragment )
				.commit();
		
		mNavigationDrawerFragment
			= (NavigationDrawerFragment) getFragmentManager()
				.findFragmentById(R.id.navigation_drawer);
		
		mTitle = getTitle();

		// Set up the drawer.
		mNavigationDrawerFragment.setUp(R.id.navigation_drawer,
				(DrawerLayout) findViewById(R.id.drawer_layout));

		mMyselfWifiP2pReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				mMyselfDevice
					= intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
				WifiP2pGroupManager.getInstance().updateMyselfDevice(mMyselfDevice);
				unregisterReceiver(mMyselfWifiP2pReceiver);
				mMyselfWifiP2pReceiver = null;
			}
		};
		IntentFilter myselfFilter
			= new IntentFilter( WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION  );
		registerReceiver( mMyselfWifiP2pReceiver, myselfFilter);
	}
	
	private void initConnectionManagers() {
		
		PeerConnectionManager.initInstance(mManager, this);
		// For the group owner, the server socket must be start,
		// as the GO does not know the address of the client.
		// This is a limitation of Android. For the wifi p2p
		// specification, the GO should allocate the address for
		// the client.
		// After the client announce itself, any member in the group can
		// connect to it. So event the client need to listen the message
		// port
		MessageConnectionManager.getInstance()
			.initInstance( this, this, mStatusBarFragment );
		
		try {
			MessageConnectionManager.getInstance().startMessageServer();
			WifiP2pGroupManager.getInstance().start();
			DataConnectionManager.getInstance().startListening();
		} catch ( IOException e ) {
			Toast.makeText( this,
							R.string.promptFailToStartListening,
							Toast.LENGTH_LONG )
				 .show();
			
			finish();
		}
	}
	
	private void initTransferService() {
		TransferReadService.start();
		TransferWriteService.start();
	}

	@Override
	public void onNavigationDrawerItemSelected(int position) {
		toFragment(position);
	}

	private void toFragment(int position) {
		mStatusBarFragment.clearStatus( );
		// update the main content by replacing fragments
		Fragment newFragment = mFragment[0];
		if( position >= 0 && position <= FRAGMENT_FILE_POSITION ) {
			newFragment = mFragment[position];
		}
		FragmentManager fragmentManager = getFragmentManager();
		fragmentManager
				.beginTransaction()
				.replace(R.id.container, newFragment )
				.commit();
		
		mTitle = getString(TITLE_RES_ID[position]);
	}

	public void restoreActionBar() {
		ActionBar actionBar = getActionBar();
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		actionBar.setDisplayShowTitleEnabled(true);
		actionBar.setTitle(mTitle);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		if (!mNavigationDrawerFragment.isDrawerOpen()) {
			// Only show items in the action bar relevant to this screen
			// if the drawer is not showing. Otherwise, let the drawer
			// decide what to show in the action bar.
			getMenuInflater().inflate(R.menu.main, menu);
			restoreActionBar();
			return true;
		}
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		if( mMyselfWifiP2pReceiver != null ) {
			unregisterReceiver(mMyselfWifiP2pReceiver);
			mMyselfWifiP2pReceiver = null;
		}
		
		MessageConnectionManager.getInstance().close();
		WifiP2pGroupManager.getInstance().close();
		
		super.onDestroy();
	}

	private void onPeerSelected( final WifiP2pPeer peer ) {
		PeerConnectionManager.getInstance().requestPeerP2pConnection( 
				peer, getWifiP2pConnectionActionListener( peer ) );
		
		mWaitPeerConnectionThread = new Thread( new Runnable() {
			@Override
			synchronized public void run() {
				PeerMessageConnection pmc
					= PeerConnectionManager.getInstance()
						.getPeerMessageConnection(peer);
				Log.d( "PeerMessageConnection is not established, "
						+ "wait for the connection for peer",
						peer );
				
				while( pmc == null ) {
					try {
						wait( 500 );
					} catch (InterruptedException e) {
						// May be cancelled by the user
						Log.d( "Thread wait for peer connection is interrupted!" );
						break;
					}
					pmc = PeerConnectionManager.getInstance()
							.getPeerMessageConnection(peer);
				}
				
				if( pmc != null ) {
					if( mWaitingForPeerConnectionDialog != null ) {
						mWaitingForPeerConnectionDialog.dismiss();
					}
					
					toFragment(FRAGMENT_FILE_POSITION);
					
					FileFragment ff = (FileFragment) mFragment[FRAGMENT_FILE_POSITION];
					ff.setPeer( peer );
				}
				mWaitPeerConnectionThread = null;
				mWaitingForPeerConnectionDialog = null;
			}
		}, "Thead-WaitPeerConnection" );
		
		mWaitPeerConnectionThread.start();
		
		mWaitingForPeerConnectionDialog
			= getWaitingForPeerConnectionDialog(peer, mWaitPeerConnectionThread );
		mWaitingForPeerConnectionDialog.show();
	}
	
	private AlertDialog getWaitingForPeerConnectionDialog( 
				final WifiP2pPeer peer, final Thread thread ) {
		
		String message
			= getString( R.string.promptWaitForPeerToConnect,
						 peer.getUserDefinedName() );
		AlertDialog dialog
			= new AlertDialog.Builder(this)
					.setMessage(message)
					.setCancelable(true)
					.setOnCancelListener(new OnCancelListener () {
						@Override
						public void onCancel(DialogInterface dialog) {
							mWaitPeerConnectionThread = null;
							mWaitingForPeerConnectionDialog = null;
							thread.interrupt();
						}
					})
					.setNegativeButton( android.R.string.cancel, null )
					.create();
		
		return dialog;
	}
	
	@Override
	public void newUploadEntry(File[] source, WifiP2pPeer peer) {
		DirectFileOperation wfo = new WriteFileOperation( source, peer, true );
		
		PeerMessageConnection pmc
			= PeerConnectionManager.getInstance().getPeerMessageConnection(peer);
		Log.d( "File operation will be sent to peer", "WriteFileOperation", wfo,
			   "PeerMessageConnection", pmc );
		
		if( pmc != null ) {
			pmc.sendOperation(wfo);
			getTransferFragment().addTransferOperation( wfo.getFileTransferInfoList() );
		}
		
		// TODO Auto-generated method stub
		toFragment(MainDrawerAdapter.TRANSFER_ITEM_POSITION);
	}

	private TransferFragment getTransferFragment() {
		return (TransferFragment) mFragment[MainDrawerAdapter.TRANSFER_ITEM_POSITION];
	}
	
	
	@Override
	public void onConnectionFatalError( PeerMessageConnection connection,
									    Exception reason ) {
		
		Log.w( reason,
			   "Fatal error to the connection hanppened, try to reconnection",
			   connection );
		try {
			connection.reconnect( );
		} catch (IOException e) {
			Log.e( e, "Reconnect failed!", connection );
		}
	}

	@Override
	public void connected(PeerMessageConnection peerMessageConnection) {
		// TODO Auto-generated method stub
		
	}
	
	private ActionListener getWifiP2pConnectionActionListener( final WifiP2pPeer peer ) {
		ActionListener listener = new ActionListener() {

			@Override
		    public void onSuccess() {
				String text
					= getString(R.string.promptSucceedToTryToConnectPeer,
								peer.getUserDefinedName() );
				mStatusBarFragment.setNormalStatus( text );
		    }

		    @Override
		    public void onFailure(int reason) {
				String reasonStr
					= ResourceUtility.getWifiP2pFailReason(MainActivity.this, reason);
				String text
					= getString(R.string.promptFailedToTryToConnectPeer,
								peer.getUserDefinedName(), reasonStr);

				mStatusBarFragment.setErrorStatus( text );
		    }

		};
		return listener;
	}
}
