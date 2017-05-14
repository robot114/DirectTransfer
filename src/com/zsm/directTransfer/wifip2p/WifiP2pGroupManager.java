package com.zsm.directTransfer.wifip2p;

import java.io.Closeable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pGroup;

import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.directTransfer.transfer.operation.DirectOperation;
import com.zsm.log.Log;

/**
 * Record the group information of a WiFi P2P group. And record all members'
 * information in the group.
 * There is a udp socket to exchange information of all the members.
 * 
 * @author zsm
 *
 */
public class WifiP2pGroupManager implements Closeable, AutoCloseable {

	private static final int TIME_FOR_NETWORK_READY = 500;

	private static final int GROUP_ANNOUNCE_PORT = 8889;
	
	private static final int REQUEST_RETRIES = 3;
	private static final byte TYPE_DEVICE_NAME = 0;
	private static final byte TYPE_PHY_ADDRESS = 1;
	private static final byte TYPE_INET_ADDRESS = 2;
	
	private static final int MAX_DEVICE_NAME_LENGTH = 254;
	private static final String NAME_ENCODE = "UTF-8";
	private static final String ADDRESS_ENCODE = "US-ASCII";
	private static final int MEMBER_PACKET_MAX_LEN = 1440;

	private static WifiP2pGroupManager mInstance;
	
	private WifiP2pPeer mMyself;
	private WifiP2pGroup mGroup;
	private InetAddress mGroupOwnerAddress;
	
	private Set<WifiP2pPeer> mMembers;
	
	private DatagramSocket mReceiveSocket;
	private ByteBuffer mOutputBuffer;
	private Thread mReceiveThread;
	private DatagramPacket mReceivePacket;
	private DatagramSocket mSendSocket;

	private InetAddress mLocalAddress;

	private WifiP2pGroupManager( ) {
		mMembers = Collections.synchronizedSet( new HashSet<WifiP2pPeer>() );
	}
	
	public static WifiP2pGroupManager getInstance() {
		if( mInstance == null ) {
			mInstance = new WifiP2pGroupManager();
		}
		
		return mInstance;
	}

	public int getPort() {
		return GROUP_ANNOUNCE_PORT;
	}

	public void start() throws SocketException {
		if( mReceiveSocket != null ) {
			throw new IllegalStateException( "Group manager has started before!" );
		}
		
		byte[] inputBuffer = new byte[MAX_DEVICE_NAME_LENGTH];
		mReceivePacket
			= new DatagramPacket( inputBuffer, MAX_DEVICE_NAME_LENGTH );
		
		mReceiveSocket = new DatagramSocket( GROUP_ANNOUNCE_PORT );
		Log.d( "Start to listern peer announce port.",
				GROUP_ANNOUNCE_PORT );
		mReceiveThread = new Thread( "Thread-ExchangeGroupInfo" ) {
			@Override
			public void run() {
				mOutputBuffer
					= ByteBuffer.allocateDirect( MEMBER_PACKET_MAX_LEN );
				try {
					receiveAnnounce();
				} catch (InterruptedException e) {
					Log.e( e, "Announcement receive thread is interrupted, "
								+ "the group manager will be closed" );
					close();
				}
			}
		};
		
		mReceiveThread.start();
	}

	private void receiveAnnounce() throws InterruptedException {
		while( mReceiveSocket != null ) {
			try {
				mReceiveSocket.receive( mReceivePacket );
			} catch (IOException e) {
				Log.e( e, "Receive packet failed" );
				Thread.sleep( 100 );
				continue;
			}
			
			int packetLen = mReceivePacket.getLength();
			byte[] buffer = mReceivePacket.getData();
			int offset = mReceivePacket.getOffset();
			Log.d( "Announce packet received", "packet len", packetLen, "offset", offset );
			
			try {
				updateMembersFromPacket(buffer, offset, packetLen);
			} catch (ArrayIndexOutOfBoundsException e) {
				// Uncompleted packet, update from the correct members
				Log.w( e, "Packet is truncated" );
			} catch (UnsupportedEncodingException e) {
				// Should not happen
				Log.d( e );
			}
			
			// May receive announce before the group info is updated
			if( mGroup != null && mGroup.isGroupOwner() ) {
				// All the members will announce to GO,
				// so it will notify all the members
				try {
					notifyAllMembers();
				} catch (InterruptedException e) {
					Log.w( e, "Notification is interrupted" );
				} catch (SocketException e) {
					Log.e( e, "Notify members failed" );
				}
			}
			
			Thread.sleep( 100 );
		}
	}
	
