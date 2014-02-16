package com.nobledesignlabs.entities;

import java.io.Serializable;

public class Address implements Serializable {
	private static final long serialVersionUID = 2310640779987082782L;
	private double latitude;
	private double longitude;
	private double altitude;
	private int typeofaddress;
	private String name;
	private String locationname;
	private boolean notifywhenqueried;
	private String userid;
	private String registrationID;

	public Address(){
		
	}
	public Address(double lati,double longi){
		this.latitude=lati;
		this.longitude=longi;
	}
	
	public Address(double lati,double longi, double altitude ){
		this.latitude=lati;
		this.longitude=longi;
		this.altitude=altitude;
	}
	public double getLatitude() {
		return latitude;
	}

	public void setLatitude(double latitude) {
		this.latitude = latitude;
	}

	public double getLongitude() {
		return longitude;
	}

	public void setLongitude(double longitude) {
		this.longitude = longitude;
	}

	public double getAltitude() {
		return altitude;
	}

	public void setAltitude(double altitude) {
		this.altitude = altitude;
	}
	public int getTypeofaddress() {
		return typeofaddress;
	}
	public void setTypeofaddress(int typeofaddress) {
		this.typeofaddress = typeofaddress;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getLocationname() {
		return locationname;
	}
	public void setLocationname(String locationname) {
		this.locationname = locationname;
	}
	public String getUserid() {
		return userid;
	}
	public void setUserid(String userid) {
		this.userid = userid;
	}
	public boolean isNotifywhenqueried() {
		return notifywhenqueried;
	}
	public void setNotifywhenqueried(boolean notifywhenqueried) {
		this.notifywhenqueried = notifywhenqueried;
	}
	public String getRegistrationID() {
		return registrationID;
	}
	public void setRegistrationID(String registrationID) {
		this.registrationID = registrationID;
	}

}
