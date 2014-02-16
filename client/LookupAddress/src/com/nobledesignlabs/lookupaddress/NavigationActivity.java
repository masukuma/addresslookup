package com.nobledesignlabs.lookupaddress;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.support.v7.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
//import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.InputFilter;
import android.text.Spanned;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.nobledesignlabs.entities.User;
import com.nobledesignlabs.lookupaddress.R;
import com.nobledesignlabs.maprelated.GMapV2Direction;
import com.nobledesignlabs.maprelated.GetDirectionsAsyncTask;
import com.nobledesignlabs.maprelated.NavigationBaseActivity;
import com.nobledesignlabs.oauth.OAuth2Helper;
import com.nobledesignlabs.oauth.Oauth2Params;
import com.nobledesignlabs.utils.CommonStuff;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.api.client.auth.oauth2.Credential;

public class NavigationActivity extends NavigationBaseActivity {
	// private static final LatLng PARIS = new LatLng(48.856132, 2.352448);
	// private static final LatLng FRANKFURT = new LatLng(50.111772, 8.682632);
	private SupportMapFragment fragment;
	private LatLngBounds latlngBounds;
	// private Button bNavigation;
	private Button bSearch;
	private JSONArray addresses;
	// private boolean isTravelingToParis = false;
	private int width, height;
	private SharedPreferences prefs;
	// private OAuth2Helper oAuth2Helper;
	private User currentuser;

