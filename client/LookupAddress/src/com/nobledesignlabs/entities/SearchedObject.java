package com.nobledesignlabs.entities;

import java.io.Serializable;
import java.util.HashMap;

public class SearchedObject implements Serializable {

	private HashMap<String, String> zoomData;

	public HashMap<String, String> getZoomData() {
		return zoomData;
	}

	public void setZoomData(HashMap<String, String> zoomData) {
		this.zoomData = zoomData;
	}
	
}
