package com.conveyal.refreq;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.onebusaway.csv_entities.EntityHandler;
import org.onebusaway.gtfs.impl.GtfsDaoImpl;
import org.onebusaway.gtfs.impl.GtfsRelationalDaoImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceDataFactoryImpl;
import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.serialization.GtfsReader;

public class Refreq {
	  public static void main(String[] args) throws Exception {

		    if (args.length != 2) {
		      System.err.println("usage: gtfs_feed_path YYYYMMDD");
		      System.exit(-1);
		    }
		    
		    // read sample date
		    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		    ServiceDate sd = new ServiceDate( sdf.parse(args[1]) );
		    System.out.println( sd );
		    
		    // establish important service windows
		    List<ServiceWindow> windows = new ArrayList<ServiceWindow>();
		    windows.add( new ServiceWindow(3600*4,3600*6) );   // pre-morning
		    windows.add( new ServiceWindow(3600*6,3600*9) );   // morning commute
		    windows.add( new ServiceWindow(3600*9,3600*15) );  // midday
		    windows.add( new ServiceWindow(3600*15,3600*18) ); // afternoon commute
		    windows.add( new ServiceWindow(3600*18,3600*24) ); // evening

		    // read in the GTFS
		    GtfsReader reader = new GtfsReader();
		    reader.setInputLocation(new File(args[0]));
		    
		    GtfsRelationalDaoImpl store = new GtfsRelationalDaoImpl();
		    reader.setEntityStore(store);

		    System.out.print("reading gtfs file...");
		    reader.run();
		    System.out.println("done.");
		    
		    // get every serviceid running on this date. laboriously.
		    CalendarServiceDataFactoryImpl  csdfi = new CalendarServiceDataFactoryImpl(store);
		    CalendarServiceData csd = csdfi.createData();
		    CalendarServiceImpl csi = new CalendarServiceImpl( csd ); 
		    Set<AgencyAndId> serviceIds = csi.getServiceIdsOnDate(sd);
		    
		    // loop through routes, generating  frequency subschedules
		    for (Route route : store.getAllRoutes()) {
		    	List<FreqSubschedule> freqSubs = buildFrequencySchedulesForRoute(
						windows, store, serviceIds, route);
		      
		        System.out.println( freqSubs );
		    }
		    
	  }

	private static List<FreqSubschedule> buildFrequencySchedulesForRoute(
			List<ServiceWindow> windows, GtfsRelationalDaoImpl store,
			Set<AgencyAndId> serviceIds, Route route) throws Exception {
		List<FreqSubschedule> freqSubs = new ArrayList<FreqSubschedule>();
		
		System.out.println("route: " + route.getShortName());
		
		// filter trips to only trips that run on the sample date
		ArrayList<Trip> trips = new ArrayList<Trip>();
		for(Trip trip : store.getTripsForRoute(route)){
			if( serviceIds.contains(trip.getServiceId()) ){
				trips.add(trip);
			}
		}
		
		// group trips into service windows
		
		// prepare service windows
		Map<ServiceWindow, SubSchedule> subSchedules = new HashMap<ServiceWindow, SubSchedule>();
		for(ServiceWindow window : windows){
			subSchedules.put(window, new SubSchedule(window));
		}
		// file trips away
		for(Trip trip : trips){
			// group into windows by origin departure time
			StopTime first = store.getStopTimesForTrip(trip).get(0);
			int departure = first.getDepartureTime();
			for(ServiceWindow window : windows){
				if( window.contains(departure) ){
					subSchedules.get(window).add(trip);
				}
			}
		}

		//determine dominant pattern in each direction for service window
		for(SubSchedule subSchedule : subSchedules.values() ){
			FreqSubschedule out = buildFreqSubschedule(store, route,
					subSchedule, "1");
			
			if(out==null){
//				System.out.println("no trips");
			} else {
				freqSubs.add(out);
//		    	System.out.println( "route "+out.getRoute().getShortName()+", direction "+out.getDirection()+", window "+out.getWindow() );
//		    	System.out.println( "rep_period:"+out.getPeriod() );
//		    	System.out.println( "rep trip:"+out.getTripProfile() );
			}
			
			FreqSubschedule inward = buildFreqSubschedule(store, route,
					subSchedule, "0");
			
			if(inward==null){
//				System.out.println("no trips");
			} else {
				freqSubs.add( inward );
//		    	System.out.println( "route "+inward.getRoute().getShortName()+", direction "+inward.getDirection()+", window "+inward.getWindow() );
//		    	System.out.println( "rep_period:"+inward.getPeriod() );
//		    	System.out.println( "rep trip:"+inward.getTripProfile() );
			}

		}
		return freqSubs;
	}

	private static FreqSubschedule buildFreqSubschedule(
			GtfsRelationalDaoImpl store, Route route, SubSchedule subSchedule,
			String direction) throws Exception {
		List<Trip> inbound = subSchedule.getTripsInDirection(direction);
		
		if(inbound.size()==0){
			return null;
		}
		
		FreqSubschedule ret = new FreqSubschedule();
		ret.setWindow( subSchedule.getWindow() );
		ret.setDirection( direction );
		ret.setRoute( route );
		
		Map<List<Stop>, List<Trip>> patterns = getPatterns( store, inbound );
		
		//pick the dominant pattern for inbound trips
		int winnerScore = 0;
		List<Trip> domPattern = null;
		for( List<Trip> patternTrips : patterns.values() ){
			if( patternTrips.size()>winnerScore ){
				domPattern = patternTrips;
				winnerScore = patternTrips.size();
			}
		}
		
		// get representative period of all trips in this route/window/direction
		Integer rep_period=null;
		if(inbound.size()==1){
			ServiceWindow sw = subSchedule.getWindow();
			rep_period = (sw.end-sw.begin)/2;
		} else {
			List<StopTime> starts = new ArrayList<StopTime>();
			for(Trip trip : inbound){
				StopTime start = store.getStopTimesForTrip(trip).get(0);
				starts.add(start);
			}
			Collections.sort(starts, new Comparator<StopTime>(){
				@Override
				public int compare(StopTime arg0, StopTime arg1) {
					return arg0.getDepartureTime()-arg1.getDepartureTime();
				}
			});
			
			ArrayList<Integer> periods = new ArrayList<Integer>();
			for(int i=0; i<starts.size()-1; i++){
				int period = starts.get(i+1).getDepartureTime()-starts.get(i).getDepartureTime();
				periods.add(period);
			}
			
			if(periods.size()>0){
				rep_period = (int)Math.mean( periods );
			}
		}
		ret.setPeriod( rep_period );
		
		if(domPattern!=null && domPattern.size()!=0){
			// get representative stop/timing profile for trip pattern
			TripProfile repTripProfile = new TripProfile( store, domPattern );
			ret.setTripProfile( repTripProfile );
		}
		return ret;
	}

		public static Map<List<Stop>, List<Trip>> getPatterns(GtfsRelationalDaoImpl store, List<Trip> trips){
		    Map<List<Stop>, List<Trip>> patterns = new HashMap<List<Stop>, List<Trip>>();
		
		    for(Trip trip : trips){ 
			  
			    List<Stop> pattern = getStopPattern( store, trip );
			  
			    List<Trip> patternTrips = patterns.get(pattern);
			    if(patternTrips==null){
				    patternTrips = new ArrayList<Trip>();
				    patterns.put(pattern, patternTrips);
			    }
			    patternTrips.add( trip );
			  
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


}


