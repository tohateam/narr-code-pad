package jp.narr.reader;

import android.text.TextUtils;

public class CssDocumentHandler implements DocumentHandler {
	//@Override
	public String getFileFormattedString(String fileString) {
		return TextUtils.htmlEncode(fileString).replace("\n", "<br>");
	}

	//@Override
	public String getFileMimeType() {
		return "text/html";
	}

	//@Override
	public String getFilePrettifyClass() {
		return "prettyprint lang-css";
	}

	//@Override
	public String getFileScriptFiles() {
		return "<script src='file:///android_asset/lang-css.js' type='text/javascript'></script> ";
	}

}
