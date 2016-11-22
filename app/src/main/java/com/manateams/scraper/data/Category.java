package com.manateams.scraper.data;

public class Category {
	
	public String id;
	public String title;
	public double weight;
	public Double average;
	public Double projectedAverage;
	public double bonus;
	public Assignment[] assignments;

	public boolean hasProjected() {
		for (Assignment a : assignments) {
			if (a.isProjected) return true;
		}
		return false;
	}

}
