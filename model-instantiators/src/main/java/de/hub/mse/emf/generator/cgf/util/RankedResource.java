package de.hub.mse.emf.generator.cgf.util;

import org.eclipse.emf.ecore.resource.Resource;

public class RankedResource {
	private Resource resource;
	private int score;
	
	public RankedResource(Resource resource, int score) {
		this.resource = resource;
		this.score = score;
	}
	
	public int getScore() {
		return score;
	}
	
	public Resource getResource() {
		return resource;
	}
	
	@Override
	public String toString() {
		return "" + this.getScore();
	}
}