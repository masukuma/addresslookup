package com.nobledesignlabs.lookupaddress;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.SnapshotReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.api.client.auth.oauth2.Credential;
import com.nobledesignlabs.entities.Address;
import com.nobledesignlabs.entities.User;
import com.nobledesignlabs.lookupaddress.R;
import com.nobledesignlabs.maprelated.GeocodeJSONParser;
import com.nobledesignlabs.maprelated.PlaceJSONParser;
import com.nobledesignlabs.oauth.OAuth2Helper;
import com.nobledesignlabs.oauth.Oauth2Params;
import com.nobledesignlabs.utils.CommonStuff;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBar;
import android.text.Editable;
import android.text.TextWatcher;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ActionBarActivity implements LocationListener {
	static final int READ_BLOCK_SIZE = 100;
	private int request_Code = 0;
	private GoogleMap mMap;

	private String[] locations;
	private Marker currentLocationMarker;
	private Marker selectedLocationMarker;
	private String selecteditem = "";
	private LatLng selectedLocation = null;
	private LatLng locationdata = null;
	private String monikerhead;
	private SharedPreferences prefs;
	private User currentuser;
	private SocketIO socket;
	private View dialoglayout;
	private Address taddress;
	private TextView tv_latitude;
	private TextView tv_lngitude;
	private String[] typesofsecurity;
	private Spinner dialogspinner;
	private CheckBox checkbox;
	private int typeofaddress;
	private Bitmap thumbnail;
	// private AutoCompleteTextView autoCompleteTextView1;
	private boolean mKeyboardShown = false;
	private AutoCompleteTextView atvPlaces;
	private PlacesTask placesTask;
	private ParserTask parserTask;
	private LatLngBounds latlngBounds;

	private int width;
	private int height;

	// CLIENT ID - 1048500612993.apps.googleusercontent.com
	// client secret - 9dwFEpZSAZEZRcFf12pe1tqd
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
		if (currentuser != null)
			monikerhead = currentuser.getMoniker();

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setTheme(android.R.style.Theme);
		setContentView(R.layout.activity_main);

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);
		// LoginDetails details = loadPreferences();

		this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
		locations = getResources().getStringArray(R.array.typeofaddress_array);

		this.setTitle("Create Addresses");
		// this.prefs.
		Credential credential = null;
		try {
			credential = new OAuth2Helper(this.prefs, Oauth2Params.GOOGLE_PLUS)
					.loadCredential();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (credential != null) {

			Bundle b = getIntent().getExtras();
			if (b != null) {
				currentuser = (User) b.getSerializable("currentuser");
				if (currentuser != null) {
					monikerhead = currentuser.getMoniker();
					Toast.makeText(getBaseContext(),
							"Welcome: " + currentuser.getName(),
							Toast.LENGTH_SHORT).show();
				} else {
					Toast.makeText(getBaseContext(), "current user is null",
							Toast.LENGTH_SHORT).show();
				}
			}
			final Spinner addressesSpinner = (Spinner) findViewById(R.id.spinner1);
			ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
					android.R.layout.simple_spinner_item, locations);
			addressesSpinner.setAdapter(adapter);
			addressesSpinner.setOnItemSelectedListener(new addressSelected());

			// autocomplete
			atvPlaces = (AutoCompleteTextView) findViewById(R.id.et_place);

			atvPlaces
					.setOnEditorActionListener(new TextView.OnEditorActionListener() {
						public boolean onEditorAction(TextView v, int actionId,
								KeyEvent event) {
							if (actionId == EditorInfo.IME_ACTION_SEARCH) {
								String text = atvPlaces.getText().toString();
								// String text = (String) hm.get("description");
								PerformSearch(text);

								// performSearch(selection);
								CommonStuff.hideKeyboard(atvPlaces);
								atvPlaces.setDropDownHeight(0);
								return true;
							}
							return false;
						}
					});
			atvPlaces.setTextColor(Color.BLACK);
			// atvPlaces.setdr

			atvPlaces.setOnItemSelectedListener(new OnItemSelectedListener() {

				public void onItemSelected(AdapterView<?> parent, View view,
						int position, long rowid) {

					// TODO Auto-generated method stub
					// InputMethodManager in = (InputMethodManager)
					// getSystemService(Context.INPUT_METHOD_SERVICE);
					// in.hideSoftInputFromWindow(arg1.getWindowToken(),
					// 0);
					// performSearch(selection);

					HashMap hm = (HashMap) parent.getItemAtPosition(position);
					String text = (String) hm.get("description");
					PerformSearch(text);
					CommonStuff.hideKeyboard(atvPlaces);
					atvPlaces.setDropDownHeight(0);
					mKeyboardShown = false;
				}

				public void onNothingSelected(AdapterView<?> arg0) {
					// TODO Auto-generated method stub

				}
			});

			atvPlaces.setOnItemClickListener(new OnItemClickListener() {

				public void onItemClick(AdapterView<?> parent, View view,
						int position, long rowId) {
					// InputMethodManager in = (InputMethodManager)
					// getSystemService(Context.INPUT_METHOD_SERVICE);
					// in.hideSoftInputFromWindow(arg1.getWindowToken(),
					// 0);

					HashMap hm = (HashMap) parent.getItemAtPosition(position);
					String text = (String) hm.get("description");
					PerformSearch(text);

					// performSearch(selection);
					CommonStuff.hideKeyboard(atvPlaces);
					atvPlaces.setDropDownHeight(0);
					mKeyboardShown = false;
				}

			});

			atvPlaces.addTextChangedListener(new TextWatcher() {

				public void afterTextChanged(Editable editable) {
					// TODO Auto-generated method stub

					if (!mKeyboardShown) {
						atvPlaces
								.setDropDownHeight(LinearLayout.LayoutParams.MATCH_PARENT);
						// CommonStuff.showKeyboard(autoCompleteTextView1);
						mKeyboardShown = true;
					}
				}

				public void beforeTextChanged(CharSequence s, int start,
						int count, int after) {
					// TODO Auto-generated method stub

				}

				public void onTextChanged(CharSequence s, int start,
						int before, int count) {

					String newText = s.toString();
					if (newText.trim().length() > 3) {
						placesTask = new PlacesTask();
						placesTask.execute(newText);
						atvPlaces.setTextColor(Color.BLACK);
					}
					// String[] parts = newText.split(",");
					// if (parts[parts.length - 1].length() > 1)
					// {
					// new getJson().execute(newText);
					// }
				}

			});

			atvPlaces.setOnKeyListener(new OnKeyListener() {
				// @Override
				public boolean onKey(View v, int keyCode, KeyEvent event) {
					if (keyCode == KeyEvent.KEYCODE_ENTER) {
						return true;
					}
					return false;
				}
			});

			final MainActivity thiswnd = this;
			createSockets();

			final Button btn1 = (Button) findViewById(R.id.bCreate);
			btn1.setOnClickListener(new View.OnClickListener() {
				public void onClick(View v) {

					try {

						Address[] addresses = currentuser.getAddresses();
						for (int i = 0; i < addresses.length; i++) {
							Address address = addresses[i];

							if (address != null) {
								if (selecteditem.toLowerCase().equals(
										address.getName().toLowerCase())) {
									String addressname = address
											.getLocationname();
									address.setAltitude(0);
									address.setLatitude(selectedLocation.latitude);
									address.setLongitude(selectedLocation.longitude);
									address.setName(selecteditem);
									address.setRegistrationID(currentuser
											.getRegistrationID());
									address.setUserid(currentuser.getId());
									address.setLocationname(addressname);

									taddress = address;
									// djsjd;
									// showDialog(0);
									Dialog d = onCreateDialog(0);
									d.show();
									break;
								}
							}
						}

					} catch (Exception d) {
						d.printStackTrace();
					}
				}
			});
			// showMap();
			int status = GooglePlayServicesUtil
					.isGooglePlayServicesAvailable(getBaseContext());

			// Showing status
			if (status != ConnectionResult.SUCCESS) {
				int rrequestCode = 10;
				Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status,
						this, rrequestCode);
				dialog.show();

			} else { // Google Play Services are available
				showMap();
			}

		} else {
			Intent i = new Intent(this, AuthenticationActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			Bundle extras = new Bundle();
			extras.putSerializable("currentuser", currentuser);
			i.putExtras(extras);
			startActivity(i);
		}
	}

	protected void PerformSearch(String location) {
		// TODO Auto-generated method stub
		if (location == null || location.equals("")) {
			Toast.makeText(getBaseContext(), "No Place is entered",
					Toast.LENGTH_SHORT).show();
			return;
		}

		String url = "https://maps.googleapis.com/maps/api/geocode/json?";

		try {
			// encoding special characters like space in the user input place
			location = URLEncoder.encode(location, "utf-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}

		String address = "address=" + location;

		String sensor = "sensor=false";

		// url , from where the geocoding data is fetched
		url = url + address + "&" + sensor;
		Log.v(CommonStuff.TAG + "onClick", "url is: " + url);
		// String modifiedURL= url.toString().replace(" ", "%20");

		// Instantiating DownloadTask to get places from Google Geocoding
		// service
		// in a non-ui thread
		DownloadTask downloadTask = new DownloadTask();

		// Start downloading the geocoding places
		downloadTask.execute(url);
	}

	private void createSockets() {
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
					if (event.equals("updated")) {
						runOnUiThread(new Runnable() {
							public void run() {
								Toast.makeText(
										getBaseContext(),
										selecteditem
												+ " has been updated successfully",
										Toast.LENGTH_SHORT).show();
							}
						});
					}
				}
			});

		} catch (Exception ex) {
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case 0:
			AlertDialog.Builder builder = new AlertDialog.Builder(this)
					.setTitle(taddress.getLocationname())
					.setMessage(
							"Your address will be created with these settings")
					.setPositiveButton("OK",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {
									if (currentuser != null) {
										if (socket != null) {
											boolean notifywhenqueried = checkbox
													.isChecked();
											int index = dialogspinner
													.getSelectedItemPosition();
											String sitem = typesofsecurity[index];
											String sd = sitem.toLowerCase();
											if (sd.equals("public")) {
												typeofaddress = CommonStuff.PUBLIC_ADDRESS;
											} else if (sd.equals("protected")) {
												typeofaddress = CommonStuff.PROTECTED_ADDRESS;
											} else if (sd.equals("private")) {
												typeofaddress = CommonStuff.PRIVATE_ADDRESS;
											}

											taddress.setTypeofaddress(typeofaddress);
											taddress.setNotifywhenqueried(notifywhenqueried);
											Address[] addresses = currentuser
													.getAddresses();
											for (int i = 0; i < addresses.length; i++) {
												Address address = addresses[i];
												if (address
														.getName()
														.toLowerCase()
														.equals(taddress
																.getName()
																.toLowerCase())) {
													addresses[i] = taddress;
												}
											}
											currentuser.setAddresses(addresses);

											try {

												if (socket == null
														|| !socket
																.isConnected()) {
													createSockets();
												}
												socket.emit(
														"createaddress",
														currentuser.toJSON(),
														CommonStuff.versionName,
														CommonStuff.versionNumber);
											} catch (JSONException e) {
												// TODO Auto-generated catch
												// block
												e.printStackTrace();
											}
										}
									}
								}
							})
					.setNegativeButton("Cancel",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										int whichButton) {

								}
							});

			final FrameLayout frameView = new FrameLayout(this);
			builder.setView(frameView);

			final AlertDialog alertDialog = builder.create();
			LayoutInflater inflater = alertDialog.getLayoutInflater();
			dialoglayout = inflater
					.inflate(R.layout.addresscreation, frameView);

			tv_latitude = (TextView) dialoglayout
					.findViewById(R.id.tv_latitude);

			tv_latitude.setText("" + selectedLocation.latitude);

			tv_lngitude = (TextView) dialoglayout
					.findViewById(R.id.tv_lngitude);

			tv_lngitude.setText("" + selectedLocation.longitude);

			typesofsecurity = getResources().getStringArray(
					R.array.typeofsecurity_array);
			checkbox = (CheckBox) dialoglayout.findViewById(R.id.notifyonquery);

			checkbox.setChecked(taddress.isNotifywhenqueried());
			dialogspinner = (Spinner) dialoglayout
					.findViewById(R.id.spnTypesOfSecurity);

			ArrayAdapter<String> adapter = new ArrayAdapter<String>(
					getBaseContext(), android.R.layout.simple_spinner_item,
					typesofsecurity);
			dialogspinner.setAdapter(adapter);
			dialogspinner
					.setOnItemSelectedListener(new OnItemSelectedListener() {
						public void onItemSelected(AdapterView<?> arg0,
								View arg1, int arg2, long arg3) {
							int index = arg0.getSelectedItemPosition();
							String sitem = typesofsecurity[index];
							String sd = sitem.toLowerCase();
							if (sd.equals("public")) {
								typeofaddress = CommonStuff.PUBLIC_ADDRESS;
							} else if (sd.equals("protected")) {
								typeofaddress = CommonStuff.PROTECTED_ADDRESS;
							} else if (sd.equals("private")) {
								typeofaddress = CommonStuff.PRIVATE_ADDRESS;
							}
							// Toast.makeText(getBaseContext(),
							// "You have selected item : " + selecteditem,
							// Toast.LENGTH_SHORT).show();
						}

						public void onNothingSelected(AdapterView<?> arg0) {
						}
					});

			if (taddress.getTypeofaddress() == CommonStuff.PUBLIC_ADDRESS) {
				dialogspinner.setSelection(0);
			} else {
				dialogspinner.setSelection(1);
			}
			return alertDialog;
		}

		return null;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		// getMenuInflater().inflate(R.menu.main, menu);
		// createMenu(menu);

		MenuInflater menuInflater = getMenuInflater();
		menuInflater.inflate(R.menu.main, menu);

		// Calling super after populating the menu is necessary here to ensure
		// that the
		// action bar helpers have a chance to handle this event.
		return super.onCreateOptionsMenu(menu);
		// return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return menuChoice(item);
	}

	public void onLocationChanged(Location d) {
		// TODO Auto-generated method stub
		double lat = d.getLatitude();
		double lng = d.getLongitude();
		double altitude = d.getAltitude();

		// /mMap.clear();
		// currentLocation = d;
		locationdata = new LatLng(lat, lng);

		if (selectedLocation == null) {
			selectedLocation = locationdata;
		}

		if (currentLocationMarker != null) {
			currentLocationMarker.remove();
		}

		currentLocationMarker = mMap.addMarker(new MarkerOptions()
				.position(locationdata)
				.snippet("Lat:" + lat + "Lng:" + lng)
				.icon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_RED))
				.title("Current Location").draggable(false).visible(true));

		new ReverseGeocodingTask(getBaseContext(), false).execute(locationdata);

		// when location changes
		mMap.animateCamera(CameraUpdateFactory.newLatLng(locationdata),
				new GoogleMap.CancelableCallback() {
					public void onFinish() {
						mMap.animateCamera(CameraUpdateFactory
								.zoomTo(CommonStuff.DEFAULT_ZOOM));
					}

					public void onCancel() {
						// TODO Auto-generated method stub
					}
				});

		currentLocationMarker.showInfoWindow();
	}

	public void onProviderDisabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void onProviderEnabled(String provider) {
		// TODO Auto-generated method stub

	}

	public void onStatusChanged(String provider, int status, Bundle extras) {
		// TODO Auto-generated method stub

	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == request_Code) {
			if (resultCode == RESULT_OK) {
				// Getting Google Play availability status
				int status = GooglePlayServicesUtil
						.isGooglePlayServicesAvailable(getBaseContext());

				// Showing status
				if (status != ConnectionResult.SUCCESS) { // Google Play
															// Services are
															// not available
					int rrequestCode = 10;
					Dialog dialog = GooglePlayServicesUtil.getErrorDialog(
							status, this, rrequestCode);
					dialog.show();

				} else { // Google Play Services are available
					showMap();
				}

				Spinner s1 = (Spinner) findViewById(R.id.spinner1);
				ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
						android.R.layout.simple_spinner_item, locations);
				s1.setAdapter(adapter);
				s1.setOnItemSelectedListener(new addressSelected());
			} else {
				Toast.makeText(this, "Login unsuccessful", Toast.LENGTH_SHORT)
						.show();
			}
		}

	}

	@Override
	protected void onResume() {
		super.onResume();
		getSreenDimanstions();

		// final GlobalClass globalVariable = (GlobalClass)
		// getApplicationContext();
		// searchedLocation = globalVariable.getSelectedLocation();

		if (latlngBounds != null) {

			mMap.setOnCameraChangeListener(new OnCameraChangeListener() {

				public void onCameraChange(CameraPosition arg0) {
					CameraUpdate update = CameraUpdateFactory.newLatLngBounds(
							latlngBounds, width, height, 0);
					// Move camera.
					mMap.moveCamera(update);
					// Remove listener to prevent position reset on camera
					// move.
					mMap.setOnCameraChangeListener(null);
				}
			});
		}
	}

	private void showMap() {
		// TODO Auto-generated method stub

		// mMap.setMyLocationEnabled(true);
		// Do a null check to confirm that we have not already instantiated the
		// map.
		try {

			if (mMap == null) {
				// mMap = ((SupportMapFragment)
				// getFragmentManager().findFragmentById(
				// R.id.map)).getMap();
				SupportMapFragment fragment = ((SupportMapFragment) getSupportFragmentManager()
						.findFragmentById(R.id.map));
				mMap = fragment.getMap();
				// Check if we were successful in obtaining the map.
				if (mMap != null) {
					// The Map is verified. It is now safe to manipulate the
					// map.
					// CommonStuff.turnGPSOn(this);
					// final GlobalClass globalVariable = (GlobalClass)
					// getApplicationContext();

					// mMap.setMapType(globalVariable.getTypeOfView());
					mMap.setMyLocationEnabled(true);
					mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
					mMap.getUiSettings().setZoomControlsEnabled(true);
					mMap.getUiSettings().setCompassEnabled(true);
					mMap.getUiSettings().setMyLocationButtonEnabled(true);
					mMap.getUiSettings().setAllGesturesEnabled(true);
					mMap.clear();
					// Getting LocationManager object from System Service
					// LOCATION_SERVICE
					LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

					// Creating a criteria object to retrieve provider
					Criteria criteria = new Criteria();

					// criteria.

					// Getting the name of the best provider
					String provider = locationManager.getBestProvider(criteria,
							true);

					// Getting Current Location
					// Location location = locationManager
					// .getLastKnownLocation(provider);

					boolean gpsEnabled = false;
					try {
						gpsEnabled = locationManager
								.isProviderEnabled(LocationManager.GPS_PROVIDER);
					} catch (Exception ex) {
					}

					boolean networkEnabled = false;
					try {
						networkEnabled = locationManager
								.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
					} catch (Exception ex) {
					}

					Location loc = null;
					if (gpsEnabled)
						loc = locationManager
								.getLastKnownLocation(LocationManager.GPS_PROVIDER);
					else if (networkEnabled)
						loc = locationManager
								.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

					// don't start listeners if no provider is enabled

					// locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
					// 0, 0, locationListenerNetwork);

					if (loc != null) {
						onLocationChanged(loc);
						new LatLng(loc.getLatitude(), loc.getLongitude());
					}
					locationManager.requestLocationUpdates(provider,
							CommonStuff.MIN_WAIT_TIME,
							CommonStuff.MIN_DISTANCE, this);

					mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

						public void onMapLongClick(LatLng arg0) {
							selectedLocation = arg0;
							// mMap.clear();
							if (selectedLocationMarker != null) {
								selectedLocationMarker.remove();
							}

							if (selecteditem.toLowerCase().equals("home")) {
								selectedLocationMarker = mMap.addMarker(new MarkerOptions()
										.position(selectedLocation)
										.icon(BitmapDescriptorFactory
												.fromResource(R.drawable.house))
										.draggable(false).visible(true));
							} else if (selecteditem.toLowerCase()
									.equals("work")) {
								selectedLocationMarker = mMap.addMarker(new MarkerOptions()
										.position(selectedLocation)
										.icon(BitmapDescriptorFactory
												.fromResource(R.drawable.officebuilding))
										.draggable(false).visible(true));
							} else if (selecteditem.toLowerCase().equals(
									"school")) {
								selectedLocationMarker = mMap.addMarker(new MarkerOptions()
										.position(selectedLocation)
										.icon(BitmapDescriptorFactory
												.fromResource(R.drawable.school))
										.draggable(false).visible(true));
							} else {
								selectedLocationMarker = mMap.addMarker(new MarkerOptions()
										.position(selectedLocation)
										.icon(BitmapDescriptorFactory
												.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
										.draggable(false).visible(true));
							}

							// When long press happens
							mMap.animateCamera(
									CameraUpdateFactory.newLatLng(arg0),
									new GoogleMap.CancelableCallback() {
										public void onFinish() {
											mMap.animateCamera(
													CameraUpdateFactory
															.zoomTo(CommonStuff.DEFAULT_ZOOM),
													new GoogleMap.CancelableCallback() {
														public void onFinish() {
															saveCurrentLocation();
														}

														public void onCancel() {
															// TODO
															// Auto-generated
															// method stub
														}
													});
										}

										public void onCancel() {
											// TODO Auto-generated method stub
										}
									});

							new ReverseGeocodingTask(getBaseContext(), true)
									.execute(selectedLocation);
							selectedLocationMarker.showInfoWindow();
						}
					});
				}
			}
		} catch (Exception d) {
			Toast.makeText(getBaseContext(), d.getMessage(), Toast.LENGTH_LONG)
					.show();
		}
	}

	// Method to check network connectivity
	public boolean isNetworkAvailable() {
		ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo activeNetworkInfo = connectivityManager
				.getActiveNetworkInfo();
		if (activeNetworkInfo != null
				&& activeNetworkInfo.isConnectedOrConnecting()) {
			// Log.d("network", "Network available:true");
			return true;
		} else {
			// Log.d("network", "Network available:false");
			return false;
		}
	}

	private void createMenu(Menu menu) {
		MenuItem mnu1 = menu.add(0, 0, 0, "Search");
		{
			mnu1.setIcon(R.drawable.ic_action_search);

			// MenuItemCompat.getActionView(mnu1);
			// mnu1.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM
			// | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
		}
	}

	private boolean menuChoice(MenuItem item) {
		Bundle extras = new Bundle();
		extras.putSerializable("currentuser", currentuser);
		Intent i;
		switch (item.getItemId()) {
		case android.R.id.home:
			i = new Intent(this, MainActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			i.putExtras(extras);
			startActivity(i);
			finish();
			return true;
		case R.id.mnu_navigate:
			i = new Intent(this, NavigationActivity.class);
			i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
			i.putExtras(extras);
			startActivity(i);
			finish();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private class addressSelected implements OnItemSelectedListener {
		public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2,
				long arg3) {
			int index = arg0.getSelectedItemPosition();
			selecteditem = locations[index];
			if (currentuser != null) {
				Address[] addresses = currentuser.getAddresses();
				for (int i = 0; i < addresses.length; i++) {
					Address address = addresses[i];
					if (address != null) {
						if (selecteditem.toLowerCase().equals(
								address.getName().toLowerCase())) {
							selectedLocation = new LatLng(
									address.getLatitude(),
									address.getLongitude());
							// taddress=address;

							address.setLocationname(monikerhead + '@'
									+ selecteditem.toLowerCase());
							taddress = address;
							if (selectedLocationMarker != null) {
								selectedLocationMarker.remove();
							}
							if (selecteditem.toLowerCase().equals("home")) {
								selectedLocationMarker = mMap
										.addMarker(new MarkerOptions()
												.position(selectedLocation)
												.snippet(
														"Lat:"
																+ address
																		.getLatitude()
																+ "Lng:"
																+ address
																		.getLongitude())
												.icon(BitmapDescriptorFactory
														.fromResource(R.drawable.house))
												.title(address
														.getLocationname())
												.draggable(false).visible(true));
							} else if (selecteditem.toLowerCase()
									.equals("work")) {
								selectedLocationMarker = mMap
										.addMarker(new MarkerOptions()
												.position(selectedLocation)
												.snippet(
														"Lat:"
																+ address
																		.getLatitude()
																+ "Lng:"
																+ address
																		.getLongitude())
												.icon(BitmapDescriptorFactory
														.fromResource(R.drawable.officebuilding))
												.title(address
														.getLocationname())
												.draggable(false).visible(true));
							} else if (selecteditem.toLowerCase().equals(
									"school")) {
								selectedLocationMarker = mMap
										.addMarker(new MarkerOptions()
												.position(selectedLocation)
												.snippet(
														"Lat:"
																+ address
																		.getLatitude()
																+ "Lng:"
																+ address
																		.getLongitude())
												.icon(BitmapDescriptorFactory
														.fromResource(R.drawable.school))
												.title(address
														.getLocationname())
												.draggable(false).visible(true));
							} else {
								selectedLocationMarker = mMap
										.addMarker(new MarkerOptions()
												.position(selectedLocation)
												.snippet(
														"Lat:"
																+ address
																		.getLatitude()
																+ "Lng:"
																+ address
																		.getLongitude())
												.icon(BitmapDescriptorFactory
														.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
												.title(monikerhead + "@"
														+ address.getName())
												.draggable(false).visible(true));
							}

							CameraPosition cameraPosition = new CameraPosition.Builder()
									.target(selectedLocation)
									.zoom(CommonStuff.DEFAULT_ZOOM) // Sets
																	// the
																	// zoom
									.build(); // Creates a
												// CameraPosition
												// from the
												// builder
							mMap.animateCamera(CameraUpdateFactory
									.newCameraPosition(cameraPosition));

							new ReverseGeocodingTask(getBaseContext(), true)
									.execute(selectedLocation);
							// when spinner is selected
							/*
							 * mMap.animateCamera( CameraUpdateFactory
							 * .newLatLng(selectedLocation), new
							 * GoogleMap.CancelableCallback() { public void
							 * onFinish() { mMap.animateCamera
							 * (CameraUpdateFactory .zoomTo(CommonStuff
							 * .DEFAULT_ZOOM)); }
							 * 
							 * public void onCancel() { // TODO //
							 * Auto-generated // method // stub } });
							 */
							selectedLocationMarker.showInfoWindow();

						}
					}
				}
			}
		}

		public void onNothingSelected(AdapterView<?> arg0) {
		}
	}

	protected void getSreenDimanstions() {
		// if (Integer.valueOf(android.os.Build.VERSION.SDK_INT) < 13) {

		Display display = getWindowManager().getDefaultDisplay();
		width = display.getWidth();
		height = display.getHeight();

		/*
		 * } else {
		 * 
		 * Display display = getWindowManager().getDefaultDisplay(); Point size
		 * = new Point(); display.getSize(size); width = size.x; height =
		 * size.y; }
		 */
	}

	/** A method to download json data from url */
	private String downloadUrl(String strUrl) throws IOException {
		String data = "";
		InputStream iStream = null;
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL(strUrl);

			// Creating an http connection to communicate with url
			// urlConnection = (HttpURLConnection) url.openConnection();

			// Connecting to url
			// urlConnection.connect();

			// Reading data from url
			// iStream = urlConnection.getInputStream();
			/*
			 * BufferedReader br = new BufferedReader(new InputStreamReader(
			 * iStream));
			 * 
			 * StringBuffer sb = new StringBuffer();
			 * 
			 * String line = ""; while ((line = br.readLine()) != null) {
			 * sb.append(line); }
			 */
			StringBuffer sb = new StringBuffer();
			// URL url = new URL(sb.toString());
			urlConnection = (HttpURLConnection) url.openConnection();
			InputStreamReader in = new InputStreamReader(
					urlConnection.getInputStream());

			// Load the results into a StringBuilder
			int read;
			char[] buff = new char[1024];
			while ((read = in.read(buff)) != -1) {
				sb.append(buff, 0, read);
			}
			data = sb.toString();

			// br.close();

		} catch (Exception e) {
			Log.d("Exception while downloading url", e.toString());
		} finally {
			if (iStream != null) {
				iStream.close();
			}

			if (urlConnection != null) {
				urlConnection.disconnect();
			}

		}
		return data;
	}

	private void saveCurrentLocation() {
		SnapshotReadyCallback callback = new SnapshotReadyCallback() {
			Bitmap bitmap;

			// @Override
			public void onSnapshotReady(Bitmap snapshot) {
				// TODO Auto-generated method stub
				bitmap = snapshot;
				try {

					ContextWrapper contextWrapper = new ContextWrapper(
							getApplicationContext());
					File outputDir = contextWrapper.getCacheDir(); // context
																	// being the
																	// Activity
																	// pointer
					File image = File.createTempFile("prefix", "extension",
							outputDir);
					FileOutputStream fos = new FileOutputStream(image);
					bitmap.compress(Bitmap.CompressFormat.PNG, 90, fos);

					// File image = new File(uri.getPath());

					BitmapFactory.Options bounds = new BitmapFactory.Options();
					bounds.inJustDecodeBounds = true;
					BitmapFactory.decodeFile(image.getPath(), bounds);
					if ((bounds.outWidth == -1) || (bounds.outHeight == -1))
						return;

					int originalSize = (bounds.outHeight > bounds.outWidth) ? bounds.outHeight
							: bounds.outWidth;

					BitmapFactory.Options opts = new BitmapFactory.Options();
					opts.inSampleSize = originalSize
							/ CommonStuff.THUMBNAIL_SIZE;
					thumbnail = BitmapFactory.decodeFile(image.getPath(), opts);

				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		mMap.snapshot(callback);
	}

	// Fetches all places from GooglePlaces AutoComplete Web Service
	class PlacesTask extends AsyncTask<String, Void, String> {

		@Override
		protected String doInBackground(String... place) {
			// For storing data from web service
			String data = "";

			// Obtain browser key from https://code.google.com/apis/console
			String key = "key=";

			String input = "";

			try {
				input = "input=" + URLEncoder.encode(place[0], "utf-8");
			} catch (UnsupportedEncodingException e1) {
				e1.printStackTrace();
			}

			// place type to be searched
			String types = "types=geocode";
			// sb.append();
			// Sensor enabled
			String sensor = "sensor=false";

			// Building the parameters to the web service
			String parameters = input + "&" + types + "&"
					+ sensor + "&" + key;

			// Output format
			String output = "json";

			// Building the url to the web service
			String url = "https://maps.googleapis.com/maps/api/place/autocomplete/"
					+ output + "?" + parameters;

			try {
				// Fetching the data from web service in background
				data = downloadUrl(url);
			} catch (Exception e) {
				Log.d("Background Task", e.toString());
			}
			return data;
		}

		@Override
		protected void onPostExecute(String result) {
			super.onPostExecute(result);

			// Creating ParserTask
			parserTask = new ParserTask();

			// Starting Parsing the JSON string returned by Web Service
			parserTask.execute(result);
		}
	}

	/** A class to parse the Google Places in JSON format */
	class ParserTask extends
			AsyncTask<String, Integer, List<HashMap<String, String>>> {

		JSONObject jObject;

		@Override
		protected List<HashMap<String, String>> doInBackground(
				String... jsonData) {

			List<HashMap<String, String>> places = null;

			PlaceJSONParser placeJsonParser = new PlaceJSONParser();

			try {
				jObject = new JSONObject(jsonData[0]);

				// Getting the parsed data as a List construct
				places = placeJsonParser.parse(jObject);

			} catch (Exception e) {
				Log.d("Exception", e.toString());
			}
			return places;
		}

		@Override
		protected void onPostExecute(List<HashMap<String, String>> result) {

			String[] from = new String[] { "description" };
			int[] to = new int[] { android.R.id.text1 };

			// Creating a SimpleAdapter for the AutoCompleteTextView
			SimpleAdapter adapter = new SimpleAdapter(getBaseContext(), result,
					android.R.layout.simple_dropdown_item_1line, from, to);

			// Setting the adapter
			atvPlaces.setAdapter(adapter);
		}
	}

	/** A class, to download Places from Geocoding webservice */
	class DownloadTask extends AsyncTask<String, Integer, String> {

		String data = null;

		// Invoked by execute() method of this object
		@Override
		protected String doInBackground(String... url) {
			try {
				data = downloadUrl(url[0]);
			} catch (Exception e) {
				Log.d("Background Task", e.toString());
			}
			return data;
		}

		// Executed after the complete execution of doInBackground() method
		@Override
		protected void onPostExecute(String result) {

			ParserTask2 parserTask = new ParserTask2();
			parserTask.execute(result);
		}

	}

	class ParserTask2 extends
			AsyncTask<String, Integer, List<HashMap<String, String>>> {

		JSONObject jObject;

		@Override
		protected List<HashMap<String, String>> doInBackground(
				String... jsonData) {

			List<HashMap<String, String>> places = null;
			GeocodeJSONParser parser = new GeocodeJSONParser();

			try {
				jObject = new JSONObject(jsonData[0]);

				/** Getting the parsed data as a an ArrayList */
				places = parser.parse(jObject);

			} catch (Exception e) {
				Log.d("Exception", e.toString());
			}
			return places;
		}

		// Executed after the complete execution of doInBackground() method
		@Override
		protected void onPostExecute(List<HashMap<String, String>> list) {

			// Clears all the existing markers
			// mMap.clear();

			for (int i = 0; i < list.size(); i++) {

				// Creating a marker
				// MarkerOptions markerOptions = new MarkerOptions();
				HashMap<String, String> hmPlace = list.get(i);

				double lat = Double.parseDouble(hmPlace.get("lat"));
				double lng = Double.parseDouble(hmPlace.get("lng"));

				double latsw = Double.parseDouble(hmPlace.get("latsw"));
				double lngsw = Double.parseDouble(hmPlace.get("lngsw"));

				double latne = Double.parseDouble(hmPlace.get("latne"));
				double lngne = Double.parseDouble(hmPlace.get("lngne"));

				// String name = hmPlace.get("formatted_address");
				LatLng latLng = new LatLng(lat, lng);
				// markerOptions.position(latLng);
				// markerOptions.title(name);
				if (mMap != null) {
					// zoomMarker = mMap.addMarker(markerOptions);

					LatLng sw = new LatLng(latsw, lngsw);

					LatLng ne = new LatLng(latne, lngne);
					latlngBounds = new LatLngBounds(sw, ne); // CommonStuff.createLatLngBoundsObject(ne,
																// sw);

					// Locate the first location
					if (i == 0) {
						// mMap.animateCamera(CameraUpdateFactory
						// .newLatLng(latLng));

						CameraUpdate update = CameraUpdateFactory
								.newLatLngBounds(latlngBounds,
										CommonStuff.convertDpToPixel(100,
												getBaseContext()));

						mMap.animateCamera(update,
								new GoogleMap.CancelableCallback() {
									public void onFinish() {
										// if the previous camera move is done,
										// check the zoom level
										// if (map.getCameraPosition().zoom >=
										// CommonStuff.MIN_ZOOM_LVL_TO_ZOOM_OUT)
										// {
										// map.animateCamera(CameraUpdateFactory
										// .zoomTo(CommonStuff.DEFAULT_ZOOM));
										// }
									}

									public void onCancel() {
										// TODO Auto-generated method stub
									}
								});
					}
				}
			}
		}
	}

	class ReverseGeocodingTask extends AsyncTask<LatLng, Void, String> {
		Context mContext;
		boolean selectedlocation;

		public ReverseGeocodingTask(Context context, boolean selectedlocation) {
			super();
			mContext = context;
			this.selectedlocation = selectedlocation;
		}

		// Finding address using reverse geocoding
		@Override
		protected String doInBackground(LatLng... params) {
			Geocoder geocoder = new Geocoder(mContext);
			double latitude = params[0].latitude;
			double longitude = params[0].longitude;

			List<android.location.Address> addresses = null;
			String addressText = "";

			try {
				addresses = geocoder.getFromLocation(latitude, longitude, 1);
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (addresses != null && addresses.size() > 0) {
				android.location.Address address = addresses.get(0);

				addressText = String.format(
						"%s, %s, %s",
						address.getMaxAddressLineIndex() > 0 ? address
								.getAddressLine(0) : "", address.getLocality(),
						address.getCountryName());
			}

			return addressText;
		}

		@Override
		protected void onPostExecute(String addressText) {
			// Setting the title for the marker.
			// This will be displayed on taping the marker
			if (selectedlocation) {
				if (selectedLocationMarker != null) {
					selectedLocationMarker.remove();
				}

				if (selecteditem.toLowerCase().equals("home")) {
					selectedLocationMarker = mMap.addMarker(new MarkerOptions()
							.position(selectedLocation)
							.snippet(addressText)
							.title(monikerhead + "@"
									+ selecteditem.toLowerCase())
							.icon(BitmapDescriptorFactory
									.fromResource(R.drawable.house))
							.draggable(false).visible(true));
				} else if (selecteditem.toLowerCase().equals("work")) {
					selectedLocationMarker = mMap.addMarker(new MarkerOptions()
							.position(selectedLocation)
							.snippet(addressText)
							.title(monikerhead + "@"
									+ selecteditem.toLowerCase())
							.icon(BitmapDescriptorFactory
									.fromResource(R.drawable.officebuilding))
							.draggable(false).visible(true));
				} else if (selecteditem.toLowerCase().equals("school")) {
					selectedLocationMarker = mMap.addMarker(new MarkerOptions()
							.position(selectedLocation)
							.snippet(addressText)
							.title(monikerhead + "@"
									+ selecteditem.toLowerCase())
							.icon(BitmapDescriptorFactory
									.fromResource(R.drawable.school))
							.draggable(false).visible(true));
				} else {
					selectedLocationMarker = mMap
							.addMarker(new MarkerOptions()
									.position(selectedLocation)
									.snippet(addressText)
									.title(monikerhead + "@"
											+ selecteditem.toLowerCase())
									.icon(BitmapDescriptorFactory
											.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
									.draggable(false).visible(true));
				}

				selectedLocationMarker.showInfoWindow();
			} else {

				if (currentLocationMarker != null) {
					currentLocationMarker.remove();
				}

				currentLocationMarker = mMap
						.addMarker(new MarkerOptions()
								.position(locationdata)
								.snippet(addressText)
								.icon(BitmapDescriptorFactory
										.defaultMarker(BitmapDescriptorFactory.HUE_RED))
								.title("Current Location").draggable(false)
								.visible(true));
				currentLocationMarker.showInfoWindow();
			}
		}
	}

}
