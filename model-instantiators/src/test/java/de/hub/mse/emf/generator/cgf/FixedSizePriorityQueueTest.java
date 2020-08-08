package de.hub.mse.emf.generator.cgf;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Comparator;

import org.junit.Test;

import de.hub.mse.emf.generator.cgf.util.FixedSizePriorityQueue;
import de.hub.mse.emf.generator.cgf.util.RankedResource;

public class FixedSizePriorityQueueTest {
	
	@Test
	public void rankedResourceTest() {
		Comparator<RankedResource> comparator =
				(RankedResource left, RankedResource right) -> left.getScore() - right.getScore();
		FixedSizePriorityQueue<RankedResource> priorityQueue = new FixedSizePriorityQueue<RankedResource>(5, comparator);
		
		// Test ascending insertion order
		int[] ascending = {1,2,3,4,5,6,7,8,9,10};
		
		for(int i : ascending) {
			RankedResource resource = new RankedResource(null, i);
			priorityQueue.add(resource);
		}
		
		assertEquals(priorityQueue.asList().toString(), "[6, 7, 8, 9, 10]");
		
		// Test descending insertion order
		priorityQueue = new FixedSizePriorityQueue<RankedResource>(5, comparator);
		int[] descending = {10,9,8,7,6,5,4,3,2,1};
		
		for(int i : descending) {
			RankedResource resource = new RankedResource(null, i);
			priorityQueue.add(resource);
		}
		assertEquals(priorityQueue.asList().toString(), "[6, 7, 8, 9, 10]");
		
		
		// Test random order
		priorityQueue = new FixedSizePriorityQueue<RankedResource>(5, comparator);
		int[] random = {4,2,1,3,8,9,10,6,7,5};
		
		for(int i : random) {
			RankedResource resource = new RankedResource(null, i);
			priorityQueue.add(resource);
		}
		assertEquals(priorityQueue.asList().toString(), "[6, 7, 8, 9, 10]");
	}
}
