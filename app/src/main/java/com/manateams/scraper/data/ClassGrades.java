package com.manateams.scraper.data;

public class ClassGrades {
	
	public String title;
	public String urlHash;
	public int period;
	public int semesterIndex;
	public int cycleIndex;
	public int average;
	public int projectedAverage;
	public Category[] categories;

	public boolean hasProjected() {
		for (Category c : categories) {
			if (c.hasProjected()) return true;
		}
		return false;
	}

}