	private SocketIO socket;
	private boolean mKeyboardShown = false;
	private List<String> suggest;
	private ArrayAdapter<String> aAdapter;
	private AutoCompleteTextView autoCompleteTextView1;
	private String data;
	// private String monikerhead;
	private Bundle mMainFragmentArgs;

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
	}

	public void saveMainFragmentState(Bundle args) {
		args.putSerializable("currentuser", currentuser);
		mMainFragmentArgs = args;
	}

	public Bundle getSavedMainFragmentState() {
		User u = (User) mMainFragmentArgs.getSerializable("currentuser");
		if (u != null) {
			currentuser = u;
		}
		return mMainFragmentArgs;
	}

	private void performSearch(String selection) {
		if (addresses != null) {
			for (int i = 0; i < addresses.length(); i++) {
				String suggestKey;
				try {
					JSONObject jaddress = addresses.getJSONObject(i);
					boolean isOwner = false;
					suggestKey = jaddress.getString("locationname");
					String userid = jaddress.getString("userid");
					if (userid.equals(currentuser.getId())) {
						isOwner = true;
					} else {
						isOwner = false;
					}
					try {
						if (socket == null || !socket.isConnected()) {
							createConnection();
						}

						if (!isOwner) {
							socket.emit("requestedaddress", jaddress,
									currentuser.toJSON(),
									CommonStuff.versionName,
									CommonStuff.versionNumber);
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}

					if (suggestKey.equals(selection)) {
						int typeofAddress = jaddress.getInt("typeofaddress");
						if (typeofAddress == CommonStuff.PRIVATE_ADDRESS
								&& !isOwner) {
							Toast.makeText(
									getBaseContext(),
									suggestKey
											+ " is a private address, the owner will be notified of your interest and will be asked to authorize it's usage",
									Toast.LENGTH_LONG).show();
							setNavigationButtonEnabled(false);

							if (socket == null || !socket.isConnected()) {
								createConnection();
							}
							socket.emit("privateaddressrequest", jaddress,
									currentuser.toJSON(),
									CommonStuff.versionName,
									CommonStuff.versionNumber);
						} else {
							setNavigationButtonEnabled(true);
							showLocation(jaddress);
						}
						break;
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	@Override
	protected int getLayoutResourceId() {
		return R.layout.activity_navigation;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// setContentView(R.layout.activity_navigation);
		Bundle b = getIntent().getExtras();
		if (b != null) {
			currentuser = (User) b.getSerializable("currentuser");
			if (currentuser != null) {
				// monikerhead = currentuser.getMoniker();
				Toast.makeText(getBaseContext(),
						"Welcome: " + currentuser.getName(), Toast.LENGTH_SHORT)
						.show();
			} else {
				Toast.makeText(getBaseContext(), "current user is null",
						Toast.LENGTH_SHORT).show();
			}
		}
		mMainFragmentArgs = new Bundle();
		this.setTitle("Search For Address");
	}

	protected void loadOtherUI() {
		createConnection();

		autoCompleteTextView1 = (AutoCompleteTextView) findViewById(R.id.autoCompleteTextView1);
		autoCompleteTextView1
				.setOnEditorActionListener(new TextView.OnEditorActionListener() {
					public boolean onEditorAction(TextView v, int actionId,
							KeyEvent event) {
						if (actionId == EditorInfo.IME_ACTION_SEARCH) {
							performSearch(autoCompleteTextView1.getText().toString());
							return true;
						}
						return false;
					}
				});

		//autoCompleteTextView1.setTextColor(Color.BLACK);
		InputFilter filter = new InputFilter() {
			// @Override
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					char character=source.charAt(i);
					if (!Character.isLetterOrDigit(character) && character != '@') {
						return "";
					}
				}
				return null;
			}

		};
		autoCompleteTextView1.setFilters(new InputFilter[] { filter });
		// autoCompleteTextView1.setDropDownBackgroundDrawable(d)

		autoCompleteTextView1
				.setOnItemSelectedListener(new OnItemSelectedListener() {

					public void onItemSelected(AdapterView<?> parent, View view,
							int position, long rowid) {
						String selection = (String)parent.getItemAtPosition(position);
						// TODO Auto-generated method stub
						// InputMethodManager in = (InputMethodManager)
						// getSystemService(Context.INPUT_METHOD_SERVICE);
						// in.hideSoftInputFromWindow(arg1.getWindowToken(), 0);
						performSearch(selection);
						CommonStuff.hideKeyboard(autoCompleteTextView1);
						autoCompleteTextView1.setDropDownHeight(0);
						mKeyboardShown = false;
					}

					public void onNothingSelected(AdapterView<?> arg0) {
						// TODO Auto-generated method stub

					}
				});

		autoCompleteTextView1.setOnItemClickListener(new OnItemClickListener() {

			public void onItemClick(AdapterView<?> parent, View view, int position,
					long rowId) {
				// InputMethodManager in = (InputMethodManager)
				// getSystemService(Context.INPUT_METHOD_SERVICE);
				// in.hideSoftInputFromWindow(arg1.getWindowToken(), 0);
				
				String selection = (String)parent.getItemAtPosition(position);
				
				performSearch(selection);
				CommonStuff.hideKeyboard(autoCompleteTextView1);
				autoCompleteTextView1.setDropDownHeight(0);
				mKeyboardShown = false;
			}

		});

		autoCompleteTextView1.addTextChangedListener(new TextWatcher() {

			public void afterTextChanged(Editable editable) {
				// TODO Auto-generated method stub
				if (!mKeyboardShown) {
					autoCompleteTextView1
							.setDropDownHeight(LinearLayout.LayoutParams.MATCH_PARENT);
					// CommonStuff.showKeyboard(autoCompleteTextView1);
					mKeyboardShown = true;
				}
			}

			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				// TODO Auto-generated method stub

			}

			public void onTextChanged(CharSequence s, int start, int before,
					int count) {

				String newText = s.toString();
				if (socket != null) {
					if (socket == null || !socket.isConnected()) {
						createConnection();
					}
					socket.emit("query", newText, currentuser,
							CommonStuff.versionName, CommonStuff.versionNumber);
				}
				// String[] parts = newText.split(",");
				// if (parts[parts.length - 1].length() > 1)
				// {
				// new getJson().execute(newText);
				// }
			}

		});

		autoCompleteTextView1.setOnKeyListener(new OnKeyListener() {
			//@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER) {
					return true;
				}
				return false;
			}
		});
	
	
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
					if (event.equals("queried")) {
						try {
							JSONArray jobj = (JSONArray) args[0];
							try {
								suggest = new ArrayList<String>();

								addresses = jobj;
								for (int i = 0; i < addresses.length(); i++) {
									String SuggestKey = addresses
											.getJSONObject(i).getString(
													"locationname");
									suggest.add(SuggestKey);
								}

							} catch (Exception e) {
								Log.w("Error", e.getMessage());
							}
							runOnUiThread(new Runnable() {
								public void run() {
									aAdapter = new ArrayAdapter<String>(
											getApplicationContext(),
											android.R.layout.simple_dropdown_item_1line,
											suggest);
									autoCompleteTextView1.setAdapter(aAdapter);
									aAdapter.notifyDataSetChanged();
								}
							});
						} catch (Exception c) {
							c.printStackTrace();
						}
					}
				}
			});

		} catch (Exception ex) {
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.navigation, menu);
		// createMenu(menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return menuChoice(item);
	}

	private boolean menuChoice(MenuItem item) {
		Bundle extras = new Bundle();
		Intent i;
		extras.putSerializable("currentuser", currentuser);
		switch (item.getItemId()) {
		case android.R.id.home:
			// Toast.makeText(this, "You clicked on the Application icon",
			// Toast.LENGTH_LONG).show();

			// go back home while clearing
			i = new Intent(this, MainActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			i.putExtras(extras);
			startActivity(i);
			finish();
			return true;
		case R.id.mnu_create:
			// Toast.makeText(this, "You Clicked Add",
			// Toast.LENGTH_LONG).show();
			i = new Intent(this, MainActivity.class);
			i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP
					| Intent.FLAG_ACTIVITY_SINGLE_TOP);
			i.putExtras(extras);
			startActivity(i);
			finish();
			return true;

		}
		return super.onOptionsItemSelected(item);
	}

}
