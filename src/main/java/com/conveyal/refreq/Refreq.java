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
import org.onebusaway.gtfs.model.Frequency;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.onebusaway.gtfs.model.calendar.ServiceDate;
import org.onebusaway.gtfs.serialization.GtfsReader;
import org.onebusaway.gtfs.serialization.GtfsWriter;

public class Refreq {
	static double WAIT_FACTOR = 1.0;
	static int DATE_RANGE_RADIUS = 5;

	public static void main(String[] args) throws Exception {

		if (args.length < 2) {
			System.err.println("usage: gtfs_feed_path YYYYMMDD [mod_file]");
			System.exit(-1);
		}

		FreqModFile mods = null;
		if (args.length == 3) {
			mods = new FreqModFile(args[2]);
		}

		// read sample date
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
		ServiceDate sd = new ServiceDate(sdf.parse(args[1]));
		System.out.println(sd);

		// establish important service windows
		List<ServiceWindow> windows = new ArrayList<ServiceWindow>();
		windows.add(new ServiceWindow(3600 * 6, 3600 * 9, "peak_am")); // morning
																		// commute
		windows.add(new ServiceWindow(3600 * 9, 3600 * 15, "midday")); // midday
		windows.add(new ServiceWindow(3600 * 15, 3600 * 18, "peak_pm")); // afternoon
																			// commute
		windows.add(new ServiceWindow(3600 * 18, 3600 * 24, "night")); // night

		// read in the GTFS
		GtfsReader reader = new GtfsReader();
		reader.setInputLocation(new File(args[0]));

		GtfsRelationalDaoImpl store = new GtfsRelationalDaoImpl();
		reader.setEntityStore(store);

		System.out.print("reading gtfs file...");
		reader.run();
		System.out.println("done.");

		// get every serviceid running on this date. laboriously.
		CalendarServiceDataFactoryImpl csdfi = new CalendarServiceDataFactoryImpl(store);
		CalendarServiceData csd = csdfi.createData();
		CalendarServiceImpl csi = new CalendarServiceImpl(csd);
		Set<AgencyAndId> serviceIds = csi.getServiceIdsOnDate(sd);

		List<FreqSubschedule> allFreqSubs = new ArrayList<FreqSubschedule>();

		// loop through routes, generating frequency subschedules
		for (FreqModRec mod : mods.mods) {
			if (mod.suppress) {
				System.out.println("suppress");
				continue;
			}

			List<FreqSubschedule> freqSubs = buildFrequencySchedulesForRoute(windows, store, serviceIds, mod);

			allFreqSubs.addAll(freqSubs);
		}

		GtfsWriter writer = new GtfsWriter();
		writer.setOutputLocation(new File("gtfs_freq.zip"));

		// upload all original agencies
		for (Agency agency : store.getAllAgencies()) {
			writer.handleEntity(agency);
		}

		// and routes
		for (Route route : store.getAllRoutes()) {
			writer.handleEntity(route);
		}

		// and stops
		for (Stop stop : store.getAllStops()) {
			writer.handleEntity(stop);
		}

		// create a service id that only runs on this one day
		// ServiceCalendarDate scald = new ServiceCalendarDate();
		// scald.setDate( sd );
		// scald.setExceptionType(ServiceCalendarDate.EXCEPTION_TYPE_ADD);
		// scald.setServiceId(new AgencyAndId(null,"1"));
		// writer.handleEntity(scald);
		ServiceCalendar scal = new ServiceCalendar();
		scal.setStartDate(sd.shift(-DATE_RANGE_RADIUS));
		scal.setEndDate(sd.shift(DATE_RANGE_RADIUS));
		scal.setMonday(1);
		scal.setTuesday(1);
		scal.setWednesday(1);
		scal.setThursday(1);
		scal.setFriday(1);
		scal.setSaturday(1);
		scal.setSunday(1);
		scal.setServiceId(new AgencyAndId(null, "1"));
		writer.handleEntity(scal);

		List<Trip> tripQueue = new ArrayList<Trip>();
		List<StopTime> stopTimeQueue = new ArrayList<StopTime>();
		List<Frequency> frequencyQueue = new ArrayList<Frequency>();

		Integer tripCounter = 0;
		for (FreqSubschedule freqSub : allFreqSubs) {
			Trip trip = freqSub.makeTrip(tripCounter.toString());
			trip.setServiceId(new AgencyAndId(null, "1")); // runs on the only
															// service period, a
															// single day
			tripCounter++;
			tripQueue.add(trip);

			List<StopTime> stopTimes = freqSub.makeStopTimes(trip);
			stopTimeQueue.addAll(stopTimes);

			frequencyQueue.add(freqSub.makeFrequency(trip, WAIT_FACTOR));

		}

		for (Trip trip : tripQueue) {
			writer.handleEntity(trip);
		}
		for (StopTime st : stopTimeQueue) {
			writer.handleEntity(st);
		}
		for (Frequency fr : frequencyQueue) {
			writer.handleEntity(fr);
		}

		writer.close();
	}

