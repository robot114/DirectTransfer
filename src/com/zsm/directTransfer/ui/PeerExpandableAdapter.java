package com.zsm.directTransfer.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Observable;
import java.util.Observer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.zsm.directTransfer.R;
import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.directTransfer.preferences.PeerListOperator;
import com.zsm.log.Log;

@SuppressLint("InflateParams")
public class PeerExpandableAdapter extends BaseExpandableListAdapter {
	
	private class PeerObservable extends Observable {
		@Override
		public synchronized void setChanged() {
			super.setChanged();
		}
	}
	
	private final static Object TAG_NAME = new Object();
	private final static Object TAG_DESCRIPTION = new Object();
	private final static Object TAG_REMOVE = new Object();
	
	protected static final int TAG_PEER = R.id.TAG_ID_PEER;
	
	private final static int CHILD_POS_ADDRESS = 0;
	private final static int CHILD_POS_STATUS = 1;
	private final static int CHILD_LAST = 2;
	
	private PeerStateBroadcastReceiver mPeerStateReceiver;

	private ArrayList<WifiP2pPeer> mPeerList;
	private Context mContext;
	private PeerListOperator mPeerListOperator;
	private OnClickListener mNameClickListener;
	private OnClickListener mRemoveClickListener;
	private OnLongClickListener mNameLongClickListener;
	private IntentFilter mIntentFilter;
	private int mAvailablePeersCount;
	// Observable used to notify the observers that the peer list changed.
	// More than one observer can be added
	private PeerObservable mPeerListObservable = new PeerObservable();
	// Observable used to notify the observer that a peer is selected.
	// Only one observer will be registered
	private PeerObservable mPeerSelectionObservable = new PeerObservable();
	private Channel mChannel;

	PeerExpandableAdapter( Context context, PeerListOperator o ) {
		mContext = context;
		mPeerListOperator = o;
		mPeerList = mPeerListOperator.getPeers( null, null );
		
		mNameClickListener = nameClickListener();
		mNameLongClickListener = nameLongClickListener();
		mRemoveClickListener = removeClickListener();
	}
	
	void setChannel( Channel channel ) {
		mChannel = channel;
	}
	
