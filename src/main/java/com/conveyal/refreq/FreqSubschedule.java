package com.conveyal.refreq;

import org.onebusaway.gtfs.model.Route;

public class FreqSubschedule {

	private ServiceWindow window;
	private int period;
	private TripProfile tripProfile;
	private String direction;
	private Route route;

	public void setWindow(ServiceWindow window) {
		this.window = window;
	}

	public void setPeriod(int period) {
		this.period = period;
	}

	public void setTripProfile(TripProfile tripProfile) {
		this.tripProfile = tripProfile;
	}

	public void setDirection(String direction) {
		this.direction = direction;
	}

	public void setRoute(Route route) {
		this.route = route;
	}

	public int getPeriod() {
		return period;
	}

	public TripProfile getTripProfile() {
		return tripProfile;
	}

	public String getDirection() {
		return direction;
	}

	public ServiceWindow getWindow() {
		return window;
	}

	public Route getRoute() {
		return route;
	}

}