	private static List<FreqSubschedule> buildFrequencySchedulesForRoute(List<ServiceWindow> windows,
			GtfsRelationalDaoImpl store, Set<AgencyAndId> serviceIds, FreqModRec mod) throws Exception {
		List<FreqSubschedule> freqSubs = new ArrayList<FreqSubschedule>();

		System.out.println("mod: " + mod);

		// filter trips to only trips that run on the sample date
		ArrayList<Trip> trips = new ArrayList<Trip>();
		for (AgencyAndId serviceId : serviceIds) {
			for (Trip trip : store.getTripsForServiceId(serviceId)) {
				if (mod.matches(trip)) {
					trips.add(trip);
				}
			}
		}

		// group trips into service windows

		// prepare service windows
		Map<ServiceWindow, SubSchedule> subSchedules = new HashMap<ServiceWindow, SubSchedule>();
		for (ServiceWindow window : windows) {
			subSchedules.put(window, new SubSchedule(window));
		}
		// file trips away
		for (Trip trip : trips) {
			// group into windows by origin departure time
			StopTime first = store.getStopTimesForTrip(trip).get(0);
			int departure = first.getDepartureTime();
			for (ServiceWindow window : windows) {
				if (window.contains(departure)) {
					subSchedules.get(window).add(trip);
				}
			}
		}

		// determine dominant pattern in each direction for service window
		for (SubSchedule subSchedule : subSchedules.values()) {

			FreqSubschedule out = buildFreqSubschedule(store, subSchedule, "1", mod.offset, mod.offsetStop);

			// get mod if it exists
			Double periodMult = null;
			if (mod != null) {
				periodMult = mod.getMult(subSchedule.getWindow().name);

				if (periodMult != null) {
					// if the mod is positive infinity, this route/window has
					// been cancelled
					if (periodMult.isInfinite()) {
						System.out.println("some trips window " + subSchedule.getWindow().name + " has been cancelled");
						continue;
					} else if (periodMult == 0) {
						// likewise if the mult is 0, it goes form no trips to
						// some trips
						// TODO handle this properly
						continue;
					}
				}
			}

			if (out == null) {
				// System.out.println("no trips");
			} else {
				freqSubs.add(out);
				// System.out.println(
				// "route "+out.getRoute().getShortName()+", direction "+out.getDirection()+", window "+out.getWindow()
				// );
				// System.out.println( "rep_period:"+out.getPeriod() );
				// System.out.println( "rep trip:"+out.getTripProfile() );
			}

			FreqSubschedule inward = buildFreqSubschedule(store, subSchedule, "0", mod.offset, mod.offsetStop);

			if (inward == null) {
				// System.out.println("no trips");
			} else {
				freqSubs.add(inward);
				// System.out.println(
				// "route "+inward.getRoute().getShortName()+", direction "+inward.getDirection()+", window "+inward.getWindow()
				// );
				// System.out.println( "rep_period:"+inward.getPeriod() );
				// System.out.println( "rep trip:"+inward.getTripProfile() );
			}

			if (mod.absolute) {
				if (out != null) {
					out.setPeriod(periodMult.intValue());
				}
				if (inward != null) {
					inward.setPeriod(periodMult.intValue());
				}
			} else {
				if (periodMult != null && periodMult != 1.0) {
					if (out != null) {
						System.out.println("some trips outbound window " + subSchedule.getWindow().name + " period ->"
								+ periodMult);
						out.setPeriod((int) (out.getPeriod() * periodMult));
					}
					if (inward != null) {
						System.out.println("some trips inbound window " + subSchedule.getWindow().name + " period ->"
								+ periodMult);
						inward.setPeriod((int) (inward.getPeriod() * periodMult));
					}
				}
			}

		}
		return freqSubs;
	}

