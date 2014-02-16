package com.nobledesignlabs.lookupaddress;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.concurrent.atomic.AtomicInteger;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.api.client.auth.oauth2.Credential;
import com.google.gson.Gson;
import com.nobledesignlabs.entities.User;
import com.nobledesignlabs.oauth.OAuth2Helper;
import com.nobledesignlabs.oauth.Oauth2Params;
import com.nobledesignlabs.utils.CommonStuff;
import com.nobledesignlabs.utils.GlobalClass;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputFilter;
import android.text.Spanned;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

/**
 * An example full-screen activity that shows and hides the system UI (i.e.
 * status bar and navigation/system bar) with user interaction.
 * 
 * @see SystemUiHider
 */
public class SplashActivity extends Activity {

	private SharedPreferences prefs;
	private OAuth2Helper oAuth2Helper;
	private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
	static final int REQUEST_CODE_RECOVER_PLAY_SERVICES = 1001;
	private GoogleCloudMessaging gcm;
	private AtomicInteger msgId = new AtomicInteger();
	private Context context;
	private String regid;
	private User currentuser;
	private TextView txtlbl;
	private SocketIO socket;
	public static final String PROPERTY_REG_ID = "registration_id";
	private static final String PROPERTY_APP_VERSION = "appVersion";

	private static final int REDIRECT_TO_MAIN = 0;
	private static final int REDIRECT_TO_LOGIN = 1;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_splash);
		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		context = getApplicationContext();
		txtlbl = (TextView) findViewById(R.id.txtlbl);
		if (txtlbl != null) {
			txtlbl.setText("Loading...");
		}
		if (checkPlayServices()) {
			DoStuffInBackground();
		} else {
			Log.i(CommonStuff.TAG, "No valid Google Play Services APK found.");
		}
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);

	}

	/**
	 * Stores the registration ID and the app versionCode in the application's
	 * {@code SharedPreferences}.
	 * 
	 * @param context
	 *            application's context.
	 * @param regId
	 *            registration ID
	 */
	private void storeRegistrationId(String regId) {
		// final SharedPreferences prefs = getGcmPreferences(context);
		int appVersion = CommonStuff.versionNumber;
		Log.i(CommonStuff.TAG, "Saving regId on app version " + appVersion);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString(PROPERTY_REG_ID, regId);
		editor.putInt(PROPERTY_APP_VERSION, appVersion);
		editor.commit();
	}

	private void DoStuffInBackground() {
		new AsyncTask<Void, Void, String>() {
			private int actiontotake = REDIRECT_TO_LOGIN;
			private Credential credential = null;
			private String apiResponse;

			@Override
			protected String doInBackground(Void... params) {
				String msg = "";
				PackageInfo pInfo;

				try {

					pInfo = context.getPackageManager().getPackageInfo(
							getPackageName(), 0);
					CommonStuff.versionName = pInfo.versionName;
					CommonStuff.versionNumber = pInfo.versionCode;
				} catch (NameNotFoundException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}

				// try {

				gcm = GoogleCloudMessaging.getInstance(context);
				regid = getRegistrationId();

				if (regid == null || "".equals(regid)) {
					runOnUiThread(new Runnable() {
						public void run() {
							if (txtlbl != null) {
								txtlbl.setText("registering device...");
							}
						}
					});
					/*
					 * regid = gcm.register(CommonStuff.SENDER_ID); if (regid ==
					 * null && "".equals(regid)) { gcm.unregister(); regid =
					 * gcm.register(CommonStuff.SENDER_ID); }
					 */

					int count = 0;
					int timeSleep = 1000;
					while (count < 5) {
						try {
							count++;
							Log.i(CommonStuff.TAG,
									"Starting GCM Registration Attempt #"
											+ count);
							regid = gcm.register(CommonStuff.SENDER_ID);
							if (regid == null || "".equals(regid)) {
								gcm.unregister();
								regid = gcm.register(CommonStuff.SENDER_ID);
							}
							if (regid != null && !"".equals(regid)) {
								break;
							}
						} catch (IOException e) {
							e.printStackTrace();
							Log.w(CommonStuff.TAG, e.getMessage());
						}

						Log.w(CommonStuff.TAG,
								"Registration failed.  Retrying in "
										+ timeSleep + " ms.");
						try {
							Thread.sleep(timeSleep);
						} catch (InterruptedException e) {
						}
					}

					msg = "Device registered, registration ID=" + regid;
					storeRegistrationId(regid);
				}
				/*
				 * runOnUiThread(new Runnable() { public void run() {
				 * Toast.makeText(getBaseContext(), "Your RegID is " + regid,
				 * Toast.LENGTH_SHORT) .show(); } });
				 */
				if (hasNotExpired()) {
					try {
						runOnUiThread(new Runnable() {
							public void run() {
								if (txtlbl != null) {
									txtlbl.setText("authenticating user...");
								}
							}
						});
						credential.refreshToken();
						if (credential.getAccessToken() == null) {
							System.err.println("Token not found");
							actiontotake = REDIRECT_TO_LOGIN;
						} else if (credential.getExpirationTimeMilliseconds() != null) {
							long expiresin = credential.getExpiresInSeconds();
							System.err.println("Token expires after "
									+ expiresin + " seconds");
							if (expiresin > 0) {
								actiontotake = REDIRECT_TO_MAIN;
							} else {
								actiontotake = REDIRECT_TO_LOGIN;
							}
						} else {
							actiontotake = REDIRECT_TO_MAIN;
						}

					} catch (Exception ex) {

					}
				} else {
					actiontotake = REDIRECT_TO_MAIN;
				}

				if (actiontotake == REDIRECT_TO_MAIN) {
					try {
						oAuth2Helper = new OAuth2Helper(prefs);
						apiResponse = oAuth2Helper.executeApiCall();
						Log.i(CommonStuff.TAG, "Received response from API : "
								+ apiResponse);
						actiontotake = REDIRECT_TO_MAIN;
					} catch (Exception ex) {
						ex.printStackTrace();
						apiResponse = ex.getMessage();
						actiontotake = REDIRECT_TO_LOGIN;
					}
				}

				if (actiontotake == REDIRECT_TO_MAIN) {
					runOnUiThread(new Runnable() {
						public void run() {
							if (txtlbl != null) {
								txtlbl.setText("initializing database connection...");
							}
						}
					});
					createConnection();
				}
				// Persist the regID - no need to register again.

				// } catch (IOException ex) {
				// msg = "Error :" + ex.getMessage();
				// If there is an error, don't just keep trying to register.
				// Require the user to click a button again, or perform
				// exponential back-off.
				// }
				return msg;
			}

			private boolean hasNotExpired() {
				try {
					credential = new OAuth2Helper(prefs,
							Oauth2Params.GOOGLE_PLUS).loadCredential();
					// credential.refreshToken();

					if (credential.getAccessToken() == null) {
						return false;
					} else if (credential.getExpirationTimeMilliseconds() != null) {
						long expiresin = credential.getExpiresInSeconds();
						System.err.println("Token expires after " + expiresin
								+ " seconds");
						if (expiresin > 0) {
							return true;
						} else {
							return false;
						}
					} else {
						return true;
					}
				} catch (Exception e) {
					// TODO Auto-generated catch block
					System.err.println("Imetupa mbao!!");
					e.printStackTrace();
					return false;
				}
			}

			@Override
			protected void onPostExecute(String msg) {
				if (actiontotake == REDIRECT_TO_LOGIN) {
					try {
						Intent i = new Intent(getApplicationContext(),
								AuthenticationActivity.class);
						i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
						startActivity(i);
						finish();
					} catch (Exception c) {
						c.printStackTrace();
					}
				} else {
					final GlobalClass globalVariable = (GlobalClass) getApplicationContext();
					currentuser = new User(apiResponse);
					currentuser.setRegistrationID(regid);

					runOnUiThread(new Runnable() {
						public void run() {
							if (txtlbl != null) {
								txtlbl.setText("user " + currentuser.getName()
										+ " found...");
							}
						}
					});

					globalVariable.setCurrentUser(currentuser);
					try {
						if (socket == null || !socket.isConnected()) {
							createConnection();
						}
						socket.emit("login", currentuser.toJSON(),
								CommonStuff.versionName,
								CommonStuff.versionNumber);
					} catch (Exception d) {
					}
				}
				// mDisplay.append(msg + "\n");
			}
		}.execute(null, null, null);
	}

	/**
	 * Gets the current registration ID for application on GCM service, if there
	 * is one.
	 * <p>
	 * If result is empty, the app needs to register.
	 * 
	 * @return registration ID, or empty string if there is no existing
	 *         registration ID.
	 */
	private String getRegistrationId() {
		// final SharedPreferences prefs = getGcmPreferences(context);
		String registrationId = prefs.getString(PROPERTY_REG_ID, "");
		if (registrationId == null && "".equals(registrationId)) {
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

	private boolean checkPlayServices() {
		int resultCode = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(this);
		if (resultCode != ConnectionResult.SUCCESS) {
			if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
				switch (resultCode) {
				case ConnectionResult.SERVICE_MISSING:
				case ConnectionResult.SERVICE_DISABLED:
				case ConnectionResult.SERVICE_INVALID:
					Toast.makeText(getApplicationContext(),
							"Unable to load Google Play Services",
							Toast.LENGTH_SHORT).show();
					break;
				case ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED:
					Toast.makeText(getApplicationContext(),
							"Please update Google Play Services",
							Toast.LENGTH_SHORT).show();
					break;
				}
				GooglePlayServicesUtil.getErrorDialog(resultCode, this,
						REQUEST_CODE_RECOVER_PLAY_SERVICES).show();
			} else {
				Log.i(CommonStuff.TAG, "This device is not supported.");
				finish();
			}
			return false;
		}
		return true;
	}

	private void createConnection() {
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
						// User loggedonuser = null;
						Object[] arguments = args;
						try {
							Gson gson = new Gson();
							currentuser = gson.fromJson(
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
							extras.putSerializable("currentuser", currentuser);
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
					try {
						if (currentuser != null) {
							String smoniker = input.getText().toString().trim()
									.toLowerCase();
							currentuser.setMoniker(smoniker);

							if ("".equals(currentuser.getName())
									|| currentuser.getName() == null) {
								currentuser.setName(smoniker);
							}

							if (socket == null || !socket.isConnected()) {
								createConnection();
							}
							socket.emit("createlogin", currentuser.toJSON(),
									CommonStuff.versionName,
									CommonStuff.versionNumber);
						}
					} catch (JSONException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
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
					.setPositiveButton("Register",
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
										String smoniker = input.getText()
												.toString().trim()
												.toLowerCase();
										currentuser.setMoniker(smoniker);

										if ("".equals(currentuser.getName())
												|| currentuser.getName() == null) {
											currentuser.setName(smoniker);
										}
										if (socket == null
												|| !socket.isConnected()) {
											createConnection();
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
			final AlertDialog d1 = new AlertDialog.Builder(this)
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
										if (socket == null
												|| !socket.isConnected()) {
											createConnection();
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
}
