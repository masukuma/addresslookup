package com.nobledesignlabs.lookupaddress;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.api.client.auth.oauth2.Credential;
import com.nobledesignlabs.entities.User;
import com.nobledesignlabs.maprelated.GMapV2Direction;
import com.nobledesignlabs.maprelated.GetDirectionsAsyncTask;
import com.nobledesignlabs.maprelated.NavigationBaseActivity;
import com.nobledesignlabs.oauth.OAuth2Helper;
import com.nobledesignlabs.oauth.Oauth2Params;
import com.nobledesignlabs.utils.CommonStuff;

import android.graphics.Color;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Display;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;

public class AuthorizedActivity extends NavigationBaseActivity {

	private String title;
	private String address;

	@Override
	protected int getLayoutResourceId() {
		return R.layout.activity_privateshare;
	}

	@Override
	protected void loadOtherUI() {

	}

	@Override
	protected void onResume() {

		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		// ---look up the notification manager service---
		// NotificationManager nm = (NotificationManager)
		// getSystemService(NOTIFICATION_SERVICE);
		// ---cancel the notification that we started---
		Bundle extras = getIntent().getExtras();

		// nm.cancel(extras.getInt("notificationID"));

		title = extras.getString("title");
		super.setTitle(title);
		address = extras.getString("address");
		
		/*
		 * runOnUiThread(new Runnable(){
		 * 
		 * public void run() { // TODO Auto-generated method stub
		 * extractAddress(); }
		 * 
		 * });
		 */
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			public void run() {				
				extractAddress();
			}
		}, 3000);
	}

	private void extractAddress() {
		try {
			JSONObject jaddress = new JSONObject(address);

			showLocation(jaddress);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.authorized, menu);
		return true;
	}

	// private String monikerhead;
	private Bundle mMainFragmentArgs;

	@Override
	public void onSaveInstanceState(Bundle outState) {
		// ---save whatever you need to persist---
		outState.putString("address", address);
		outState.putString("title", title);
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onRestoreInstanceState(Bundle savedInstanceState) {
		super.onRestoreInstanceState(savedInstanceState);
		// ---retrieve the information persisted earlier---
		address = (String) savedInstanceState.getString("address");
		title = (String) savedInstanceState.getString("title");
		extractAddress();
	}

	public void saveMainFragmentState(Bundle args) {
		args.putString("address", address);
		args.putString("title", title);
		mMainFragmentArgs = args;
	}

	public Bundle getSavedMainFragmentState() {
		title = mMainFragmentArgs.getString("title");
		address = mMainFragmentArgs.getString("address");

		return mMainFragmentArgs;
	}

}
