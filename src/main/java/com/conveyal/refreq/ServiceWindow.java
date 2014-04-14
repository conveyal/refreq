package com.conveyal.refreq;

public class ServiceWindow {
	public int begin;
	public int end;
	
	ServiceWindow(int begin, int end){
		this.begin=begin;
		this.end=end;
	}

	public boolean contains(int departure) {
		return departure>=begin && departure<end;
	}
	
	public String toString(){
		return "("+begin+"-"+end+")";
	}
}
