package com.conveyal.refreq;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;

public class FreqSubschedule {

	private ServiceWindow window;
	private int period;
	private TripProfile tripProfile;
	private String direction;
	private Double offset;
	private String offsetStop;

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
	
	public Trip makeTrip(String id) {
		Trip trip = new Trip();
		
		trip.setDirectionId(this.direction);
		
		trip.setRoute(this.tripProfile.exemplar.getRoute());
		trip.setId( new AgencyAndId(this.tripProfile.exemplar.getRoute().getAgency().getId(), id) );
		
		return trip;
	}

	public List<StopTime> makeStopTimes(Trip trip) {
		Integer preFixedOffset=null;
		
		List<StopTime> stopTimes = new ArrayList<StopTime>();
		int time=0;
		for(int i=0; i<tripProfile.stops.size(); i++){
			Stop stop = tripProfile.stops.get(i);
			
			StopTime st = new StopTime();
			
			st.setTrip(trip);
			st.setStop(stop);
			st.setStopSequence(i);
			
			if(i!=0){
				time += tripProfile.meanCrossings.get(i-1);
			}
			
			if(this.offsetStop!=null && stop.getId().getId().equals(this.offsetStop)){
				preFixedOffset = time;
			}
			
			st.setArrivalTime(time);
			time += tripProfile.meanDwells.get(i);
			st.setDepartureTime(time);
			
			stopTimes.add(st);
		}
		
//		// slide everything around by the offset if possible
//		if(preFixedOffset!=null && this.offset!=null){
//			Integer diff = this.offset.intValue() - preFixedOffset;
//			for(StopTime st : stopTimes){
//				st.setArrivalTime(st.getArrivalTime() + diff);
//				st.setDepartureTime(st.getDepartureTime() + diff);
//			}
//		}
		
		return stopTimes;
	}
	
	public Integer getPreFixedOffset(Trip trip) {
		if( this.offsetStop==null ){
			return null;
		}
		
		int time=0;
		for(int i=0; i<tripProfile.stops.size(); i++){
			Stop stop = tripProfile.stops.get(i);
			
			if(i!=0){
				time += tripProfile.meanCrossings.get(i-1);
			}
			
			if(stop.getId().getId().equals(this.offsetStop)){
				return time;
			}
			
			time += tripProfile.meanDwells.get(i);
		}
		
		return null;
	}
	
	public Frequency makeFrequency(Trip trip, double wait_factor){
		Frequency ret = new Frequency();
		
		Integer offset = getPreFixedOffset(trip);
		if(this.offset!=null && offset!=null){
			ret.setStartTime( this.window.begin-(offset-this.offset.intValue()) );
		} else {
			ret.setStartTime( this.window.begin );
		}
		ret.setEndTime( this.window.end );
		
		ret.setHeadwaySecs( (int)(this.period/wait_factor) );
		ret.setTrip(trip);
		return ret;
	}
	
	public Frequency makeFrequency(Trip trip){
		return makeFrequency(trip,1.0);
	}

	public void setOffset(Double offset) {
		this.offset = offset;
	}

	public void setOffsetStop(String offsetStop) {
		this.offsetStop = offsetStop;
	}

}
