package com.conveyal.refreq;

public class ServiceWindow {
	public int begin;
	public int end;
	public String name;
	
	ServiceWindow(int begin, int end, String name){
		this.begin=begin;
		this.end=end;
		this.name=name;
	}

	public boolean contains(int departure) {
		return departure>=begin && departure<end;
	}
	
	public String toString(){
		return "("+begin+"-"+end+")";
	}
}
