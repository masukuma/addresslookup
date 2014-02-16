package com.nobledesignlabs.maprelated;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.internal.bt;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.api.client.auth.oauth2.Credential;
import com.nobledesignlabs.entities.User;
import com.nobledesignlabs.lookupaddress.R;
import com.nobledesignlabs.oauth.OAuth2Helper;
import com.nobledesignlabs.oauth.Oauth2Params;
import com.nobledesignlabs.utils.CommonStuff;
import com.nobledesignlabs.utils.GlobalClass;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.Display;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public abstract class NavigationBaseActivity extends ActionBarActivity
		implements LocationListener {
	// private User currentuser;
	protected LatLng searchedLocation;
	protected Location currentLocation;
	private Marker currentLocationMarker;
	protected Marker searchedLocationMarker;
	protected GoogleMap map;
	private SupportMapFragment fragment;
	private LatLngBounds latlngBounds;
	private Button bNavigation;
	private Polyline newPolyline;
	private int width;
	private int height;
	private boolean directionsought;
	private String name;
	private String suggestKey;

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// ---save whatever you need to persist---
		outState.putBoolean("directionsought", directionsought);
		if (searchedLocation != null) {
			outState.putDouble("lat", searchedLocation.latitude);
			outState.putDouble("lng", searchedLocation.longitude);
		} else {
			outState.putDouble("lat", 0);
			outState.putDouble("lng", 0);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// ---retrieve the information persisted earlier---
		directionsought = (boolean) savedInstanceState
				.getBoolean("directionsought");

		double latitude = savedInstanceState.getDouble("lat");
		double longitude = savedInstanceState.getDouble("lng");
		if (longitude > 0 && latitude > 0)
			searchedLocation = new LatLng(latitude, longitude);
		// extractAddress();
	}

	@Override
	protected void onResume() {

		super.onResume();
		getSreenDimanstions();
		// final GlobalClass globalVariable = (GlobalClass)
		// getApplicationContext();
		// searchedLocation = globalVariable.getSelectedLocation();
		if (directionsought) {
			if (currentLocation != null && searchedLocation != null) {
				latlngBounds = CommonStuff.createLatLngBoundsObject(
						new LatLng(currentLocation.getLatitude(),
								currentLocation.getLongitude()),
						searchedLocation);

				map.setOnCameraChangeListener(new OnCameraChangeListener() {

					public void onCameraChange(CameraPosition arg0) {
						CameraUpdate update = CameraUpdateFactory
								.newLatLngBounds(latlngBounds, width, height, 0);
						// Move camera.
						map.moveCamera(update);
						// Remove listener to prevent position reset on camera
						// move.
						map.setOnCameraChangeListener(null);
					}
				});
			}
		} else {
			if (searchedLocation != null) {
				CameraPosition cameraPosition = new CameraPosition.Builder()
						.target(searchedLocation)
						.zoom(CommonStuff.DEFAULT_ZOOM) // Sets
														// the
														// zoom
						.build(); // Creates a CameraPosition from the builder
				map.animateCamera(CameraUpdateFactory
						.newCameraPosition(cameraPosition));
			} else if (currentLocation != null) {
				// if (searchedLocation != null) {
				CameraPosition cameraPosition = new CameraPosition.Builder()
						.target(new LatLng(currentLocation.getLatitude(),
								currentLocation.getLongitude()))
						.zoom(CommonStuff.DEFAULT_ZOOM) // Sets
						// the
						// zoom
						.build(); // Creates a CameraPosition from the builder
				map.animateCamera(CameraUpdateFactory
						.newCameraPosition(cameraPosition));
				// }
			}
		}
		// map.moveCamera(update);
	}

	protected abstract void loadOtherUI();

	@Override
	protected void onCreate(Bundle bundle) {
		super.onCreate(bundle);
		setContentView(getLayoutResourceId());

		int status = GooglePlayServicesUtil
				.isGooglePlayServicesAvailable(getBaseContext());

		ActionBar actionBar = getSupportActionBar();
		actionBar.setDisplayHomeAsUpEnabled(true);

		// Showing status
		if (status != ConnectionResult.SUCCESS) {
			int rrequestCode = 10;
			Dialog dialog = GooglePlayServicesUtil.getErrorDialog(status, this,
					rrequestCode);
			dialog.show();

		} else { // Google Play Services are available
			fragment = ((SupportMapFragment) getSupportFragmentManager()
					.findFragmentById(R.id.map));
			map = fragment.getMap();
			if (map != null) {
				// CommonStuff.turnGPSOn(this);

				map.setMyLocationEnabled(true);
				map.setMapType(GoogleMap.MAP_TYPE_HYBRID);
				map.getUiSettings().setZoomControlsEnabled(true);
				map.getUiSettings().setCompassEnabled(true);
				map.getUiSettings().setMyLocationButtonEnabled(true);
				map.getUiSettings().setAllGesturesEnabled(true);

				PreferenceManager.getDefaultSharedPreferences(this);

				// Credential credential = null;

				// Getting LocationManager object from System Service
				// LOCATION_SERVICE
				LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);

				boolean gpsEnabled = false;
				boolean networkEnabled = false;

				try {
					gpsEnabled = locationManager
							.isProviderEnabled(LocationManager.GPS_PROVIDER);
				} catch (Exception ex) {
				}

				try {
					networkEnabled = locationManager
							.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
				} catch (Exception ex) {
				}

				while (!(gpsEnabled || networkEnabled)) {
					Toast.makeText(
							this,
							"Ensure that you have enabled acquisition of your current location via a GPS or via Wireless networks",
							Toast.LENGTH_LONG).show();
					try {
						Thread.sleep(5000);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					startActivity(new Intent(
							android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));

					try {
						gpsEnabled = locationManager
								.isProviderEnabled(LocationManager.GPS_PROVIDER);
					} catch (Exception ex) {
					}

					try {
						networkEnabled = locationManager
								.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
					} catch (Exception ex) {
					}

				}

				Location loc = null;
				if (gpsEnabled)
					loc = locationManager
							.getLastKnownLocation(LocationManager.GPS_PROVIDER);
				else if (networkEnabled)
					loc = locationManager
							.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

				// Creating a criteria object to retrieve provider
				Criteria criteria = new Criteria();
				// criteria.

				// Getting the name of the best provider
				String provider = locationManager.getBestProvider(criteria,
						true);

				if (currentLocation != null) {
					onLocationChanged(currentLocation);
				}
				locationManager.requestLocationUpdates(provider,
						CommonStuff.MIN_WAIT_TIME, CommonStuff.MIN_DISTANCE,
						this);
				// ---get the data using the getString()---

				// /currentLocation = new LatLng(lat, lng);

				bNavigation = (Button) findViewById(R.id.bNavigation);
				if (bNavigation != null) {
					bNavigation.setOnClickListener(new View.OnClickListener() {

						public void onClick(View v) {
							// final GlobalClass globalVariable =
							// (GlobalClass)
							// getApplicationContext();
							// searchedLocation = globalVariable
							// .getSelectedLocation();
							if (searchedLocation != null
									&& currentLocation != null) {
								findDirections(currentLocation.getLatitude(),
										currentLocation.getLongitude(),
										searchedLocation.latitude,
										searchedLocation.longitude,
										GMapV2Direction.MODE_DRIVING);
							}
						}
					});
				}
			}
			loadOtherUI();

		}
	}

	protected void showLocation(JSONObject jaddress) throws JSONException {

		// currentuser = new User(apiResponse);
		// currentuser.setRegistrationID(regid);
		directionsought = false;
		name = jaddress.getString("name");
		name = name.toLowerCase();
		suggestKey = jaddress.getString("locationname");
		double latitude = jaddress.getDouble("latitude");
		double longitude = jaddress.getDouble("longitude");
		// int typeofAddress = jaddress.getInt("typeofaddress");
		searchedLocation = new LatLng(latitude, longitude);
		// globalVariable.setSelectedLocation(searchedLocation);

		if (searchedLocationMarker != null) {
			searchedLocationMarker.remove();
		}

		if (name.equals("home")) {
			searchedLocationMarker = map.addMarker(new MarkerOptions()
					.position(searchedLocation)
					.snippet("Lat:" + latitude + "Lng:" + longitude)
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.house)).title(suggestKey)
					.draggable(false).visible(true));

		} else if (name.equals("work")) {
			searchedLocationMarker = map.addMarker(new MarkerOptions()
					.position(searchedLocation)
					.snippet("Lat:" + latitude + "Lng:" + longitude)
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.officebuilding))
					.title(suggestKey).draggable(false).visible(true));

		} else if (name.equals("school")) {
			searchedLocationMarker = map.addMarker(new MarkerOptions()
					.position(searchedLocation)
					.snippet("Lat:" + latitude + "Lng:" + longitude)
					.icon(BitmapDescriptorFactory
							.fromResource(R.drawable.school)).title(suggestKey)
					.draggable(false).visible(true));

		} else {
			searchedLocationMarker = map.addMarker(new MarkerOptions()
					.position(searchedLocation)
					.snippet("Lat:" + latitude + "Lng:" + longitude)
					.icon(BitmapDescriptorFactory
							.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
					.title(suggestKey).draggable(false).visible(true));
		}
		float zoom = CommonStuff.DEFAULT_ZOOM;
		;

		CameraPosition cameraPosition = new CameraPosition.Builder()
				.target(searchedLocation).zoom(zoom) // Sets the zoom
				.build(); // Creates a CameraPosition from the builder
		map.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

		new ReverseGeocodingTask(getBaseContext(), true)
				.execute(searchedLocation);

		searchedLocationMarker.showInfoWindow();

		/*
		 * map.animateCamera(CameraUpdateFactory.newLatLng(searchedLocation),
		 * new GoogleMap.CancelableCallback() { public void onFinish() {
		 * map.animateCamera(CameraUpdateFactory
		 * .zoomTo(CommonStuff.DEFAULT_ZOOM)); }
		 * 
		 * public void onCancel() { // TODO // Auto-generated // method stub }
		 * });
		 */
	}

	public void handleGetDirectionsResult(ArrayList<LatLng> directionPoints) {
		PolylineOptions rectLine = new PolylineOptions().width(5).color(
				Color.RED);
		directionsought = true;
		for (int i = 0; i < directionPoints.size(); i++) {
			rectLine.add(directionPoints.get(i));
		}
		if (newPolyline != null) {
			newPolyline.remove();
		}
		newPolyline = map.addPolyline(rectLine);
		// final GlobalClass globalVariable = (GlobalClass)
		// getApplicationContext();
		// searchedLocation = globalVariable.getSelectedLocation();
		if (currentLocation != null && searchedLocation != null) {
			latlngBounds = CommonStuff.createLatLngBoundsObject(
					new LatLng(currentLocation.getLatitude(), currentLocation
							.getLongitude()), searchedLocation);
			// map.animateCamera(CameraUpdateFactory.newLatLngBounds(latlngBounds,width,height,

			CameraUpdate update = CameraUpdateFactory.newLatLngBounds(
					latlngBounds, CommonStuff.convertDpToPixel(100, this));

			map.animateCamera(update, new GoogleMap.CancelableCallback() {
				public void onFinish() {
					// if the previous camera move is done, check the zoom level
					// if (map.getCameraPosition().zoom >=
					// CommonStuff.MIN_ZOOM_LVL_TO_ZOOM_OUT) {
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

	public void findDirections(double fromPositionDoubleLat,
			double fromPositionDoubleLong, double toPositionDoubleLat,
			double toPositionDoubleLong, String mode) {
		Map<String, String> map = new HashMap<String, String>();
		map.put(GetDirectionsAsyncTask.USER_CURRENT_LAT,
				String.valueOf(fromPositionDoubleLat));
		map.put(GetDirectionsAsyncTask.USER_CURRENT_LONG,
				String.valueOf(fromPositionDoubleLong));
		map.put(GetDirectionsAsyncTask.DESTINATION_LAT,
				String.valueOf(toPositionDoubleLat));
		map.put(GetDirectionsAsyncTask.DESTINATION_LONG,
				String.valueOf(toPositionDoubleLong));
		map.put(GetDirectionsAsyncTask.DIRECTIONS_MODE, mode);

		GetDirectionsAsyncTask asyncTask = new GetDirectionsAsyncTask(this);
		asyncTask.execute(map);
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

	public void onLocationChanged(Location d) {
		// TODO Auto-generated method stub
		double lat = d.getLatitude();
		double lng = d.getLongitude();
		double altitude = d.getAltitude();

		currentLocation = d;

		LatLng locationdata = new LatLng(lat, lng);
		// Toast.makeText(this, "Lat:" + lat + ",Lng:" + lng,
		// Toast.LENGTH_SHORT)
		// .show();
		if (currentLocationMarker != null) {
			currentLocationMarker.remove();
		}
		currentLocationMarker = map.addMarker(new MarkerOptions()
				.position(locationdata)
				.snippet("Lat:" + lat + "Lng:" + lng)
				.icon(BitmapDescriptorFactory
						.defaultMarker(BitmapDescriptorFactory.HUE_RED))
				.title("Current Location").draggable(false).visible(true));

		new ReverseGeocodingTask(getBaseContext(), false)
		.execute(locationdata);
		
		float zoom = CommonStuff.DEFAULT_ZOOM;

		if (!directionsought) {
			if (searchedLocation != null) {
				CameraPosition cameraPosition = new CameraPosition.Builder()
						.target(searchedLocation).zoom(zoom) // Sets the zoom
						.build(); // Creates a CameraPosition from the builder
				map.animateCamera(CameraUpdateFactory
						.newCameraPosition(cameraPosition));
			} else {
				CameraPosition cameraPosition = new CameraPosition.Builder()
						.target(locationdata).zoom(zoom) // Sets the zoom
						.build(); // Creates a CameraPosition from the builder
				map.animateCamera(CameraUpdateFactory
						.newCameraPosition(cameraPosition));
			}
		}
	}

	protected abstract int getLayoutResourceId();

	protected void setNavigationButtonEnabled(boolean enabled) {
		if (bNavigation != null) {
			bNavigation.setEnabled(enabled);
		}
	}

	class ReverseGeocodingTask extends AsyncTask<LatLng, Void, String> {
		Context mContext;
		boolean selected;

		public ReverseGeocodingTask(Context context, boolean selected) {
			super();
			mContext = context;
			this.selected = selected;
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
			if (selected) {
				if (searchedLocationMarker != null) {
					searchedLocationMarker.remove();
				}

				if (name.equals("home")) {
					searchedLocationMarker = map.addMarker(new MarkerOptions()
							.position(searchedLocation)
							.snippet(addressText)
							.icon(BitmapDescriptorFactory
									.fromResource(R.drawable.house))
							.title(suggestKey).draggable(false).visible(true));

				} else if (name.equals("work")) {
					searchedLocationMarker = map.addMarker(new MarkerOptions()
							.position(searchedLocation)
							.snippet(addressText)
							.icon(BitmapDescriptorFactory
									.fromResource(R.drawable.officebuilding))
							.title(suggestKey).draggable(false).visible(true));

				} else if (name.equals("school")) {
					searchedLocationMarker = map.addMarker(new MarkerOptions()
							.position(searchedLocation)
							.snippet(addressText)
							.icon(BitmapDescriptorFactory
									.fromResource(R.drawable.school))
							.title(suggestKey).draggable(false).visible(true));

				} else {
					searchedLocationMarker = map
							.addMarker(new MarkerOptions()
									.position(searchedLocation)
									.snippet(addressText)
									.icon(BitmapDescriptorFactory
											.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
									.title(suggestKey).draggable(false)
									.visible(true));
				}

				searchedLocationMarker.showInfoWindow();
			} else {

				LatLng locationdata = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
				// Toast.makeText(this, "Lat:" + lat + ",Lng:" + lng,
				// Toast.LENGTH_SHORT)
				// .show();
				if (currentLocationMarker != null) {
					currentLocationMarker.remove();
				}
				currentLocationMarker = map.addMarker(new MarkerOptions()
						.position(locationdata)
						.snippet(addressText)
						.icon(BitmapDescriptorFactory
								.defaultMarker(BitmapDescriptorFactory.HUE_RED))
						.title("Current Location").draggable(false).visible(true));
				
				currentLocationMarker.showInfoWindow();
			}
		}
	}
}
