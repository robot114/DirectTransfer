package com.zsm.directTransfer.preferences;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.provider.DocumentsContract;

import com.zsm.android.ui.fileselector.FileOperation;
import com.zsm.android.ui.fileselector.FileSelector;
import com.zsm.android.ui.fileselector.OnHandleFileListener;
import com.zsm.directTransfer.R;
import com.zsm.directTransfer.app.ApplicationInterface;
import com.zsm.log.Log;

public class MainPreferencesFragment extends PreferenceFragment {

	private static final int REQUEST_CODE_DOWNLOAD_DIR = 142;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);
	}

	@Override
	public void onResume() {
		super.onResume();
		
		initDownloadDirPref();

	}

	private void initDownloadDirPref() {
		final Preference pref = findPreference(Preferences.KEY_WRITE_PATH);
		updateDownloadDirSummary(pref);
		pref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			@Override
			public boolean onPreferenceClick(final Preference preference) {
				
				if (ApplicationInterface.isSafSystem()) {
					selectDownloadDirBySAF();
				} else {
					selectDownloadDirByFileSelector(preference);
				}
				
				return true;
			}
		});
	}

	private void updateDownloadDirSummary(final Preference pref) {
		String path = null;
		if( Preferences.getInstance().isStorageAsscessFramework() ) {
			Uri uri = Preferences.getInstance().getWriteUri();
			path = DocumentsContract.getTreeDocumentId( uri );
		} else {
			path = Preferences.getInstance().getWritePath();
		}
		String dir
			= getActivity().getString(R.string.prefSummary_DownloadTarget, path);
		
		pref.setSummary( dir );
	}

	private void selectDownloadDirByFileSelector(final Preference preference) {
		final OnHandleFileListener onHandleFileListener
				= new OnHandleFileListener(){
			
			@Override
			public void handleFile( FileOperation operation,
									String filePath ) {
				
				Preferences.getInstance().setWritePath(filePath);
				updateDownloadDirSummary(preference);
			}
		};
		
		new FileSelector( getActivity(), FileOperation.FOLDER,
				Preferences.getInstance().getWritePath(),
				onHandleFileListener,
				null, true, true ).show();
	}

	private void selectDownloadDirBySAF() {
		Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
		startActivityForResult(intent, REQUEST_CODE_DOWNLOAD_DIR);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if( requestCode == REQUEST_CODE_DOWNLOAD_DIR
        	&& resultCode == Activity.RESULT_OK && data != null ) {
        	
            Uri treeUri = data.getData();
            Log.d( "Uri selected to store the download files.", "uri", treeUri,
            	   "document id", DocumentsContract.getTreeDocumentId(treeUri) );
            Log.d( "Test file: ", DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, "test" ) );
    		// from https://developer.android.com/guide/topics/providers/document-provider.html#permissions :
    		final int takeFlags = data.getFlags()
    	            & (Intent.FLAG_GRANT_READ_URI_PERMISSION
    	            | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
	    	// Check for the freshest data.
	    	getActivity().getContentResolver()
	    		.takePersistableUriPermission(treeUri, takeFlags);
	    	Preferences.getInstance().setWriteUri( treeUri );
        }
		
		super.onActivityResult(requestCode, resultCode, data);
	}
}