	private void updateMembersFromPacket( byte[] buffer, int offset, int packetLen )
					throws UnsupportedEncodingException, 
						   ArrayIndexOutOfBoundsException {
		
		int end = packetLen + offset;
		for( int i = offset; i < end; ) {
			WifiP2pDevice device = new WifiP2pDevice();
			WifiP2pPeer peer = new WifiP2pPeer( device );
			
			byte type = buffer[i];
			i++;
			if( type != TYPE_DEVICE_NAME ) {
				// The following data are invalid, truncate them
				Log.d( "Invalid type, need TYPE_DEVICE_NAME. "
							+ "Follwoing are to be truncated",
						"Type", type, "position", i );
				break;
			}
			int valueLen = DirectOperation.bytesToShort(buffer, i);
			i += 2;
			device.deviceName
				= new String( buffer, i, valueLen, NAME_ENCODE );
			i += valueLen;
			
			type = buffer[i];
			i++;
			if( type != TYPE_PHY_ADDRESS ) {
				// The following data are invalid, truncate them
				Log.d( "Invalid type, need TYPE_PHY_ADDRESS. "
							+ "Follwoing are to be truncated",
						"Type", type, "position", i );
				break;
			}
			valueLen = DirectOperation.bytesToShort(buffer, i);
			i += 2;
			device.deviceAddress
				= new String( buffer, i, valueLen, ADDRESS_ENCODE );
			i+= valueLen;
			
			type = buffer[i];
			if( type == TYPE_INET_ADDRESS ) {
				// InetAddress is optional
				i++;
				valueLen = DirectOperation.bytesToShort(buffer, i);
				i += 2;
				String addressStr
					= new String( buffer, i, valueLen, ADDRESS_ENCODE );
				i+= valueLen;
				try {
					InetAddress inetAddress
						= InetAddress.getByName( addressStr );
					peer.setInetAddress( inetAddress );
				} catch (UnknownHostException e) {
					Log.e( "Invalid inet address.", addressStr );
					// Data of this peer is invalid, skip it
					continue;
				}
			} else if( type != TYPE_DEVICE_NAME ) {
				// The following data are invalid, truncate them
				Log.d( "Invalid type, need TYPE_DEVICE_NAME or TYPE_INET_ADDRESS. "
							+ "Follwoing are to be truncated",
						"Type", type, "position", i );
				break;
			} else {
				// The next TYPE_DEVICE_NAME. Do not move the i as the next loop
				// need to read the type again
				Log.d( "Peer without ip", peer );
			}
			
			if( !peer.equals( mMyself ) ) {
				mMembers.remove(peer);
				mMembers.add(peer);
				Log.d( "A member added", peer, "total", mMembers.size() );
			} else {
				Log.d( "Myslef will not be updated from remote" );
			}
		}
	}

	synchronized private void notifyAllMembers()
					throws InterruptedException, SocketException {
		
		mOutputBuffer.clear();
		for( WifiP2pPeer peer : mMembers ) {
			mOutputBuffer.mark();
			try {
				addOneDeviceToBuffer(mOutputBuffer, peer );
			} catch ( Exception e ) {
				mOutputBuffer.reset();
				Log.w( e, "The following will be truncated.",
						"length", mOutputBuffer.position() );
				break;
			}
		}
		
		int arrayOffset = mOutputBuffer.arrayOffset();
		int position = mOutputBuffer.position();
		DatagramPacket packet
			= new DatagramPacket( mOutputBuffer.array(), arrayOffset, position );
		for( WifiP2pPeer peer : mMembers ) {
			if( peer.getInetAddress() != null && !peer.equals( mMyself) ) {
				sendNotifyPacket(peer.getInetAddress(), packet);
				Log.d( "Announce all members to peer. ", "members", mMembers, "peer", peer );
			}
		}
	}
	
