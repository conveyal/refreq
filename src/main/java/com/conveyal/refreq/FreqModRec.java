package com.conveyal.refreq;

public class FreqModRec {

	private String route;
	private Double peak_am;
	private Double midday;
	private Double peak_pm;
	private Double night;
	private Double sat;
	private Double sun;
	boolean suppress = false;

	public FreqModRec(String line) {
		String[] fields = line.split(",");
		if(fields.length==2){
			if(fields[1].equals("SUPPRESS")){
				this.route = fields[0];
				this.suppress = true;
			}
			return;
		}
		
		this.route = fields[0];
		this.peak_am = parseField(fields[1]);
		this.midday = parseField(fields[2]);
		this.peak_pm = parseField(fields[3]);
		this.night = parseField(fields[4]);
		this.sat = parseField(fields[5]);
		this.sun = parseField(fields[6]);
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

}
