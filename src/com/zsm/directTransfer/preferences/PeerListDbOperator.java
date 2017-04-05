package com.zsm.directTransfer.preferences;

import java.util.ArrayList;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.wifi.p2p.WifiP2pDevice;

import com.zsm.directTransfer.data.WifiP2pPeer;
import com.zsm.log.Log;

public class PeerListDbOperator implements PeerListOperator {

	private static final String TABLE_NAME = "PeersTable";

	private static final String COLUMN_ADDRESS = "DeviceAddress";
	private static final String COLUMN_USER_DEFINED_NAME = "UserDefinedName";
	private static final String COLUMN_DEVICE_NAME = "DeviceName";
	private static final String COLUMN_PRIMARY_TYPE = "DevicePrimaryType";
	
	private final static String CREATE_SQL
		= "CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " ("
		  + COLUMN_ADDRESS + " text(17) not null primary key, "
		  + COLUMN_USER_DEFINED_NAME + " text, "
		  + COLUMN_DEVICE_NAME + " text, "
		  + COLUMN_PRIMARY_TYPE + " text)";
	
	private static final String ADDRESS_WHERE_CLAUSE = COLUMN_ADDRESS + "=?";
	
	private static final String[] COLUMNS
		= new String[] { COLUMN_ADDRESS, COLUMN_USER_DEFINED_NAME,
						 COLUMN_DEVICE_NAME, COLUMN_PRIMARY_TYPE };
	
	private SQLiteOpenHelper mSqliteOpenHelper;

	PeerListDbOperator( ) {
	}
	
	void setSQLiteOpenHelper( SQLiteOpenHelper helper ) {
		mSqliteOpenHelper = helper;
	}
	
	void createPeerTable( SQLiteDatabase db ) {
		db.execSQL(CREATE_SQL);
	}

	public void updatePeerTable( SQLiteDatabase db ) {
		db.execSQL( "DROP TABLE IF EXIST " + TABLE_NAME );
		createPeerTable( db );
	}
	
	/**
	 * Insert new peer.
	 * 
	 * @param peer Peer device
	 * @return id of the new added peer
	 * 
	 */
	@Override
	public long addPeer(WifiP2pPeer peer) {
		long id = -1;
		SQLiteDatabase db = mSqliteOpenHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			id = db.insertOrThrow(TABLE_NAME, null, convertData(peer) );
			db.setTransactionSuccessful();
			Log.d( "Peer added", "name", peer.getShowName(),
				   "address", peer.getMacAddress() );
		} catch ( SQLException e ) {
			Log.e( e, "Add new peer failed", peer );
		} finally {
		    db.endTransaction();
		}
		
		return id;
	}
	
	/**
	 * Update an existing peer. The key is the address
	 * 
	 * @param peer Peer device
	 * @return true, update successfully; false, otherwise 
	 * 
	 */
	@Override
	public boolean updatePeer(WifiP2pPeer peer) {
		int rows = 0;
		SQLiteDatabase db = mSqliteOpenHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			rows = db.update(TABLE_NAME, convertData(peer), ADDRESS_WHERE_CLAUSE,
							 new String[]{peer.getMacAddress()} );
			db.setTransactionSuccessful();
			Log.d( "Peer update", "name", peer.getShowName(),
				   "address", peer.getMacAddress() );
		} catch ( SQLException e ) {
			Log.e( e, "Update peer failed", peer );
		} finally {
		    db.endTransaction();
		}
		
		return rows == 1;
	}
	
	/**
	 * Delete an existing peer. The key is the address
	 * 
	 * @param peer Peer device
	 * @return true, delete successfully; false, otherwise 
	 * 
	 */
	@Override
	public boolean deletePeer(WifiP2pPeer peer) {
		int rows = 0;
		SQLiteDatabase db = mSqliteOpenHelper.getWritableDatabase();
		db.beginTransaction();
		try {
			rows = db.delete(TABLE_NAME, ADDRESS_WHERE_CLAUSE,
							 new String[]{peer.getMacAddress()} );
			db.setTransactionSuccessful();
			Log.d( "Peer delete", "name", peer.getShowName(),
				   "address", peer.getMacAddress() );
		} catch ( SQLException e ) {
			Log.e( e, "delete peer failed", peer );
		} finally {
		    db.endTransaction();
		}
		
		return rows == 1;
	}
	
	@Override
	public ArrayList<WifiP2pPeer> getPeers( String selection, String[] selectionArgs ) {
		SQLiteDatabase db = mSqliteOpenHelper.getReadableDatabase();
		Cursor c
			= db.query( TABLE_NAME, COLUMNS, selection, selectionArgs,
						null, null, COLUMN_USER_DEFINED_NAME );
		ArrayList<WifiP2pPeer> list = new ArrayList<WifiP2pPeer>( c.getCount() );
		int userDefinedNameColumnIndex = c.getColumnIndex(COLUMN_USER_DEFINED_NAME);
		int addrColumnIndex = c.getColumnIndex(COLUMN_ADDRESS);
		int deviceNameColumnIndex = c.getColumnIndex(COLUMN_DEVICE_NAME);
		int primaryColumnIndex = c.getColumnIndex(COLUMN_PRIMARY_TYPE);
		while( c.moveToNext() ) {
			String udn = c.getString( userDefinedNameColumnIndex );
			
			WifiP2pDevice device = new WifiP2pDevice();
			device.deviceAddress = c.getString( addrColumnIndex );
			device.deviceName = c.getString( deviceNameColumnIndex );
			device.primaryDeviceType = c.getString( primaryColumnIndex );
			device.status = WifiP2pDevice.UNAVAILABLE;
			
			WifiP2pPeer peer = new WifiP2pPeer( udn, device );
			
			peer.setPersistened(true);
			
			list.add(peer);
		}
		db.close();
		return list;
	}
	
	@Override
	public boolean peerExist( WifiP2pPeer peer ) {
		SQLiteDatabase db = mSqliteOpenHelper.getReadableDatabase();
		Cursor c
			= db.query( TABLE_NAME, COLUMNS, COLUMN_ADDRESS + "= ?",
						new String[]{ peer.getMacAddress() },
						null, null, COLUMN_USER_DEFINED_NAME );

		return c.getCount() == 1;
	}
	
	private ContentValues convertData( WifiP2pPeer peer ) {
		ContentValues data = new ContentValues();
		data.put(COLUMN_ADDRESS, peer.getMacAddress());
		data.put(COLUMN_USER_DEFINED_NAME, peer.getUserDefinedName());
		data.put(COLUMN_DEVICE_NAME, peer.getDeviceName());
		data.put(COLUMN_PRIMARY_TYPE, peer.getType());
		return data;
	}
}
