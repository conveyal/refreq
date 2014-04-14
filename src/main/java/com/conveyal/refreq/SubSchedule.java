package com.conveyal.refreq;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;

public class SubSchedule {

	private ServiceWindow window;
	private ArrayList<Trip> trips;

	public SubSchedule(ServiceWindow window) {
		this.window = window;
		this.trips = new ArrayList<Trip>();
	}

	public void add(Trip trip) {
		this.trips.add(trip);
	}
	
	public String toString(){
		return "("+window.begin+"-"+window.end+") nTrips:"+trips.size();
	}
	
	public Map<List<Stop>, List<Trip>> getPatterns(String dir, GtfsRelationalDaoImpl store){
	    Map<List<Stop>, List<Trip>> patterns = new HashMap<List<Stop>, List<Trip>>();
	
	    for(Trip trip : trips){
	    	
	    	if(!trip.getDirectionId().equals(dir)){
	    		continue;
	    	}
		  
		    List<Stop> pattern = getStopPattern( store, trip );
		  
		    List<Trip> trips = patterns.get(pattern);
		    if(trips==null){
			    trips = new ArrayList<Trip>();
			    patterns.put(pattern, trips);
		    }
		    trips.add( trip );
		  
	    }
	    
	    return patterns;
	}
	
	private static List<Stop> getStopPattern(GtfsRelationalDaoImpl store, Trip trip) {
		List<Stop> pattern = new ArrayList<Stop>();
		
		List<StopTime> stopTimes = store.getStopTimesForTrip( trip );
		for( StopTime stopTime : stopTimes ){
		    pattern.add( stopTime.getStop() );
		}
		  
		return pattern;
	}

	public List<Trip> getTripsInDirection(String string) {
		List<Trip> out = new ArrayList<Trip>();
		for(Trip trip : trips){
			if(trip.getDirectionId().equals(string)){
				out.add(trip);
			}
		}
		return out;
	}

	public ServiceWindow getWindow() {
		return window;
	}

}
