package com.zsm.directTransfer.ui;

import java.io.File;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MenuItem.OnMenuItemClickListener;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ExpandableListView;
import android.widget.Toast;

import com.anthonycr.grant.PermissionsManager;
import com.anthonycr.grant.PermissionsResultAction;
import com.zsm.android.ui.CheckableExpandableChildListAdapter;
import com.zsm.android.ui.fileselector.FileOperation;
import com.zsm.android.ui.fileselector.FileSelector;
import com.zsm.android.ui.fileselector.OnHandleFileListener;
import com.zsm.directTransfer.R;
import com.zsm.directTransfer.preferences.Preferences;
import com.zsm.directTransfer.ui.data.ExpandableChileFileData;

public class FileFragment extends Fragment
				implements OnMenuItemClickListener, OnHandleFileListener {

	private Context mContext;
	private CheckableExpandableChildListAdapter<ExpandableChileFileData> mListAdapter;
	private MenuItem mSelectAllMenuItem;

	public FileFragment(Context context) {
		mContext = context;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
	    setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		
		View view = inflater.inflate( R.layout.fragment_file, (ViewGroup)null );
		ExpandableListView listView
			= (ExpandableListView) view.findViewById( R.id.listViewFile );
		mListAdapter
			= new CheckableExpandableChildListAdapter<ExpandableChileFileData>(
						mContext, false );
		listView.setAdapter(mListAdapter);
		
		return view;
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate( R.menu.fragment_file, menu);
		
		menu.findItem( R.id.itemAddFile ).setOnMenuItemClickListener( this );
		menu.findItem( R.id.itemSearchFolder ).setOnMenuItemClickListener(this);
		menu.findItem( R.id.itemClearList ).setOnMenuItemClickListener(this);
		mSelectAllMenuItem = menu.findItem(R.id.itemSelectAll );
		mSelectAllMenuItem.setOnMenuItemClickListener( this );
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onMenuItemClick(MenuItem item) {
		switch( item.getItemId() ) {
			case R.id.itemAddFile:
				addOnFileForGrant();
				break;
			case R.id.itemSearchFolder:
				searchFolder();
				break;
			case R.id.itemClearList:
				clearList();
				break;
			case R.id.itemSelectAll:
				selectOrUnselectAll();
				break;
			default:
				return false;
		}
		return true;
	}

	@TargetApi(23)
	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String[] permissions,
										   int[] grantResults) {
		
		PermissionsManager.getInstance()
			.notifyPermissionsChange(permissions, grantResults);
	}

	private void addOnFileForGrant() {
		PermissionsManager.getInstance()
			.requestPermissionsIfNecessaryForResult( 
					this,
					new String[]{ Manifest.permission.READ_EXTERNAL_STORAGE },
					new PermissionsResultAction() {
						@Override
						public void onGranted() {
							addOneFile();
						}

						@Override
						public void onDenied(String permission) {
							promptReadStorageDenied();
						}
						
					});
	}

	private void searchFolder() {
		// TODO Auto-generated method stub
		
	}

	private void clearList() {
		new AlertDialog.Builder(mContext)
			.setTitle( R.string.app_name )
			.setIcon( android.R.drawable.ic_dialog_alert )
			.setMessage( R.string.promptClearFileList )
			.setPositiveButton( android.R.string.yes,
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						mListAdapter.clear();
						setSelectAllIcon( false);
					}
			} )
			.setNegativeButton(android.R.string.no, null )
			.show();
	}

	private void selectOrUnselectAll() {
		boolean allSelected
			= ( mListAdapter.getCheckedGroupCount()
				== mListAdapter.getGroupCount() );
		
		mListAdapter.setCheckAll( !allSelected );
		setSelectAllIcon( !allSelected );
	}

	private void setSelectAllIcon(boolean allSelected) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void handleFile(FileOperation operation, String filePath) {
		File fileDir = new File( filePath );
		switch( operation ) {
			case LOAD:
				mListAdapter.add( new ExpandableChileFileData( fileDir ) );
				break;
			case FOLDER:
				break;
		}
		
		Preferences.getInstance().setReadPath( fileDir.getAbsolutePath() );
		// TODO Auto-generated method stub
		
	}

	private void addOneFile() {
		FileSelector fileSelector
			= new FileSelector( getActivity(), FileOperation.LOAD,
								Preferences.getInstance().getReadPath(), this,
								null );
		fileSelector.show();
	}

	private void promptReadStorageDenied() {
		Toast.makeText( mContext,  
					    R.string.promptPermissReadStorage,
					    Toast.LENGTH_SHORT )
			 .show();
	}

}
