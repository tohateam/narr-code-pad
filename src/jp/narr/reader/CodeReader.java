package jp.narr.reader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.*;
import java.util.*;

import java.net.URI;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.Window;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import android.text.TextUtils;

import org.mozilla.universalchardet.UniversalDetector;


/**
 * @author Kosuke Miyoshi
 *
 * I wrote this code based on Cosme Zamudio's Android CodePad.
 * I made some changes for usability, and preferences.
 *
 * @author Cosme Zamudio - Android SDK Examples
 * I Grabbed this class from the SDK Examples, i actually made a lot of changes, so it doesnt look like the original one.
 * it has the prettify functionality inside the class, it may look ugly.. but it works fine and its well organized (thats what i think)
 * Note: Im Leaving the original comments, they might come in handy for other users reading the code 
 *
 * Wraps a WebView widget within an Activity. When launched, it uses the 
 * URI from the intent as the URL to load into the WebView. 
 * It supports all URLs schemes that a standard WebView supports, as well as
 * loading the top level markup using the file scheme.
 * The WebView default settings are used with the exception of normal layout 
 * is set.
 * This activity shows a loading progress bar in the window title and sets
 * the window title to the title of the content.
 *
 */
public class CodeReader extends Activity {
	private static final int FONT_SMALL  = 0;
	private static final int FONT_MEDIUM = 1;
	private static final int FONT_LARGE  = 2;

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
	//private boolean menuOpened = false;
	private SharedPreferences prefs;
	private int fontType = FONT_SMALL;
	private int tabSize = 4;
	private boolean highlighting = true;
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);
		return true;
	}

	private void loadPreference() {
		fontType = prefs.getInt("font", FONT_SMALL );
		tabSize = prefs.getInt("tab", 4 );
		highlighting = prefs.getBoolean("highlight", true );
	}
	
	private void saveFontPreference() {
		SharedPreferences.Editor ed = prefs.edit();
		ed.putInt("font", fontType);
		ed.commit();
	}

	private void saveTabPreference() {
		SharedPreferences.Editor ed = prefs.edit();
		ed.putInt("tab", tabSize);
		ed.commit();		
	}

	private void saveHighlightingPreference() {
		SharedPreferences.Editor ed = prefs.edit();
		ed.putBoolean("highlight", highlighting);
		ed.commit();		
	}
	
	/*
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Log.v(LOGTAG," menu opened");
		menuOpened = true;
		return super.onPrepareOptionsMenu(menu);
	}
	public void onOptionsMenuClosed(Menu menu) {
		Log.v(LOGTAG," menu closed");
		menuOpened = false;
		super.onOptionsMenuClosed(menu);
	}
	*/
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		webView.saveState(outState);
	}

	@Override
	protected void onStop() {
		super.onStop();
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
			
		case R.id.small_font:
			fontType = FONT_SMALL;
			saveFontPreference();
			reload();
			break;
		case R.id.medium_font:
			fontType = FONT_MEDIUM;
			saveFontPreference();
			reload();
			break;
		case R.id.large_font:
			fontType = FONT_LARGE;
			saveFontPreference();
			reload();
			break;
			
		case R.id.tab2:
			tabSize = 2;
			saveTabPreference();
			reload();
			break;
		case R.id.tab4:
			tabSize = 4;
			saveTabPreference();
			reload();
			break;
		case R.id.tab8:
			tabSize = 8;
			saveTabPreference();
			reload();
			break;

		case R.id.highlight_on:
			highlighting = true;
			saveHighlightingPreference();
			reload();
			break;
		case R.id.highlight_off:
			highlighting = false;
			saveHighlightingPreference();
			reload();
			break;
			
		case R.id.quit_menu:
			Log.v(LOGTAG," menu quit: ");
			quitApplication();
			break;
		}
		return false;
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Log.v(LOGTAG," down, keycode reader: " + keyCode);
		//if( !fileBrowsing && !menuOpened ) {
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
			if( keyCode == KeyEvent.KEYCODE_SEARCH ) {
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

		requestWindowFeature( Window.FEATURE_NO_TITLE );

		prefs = getSharedPreferences( "pref",
									  MODE_WORLD_READABLE | MODE_WORLD_WRITEABLE );
		loadPreference();

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

	private Uri lastURI = null;
	private String lastMIMEType = null;

	private void reload() {
		if( lastURI != null ) {
			loadFile(lastURI, lastMIMEType);
		}
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
		lastURI = uri;
		lastMIMEType = mimeType;

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

		//long time0 = System.currentTimeMillis();

		String encoding = null;
		UniversalDetector detector = new UniversalDetector(null);
		detector.handleData(array, 0, (int)length);
		detector.dataEnd();
		encoding = detector.getDetectedCharset();
		Log.v(LOGTAG, "encoding: " + encoding);

		//long time1 = System.currentTimeMillis();
		//Log.v(LOGTAG, "time: " + (time1-time0));

		String sourceString = "";
		try{
			if( encoding != null ) {
				sourceString = new String(array, encoding);
			} else {
				sourceString = new String(array);
			}
		} catch(Exception e) {
			Log.e(LOGTAG, "Failed to decode: ", e);
			sourceString = new String(array);
		}

		// internal encoding of Android seems to be UTF-8, so now sourceString is in UTF-8

		String contentString = "";
		setTitle("Narr CodePad - " + path);

		contentString += "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">"; //..

		contentString += "<html><head>";
		contentString += ("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=\"UTF-8\">");
		contentString += "<title>" + path + "</title>";

		contentString += "<link href='file:///android_asset/prettify.css' rel='stylesheet' type='text/css'/> ";
		contentString += "<style type='text/css'>";

		contentString += ".small { font-size: 8pt; }";
		contentString += ".medium { font-size: 10pt; }";
		contentString += ".large { font-size: 12pt; }";
		
		contentString += "</style>";

		if( highlighting ) {
			contentString += "<script src='file:///android_asset/prettify.js' type='text/javascript'></script> ";
			contentString += handler.getFileScriptFiles();
			contentString += "<script type='text/javascript'>";
			contentString += "window['PR_TAB_WIDTH'] = " + tabSize + ";";
			contentString += "</script> ";
		}

		String fontSizeStyle;

		switch(fontType) {
		case FONT_SMALL:
			fontSizeStyle = "small";
			break;
		case FONT_MEDIUM:
			fontSizeStyle = "medium";
			break;
		case FONT_LARGE:
			fontSizeStyle = "large";
			break;
		default:
			fontSizeStyle = "medium";
		}

		if( highlighting ) {
			contentString += "</head><body onload='prettyPrint()'><code class='" + handler.getFilePrettifyClass() + " " + fontSizeStyle + "'>";
			contentString += handler.getFileFormattedString(sourceString);
			contentString += "</code> </html> ";
		} else {
			contentString += "</head><pre><body><code class='" + fontSizeStyle + "'>";
			//contentString += sourceString;
			contentString += getTabbedString(sourceString, tabSize);
			contentString += "</code></pre></html> ";
		}
		webView.getSettings().setUseWideViewPort(true);
		webView.loadDataWithBaseURL("file:///android_asset/", contentString, handler.getFileMimeType(), "UTF-8", "");
		Log.v(LOGTAG, "File Loaded: " + path);
	}

	private char buffer[];

	private String getTabbedString( String str, int tabSize ) {
		if( buffer == null ) {
			buffer = new char[1024];
		}

		StringTokenizer st = new StringTokenizer(str, "\n");
		StringBuilder sb = new StringBuilder();
		while( st.hasMoreTokens() ) {
			String line = st.nextToken();
			line = TextUtils.htmlEncode(line); //.. 
			line = processTabLine(line, tabSize);
			sb.append(line);
		}
		return sb.toString();
	}

	private String processTabLine(String str, int tabSize) {
		int size = str.length();
		if( size > buffer.length-128 ) {
			buffer = new char[size+256];
		}
		
		int bufPos = 0;
		for(int i=0; i<size; ++i) {
			char ch = str.charAt(i);
			if( ch == '\t' ) {
				int padding = tabSize - (bufPos % tabSize);
				for(int j=0; j<padding; ++j) {
					buffer[bufPos] = ' ';
					bufPos++;
				}
			} else {
				buffer[bufPos] = ch;
				bufPos++;
			}
		}
		buffer[bufPos] = '\n';
		bufPos++;
		return new String(buffer, 0, bufPos);
	}

}
