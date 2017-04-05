package com.zsm.directTransfer.data;

import java.net.InetAddress;
import java.util.Locale;
import java.util.Observable;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pManager.Channel;
import android.os.Parcel;
import android.os.Parcelable;

public class WifiP2pPeer extends Observable implements Parcelable {

	private String mUserDefinedName;
	private WifiP2pDevice mDevice;
	private InetAddress mInetAddress;
	private boolean mPersistened;
	private Channel mChannel;
	
	public WifiP2pPeer( String userDefinedName, WifiP2pDevice peer ) {
		if( userDefinedName != null && userDefinedName.trim().length() == 0 ) {
			throw new IllegalArgumentException(
						"userDefinedName MUST be null or non-empty" );
		}
		if( peer == null ) {
			throw new IllegalArgumentException(
						"wrapped device cannot be null!" );
		}
		mUserDefinedName = userDefinedName;
		mDevice = peer; 
	}
	
	public WifiP2pPeer( WifiP2pDevice peer ) {
		this( null, peer);
	}
	
	public boolean hasUderDefinedName() {
		return mUserDefinedName != null;
	}
	
	public String getShowName() {
		return mUserDefinedName == null ? mDevice.deviceName : mUserDefinedName;
	}

	public String getUserDefinedName() {
		return mUserDefinedName;
	}

	public void setUserDefinedName(String name) {
		String trimmed = name.trim();
		mUserDefinedName = trimmed.length() == 0 ? null : trimmed;
	}

	public void setDevice(WifiP2pDevice device) {
		mDevice = device;
	}

	public WifiP2pDevice getDevice() {
		return mDevice;
	}
	
	public boolean isPersistened() {
		return mPersistened;
	}

	public void setPersistened(boolean persistened) {
		mPersistened = persistened;
	}

    public InetAddress getInetAddress() {
		return mInetAddress;
	}

	public void setInetAddress(InetAddress mInetAddress) {
		this.mInetAddress = mInetAddress;
	}

	public String getDeviceName() {
		return mDevice.deviceName;
	}
	
	public String getMacAddress() {
		return mDevice.deviceAddress;
	}
	
	public String getType() {
		return mDevice.primaryDeviceType;
	}
	
	public int getStatus() {
		return mDevice.status;
	}

	public void setChannel(Channel channel) {
		mChannel = channel;
	}
	
	public Channel getChannel() {
		return mChannel;
	}
	
	public String getDescription() {
        StringBuffer sbuf = new StringBuffer();
    	sbuf.append("Device: ")
    		.append(mUserDefinedName == null 
    				? mDevice.deviceName : mUserDefinedName );
    	
        if( mInetAddress != null ) {
        	sbuf.append("\n IP Address: ").append(mInetAddress.getHostAddress());
        }
        
        sbuf.append("\n Mac Address: ").append(mDevice.deviceAddress);
        sbuf.append("\n Type: ").append(mDevice.primaryDeviceType);
        sbuf.append("\n Status: ").append(mDevice.status);
        return sbuf.toString();
	}

	public String getVerboseDescription() {
        return mDevice.toString();
	}

	@Override
	public String toString() {
		return getDescription();
	}

	@Override
	public int hashCode() {
		return getMacAddress().toLowerCase(Locale.US).hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		WifiP2pPeer peer = (WifiP2pPeer)obj;
		
		if( peer == null ) {
			return false;
		}
		
		return peer == this || peer.getMacAddress().equalsIgnoreCase(getMacAddress());
	}

	@Override
	public int describeContents() {
		return 0;
	}
	
    /** Implement the Parcelable interface */
    public void writeToParcel(Parcel dest, int flags) {
    	dest.writeString(mUserDefinedName);
    	dest.writeParcelable( mDevice, 0 );
    }

	/** Implement the Parcelable interface */
    public static final Creator<WifiP2pPeer> CREATOR =
        new Creator<WifiP2pPeer>() {
            public WifiP2pPeer createFromParcel(Parcel in) {
                String udn = in.readString();
                WifiP2pDevice device = in.readParcelable(null);
                return new WifiP2pPeer( udn, device );
            }

            public WifiP2pPeer[] newArray(int size) {
                return new WifiP2pPeer[size];
            }
        };
}
