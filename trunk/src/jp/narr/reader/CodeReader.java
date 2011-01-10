package jp.narr.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
//import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;


public class CodeReader extends Activity {
	private WebView webView;
	
	/**
	 * As the file content is loaded completely into RAM first, set 
	 * a limitation on the file size so we don't use too much RAM. If someone
	 * wants to load content that is larger than this, then a content
	 * provider should be used.
	 */
	static final int MAXFILESIZE = 16172;
	static final String LOGTAG = "CodeReader";
	private static final int PICK_REQUEST_CODE = 0;
	private boolean fileBrowsing = false;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}
	
	/**
	 * Modify the menus according to the searching mode and matches
	 */
	/*
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		return super.onPrepareOptionsMenu(menu);
	}
	*/
	
	@Override
	protected void onResume() {
		super.onResume();
		//CookieSyncManager.getInstance().startSync(); 
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		webView.saveState(outState);
	}

	@Override
	protected void onStop() {
		super.onStop();
		//CookieSyncManager.getInstance().stopSync(); 
		webView.stopLoading();	   
	}
	
	@Override
	protected void onDestroy() {
		super.onDestroy();
		webView.destroy();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch( item.getItemId() ) {
		case R.id.open_menu:
			openFileIntent();
			break;
		case R.id.quit_menu:
			quitApplication();
			break;
		}
		return false;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.v(LOGTAG," down, keycode reader: " + keyCode);
		if( !fileBrowsing ) {
			if( keyCode == KeyEvent.KEYCODE_BACK ) {
				openFileIntent();
				return true;
			}
		}

		return super.onKeyDown(keyCode, event);
	}

	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		Log.v(LOGTAG," up, keycode reader: " + keyCode);

		if( !fileBrowsing ) {
			if( keyCode == KeyEvent.KEYCODE_BACK ) {
				openFileIntent();
				return true;
			} else if( keyCode == KeyEvent.KEYCODE_SEARCH ) {
				openFileIntent();
				return true;
			}
		}
		return super.onKeyUp(keyCode, event); 		
	}
	
	/**
	 * Added to avoid refreshing the page on orientation change
	 * saw it on stackoverflow, dont remember wich article
	 */
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}
	
	/**
	 * Gets the result from the file picker activity
	 * thats the only intent im actually calling (and expecting results from) right now
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
		if (requestCode == PICK_REQUEST_CODE) {
			fileBrowsing = false;
			if (resultCode == RESULT_OK) {
				Uri uri = intent.getData();
				if (uri != null) {
					String path = uri.toString();
					if (path.toLowerCase().startsWith("file://")) {
						path = (new File(URI.create(path))).getAbsolutePath();
						loadFile(Uri.parse(path), "text/html");
					}
				}
			}
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		//CookieSyncManager.createInstance(this);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		webView = new WebView(this);
		setContentView(webView);
		
		// Setup callback support for title and progress bar
		webView.setWebViewClient( new ProgressWebViewClient() );
		
		// Configure the webview
		WebSettings s = webView.getSettings();
		s.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NORMAL);
		s.setUseWideViewPort(false);
		s.setAllowFileAccess(true);
		//s.setBuiltInZoomControls(true);
		s.setBuiltInZoomControls(false); //..
		s.setLightTouchEnabled(true);
		s.setLoadsImagesAutomatically(true);
		s.setPluginsEnabled(false);
		//s.setSupportZoom(true);
		s.setSupportZoom(false); //..
		s.setSupportMultipleWindows(true);
		s.setJavaScriptEnabled(true);
		
		
		// Restore a webview if we are meant to restore
		if (savedInstanceState != null) {
			webView.restoreState(savedInstanceState);
		} else {
			Intent intent = getIntent();
			if (intent.getData() != null) {
				Uri uri = intent.getData();
				if ("file".equals(uri.getScheme())) { //are we opening a file, or some data?
					loadFile(uri, intent.getType());
				} else {
					webView.loadUrl(intent.getData().toString());
				}
			} else {
				//Home Screen, Simple explanation
				//..webView.loadUrl("file:///android_asset/home.html");
				openFileIntent();
			}
		}
	}

	/**
	 * Get a Document Handler Depending on the filename extension
	 * @param filename The filename to retrieve the handler from
	 * @return The new document handler
	 */
	public DocumentHandler getHandlerByExtension(String filename) {
		DocumentHandler handler = null;
		
		if (filename.endsWith(".html") || filename.endsWith(".htm") || filename.endsWith(".xhtml")) {
			handler = new HtmlDocumentHandler();
		}
		if (filename.endsWith(".css")) handler = new CssDocumentHandler();
		if (filename.endsWith(".sql")) handler = new SqlDocumentHandler();
		if (filename.endsWith(".txt")) handler = new TextDocumentHandler();
		if (handler == null) handler = new NormalDocumentHandler();
		Log.v(LOGTAG," Handler: " + filename);
		return handler;
	}
	
	/**
	 * Call the intent to open files
	 */
	public void openFileIntent() {
		Intent fileIntent = new Intent( CodeReader.this, FileBrowser.class );
		fileBrowsing = true;
		startActivityForResult(fileIntent, PICK_REQUEST_CODE);
	}

	/***
	 * Closes the application
	 */
	public void quitApplication() {
		finish();
	}
	
	/**
	 * Load the HTML file into the webview by converting it to a data:
	 * URL. If there were any relative URLs, then they will fail as the
	 * webview does not allow access to the file:/// scheme for accessing 
	 * the local file system, 
	 * 
	 * Note: Before actually loading the info in webview, i add the prettify libraries to do the syntax highlight
	 * also i organize the data where it has to be. works fine now but it needs some work
	 * 
	 * @param uri file URI pointing to the content to be loaded
	 * @param mimeType mimetype provided
	 */
	private void loadFile(Uri uri, String mimeType) {
		String path = uri.getPath();
		DocumentHandler handler = getHandlerByExtension(path);
		
		File f = new File(path);
		final long length = f.length();
		if (!f.exists()) {
			Log.e(LOGTAG, "File doesnt exists: " + path);
			return;
		}
		
		if (handler == null) {
			Log.e(LOGTAG,"Filetype not supported");
			Toast.makeText(CodeReader.this, "Filetype not supported", 2000);
			return;
		}
		
		// typecast to int is safe as long as MAXFILESIZE < MAXINT
		byte[] array = new byte[(int)length];
		
		try {
			InputStream is = new FileInputStream(f);
			is.read(array);
			is.close();
		} catch (FileNotFoundException ex) {
			// Checked for file existance already, so this should not happen
			Log.e(LOGTAG, "Failed to access file: " + path, ex);
			return;
		} catch (IOException ex) {
			// read or close failed
			Log.e(LOGTAG, "Failed to access file: " + path, ex);
			return;
		}
		String contentString = "";
		setTitle("Narrative Code Reader - " + path);
		contentString += "<html><head><title>" + path + "</title>";

		contentString += "<link href='file:///android_asset/prettify.css' rel='stylesheet' type='text/css'/> ";
		contentString += "<style type='text/css'>";

		contentString += ".small { font-size: 8pt; }";
		contentString += ".midum { font-size: 10pt; }";
		contentString += ".large { font-size: 12pt; }";
		
		contentString += "</style>";

		contentString += "<script src='file:///android_asset/prettify.js' type='text/javascript'></script> ";
		contentString += handler.getFileScriptFiles();
		String fontSizeStyle = "small";
		contentString +=  "</head><body onload='prettyPrint()'><code class='" + handler.getFilePrettifyClass() + " " + fontSizeStyle + "'>";

		String sourceString = new String(array);
		
		contentString += handler.getFileFormattedString(sourceString);
		contentString += "</code> </html> ";
		webView.getSettings().setUseWideViewPort(true);
		webView.loadDataWithBaseURL("file:///android_asset/", contentString, handler.getFileMimeType(), "", "");
		Log.v(LOGTAG, "File Loaded: " + path);
	}

}
