package de.hub.mse.emf.generator.cgf;

import java.util.ArrayList;
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
import de.hub.mse.emf.generator.serge.SERGeUMLGenerator;
import de.hub.mse.emf.generator.serge.EditRuleWrapper.OperationKind;
import de.hub.mse.emf.fuzz.CoverageGuidance;
import de.hub.mse.emf.fuzz.junit.quickcheck.ModelGenerationStatus;

public class GuidedSERGeUMLGenerator extends InputSavingGenerator<Resource>{
	
	/** The graph-based model generator/mutator to produce initial seed inputs and mutate existing inputs. */
	private SERGeUMLGenerator modelGenerator = new SERGeUMLGenerator();
	
	/** The cyclic queue storing all saved inputs. */
	private ArrayList<Resource> inputQueue = new ArrayList<Resource>();	
	
	/**
	 * List storing the number of new branches covered by each input in the queue
	 * (responsibilities)
	 */
	private List<Integer> responsibilityCounts = new ArrayList<Integer>();
	
	/** Queue storing the 50 most recent inputs that exhibited new coverage */
	Queue<Resource> seedQueue = new LinkedList<Resource>();
	
	/** Number of slots available in the queue */
	int availableSeedSlots = 50;
	
	/** Flag indicating whether we are in the generational or mutational phase */
	private boolean doMutations = false;
	
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
	
	/** The maximum model size. */
	private int maxModelSize = -1;
	
	/** The current queue size */
	private int queueSize = 0;
	
	public GuidedSERGeUMLGenerator() {
		super(Resource.class);
	}
	
	@Override
	public Resource generate(SourceOfRandomness random, GenerationStatus genStatus) {
		
		/*
		 * Phase 1: Collection of a seed corpus
		 * Generate diverse models by applying as many different construction rules as possible
		 * Save the top 50 inpus (with the most number of newly discovered branches)
		 */
		if(inputQueue.isEmpty()) {
			try {
				currentInput = modelGenerator.getSeedModel(random);
			}catch(IllegalStateException e) {
				return null;
			}
			if(genStatus instanceof ModelGenerationStatus) {
				((ModelGenerationStatus) genStatus).update(queueSize, currentInputIdx, currentChildCount, currentTargetChildCount, cycleCount);
			}
			
			if (modelGenerator.getCoverageByKind(OperationKind.CREATE) >= 0.5) {
				doMutations = true;
				
				// To start at 0
				cycleCount = -1;
				currentInputIdx = -1;

				while(!seedQueue.isEmpty()) {
					inputQueue.add(seedQueue.poll());
					responsibilityCounts.add(200); // default value for seeds
				}
			}
			
			return currentInput;
		}
		else {
			// Get current parent from queue
			// Resource parent = inputQueue.get(currentInputIdx);
			
			// Have we already reached the target number of child inputs?
			if(currentChildCount >= currentTargetChildCount) {
				
				// Remove last model copy
				if (currentInput != null) {
					currentInput.unload();
					// currentInput.delete(Collections.EMPTY_MAP);
				}
				
				// Move next in (cyclic) queue
				currentInputIdx = (currentInputIdx + 1) % inputQueue.size();
				
				// Create copy of current parent
				Resource parent = inputQueue.get(currentInputIdx);
				currentInput = modelGenerator.copyModel(parent);
				
				//Determine number of child inputs to be generated
				currentTargetChildCount = getTargetChildCount();
				currentChildCount = 0;
			}
			
			// Mutate current input, which is a copy of the parent at the current input idx
			try {
				modelGenerator.mutateModel(random, currentInput, 10);
				currentChildCount++;
			}
			catch(IllegalStateException e) {
				// Skip to next input in queue
				currentChildCount = currentTargetChildCount;
			}
			
			//modelProvider.mutate(parent, random, genStatus);			
			if(genStatus instanceof ModelGenerationStatus) {
				((ModelGenerationStatus) genStatus).update(queueSize, currentInputIdx, currentChildCount, currentTargetChildCount, cycleCount);
			}
			
			//System.out.println(modelProvider.getEReferenceCoverage()+" - "+modelProvider.getEClassCoverage());
			return currentInput;
		}
	}

	@Override
	public void saveInput(int responsibilities) {
		//inputQueue.add(currentInput);
		if (!doMutations) {
			if(availableSeedSlots > 0) {
				seedQueue.add(modelGenerator.copyModel(currentInput));
				availableSeedSlots--;
			}
			else {
				// Remove oldest entry
				Resource oldest = seedQueue.poll();
				oldest.unload();
				
				// Add new entry to end
				seedQueue.add(modelGenerator.copyModel(currentInput));
			}
		} else {
			// TODO: Only save if responsibilities > N ?
			inputQueue.add(modelGenerator.copyModel(currentInput));
			responsibilityCounts.add(responsibilities);
		}
		//System.out.println("New branches: " + responsibilities + "\t Child inputs: " + Math.round(Math.log10(responsibilities) * 100));
	}
	
	private int getTargetChildCount() {
		// Number of branches this input is responsible for
		int responsibilities = responsibilityCounts.get(currentInputIdx);
		return (int) Math.max(Math.round(Math.log10(responsibilities) * 100), 100);
	}

}
