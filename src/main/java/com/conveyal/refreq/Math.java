package com.conveyal.refreq;

import java.util.ArrayList;
import java.util.List;

public class Math {
	static double mean(List<Integer> col) {
		int sum=0;
		for(Integer i : col){
			sum+=i;
		}
		return ((double)sum)/col.size();
	}
	
	static List<Integer> getColumnAverages(List<List<Integer>> grid) {
		List<Integer> colMeans = new ArrayList<Integer>();
		
		int gridWidth = grid.get(0).size();
		for(int i=0; i<gridWidth; i++){
			List<Integer> col = new ArrayList<Integer>();
			
			for(List<Integer> row : grid){
				col.add(row.get(i));
			}
			
			int colMean = (int)Math.mean(col);
			colMeans.add(colMean);
		}
		
		return colMeans;
	}
}