	@Override
	public void close() {
		Log.d( "Group manager is to be closed" );
		if( mReceiveSocket != null && mReceiveSocket.isConnected() ) {
			mReceiveSocket.close();
		}
		mReceiveSocket = null;
		
		mOutputBuffer = null;
		
		mReceivePacket = null;
		
		mMembers.clear();
	}
	
	private void announceMyselfToGO() {
		if( mMyself == null ) {
			Log.w( "Myself is not updated" );
		}
		new Thread( "Thread-AnnouncePeerToGO" ) {
			@Override
			public void run() {
				try {
					announcePeerToGO( mMyself );
				} catch (InterruptedException e) {
					Log.w( e, "Announce myself is interrupted" );
				} catch (SocketException e) {
					Log.e( e, "Announce myself failed" );
				}
			}
		}.start();
	}
	
	synchronized private void announcePeerToGO( WifiP2pPeer peer )
					throws InterruptedException, SocketException {
		
		Log.d( "Start to announce peer to GO. ", peer );
		mOutputBuffer.clear();
		addOneDeviceToBuffer(mOutputBuffer, peer);
		DatagramPacket packet
			= new DatagramPacket( mOutputBuffer.array(),
								  mOutputBuffer.arrayOffset(),
								  mOutputBuffer.position() );
		sendNotifyPacket(mGroupOwnerAddress, packet);
	}

	public void updateGroup( WifiP2pGroup group, InetAddress groupOwnerAddress ) {
		mGroup = group;
		mGroupOwnerAddress = groupOwnerAddress;
		
		if( mGroup.isGroupOwner() ) {
			mLocalAddress = mGroupOwnerAddress;
			// GO is not necessary to announce itself to itself
		} else {
			mLocalAddress = getLocalInetAddressSameSubnet(mGroupOwnerAddress);
		}
		if( mMyself != null ) {
			mMyself.setInetAddress(mLocalAddress);
		}
		
		Log.d( "Group info is updated", group, "Local IP", mLocalAddress );
		if( !mGroup.isGroupOwner() ) {
			// Only client need to and is able to announce itself to the GO
			announceMyselfToGO();
		}
		
		// TODO remove the disconnected memebers
	}
	
	public void updateMyselfDevice( WifiP2pDevice myself ) {
		mMyself = new WifiP2pPeer( myself );
		if( mLocalAddress != null ) {
			mMyself.setInetAddress(mLocalAddress);
		}
		mMembers.add(mMyself);
		Log.d( "Myself updated", mMyself );
	}
	
	private void addOneDeviceToBuffer( ByteBuffer buffer, WifiP2pPeer device ) {
	
		buffer.put( TYPE_DEVICE_NAME );
		byte[] nameBytes = null;
		String deviceName = device.getDevice().deviceName;
		int nameLen = 0;
		try {
			nameBytes = deviceName.getBytes( NAME_ENCODE );
			nameLen = Math.min( nameBytes.length, MAX_DEVICE_NAME_LENGTH );
		} catch (UnsupportedEncodingException e) {
			Log.e( e, "Encode code not supported, no name sent", "code", NAME_ENCODE,
				   "device name", deviceName );
			nameLen = 0;
		}
		buffer.putShort( (short)nameLen );
		buffer.put( nameBytes, 0, nameLen );
		
		buffer.put( TYPE_PHY_ADDRESS );
		putAddress( buffer, device.getDevice().deviceAddress );
		if( device.getInetAddress() != null ) {
			buffer.put( TYPE_INET_ADDRESS );
			putAddress(buffer, device.getInetAddress().getHostAddress() );
		}
	}
	