	private OnClickListener removeClickListener() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				WifiP2pPeer wifiP2pPeer = (WifiP2pPeer) v.getTag( TAG_PEER );
				((ImageView)v).setImageResource( R.drawable.rotate_remove );
				removePeerFromHistory(v, wifiP2pPeer);
			}
		};
	}

	private OnClickListener nameClickListener() {
		OnClickListener listener = new OnClickListener() {
			@Override
			public void onClick(View v) {
				WifiP2pPeer wifiP2pPeer = (WifiP2pPeer) v.getTag( TAG_PEER );
				if( wifiP2pPeer.isPersistened() ) {
					onSelectPeer( wifiP2pPeer );
				} else {
					addPeerOrRename( wifiP2pPeer );
				}
			}
		};
		
		return listener;
	}

	private OnLongClickListener nameLongClickListener() {
		return new OnLongClickListener() {
			@Override
			public boolean onLongClick(View v) {
				WifiP2pPeer peer = (WifiP2pPeer) v.getTag(TAG_PEER);
				if( peer.isPersistened() ) {
					showContextMenuForNameView(v);
					return true;
				}
				return false;
			}
		};
	}

	void peerDiscovered( WifiP2pPeer peer ) {
		int n = mPeerList.indexOf(peer);
		if( n >= 0 ) {
			WifiP2pPeer savedPeer = mPeerList.get(n);
			savedPeer.setDevice( peer.getDevice() );
			savedPeer.setPersistened(true);
			savedPeer.setChannel( mChannel );
		} else {
			peer.setPersistened(false);
			peer.setChannel( mChannel );
			mPeerList.add(peer);
		}
	}
	
	void clearPeersNotPersisitened() {
		int size = mPeerList.size();
		for( int i = size - 1; i >= 0; i-- ) {
			WifiP2pPeer peer = mPeerList.get(i);
			if( !peer.isPersistened() ) {
				mPeerList.remove( i );
			} else {
				peer.getDevice().status = WifiP2pDevice.UNAVAILABLE;
			}
		}
	}
	
	@Override
	public int getGroupCount() {
		return mPeerList.size();
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return CHILD_LAST;
	}

	@Override
	public Object getGroup(int groupPosition) {
		return mPeerList.get(groupPosition).getShowName();
	}

	@Override
	public Object getChild(int groupPosition, int childPosition) {
		return mPeerList.get(groupPosition).getDescription();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public View getGroupView(final int groupPosition, boolean isExpanded,
							 View convertView, ViewGroup parent) {
		
		TextView textView;
		ImageView removeView;
		if( convertView == null ) {
			String infService = Context.LAYOUT_INFLATER_SERVICE;
			LayoutInflater li
				= (LayoutInflater)mContext.getSystemService( infService );
			convertView = li.inflate( R.layout.peer_item, null);

			textView = (TextView)convertView.findViewById( R.id.textViewPeerName );
			textView.setTag( TAG_NAME );
			textView.setOnClickListener( mNameClickListener );
			textView.setOnLongClickListener( mNameLongClickListener );
			
			removeView = (ImageView)convertView.findViewById( R.id.imageRemove );
			removeView.setTag( TAG_REMOVE );
			removeView.setOnClickListener( mRemoveClickListener );
		} else {
			textView = (TextView) convertView.findViewWithTag(TAG_NAME);
			removeView = (ImageView) convertView.findViewWithTag( TAG_REMOVE );
		}
		
		WifiP2pPeer wifiP2pPeer = mPeerList.get(groupPosition);
		textView.setTag(TAG_PEER, wifiP2pPeer);
		removeView.setTag(TAG_PEER, wifiP2pPeer);
		textView.setText( wifiP2pPeer.getShowName() );
		setStyleByStatus( textView, wifiP2pPeer );
		showRemoveButtonByStatus( removeView, wifiP2pPeer );
		
		return convertView;
	}

	private void showRemoveButtonByStatus(ImageView removeView,
										  WifiP2pPeer wifiP2pPeer) {
		
		removeView.setVisibility( wifiP2pPeer.isPersistened() 
									? View.VISIBLE : View.INVISIBLE );
	}

	protected void showContextMenuForNameView(View v) {
		PopupMenu menu = new PopupMenu(mContext, v);
		MenuInflater inflater = menu.getMenuInflater();
		inflater.inflate(R.menu.discover_context, menu.getMenu());
		final WifiP2pPeer wifiP2pPeer = (WifiP2pPeer) v.getTag(TAG_PEER);
		menu.setOnMenuItemClickListener( new OnMenuItemClickListener() {
			@Override
			public boolean onMenuItemClick(MenuItem item) {
				switch( item.getItemId() ) {
					case R.id.menuItemRenamePeer:
						addPeerOrRename(wifiP2pPeer);
						break;
					default:
						return false;
				}
				
				return true;
			}
		} );
		menu.show();
	}

	private void setStyleByStatus(TextView textView, WifiP2pPeer peer) {
		int resId = R.style.PeerAppearance_NotAdded;
		if( peer.isPersistened() ) {
			switch( peer.getStatus() ) {
				case WifiP2pDevice.AVAILABLE:
					resId = R.style.PeerAppearance_Availad;
					break;
				case WifiP2pDevice.CONNECTED:
					resId = R.style.PeerAppearance_Connected;
					break;
				case WifiP2pDevice.FAILED:
					resId = R.style.PeerAppearance_Failed;
					break;
				case WifiP2pDevice.INVITED:
					resId = R.style.PeerAppearance_Invited;
					break;
				case WifiP2pDevice.UNAVAILABLE:
				default:
					resId = R.style.PeerAppearance_Unavailad;
					break;
			}
		} else {
		}
		textView.setTextAppearance( mContext, resId );
	}

	private void addPeerOrRename(final WifiP2pPeer wifiP2pPeer) {
		final EditText edit = new EditText(mContext);
		edit.setText( wifiP2pPeer.getShowName() );
		edit.setSelectAllOnFocus(true);
		edit.setMaxLines( 1 );
		FrameLayout.LayoutParams params
			= new  FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
											ViewGroup.LayoutParams.WRAP_CONTENT);
		Resources resources = mContext.getResources();
		int margin = resources.getDimensionPixelSize( R.dimen.defaultMargin );
		params.leftMargin = margin;
		params.rightMargin = margin;
		edit.setLayoutParams(params);
		FrameLayout container = new FrameLayout(mContext);
		container.addView(edit);
		
		new AlertDialog.Builder(mContext)
			.setTitle(R.string.titleDialogEnterPeerName)
			.setIcon( android.R.drawable.ic_dialog_info)
			.setView(container)
			.setCancelable(false)
			.setPositiveButton(android.R.string.ok,
							   new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					String name = edit.getText().toString().trim();
					wifiP2pPeer.setUserDefinedName( name );
					if( mPeerListOperator.peerExist(wifiP2pPeer) ) {
						mPeerListOperator.updatePeer(wifiP2pPeer);
					} else {
						mPeerListOperator.addPeer(wifiP2pPeer);
					}
					wifiP2pPeer.setPersistened(true);
					notifyDataSetChanged();
					onSelectPeer( wifiP2pPeer );
				}
			})
			.setNegativeButton(android.R.string.cancel, null)
			.show();
	}
	
	private void removePeerFromHistory(final View v, final WifiP2pPeer wifiP2pPeer) {
		new AlertDialog.Builder(mContext)
			.setTitle(R.string.titleDialogRemoveFromPersistence)
			.setMessage( R.string.promptRemovePeerFromPersistence )
			.setIcon( android.R.drawable.ic_dialog_alert )
			.setCancelable(false)
			.setPositiveButton(android.R.string.ok,
							   new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					((ImageView)v).setImageResource( R.drawable.remove );
					
					mPeerListOperator.deletePeer(wifiP2pPeer);
					wifiP2pPeer.setPersistened(false);
					if( !isPeerAvailable(wifiP2pPeer) ) {
						
						mPeerList.remove(wifiP2pPeer);
					}
					notifyDataSetChanged();
				}
			})
			.setNegativeButton(android.R.string.cancel,
							   new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {
					((ImageView)v).setImageResource( R.drawable.remove );
				}
			})
			.show();
	}

	@Override
	public View getChildView(int groupPosition, int childPosition,
							 boolean isLastChild, View convertView,
							 ViewGroup parent) {
		
		TextView textView;
		if( convertView == null ) {
			String infService = Context.LAYOUT_INFLATER_SERVICE;
			LayoutInflater li
				= (LayoutInflater)mContext.getSystemService( infService );
			convertView = li.inflate( R.layout.peer_item_sub, null);
			textView = (TextView)convertView.findViewById( R.id.textViewDetail );
			textView.setTag( TAG_DESCRIPTION );
		} else {
			textView = (TextView) convertView.findViewWithTag(TAG_DESCRIPTION);
		}
		
		WifiP2pPeer peer = mPeerList.get(groupPosition);
		StringBuffer buf = new StringBuffer();
		Resources resources = mContext.getResources();
		switch( childPosition ) {
			case CHILD_POS_ADDRESS:
				buf.append( resources.getString( R.string.peerDataAddress ) )
				   .append( peer.getMacAddress() );
				break;
			case CHILD_POS_STATUS:
				buf.append(  resources.getString(R.string.peerDataStatus ))
				   .append( getPeerStatusString( peer.getStatus() ) );
				break;
			default:
				throw new IllegalArgumentException( 
						"Illegal child position: childPosition >= " + childPosition );
		}
		textView.setText( buf.toString() );
		setStyleByStatus( textView, peer );
		
		return convertView;
	}
	
	private String getPeerStatusString( int status ) {
		Resources res = mContext.getResources();
		
		switch( status ) {
		case WifiP2pDevice.CONNECTED:
			return res.getString( R.string.peerStatusConntected );
		case WifiP2pDevice.INVITED:
			return res.getString( R.string.peerStatusInvited );
		case WifiP2pDevice.FAILED:
			return res.getString( R.string.peerStatusFailed );
		case WifiP2pDevice.AVAILABLE:
			return res.getString( R.string.peerStatusAvailable );
		case WifiP2pDevice.UNAVAILABLE:
			return res.getString( R.string.peerStatusUnavailable );
		default:
			return res.getString( R.string.unknown );
		}
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return false;
	}

	int getAvailablePeersCount() {
		return mAvailablePeersCount;
	}

	void registerPeerStateBroadcastReceiver() {
		if( mPeerStateReceiver == null ) {
		    mPeerStateReceiver = new PeerStateBroadcastReceiver();
		    mIntentFilter
	    		= new IntentFilter( WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION );
			mContext.registerReceiver(mPeerStateReceiver, mIntentFilter );
		}
	}

	public void unregisterPeerStateBroadcastReceiver() {
		if( mPeerStateReceiver != null ) {
			mContext.unregisterReceiver(mPeerStateReceiver);
			mPeerStateReceiver = null;
		}
	}

	public void addPeerListObserver( Observer o ) {
		mPeerListObservable.addObserver(o);
	}
	
	public void registerPeerSelectionObserver(Observer observer) {
		mPeerSelectionObservable.deleteObservers();
		mPeerSelectionObservable.addObserver( observer );
	}

	@Override
	public void notifyDataSetChanged() {
		super.notifyDataSetChanged();
		// Notify the observer of notifier too
		mPeerListObservable.setChanged();
		mPeerListObservable.notifyObservers(mPeerList);
	}
	
	private void onSelectPeer( WifiP2pPeer peer ) {
		if( isPeerAvailable( peer ) ) {
			mPeerSelectionObservable.setChanged();
			mPeerSelectionObservable.notifyObservers(peer);
		} else {
			String str
				= mContext.getString( R.string.promptPeerUnavailable,
									  peer.getShowName() );
			Toast.makeText(mContext, str, Toast.LENGTH_SHORT).show();
		}
	}

	private boolean isPeerAvailable(WifiP2pPeer peer) {
		int status = peer.getStatus();
		return status == WifiP2pDevice.AVAILABLE
			|| status == WifiP2pDevice.CONNECTED
			|| status == WifiP2pDevice.INVITED;
	}
	
	private class PeerStateBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			Log.d( "Peer changed broadcast received.", intent );
			WifiP2pDeviceList list
				= intent.getParcelableExtra( WifiP2pManager.EXTRA_P2P_DEVICE_LIST );
			if( WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(intent.getAction()) ) {
				
				clearPeersNotPersisitened();
				if( list != null ) {
					Collection<WifiP2pDevice> deviceList = list.getDeviceList();
					for( WifiP2pDevice device : deviceList ) {
						peerDiscovered( new WifiP2pPeer( device ) );
					}
					mAvailablePeersCount = deviceList.size();
				} else {
					mAvailablePeersCount = 0;
				}
				notifyDataSetChanged();
			}
		}
		
	}
}
