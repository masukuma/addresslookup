package com.nobledesignlabs.entities;

import java.io.Serializable;
import java.lang.reflect.Type;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

public class User implements Serializable {
	private static final long serialVersionUID = 2310640779687082782L;
	private String id;
	private String email;
	private String verified;
	private String name;
	private String given_name;
	private String profile_link;
	private String picture;
	private String gender;
	private String locale;
	private Address[] addresses;
	private String moniker;
	private String registrationID;

	
	public User(String strJson) {

		try {
			
			JSONObject jsonResponse = new JSONObject(strJson);
			setId(jsonResponse.optString("id").toString());
			setEmail(jsonResponse.optString("email").toString());
			setVerified(jsonResponse.optString("verified").toString());
			setName(jsonResponse.optString("name").toString());
			setGiven_name(jsonResponse.optString("given_name").toString());
			setProfile_link(jsonResponse.optString("profile_link").toString());
			setPicture(jsonResponse.optString("picture").toString());
			setGender(jsonResponse.optString("gender").toString());
			setLocale(jsonResponse.optString("locale").toString());
			if(jsonResponse.optString("addresses") != null){
				
			}
			
		
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getVerified() {
		return verified;
	}

	public void setVerified(String verified) {
		this.verified = verified;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getGiven_name() {
		return given_name;
	}

	public void setGiven_name(String given_name) {
		this.given_name = given_name;
	}

	public String getProfile_link() {
		return profile_link;
	}

	public void setProfile_link(String profile_link) {
		this.profile_link = profile_link;
	}

	public String getPicture() {
		return picture;
	}

	public void setPicture(String picture) {
		this.picture = picture;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public String getLocale() {
		return locale;
	}

	public void setLocale(String locale) {
		this.locale = locale;
	}

	public Address[] getAddresses() {
		return addresses;
	}

	public void setAddresses(Address[] addresses) {
		this.addresses = addresses;
	}

	public JSONObject toJSON() throws JSONException {
		String str="";
		// txtApiResponse.setText(apiResponse);
		try {
			Type restype = new TypeToken<User>() {
			}.getType();
			Gson gson = new GsonBuilder().setPrettyPrinting().create();
			str = gson.toJson(this, restype);
			// System.out.println(val);
		} catch (Exception e) {

		}
		return new JSONObject(str);
	}

	public String getMoniker() {
		return moniker;
	}

	public void setMoniker(String moniker) {
		this.moniker = moniker;
	}

	public String getRegistrationID() {
		return registrationID;
	}

	public void setRegistrationID(String registrationID) {
		this.registrationID = registrationID;
	}
}
