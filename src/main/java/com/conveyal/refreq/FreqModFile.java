package com.conveyal.refreq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class FreqModFile {
	ArrayList<FreqModRec> mods;

	public FreqModFile(String filename) throws IOException {
		mods = new ArrayList<FreqModRec>();
		
		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		
		Map<String, Integer> header = parseHeader( br.readLine() ); //read header line
		
		String line;
		while ((line = br.readLine()) != null) {
			FreqModRec mod = new FreqModRec(line, header);
			mods.add(mod);
		}
		br.close();
	}

	private Map<String, Integer> parseHeader(String readLine) {
		String[] fields = readLine.split(",");
		HashMap<String,Integer> ret = new HashMap<String,Integer>();
		for(int i=0; i<fields.length; i++){
			ret.put(fields[i], i);
		}
		return ret;
	}

	public FreqModRec getRoute(String shortName) {
		for(FreqModRec fmr : mods ){
			if(fmr.getRoute().equals(shortName)){
				return fmr;
			}
		}
		return null;
	}

}
