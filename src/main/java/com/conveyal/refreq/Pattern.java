package com.conveyal.refreq;

import java.util.ArrayList;
import java.util.List;

import org.onebusaway.gtfs.model.Stop;

public class Pattern {
	
	List<Stop> stops = new ArrayList<Stop>();

	public void add(Stop stop) {
		stops.add(stop);
	}

}
