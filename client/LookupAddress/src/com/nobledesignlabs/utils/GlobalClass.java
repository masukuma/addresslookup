package com.nobledesignlabs.utils;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.nobledesignlabs.entities.User;

import android.app.Application;

public class GlobalClass extends Application {
	private User currentUser;
	private LatLng loc;
	private int typeOfView=GoogleMap.MAP_TYPE_NORMAL;

	public User getCurrentUser() {
		return currentUser;
	}

	public void setCurrentUser(User currentUser) {
		this.currentUser = currentUser;
	}

	public void setSelectedLocation(LatLng searchedLocation) {
		// TODO Auto-generated method stub
		this.loc=searchedLocation;
	}
	
	public LatLng getSelectedLocation(){
		return this.loc;
	}

	public int getTypeOfView() {
		return typeOfView;
	}

	public void setTypeOfView(int typeOfView) {
		this.typeOfView = typeOfView;
	}
	
	

}
