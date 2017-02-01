package com.zsm.directTransfer.ui;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import android.app.Fragment;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;

import com.zsm.android.wifi.WifiUtility;
import com.zsm.android.wifi.WifiUtility.EnableResultListener;
import com.zsm.directTransfer.R;
import com.zsm.directTransfer.preferences.Preferences;
import com.zsm.log.Log;

public class PeerFragment extends Fragment {

	private Context mContext;
	private StatusBarOperator mStatusBarOperator;
	
	private WifiP2pManager mManager;
	private Channel mChannel;
	private ExpandableListView mPeersListView;
	private PeerExpandableAdapter mListAdapter;
	private boolean mIsDiscovering;
	
	private Observer mPeerListObserver;

	public PeerFragment(Context context, StatusBarOperator statusOperator) {
		mContext = context;
		mStatusBarOperator = statusOperator;
		
	    // Initialize List adapter here to avoid NullPointerException when 
	    // registerPeerSelectionObserver and addPeerListObserver(Observer)
	    // are invoked.
		mListAdapter
			= new PeerExpandableAdapter( mContext,
							Preferences.getInstance().getPeerListOperator() );
		
		mListAdapter.registerPeerStateBroadcastReceiver();
		addPeerListObserver();
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	    mManager
	    	= (WifiP2pManager) mContext.getSystemService(
	    			Context.WIFI_P2P_SERVICE);
	    
	    mChannel = mManager.initialize(mContext, mContext.getMainLooper(), null);

	    mIsDiscovering = false;
	    
	    checkWifiAndStartToDiscover( );
	    
	    setHasOptionsMenu(true);
	}

	private void checkWifiAndStartToDiscover( ) {
		if( isWifiEnabled() ) {
			startToDiscover();
		} else {
			EnableResultListener listener = new EnableResultListener() {
				
				@Override
				public void success() {
					Log.d( "Enable wifi successfully. Start to discover peers." );
					startToDiscover();
				}
				
				@Override
				public void failed(int reason) {
					int hintResId;
					switch( reason ) {
						case EnableResultListener.REASON_FAIL_PERMISSION_DENY:
							hintResId = R.string.promptDenyToEnableWifiOfDiscoverPeer;
							break;
						case EnableResultListener.REASON_FAIL_FAILED:
						default:
							hintResId = R.string.promptFailedToEnableWifiOfDiscoverPeer;
						break;
					}
					
					setHint(hintResId, StatusBarOperator.STATUS_WARNING);
					
					Log.w( "Fail to enable wifi. Reason code", reason );
				}
			};
				
			WifiUtility.getInstance().enableWifi( mContext, listener);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = inflater.inflate( R.layout.fragment_discover, (ViewGroup)null );
		
		mPeersListView = (ExpandableListView)view.findViewById( R.id.listViewPeers );
	    
		mPeersListView.setAdapter(mListAdapter);
		
		return view;
	}

	private boolean isWifiEnabled() {
		WifiManager wifi
			= (WifiManager)mContext.getSystemService(Context.WIFI_SERVICE);
		return wifi.isWifiEnabled();
	}
	
	private void startToDiscover() {
		mManager.discoverPeers(mChannel, new DiscoverListener( true ) );
	}
	
	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.discover, menu);
		menu.findItem( R.id.itemRefresh )
			.setOnMenuItemClickListener( new OnMenuItemClickListener() {
				
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				checkWifiAndStartToDiscover();
				return true;
			}
		} );

	}

	@Override
	public void onDetach() {
		super.onDetach();
		WifiUtility.getInstance().unregisterWifiEnableReceiver( mContext );
		mListAdapter.unregisterPeerStateBroadcastReceiver();
		if( mIsDiscovering ) {
			mManager.stopPeerDiscovery(mChannel, new DiscoverListener( false ) );
			mIsDiscovering = false;
		}
	}

	private void setHint(int textResId, int status) {
		mStatusBarOperator.setStatus(textResId, status);
	}

	private void setHint( String text, int status ) {
		mStatusBarOperator.setStatus(text, status);
	}
	
	private void addPeerListObserver() {
		mPeerListObserver = new Observer() {
			@Override
			public void update(Observable o, Object arg) {
				ArrayList<?> list = (ArrayList<?>)arg;
				setHint( list.size() == 0 
							? R.string.hintStartDiscovering 
							: R.string.hintSelectPeerToTransfer,
						 StatusBarOperator.STATUS_NORMAL );
			}
		};
		
		addPeerListObserver(mPeerListObserver);
	}
	
	void addPeerListObserver( Observer o ) {
		mListAdapter.addPeerListObserver(o);
	}
	
	public void registerPeerSelectionObserver( Observer o ) {
		mListAdapter.registerPeerSelectionObserver(o);
	}
	
	private class DiscoverListener implements WifiP2pManager.ActionListener {
		
		private boolean mStart;

		DiscoverListener( boolean start ) {
			mStart = start;
		}

		@Override
		public void onSuccess() {
			
			int hintResId;
			if( mStart ) {
				Log.d( "Start to discover peers successfully!" );
				hintResId = R.string.hintStartDiscovering;
			} else {
				Log.d( "Stop discovering peers successflly!" );
				hintResId = R.string.hintStopDiscovering;
			}
			
			setHint( hintResId, StatusBarOperator.STATUS_NORMAL );
		}

		@Override
		public void onFailure(int reason) {
			Log.w( "Discover peers failed. Reason is", reason, "action is start: ", mStart );

			int reasonId;
			switch( reason ) {
				case WifiP2pManager.P2P_UNSUPPORTED:
					reasonId = R.string.promptFailedToDiscoverPeerReasonUnsupport;
					break;
				case WifiP2pManager.BUSY:
					reasonId = R.string.promptFailedToDiscoverPeerReasonBusy;
					break;
				default:
					reasonId = R.string.promptFailedToDiscoverPeerReasonError;
					break;
			}
			
			String reasonStr = mContext.getString( reasonId );
			String text
				= mContext.getString( R.string.promptFailedToDiscoverPeer,
									   reasonStr );
			
			setHint( text, StatusBarOperator.STATUS_WARNING );
			mIsDiscovering = false;
		}
		
	}

}
