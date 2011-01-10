package jp.narr.reader;

import android.app.ProgressDialog;
import android.graphics.Bitmap;
import android.webkit.WebView;
import android.webkit.WebViewClient;

class ProgressWebViewClient extends WebViewClient {
	private ProgressDialog progressDialog;

	@Override
	public boolean shouldOverrideUrlLoading(WebView view, String url) {
		return false;
	}
    
	@Override
	public void onPageFinished(WebView view, String url) {
		if (progressDialog != null) {
			progressDialog.dismiss();
		}
		progressDialog = null;
		super.onPageFinished(view, url);
	}
    
	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		progressDialog = ProgressDialog.show( view.getContext(),
											  "Please wait...", 
											  "Opening File...", 
											  true );
		super.onPageStarted(view, url, favicon);
	}
 }   
