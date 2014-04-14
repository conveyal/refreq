package com.conveyal.refreq;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;

public class TripProfile {

	private ArrayList<Stop> stops;
	private List<Integer> meanCrossings;
	private List<Integer> meanDwells;

	public TripProfile(GtfsRelationalDaoImpl store, List<Trip> trips) throws Exception {
		stops = new ArrayList<Stop>();
		
		
		//get representative list of stops
		if(trips.size()==0){
			throw new Exception( "The list of trips can't be empty." );
		}
		
		// Grab every stopTime for every trip. We'll need them.
		List<List<StopTime>> grid = new ArrayList<List<StopTime>>();
		for(Trip trip : trips){
			grid.add( (List<StopTime>) store.getStopTimesForTrip(trip) );
		}
		
		// get rep stop sequence
		List<StopTime> stopTimes = grid.get(0);
		for(StopTime stopTime : stopTimes){
			stops.add(stopTime.getStop());
		}
		
		// get representative time profile
		List<List<Integer>> dwellGrid = new ArrayList<List<Integer>>();
		List<List<Integer>> crossingGrid = new ArrayList<List<Integer>>();
		for(List<StopTime> seq : grid){
			List<Integer> dwells = new ArrayList<Integer>();
			List<Integer> crossings = new ArrayList<Integer>();
			
			for(StopTime st: seq){
				int dwell = st.getDepartureTime()-st.getArrivalTime();
				dwells.add(dwell);
			}
			
			for(int i=0; i<seq.size()-1; i++){
				StopTime st0 = seq.get(i);
				StopTime st1 = seq.get(i+1);
				int crossing = st1.getArrivalTime() - st0.getDepartureTime();
				crossings.add(crossing);
			}
			
			dwellGrid.add(dwells);
			crossingGrid.add(crossings);
		}
		
		// get mean dwells
		meanDwells = Math.getColumnAverages(dwellGrid);
		meanCrossings = Math.getColumnAverages(crossingGrid);
		
	}

	public String toString(){
		String out = "\""+stops.get(0).getId().getId()+"\"("+meanDwells.get(0)+")";
		for(int i=0; i<meanCrossings.size(); i++){
			out += "-"+meanCrossings.get(i)+"-\""+stops.get(i+1).getId().getId()+"\"("+meanDwells.get(i+1)+")";
		}
		return out;
	}

}
