package de.hub.mse.emf.generator.cgf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.emf.ecore.resource.Resource;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.fuzz.junit.quickcheck.generator.InputSavingGenerator;
import de.hub.mse.emf.generator.cgf.util.FixedSizePriorityQueue;
import de.hub.mse.emf.generator.cgf.util.RankedResource;
import de.hub.mse.emf.fuzz.CoverageGuidance;
import de.hub.mse.emf.fuzz.junit.quickcheck.ModelGenerationStatus;

public class CoverageGuidedUMLGenerator extends InputSavingGenerator<Resource> {

	/**
	 * The instance model generator/mutator to produce initial seed inputs and
	 * mutate existing inputs.
	 */
	private UMLModelProvider modelProvider = new UMLModelProvider();

	/** The cyclic queue storing all saved inputs. */
	// TODO Store each model in a separate resource or all in a single resource?
	private List<Resource> inputQueue = new ArrayList<Resource>();

	/** Comparator to compare ranked resources by their score */
	Comparator<RankedResource> comparator = (RankedResource left, RankedResource right) -> left.getScore()
			- right.getScore();

	/**
	 * Priority queue with fixed size to store the top 50 inputs during the
	 * generation phase
	 */
	//FixedSizePriorityQueue<RankedResource> seedPriorityQueue = new FixedSizePriorityQueue<RankedResource>(50,
			//comparator);
	
	/** Queue storing the 50 most recent inputs that exhibited new coverage */
	Queue<Resource> seedQueue = new LinkedList<Resource>();
	
	/** Number of slots available in the queue */
	int availableSeedSlots = 50;

	/**
	 * List storing the number of new branches covered by each input in the queue
	 * (responsibilities)
	 */
	private List<Integer> responsibilityCounts = new ArrayList<Integer>();

	/** Index pointing to the current parent input in the queue. */
	private int currentInputIdx = 0;

	/** Number of currently generated child inputs for the current parent input. */
	private int currentChildCount = 0;

	/** Target number of child inputs to be generated for current parent input. */
	private int currentTargetChildCount = 0;

	/** Number of cycles completed. */
	private int cycleCount = 0;

	/** The current input. */
	private Resource currentInput = null;

	private int runsSinceLastCoverage = 0;
	private int generateThreshold = 500; // No new coverage after 500 execs

	private int queueSize;

	private boolean doMutations = false;

	public CoverageGuidedUMLGenerator() {
		super(Resource.class);
	}

	@Override
	public Resource generate(SourceOfRandomness random, GenerationStatus genStatus) {
		
		runsSinceLastCoverage++;
		if (!doMutations || inputQueue.isEmpty()) {
			currentInput = modelProvider.generate(random, genStatus);
			if (genStatus instanceof ModelGenerationStatus) {
				((ModelGenerationStatus) genStatus).update(queueSize, currentInputIdx, currentChildCount,
						currentTargetChildCount, cycleCount);
			}

			if (modelProvider.baseCoverageReached()) {
				doMutations = true;
				
				// To start at 0
				cycleCount = -1;
				currentInputIdx = -1;
				// Add seed models from priority queue to input queue
				// Note that the list is in reversed order of priority
				/*
				ArrayList<RankedResource> seedResources = seedPriorityQueue.asList();
				for (int i = seedResources.size() - 1; i >= 0; i--) {
					RankedResource seedResource = seedResources.get(i);
					inputQueue.add(seedResource.getResource());

					responsibilityCounts.add(seedResource.getScore());
				}
				*/
				while(!seedQueue.isEmpty()) {
					inputQueue.add(seedQueue.poll());
					responsibilityCounts.add(200); // default value for seeds
				}
			}
			
			if(runsSinceLastCoverage >= generateThreshold) {
				doMutations = true;
			}

			return currentInput;
		} else {
			// Get current parent from queue
			//Resource parent = inputQueue.get(currentInputIdx);

			// Have we already reached the target number of child inputs?
			if (currentChildCount >= currentTargetChildCount) {
				if(currentInputIdx == 0) {
					cycleCount++;
				}
				
				// Remove last model copy
				if (currentInput != null) {
					currentInput.unload();
					// currentInput.delete(Collections.EMPTY_MAP);
				}

				// Flush all previous commands
				modelProvider.flushCommandStack();

				// Move next in (cyclic) queue
				currentInputIdx = (currentInputIdx + 1) % inputQueue.size();
				Resource parent = inputQueue.get(currentInputIdx);

				// Create new copy to apply mutations on
				currentInput = modelProvider.createCopyResource(parent);

				// Determine number of child inputs to be generated
				currentTargetChildCount = getTargetChildCount();
				currentChildCount = 0;
				
			}

			// Mutate copy of current parent
			//modelProvider.undoLastMutation();
			modelProvider.mutate(currentInput, random, genStatus);
			currentChildCount++;

			if (genStatus instanceof ModelGenerationStatus) {
				((ModelGenerationStatus) genStatus).update(inputQueue.size(), currentInputIdx, currentChildCount,
						currentTargetChildCount, cycleCount);
			}

			// System.out.println(modelProvider.getEReferenceCoverage()+" -
			// "+modelProvider.getEClassCoverage());
			return currentInput;
		}
	}

	@Override
	public void saveInput(int responsibilities) {
		runsSinceLastCoverage = 0; // reset counter
		// inputQueue.add(currentInput);
		if (!doMutations) {
			//RankedResource modelResource = new RankedResource(modelProvider.createCopyResource(currentInput),
			//		responsibilities);
			/*
			if (!seedPriorityQueue.add(modelResource)) {
				// Not added to the seed list, throw away
				modelResource.getResource().unload();
			}
			*/
			if(availableSeedSlots > 0) {
				seedQueue.add(modelProvider.createCopyResource(currentInput));
				availableSeedSlots--;
			}
			else {
				// Remove oldest entry
				Resource oldest = seedQueue.poll();
				oldest.unload();
				
				// Add new entry to end
				seedQueue.add(modelProvider.createCopyResource(currentInput));
			}
		} else {
			// TODO: Only save if responsibilities > N ?
			inputQueue.add(modelProvider.createCopyResource(currentInput));
			responsibilityCounts.add(responsibilities);
			modelProvider.commitTempCoveredEClasses();
		}
		// responsibilityCountMap.put(currentInputIdx, responsibilities);
		// System.out.println("New branches: " + responsibilities + "\t Child inputs: "
		// + Math.round(Math.log10(responsibilities) * 100));
	}

	public boolean isMutationPhase() {
		return doMutations;
	}

	private int getTargetChildCount() {
		// Number of branches this input is responsible for
		int responsibilities = responsibilityCounts.get(currentInputIdx);
		// At least 100 mutations
		return (int) Math.max(Math.round(Math.log10(responsibilities) * 100), 100);
	}

}