	private static FreqSubschedule buildFreqSubschedule(GtfsRelationalDaoImpl store, SubSchedule subSchedule,
			String direction, Double offset, String offsetStop) throws Exception {
		List<Trip> inbound = subSchedule.getTripsInDirection(direction);

		if (inbound.size() == 0) {
			return null;
		}

		FreqSubschedule ret = new FreqSubschedule();
		ret.setOffset( offset );
		ret.setOffsetStop( offsetStop );
		ret.setWindow(subSchedule.getWindow());
		ret.setDirection(direction);

		Map<List<Stop>, List<Trip>> patterns = getPatterns(store, inbound);

		// pick the dominant pattern for inbound trips
		int winnerScore = 0;
		List<Trip> domPattern = null;
		for (List<Trip> patternTrips : patterns.values()) {
			if (patternTrips.size() > winnerScore) {
				domPattern = patternTrips;
				winnerScore = patternTrips.size();
			}
		}

		// get representative period of all trips in this route/window/direction
		Integer rep_period = null;
		if (inbound.size() == 1) {
			ServiceWindow sw = subSchedule.getWindow();
			rep_period = (sw.end - sw.begin) / 2;
		} else {
			List<StopTime> starts = new ArrayList<StopTime>();
			for (Trip trip : inbound) {
				StopTime start = store.getStopTimesForTrip(trip).get(0);
				starts.add(start);
			}
			Collections.sort(starts, new Comparator<StopTime>() {
				@Override
				public int compare(StopTime arg0, StopTime arg1) {
					return arg0.getDepartureTime() - arg1.getDepartureTime();
				}
			});

			ArrayList<Integer> periods = new ArrayList<Integer>();
			for (int i = 0; i < starts.size() - 1; i++) {
				int period = starts.get(i + 1).getDepartureTime() - starts.get(i).getDepartureTime();
				periods.add(period);
			}

			if (periods.size() > 0) {
				rep_period = (int) Math.mean(periods);
			}
		}
		ret.setPeriod(rep_period);

		if (domPattern != null && domPattern.size() != 0) {
			// get representative stop/timing profile for trip pattern
			TripProfile repTripProfile = new TripProfile(store, domPattern);
			ret.setTripProfile(repTripProfile);
		}
		return ret;
	}

	public static Map<List<Stop>, List<Trip>> getPatterns(GtfsRelationalDaoImpl store, List<Trip> trips) {
		Map<List<Stop>, List<Trip>> patterns = new HashMap<List<Stop>, List<Trip>>();

		for (Trip trip : trips) {

			List<Stop> pattern = getStopPattern(store, trip);

			List<Trip> patternTrips = patterns.get(pattern);
			if (patternTrips == null) {
				patternTrips = new ArrayList<Trip>();
				patterns.put(pattern, patternTrips);
			}
			patternTrips.add(trip);

		}

		return patterns;
	}

	private static List<Stop> getStopPattern(GtfsRelationalDaoImpl store, Trip trip) {
		List<Stop> pattern = new ArrayList<Stop>();

		List<StopTime> stopTimes = store.getStopTimesForTrip(trip);
		for (StopTime stopTime : stopTimes) {
			pattern.add(stopTime.getStop());
		}

		return pattern;
	}

}
