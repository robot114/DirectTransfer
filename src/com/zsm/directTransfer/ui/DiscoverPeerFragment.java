package com.zsm.directTransfer.ui;

import java.util.Collection;

import android.app.Activity;
import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
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
import android.widget.TextView;

import com.zsm.android.ui.Utility;
import com.zsm.android.wifi.WifiUtility;
import com.zsm.android.wifi.WifiUtility.EnableResultListener;
import com.zsm.directTransfer.R;
import com.zsm.directTransfer.data.FakeWifiP2pPeer;
import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.directTransfer.preferences.Preferences;
import com.zsm.log.Log;

public class DiscoverPeerFragment extends Fragment {

	private WifiP2pManager mManager;
	private Channel mChannel;
	private ExpandableListView mPeersListView;
	private PeerStateBroadcastReceiver mPeerStateReceiver;
	private IntentFilter mIntentFilter;
	private PeerExpandableAdapter mListAdapter;
	private TextView mHintView;
	private String mHintText;
	private int mHintDrawableId;
	private boolean mIsDiscovering;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		Context context = getActivity();
	    mManager = (WifiP2pManager) context.getSystemService(Context.WIFI_P2P_SERVICE);
	    mChannel = mManager.initialize(context, context.getMainLooper(), null);

	    mIsDiscovering = false;
	    mPeerStateReceiver = new PeerStateBroadcastReceiver();
	    mIntentFilter
	    	= new IntentFilter( WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION );
	    getActivity().registerReceiver(mPeerStateReceiver, mIntentFilter);
	    
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
					
					mHintDrawableId = R.drawable.back_border_error;
					setHint(hintResId, mHintDrawableId);
					
					Log.w( "Fail to enable wifi. Reason code", reason );
				}
			};
				
			WifiUtility.getInstance().enableWifi( getActivity(), listener);
		}
	}
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = inflater.inflate( R.layout.fragment_discover, (ViewGroup)null );
		
		mPeersListView = (ExpandableListView)view.findViewById( R.id.listViewPeers );
		mListAdapter
			= new PeerExpandableAdapter( getActivity(),
							Preferences.getInstance().getPeerListOperator() );
		
		mPeersListView.setAdapter(mListAdapter);
		mHintView = (TextView)view.findViewById( R.id.textViewHint );
		
		fixHintHeight();
		
		return view;
	}

	private void fixHintHeight() {
		int hintViewHeight = Utility.getTextViewHeight(mHintView);
		mHintView.setMinHeight( hintViewHeight );
		mHintView.setMaxHeight( hintViewHeight );
	}

	private boolean isWifiEnabled() {
		WifiManager wifi
			= (WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
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
		WifiUtility.getInstance().unregisterWifiEnableReceiver( getActivity() );
		if( mPeerStateReceiver != null ) {
			getActivity().unregisterReceiver(mPeerStateReceiver);
			mPeerStateReceiver = null;
		}
		
		if( mIsDiscovering ) {
			mManager.stopPeerDiscovery(mChannel, new DiscoverListener( false ) );
			mIsDiscovering = false;
		}
	}

	private void setHint( ) {
		if( mHintView != null ) {
			mHintView.setText( mHintText );
			mHintView.setBackgroundResource( mHintDrawableId );
		}
	}
	
	private void setHint(int textResId, int backgroundResId) {
		Activity activity = getActivity();
		if( activity != null ) {
			mHintText = activity.getResources().getString(textResId);
			mHintDrawableId = backgroundResId;
			setHint( );
		}
	}

	private void setHint( String text, int backgroundResId ) {
		mHintText = text;
		mHintDrawableId = backgroundResId;
		setHint( );
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
			
			setHint( hintResId, R.drawable.back_border );
		}

		@Override
		public void onFailure(int reason) {
			Log.w( "Discover peers failed. Reason is", reason, "action is start: ", mStart );

			Activity activity = getActivity();
			if( activity == null ) {
				return;
			}
			
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
			
			String reasonStr = activity.getString( reasonId );
			String text
				= activity.getString( R.string.promptFailedToDiscoverPeer,
									  reasonStr );
			
			setHint( text, R.drawable.back_border_error );
			mIsDiscovering = false;
		}
		
	}
	
	private class PeerStateBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			WifiP2pDeviceList list
				= intent.getParcelableExtra( WifiP2pManager.EXTRA_P2P_DEVICE_LIST );
			if( WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(intent.getAction())
				&& list != null	) {
				
				Collection<WifiP2pDevice> deviceList = list.getDeviceList();
				mListAdapter.clearPeersNotPersisitened();
				for( WifiP2pDevice device : deviceList ) {
					mListAdapter.peerDiscovered( new WifiP2pPeer( device ) );
				}
				
				for( int i = 0; i < 20; i++ ) {
					mListAdapter.peerDiscovered( new FakeWifiP2pPeer( ""+i ) );
				}
				mListAdapter.notifyDataSetChanged();
				setHint( deviceList.size() == 0 
							? R.string.hintStartDiscovering 
							: R.string.hintSelectPeerToTransfer,
				R.drawable.back_border );
			}
		}
		
	}
}