	private ByteBuffer putAddress( ByteBuffer buffer, String address ) {
		try {
			byte[] bytes = address.getBytes(ADDRESS_ENCODE);
			buffer.putShort( (short)bytes.length );
			buffer.put(bytes);
		} catch (UnsupportedEncodingException e) {
			Log.w( e, "Charset for address is not supported", ADDRESS_ENCODE,
				   address );
			// US-ASCII should be supported by any one
		}
		
		return buffer;
	}

	private void sendNotifyPacket(InetAddress target, DatagramPacket packet)
				throws InterruptedException, SocketException {
		
		if( mSendSocket == null ) {
			mSendSocket = new DatagramSocket();
		}
		mSendSocket.connect( target, GROUP_ANNOUNCE_PORT );
		Log.d( "Connect to peer to notify peer successfully. ", "target", target );
//		updateMyselfFromSocket(mSendSocket);
		int retries = REQUEST_RETRIES;
		while( retries-- > 0 ) {
			try {
				mSendSocket.send(packet);
				Log.d( "Send notify packet successfully.", "packet len",
					   packet.getLength() );
				break;
			} catch (IOException e) {
				Log.w( e, "Request members sent from client failed.",
					   "reties left", retries );
				Thread.sleep( TIME_FOR_NETWORK_READY );
			}
		}
		mSendSocket.disconnect();
	}

//	private void updateMyselfFromSocket(DatagramSocket socket) {
//		if( mLocalAddress == null ) {
//			mLocalAddress = socket.getLocalAddress();
//		}
//		if( mMyself != null && mLocalAddress != null ) {
//			mMyself.setInetAddress(mLocalAddress);
//		}
//	}
//	
	public InetAddress getPeerInetAddress( WifiP2pPeer peer ) {
		InetAddress inetAddress = peer.getInetAddress();
		
		if( inetAddress != null ) {
			return inetAddress;
		}
		
		for( WifiP2pPeer p : mMembers ) {
			if( p.getMacAddress().equalsIgnoreCase( peer.getMacAddress() ) ) {
				inetAddress = p.getInetAddress();
				peer.setInetAddress(inetAddress);
				return inetAddress;
			}
		}
		
		return null;
	}
	
	public static InetAddress getLocalInetAddressSameSubnet( InetAddress address ) {
		
		Log.d( "Looking for a local address which is in the same subnet as ", address );
		byte[] addressBytes = address.getAddress();
		if( addressBytes.length != 4 ) {
			throw new IllegalArgumentException( 
					"Only IPv4 address is supprted right now: " + address );
		}
		int addressNumber = getAddressNumber(addressBytes);
		
		Enumeration<NetworkInterface> niList;
		try {
			niList = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e) {
			Log.e( e, "Cannot get inerfaces" );
			return null;
		}
		
		while( niList.hasMoreElements() ) {
			NetworkInterface ni = niList.nextElement();
			List<InterfaceAddress> addressList = ni.getInterfaceAddresses();
			for( InterfaceAddress ia : addressList ) {
				InetAddress localAddress = ia.getAddress();
				byte[] lab = localAddress.getAddress();
				if( lab.length != addressBytes.length ) {
					continue;
				}
				
				int mask = 0xFFFFFFFF << (32-ia.getNetworkPrefixLength());
				int lan = getAddressNumber( lab );
				
				if( ( lan & mask ) == ( addressNumber & mask ) ) {
					Log.d( "Matched ip address found", ia );
					return localAddress;
				}
			}
		}
		
		Log.d( "No match ip address found" );
		return null;
	}
	
	private static int getAddressNumber( byte[] address ) {
		return DirectOperation.bytesToInt(address, 0);
	}
	
	public WifiP2pPeer findPeerByAddress( InetAddress address ) {
		for( WifiP2pPeer p : mMembers ) {
			if( p.getInetAddress().equals( address ) ) {
				return p;
			}
		}
		
		return null;
	}
}
