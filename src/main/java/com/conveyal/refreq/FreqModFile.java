package com.conveyal.refreq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FreqModFile {
	Map<String,FreqModRec> mods;

	public FreqModFile(String filename) throws IOException {
		mods = new HashMap<String,FreqModRec>();
		
		BufferedReader br = new BufferedReader(new FileReader(new File(filename)));
		
		br.readLine(); //read header line
		
		String line;
		while ((line = br.readLine()) != null) {
			FreqModRec mod = new FreqModRec(line);
			mods.put(mod.getRoute(), mod);
		}
		br.close();
	}

	public FreqModRec getRoute(String shortName) {
		return mods.get(shortName);
	}

}
