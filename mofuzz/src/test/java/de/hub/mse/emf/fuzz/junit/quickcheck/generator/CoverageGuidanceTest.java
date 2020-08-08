package de.hub.mse.emf.fuzz.junit.quickcheck.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.junit.Test;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.fuzz.junit.quickcheck.ModelGenerationStatus;


/**
 * Tests the standard coverage-guided greybox fuzzing algorithm on a string generator.
 * Involves input selection, mutation/generation, evaluation, and (possibly) saving to the queue.
 * 
 * @author Lam
 */
public class CoverageGuidanceTest {
	
	/**
	 * Runs the fuzzing loop for a fixed budget and records the final coverage.
	 * Guided fuzzing should be able to cover all branches, while random fuzzing should not.
	 * @param guided whether to use coverage-guided fuzzing (true) or random fuzzing (false).
	 */
	public void testCoverage(boolean guided) {
		InputSavingGenerator generator = new SimpleInputSavingGenerator(String.class);
		ModelGenerationStatus genStatus = new ModelGenerationStatus(generator);
		
		SourceOfRandomness random = new SourceOfRandomness(new Random());
		HashSet<Integer> totalCoverage = new HashSet<Integer>();
		
		// Repeat the fuzzing loop for a fixed budget of 100000 iterations.
		for(int i = 1; i < 100000; i++) {
			
			// 1. Generate input
			Object input = generator.generate(random, genStatus);
			
			// 2. Execute		
			Set<Integer> runCoverage = getPassCoverage(input.toString());
			
			// 3. Handle result
			// addAll returns true if any new elements (i.e. new branches) are added to the set
			if(totalCoverage.addAll(runCoverage)) {
				
				// Only save input if we use guided search
				if(guided) genStatus.saveInput(-1);
				//System.out.println("New Coverage for: " + input.toString());
			}
		}
		
		// all branches: [-1, 1, -2, 2, -3, 3, -4, 4, -5, 5, -6, 6, -7, 7, -8, 8]
		
		if(guided) {
			// Guided search should be able to cover all branches
			assertEquals(16, totalCoverage.size());
		}
		else {
			// Random search should NOT be able to cover all branches
			assertNotEquals(16, totalCoverage.size());
		}
	}
	
	/**
	 * Ensure that coverage guided fuzzing is more effective than random fuzzing.
	 */
	@Test
	public void testCoverageGuidance() {
		for(int i = 0; i < 10; i++) {
			testCoverage(true); // Guided fuzzing
			testCoverage(false); // Random fuzzing
		}
	}
	
	
	/**
	 * A simple system under test that returns the set of covered branches.
	 * The branches are encoded as integers, where positive values denote the
	 * "true" branch and negative ones the "false" branches, respectively.
	 * 
	 * @param the input string
	 * @return the set of covered branches
	 */
	private Set<Integer> getPassCoverage(String input) {
		HashSet<Integer> coverage = new HashSet<Integer>();
		
		String secret = "PASSWORD";
		for(int i = 0; i < Math.min(input.length(), secret.length()); i++) {
			if(input.charAt(i) == secret.charAt(i)) {
				
				// Add 1 since we can't distinguish between 0 and -0
				coverage.add(i+1);
			}
			else {
				coverage.add(-(i+1));
				break;
			}
		}
		return coverage;
	}
	
	/**
	 * This class implements a mutation-based string input provider.
	 * New inputs are produced by selecting an input from the queue and mutating it.
	 * Inputs that cover new branches are saved in a cyclic queue.
	 * @author Lam
	 *
	 */
	public class SimpleInputSavingGenerator extends InputSavingGenerator<String>{
		
		/** The cyclic queue storing all saved inputs. */
		private ArrayList<String> inputQueue = new ArrayList<String>();
		
		/** Index pointing to the current parent input in the queue. */
		private int currentInputIdx = 0;
		
		/** Number of currently generated child inputs for the current parent input. */
		private int currentChildCount = 0;
		
		/** Target number of child inputs to be generated for current parent input. */
		private int currentTargetChildCount = 0;
		
		/** The current input. */
		private String currentInput = null;
		
		/** The maximum input length. */
		private int maxInputLength = 8;
		
		protected SimpleInputSavingGenerator(Class<String> type) {
			super(type);
		}
		
		/**
		 * Generates a random string by mutating the next input from the queue.
		 * If the queue is empty, a random string is generated.
		 * @param random source of randomness
		 * @param status generation status instance
		 * @return a new input string, either randomly generated or mutated from another string
		 */
		@Override
		public String generate(SourceOfRandomness random, GenerationStatus status) {
			// Queue is empty, generate random input
			if(inputQueue.isEmpty()) {
				String result = "";
				for(int i = 0; i < random.nextInt(maxInputLength); i++) {
					result += random.nextChar('A', 'Z');
				}
				currentInput = result;
				
				// System.out.println("Queue empty, produced random input: " + currentInput);

				return currentInput;
			}
			else {
				// Get current parent from queue
				String parent = inputQueue.get(currentInputIdx);
				
				// Have we already reached the target number of child inputs?
				if(currentChildCount >= currentTargetChildCount) {
					
					// Move next in (cyclic) queue
					currentInputIdx = (currentInputIdx + 1) % inputQueue.size();
					parent = inputQueue.get(currentInputIdx);
					
					// Determine number of child inputs to be generated
					currentTargetChildCount = getTargetChildCount(parent);
					currentChildCount = 0;						
				}
				
				// Mutate parent
				currentInput = mutate(parent, random);
				currentChildCount++;
				
				//System.out.println("Parent input: " + parent + " Child: " + currentInput);
				return currentInput;
			}
		}
		
		/**
		 * Mutates a given string, by adding, deleting, or replacing a char.
		 * @param string to be mutated
		 * @param random number generator
		 * @return the mutated string
		 */
		private String mutate(String input, SourceOfRandomness random) {
			int len = input.length();
			String result;
			
			// The index where we are going to perform the mutation
			int mutationIdx = random.nextInt(len);
			
			// We can only perform char addition if we haven't reached the max length yet
			int mutationChoices = (input.length() < maxInputLength) ? 3 : 2;
			
			switch(random.nextInt(mutationChoices)) {
			case 0: // replacement
				result = input.substring(0, mutationIdx) + random.nextChar('A', 'Z')
							+ input.substring(mutationIdx + 1, len);
				break;
			case 1: // deletion
				result = input.substring(0, mutationIdx)
							+ input.substring(mutationIdx + 1, len);
				break;
			case 2: // addition
				result = input.substring(0, mutationIdx) + random.nextChar('A', 'Z')
							+ input.substring(mutationIdx, len);
				break;
			default:
				result = "";
				break;
			}
			return result;
		}
		
		/**
		 * Determines the number of child inputs to be generated of the parent input.
		 * In this example, we fix it to 10.
		 * 
		 * @param parentInput
		 * @return the number of child inputs to be generated
		 */
		private int getTargetChildCount(String parentInput) {
			return 100;
		}
		
		/**
		 * Saves the input to the queue.
		 * @param value
		 */
		@Override
		public void saveInput(int value) {
			inputQueue.add(currentInput);
		}
		
	}
}
