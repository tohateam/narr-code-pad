package jp.narr.reader;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

/***
 * @author John Lombard
 * This class belongs to Jonh Lombard from anddev.org, its a very basic class  but its a good starting point for a filebrowser
 * i made a few changes to make it compatible to 1.6, all the credit belong to him.
 */
public class FileBrowser extends ListActivity {
	private enum DISPLAYMODE {
		ABSOLUTE, 
		RELATIVE;
	}

	static final String LOGTAG = "FileBrowser";
	private final DISPLAYMODE displayMode = DISPLAYMODE.RELATIVE;
	private List<String> directoryEntries = new ArrayList<String>();
	private File currentDirectory = new File("/sdcard/");
	private SharedPreferences prefs;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		// setContentView() gets called within the next line,
		// so we do not need it here.

		prefs = getSharedPreferences( "pref",
									  MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE );
		String path = prefs.getString("dir", "/sdcard" );
		File dir = new File(path);
		if( dir.exists() ) {
			browseTo(dir);
		} else {
			browseToRoot();
		}
	}

	/**
	 * This function browses to the root-directory of the file-system.
	 */
	private void browseToRoot() {
		browseTo( new File("/sdcard/") );
	}

	/**
	 * This function browses up one level according to the field:
	 * currentDirectory
	 */
	private void upOneLevel() {
		if ( currentDirectory.getParent() != null ) {
			browseTo(currentDirectory.getParentFile());
		}
	}

	@Override
	// if "keyUp", Code Reader takes the event beforehand??
	//public boolean onKeyUp(int keyCode, KeyEvent event) {
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.v(LOGTAG," down, keycode file browser: " + keyCode);
		if(keyCode == KeyEvent.KEYCODE_BACK) { 
			upOneLevel();
			return true;
		} else if( keyCode == KeyEvent.KEYCODE_MENU ) { 
			setResult(RESULT_CANCELED, null);
			finish();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	}	

	private void browseTo(final File dir) {
		if (dir.isDirectory()) {
			currentDirectory = dir;
			fill(dir.listFiles());
		} else {
			// save directory
			File parent = dir.getParentFile();
			SharedPreferences.Editor ed = prefs.edit();
			ed.putString("dir", parent.getPath());
			ed.commit();			

			Intent resultIntent = 
				new Intent( android.content.Intent.ACTION_VIEW, 
							Uri.parse("file://" + dir.getAbsolutePath()) );
			setResult(RESULT_OK, resultIntent);
			finish();
		}
	}

	private void fill(File[] files) {
		directoryEntries.clear();

		try {
			Thread.sleep(10);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//directoryEntries.add(".");
		if (currentDirectory.getParent() != null) {
			directoryEntries.add("..");
		}
		if (files != null) {
			switch (displayMode) {
				case ABSOLUTE:
					for (File file : files) {
						directoryEntries.add(file.getPath());
					}
					break;
				case RELATIVE: // On relative Mode, we have to add the current-path to the beginning
					int currentPathStringLenght = currentDirectory.getAbsolutePath().length();
					for (File file : files) {
						directoryEntries.add(file.getAbsolutePath().substring(currentPathStringLenght));
					}
					break;
			}
		}

		ArrayAdapter<String> directoryList = 
			new ArrayAdapter<String>( this, R.layout.file_row, directoryEntries );

		setListAdapter(directoryList);
	}
	
	@Override
	protected void onListItemClick(ListView listView, View view, int position, long id) {
		int selectionRowID = (int)listView.getItemIdAtPosition(position);
		String selectedFileString = directoryEntries.get(selectionRowID);
		/*
		if (selectedFileString.equals(".")) {
			// Refresh
			browseTo(currentDirectory);
		} else
		*/
		if (selectedFileString.equals("..")) {
			upOneLevel();
		} else {
			File clickedFile = null;
			switch (displayMode) {
			case RELATIVE:
				clickedFile = new File(currentDirectory.getAbsolutePath()
						+ directoryEntries.get(selectionRowID));
				break;
			case ABSOLUTE:
				clickedFile = new File( directoryEntries.get(selectionRowID) );
				break;
			}
			if (clickedFile != null) {
				browseTo(clickedFile);
			}
		}
	}
}
