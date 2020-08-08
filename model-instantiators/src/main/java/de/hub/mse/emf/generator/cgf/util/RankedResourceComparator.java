package de.hub.mse.emf.generator.cgf.util;

import java.util.Comparator;

public class RankedResourceComparator implements Comparator<RankedResource> {
	
	@Override
	public int compare(RankedResource first, RankedResource second) {
		return (first.getScore() - second.getScore());
	}
}
