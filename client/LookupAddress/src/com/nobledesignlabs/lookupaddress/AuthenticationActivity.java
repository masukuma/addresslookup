package com.nobledesignlabs.lookupaddress;

import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;
import io.socket.IOAcknowledge;

import java.io.Console;
import java.io.IOException;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicInteger;

//import org.ifes.businessobjects.PollingStationSummary;
import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.api.client.auth.oauth2.Credential;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.nobledesignlabs.entities.User;
import com.nobledesignlabs.oauth.OAuth2Helper;
import com.nobledesignlabs.oauth.OAuthAccessTokenActivity;
import com.nobledesignlabs.oauth.Oauth2Params;
import com.nobledesignlabs.utils.CommonStuff;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

public class AuthenticationActivity extends Activity {
	private Button btnOAuthGooglePlus;
	private int request_Code = 0;
	private SharedPreferences prefs;
	private OAuth2Helper oAuth2Helper;
	private SocketIO socket;
	private User currentuser;
	private View dialoglayout = null;
	private String regid;
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// ---save whatever you need to persist---
		outState.putSerializable("currentuser", currentuser);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// ---retrieve the information persisted earlier---
		currentuser = (User) savedInstanceState.getSerializable("currentuser");
		// setMonikerHead();
	}

	/**
	 * Check the device to make sure it has the Google Play Services APK. If it
	 * doesn't, display a dialog that allows users to download the APK from the
	 * Google Play Store or enable it in the device's system settings.
	 */

	/**
	 * Registers the application with GCM servers asynchronously.
	 * <p>
	 * Stores the registration ID and the app versionCode in the application's
	 * shared preferences.
	 */

	/**
	 * Sends the registration ID to your server over HTTP, so it can use
	 * GCM/HTTP or CCS to send messages to your app. Not needed for this demo
	 * since the device sends upstream messages to a server that echoes back the
	 * message using the 'from' address in the message.
	 */
	// private void sendRegistrationIdToBackend() {
	// Your implementation here.
	// socket.emit("login", currentuser.toJSON(),
	// Constants.versionName, Constants.versionNumber);
	// }

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_authentication);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);

		CommonStuff.OAUTH2PARAMS = Oauth2Params.GOOGLE_PLUS;
		this.setTitle("Authentication");
		regid = getRegistrationId();
		connectToServer();
		btnOAuthGooglePlus = (Button) findViewById(R.id.btn_oauth_googleplus);
		if (btnOAuthGooglePlus != null) {
			btnOAuthGooglePlus.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {
					startOauthFlow(Oauth2Params.GOOGLE_PLUS);
				}
			});
		}
		// Check device for Play Services APK. If check succeeds, proceed with
		// GCM registration.

	}

	private String getRegistrationId() {
		// final SharedPreferences prefs = getGcmPreferences(context);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId == null || "".equals(registrationId)) {
			Log.i(CommonStuff.TAG, "Registration not found.");
			return "";
		}
		// Check if app was updated; if so, it must clear the registration ID
		// since the existing regID is not guaranteed to work with the new
		// app version.
		int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION,
				Integer.MIN_VALUE);
		int currentVersion = CommonStuff.versionNumber;
		if (registeredVersion != currentVersion) {
			Log.i(CommonStuff.TAG, "App version changed.");
			return "";
		}
		return registrationId;
	}

	private void connectToServer() {
		// TODO Auto-generated method stub
		try {
			socket = new SocketIO(CommonStuff.serverName);

			socket.connect(new IOCallback() {

				public void onMessage(JSONObject json, IOAcknowledge ack) {
					try {
						System.out.println("Server said:" + json.toString(2));

					} catch (JSONException e) {
						e.printStackTrace();
					}
				}

				public void onMessage(String data, IOAcknowledge ack) {
					System.out.println("Server said: " + data);
				}

				public void onError(SocketIOException socketIOException) {
					System.out.println("an Error occured");
					socketIOException.printStackTrace();
				}

				public void onDisconnect() {
					System.out.println("Connection terminated.");
				}

				public void onConnect() {
					System.out.println("Connection established");
				}

				public void on(String event, IOAcknowledge ack, Object... args) {
					System.out
							.println("Server triggered event '" + event + "'");
					if (event.equals("loggedin")) {
						User loggedonuser = null;
						Object[] arguments = args;
						try {
							Gson gson = new Gson();
							loggedonuser = gson.fromJson(
									arguments[0].toString(), User.class);
						} catch (Exception d) {
							d.printStackTrace();
						}

						try {
							Intent i = new Intent(getApplicationContext(),
									NavigationActivity.class);
							i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
									| Intent.FLAG_ACTIVITY_SINGLE_TOP);

							// System.out.println("Results sent back ");
							// CurrentUser user= new CurrentUser(args);
							Bundle extras = new Bundle();
							extras.putSerializable("currentuser", loggedonuser);
							i.putExtras(extras);
							startActivity(i);
							finish();
						} catch (Exception c) {
							c.printStackTrace();
						}
					} else if (event.equals("nosuchuser")) {
						// User loggedonuser = null;
						Object[] arguments = args;
						try {
							Gson gson = new Gson();
							currentuser = gson.fromJson(
									arguments[0].toString(), User.class);
							currentuser.setRegistrationID(regid);
						} catch (Exception d) {
							d.printStackTrace();
						}

						runOnUiThread(new Runnable() {
							public void run() {
								showDialog(0);
							}
						});
					} else if (event.equals("logincreationfailed")) {
						Object[] arguments = args;
						try {
							Gson gson = new Gson();
							currentuser = gson.fromJson(
									arguments[0].toString(), User.class);
						} catch (Exception d) {
							d.printStackTrace();
						}

						runOnUiThread(new Runnable() {
							public void run() {
								showDialog(1);
							}
						});
					}
				}
			});

		} catch (MalformedURLException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
	}

	private void enableButton(Credential credential) {
		System.out.println(credential.getTokenServerEncodedUrl());
		oAuth2Helper = new OAuth2Helper(this.prefs);
		runOnUiThread(new Runnable() {
			public void run() {
				btnOAuthGooglePlus.setText("Please Wait...");
			}
		});
		performApiCall();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.authentication, menu);
		return true;
	}

	private void startOauthFlow(Oauth2Params oauth2Params) {
		// CommonStuff.OAUTH2PARAMS = oauth2Params;
		Intent i = new Intent().setClass(this, OAuthAccessTokenActivity.class);

		startActivityForResult(i, request_Code);
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == request_Code) {
			if (resultCode == RESULT_OK) {
				Credential credential = null;
				try {
					credential = new OAuth2Helper(this.prefs,
							Oauth2Params.GOOGLE_PLUS).loadCredential();

					if (credential != null) {
						enableButton(credential);
					}

				} catch (IOException e) {
					// TODO Auto-generated catch block
					System.err.println("Imetupa mbao tena!!");
					e.printStackTrace();
				}

			} else {

			}
		}
	}

	/**
	 * Performs an authorized API call.
	 */
	private void performApiCall() {
		new ApiCallExecutor().execute();
	}

	private class ApiCallExecutor extends AsyncTask<Uri, Void, Void> {

		String apiResponse = null;

		@Override
		protected Void doInBackground(Uri... params) {

			try {
				apiResponse = oAuth2Helper.executeApiCall();
				Log.i(CommonStuff.TAG, "Received response from API : "
						+ apiResponse);
			} catch (Exception ex) {
				ex.printStackTrace();
				apiResponse = ex.getMessage();
			}
			return null;
		}

		@Override
		protected void onPostExecute(Void result) {
			// String str = apiResponse;
			currentuser = new User(apiResponse);
			currentuser.setRegistrationID(regid);
			try {
				if (socket == null || !socket.isConnected()) {
					connectToServer();
				}
				socket.emit("login", currentuser.toJSON(),
						CommonStuff.versionName, CommonStuff.versionNumber);
			} catch (Exception d) {
			}
		}

	}

	@Override
	protected Dialog onCreateDialog(int id) {
		final EditText input = new EditText(this);
		InputFilter filter = new InputFilter() {
			// @Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (!Character.isLetterOrDigit(source.charAt(i))) {
						return "";
					}
				}
				return null;
			}

		};
		input.setFilters(new InputFilter[] { filter });
		input.setImeOptions(EditorInfo.IME_ACTION_DONE);
		input.setOnEditorActionListener(new EditText.OnEditorActionListener() {
			// @Override
			public boolean onEditorAction(TextView v, int actionId,
					KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_SEARCH
						|| actionId == EditorInfo.IME_ACTION_DONE
						|| event.getAction() == KeyEvent.ACTION_DOWN
						&& event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
					onSearchAction(v);
					return true;
				}
				return false;
			}

		});
		switch (id) {
		case 0:

			final AlertDialog d = new AlertDialog.Builder(this)
					.setIcon(R.drawable.direction_uturn)
					.setTitle("Welcome " + currentuser.getName())
					.setMessage(
							"As a first time user you are required to create a moniker for your account")
					.setView(input)
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).create();

			d.setOnShowListener(new DialogInterface.OnShowListener() {

				public void onShow(DialogInterface dialog) {

					Button b = d.getButton(AlertDialog.BUTTON_POSITIVE);
					b.setOnClickListener(new View.OnClickListener() {

						public void onClick(View view) {
							// TODO Do something
							if ("".equals(input.getText().toString().trim())) {
								Toast.makeText(getBaseContext(),
										"The Moniker cannot be empty",
										Toast.LENGTH_SHORT).show();
							} else {
								try {
									if (currentuser != null) {
										currentuser.setMoniker(input
												.getText().toString()
												.toLowerCase().trim());
										if (socket == null
												|| !socket.isConnected()) {
											connectToServer();
										}

										socket.emit("createlogin",
												currentuser.toJSON(),
												CommonStuff.versionName,
												CommonStuff.versionNumber);
									}
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								// Dismiss once everything is OK.
								d.dismiss();
							}
						}
					});
				}
			});
			
			return d;

		case 1:
			final AlertDialog d1= new AlertDialog.Builder(this)
					.setIcon(R.drawable.direction_uturn)
					.setTitle("Welcome " + currentuser.getName())
					.setMessage(
							"The '"
									+ currentuser.getMoniker()
									+ "' moniker has already been used, use someting else as a moniker for your account")
					.setView(input)
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							}).create();

			d1.setOnShowListener(new DialogInterface.OnShowListener() {

				public void onShow(DialogInterface dialog) {

					Button b = d1.getButton(AlertDialog.BUTTON_POSITIVE);
					b.setOnClickListener(new View.OnClickListener() {

						public void onClick(View view) {
							// TODO Do something
							if ("".equals(input.getText().toString().trim())) {
								Toast.makeText(getBaseContext(),
										"The Moniker cannot be empty",
										Toast.LENGTH_SHORT).show();
							} else {
								try {
									if (currentuser != null) {
										String smoniker = input.getText()
												.toString().trim()
												.toLowerCase();
										currentuser.setMoniker(smoniker);

										if ("".equals(currentuser.getName())
												|| currentuser.getName() == null) {
											currentuser.setName(smoniker);
										}
										socket.emit("createlogin",
												currentuser.toJSON(),
												CommonStuff.versionName,
												CommonStuff.versionNumber);
									}
								} catch (JSONException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
								// Dismiss once everything is OK.
								d1.dismiss();
							}
						}
					});
				}
			});
			
			return d1;

		}

		return null;
	}

	private void onSearchAction(TextView v) {
		// TODO Auto-generated method stub
		try {
			if (currentuser != null) {
				String smoniker = v.getText().toString().trim().toLowerCase();
				currentuser.setMoniker(smoniker);

				if ("".equals(currentuser.getName())
						|| currentuser.getName() == null) {
					currentuser.setName(smoniker);
				}
				if (socket == null || !socket.isConnected()) {
					connectToServer();
				}

				socket.emit("createlogin", currentuser.toJSON(),
						CommonStuff.versionName, CommonStuff.versionNumber);
			}
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
