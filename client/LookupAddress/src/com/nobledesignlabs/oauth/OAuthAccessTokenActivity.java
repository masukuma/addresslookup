package com.nobledesignlabs.oauth;

import java.net.URLDecoder;

import com.nobledesignlabs.utils.CommonStuff;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;

/**
 * Execute the OAuthRequestTokenTask to retrieve the request, and authorize the
 * request. After the request is authorized by the user, the callback URL will
 * be intercepted here.
 * 
 */
@SuppressLint("SetJavaScriptEnabled")
public class OAuthAccessTokenActivity extends Activity {

	private SharedPreferences prefs;
	private OAuth2Helper oAuth2Helper;
	private int resultcode=RESULT_CANCELED;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Log.i(CommonStuff.TAG, "Starting task to retrieve request token.");
		this.setTitle("Authentication");
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		oAuth2Helper = new OAuth2Helper(this.prefs);
		webview = new WebView(this);
		webview.getSettings().setJavaScriptEnabled(true);
		webview.setVisibility(View.VISIBLE);
		setContentView(webview);

		String authorizationUrl = oAuth2Helper.getAuthorizationUrl();
		Log.i(CommonStuff.TAG, "Using authorizationUrl = " + authorizationUrl);

		handled = false;

		webview.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageStarted(WebView view, String url, Bitmap bitmap) {
				Log.d(CommonStuff.TAG, "onPageStarted : " + url + " handled = "
						+ handled);
			}

			@Override
			public void onPageFinished(final WebView view, final String url) {
				Log.d(CommonStuff.TAG, "onPageFinished : " + url + " handled = "
						+ handled);

				if (url.startsWith(CommonStuff.OAUTH2PARAMS.getRederictUri())) {
					webview.setVisibility(View.INVISIBLE);

					if (!handled) {
						new ProcessToken(url, oAuth2Helper).execute();
					}
				} else {
					webview.setVisibility(View.VISIBLE);
				}
			}

		});

		webview.loadUrl(authorizationUrl);
	}

	private WebView webview;

	boolean handled = false;
	private boolean hasLoggedIn;

	@Override
	protected void onResume() {
		super.onResume();
		Log.i(CommonStuff.TAG, "onResume called with " + hasLoggedIn);
		if (hasLoggedIn) {
			finish();
		}
	}

	private class ProcessToken extends AsyncTask<Uri, Void, Void> {

		String url;
		boolean startActivity = false;

		public ProcessToken(String url, OAuth2Helper oAuth2Helper) {
			this.url = url;
		}

		@Override
		protected Void doInBackground(Uri... params) {

			if (url.startsWith(CommonStuff.OAUTH2PARAMS.getRederictUri())) {
				Log.i(CommonStuff.TAG, "Redirect URL found" + url);
				handled = true;
				try {
					if (url.indexOf("code=") != -1) {
						String authorizationCode = extractCodeFromUrl(url);

						Log.i(CommonStuff.TAG, "Found code = "
								+ authorizationCode);

						oAuth2Helper
								.retrieveAndStoreAccessToken(authorizationCode);
						startActivity = true;
						hasLoggedIn = true;
						resultcode = RESULT_OK;
					} else if (url.indexOf("error=") != -1) {
						startActivity = true;
						resultcode = RESULT_CANCELED;
					}

				} catch (Exception e) {
					e.printStackTrace();
				}

			} else {
				Log.i(CommonStuff.TAG, "Not doing anything for url " + url);
			}
			return null;
		}

		private String extractCodeFromUrl(String url) throws Exception {
			String encodedCode = url.substring(CommonStuff.OAUTH2PARAMS
					.getRederictUri().length() + 7, url.length());
			return URLDecoder.decode(encodedCode, "UTF-8");
		}

		@Override
		protected void onPreExecute() {

		}

		/**
		 * When we're done and we've retrieved either a valid token or an error
		 * from the server, we'll return to our original activity
		 */
		@Override
		protected void onPostExecute(Void result) {
			if (startActivity) {
				Log.i(CommonStuff.TAG, " ++++++++++++ Starting mainscreen again");
				// startActivity(new
				// Intent(OAuthAccessTokenActivity.this,MainActivity.class));
				Intent data = new Intent();
				// data.setData(Uri.parse());
				setResult(resultcode, data);
				finish();
			}

		}

	}
}
