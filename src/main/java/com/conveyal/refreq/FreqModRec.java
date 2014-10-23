package com.conveyal.refreq;

import java.util.Map;

import org.onebusaway.gtfs.model.Trip;

public class FreqModRec {

	private String route=null;
	private String trip=null;
	private Double peak_am=null;
	private Double midday=null;
	private Double peak_pm=null;
	private Double night=null;
	private Double sat=null;
	private Double sun=null;
	boolean suppress = false;
	boolean absolute = false;
	Double offset;
	String offsetStop;

	public FreqModRec(String line, Map<String, Integer> header) {
		String[] fields = line.split(",");
		if(fields.length==2){
			if(fields[1].equals("SUPPRESS")){
				this.route = fields[0];
				this.suppress = true;
			}
			return;
		}
		
		if(header.containsKey("route")){
			this.route = fields[header.get("route")];
		}
		if(header.containsKey("trip")){
			this.trip = fields[header.get("trip")];
		}
		if(header.containsKey("peak_am")) this.peak_am = parseField(fields[header.get("peak_am")]);
		if(header.containsKey("midday")) this.midday = parseField(fields[header.get("midday")]);
		if(header.containsKey("peak_pm")) this.peak_pm = parseField(fields[header.get("peak_pm")]);
		if(header.containsKey("night")) this.night = parseField(fields[header.get("night")]);
		if(header.containsKey("sat")) this.sat = parseField(fields[header.get("sat")]);
		if(header.containsKey("sun")) this.sun = parseField(fields[header.get("sun")]);
		
		if(header.containsKey("absolute")){
			this.absolute = Boolean.parseBoolean( fields[header.get("absolute")] );
		}
		
		if(header.containsKey("offset")){
			this.offset = parseField( fields[header.get("offset")] );
		}
		
		if(header.containsKey("offset_stop")){
			this.offsetStop = fields[header.get("offset_stop")];
		}
		
	}

	private Double parseField(String string) {
		if(string.equals("inf")){
			return Double.POSITIVE_INFINITY;
		}
		if(string.equals("None")){
			return null;
		}
		return Double.parseDouble(string);
	}

	public String getRoute() {
		return route;
	}
	
	public String getTrip() {
		return trip;
	}

	public Double getMult(String name) throws Exception {
		if(name.equals("peak_am")){
			return this.peak_am;
		} else if(name.equals("midday")){
			return this.midday;
		} else if(name.equals("peak_pm")){
			return this.peak_pm;
		} else if(name.equals("night")){
			return this.night;
		} else if(name.equals("sat")){
			return this.sat;
		} else if(name.equals("sun")){
			return this.sun;
		} else {
			throw new Exception( "unknown name" );
		}
	}

	public boolean matches(Trip trip) {
		return (this.route!=null && trip.getRoute().getShortName().equals(this.route)) || (this.trip!=null && trip.getId().getId().matches(this.trip)); 
	}

}
