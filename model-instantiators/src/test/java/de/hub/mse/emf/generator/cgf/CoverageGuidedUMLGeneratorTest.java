package de.hub.mse.emf.generator.cgf;

import java.util.Random;

import org.junit.Test;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.fuzz.junit.quickcheck.ModelGenerationStatus;

public class CoverageGuidedUMLGeneratorTest {
	@Test
	public void testGenerator() {
		CoverageGuidedUMLGenerator generator = new CoverageGuidedUMLGenerator();
		SourceOfRandomness random = new SourceOfRandomness(new Random(24));
		ModelGenerationStatus genStatus = new ModelGenerationStatus(generator);
		
		int i = 0;
		while(!generator.isMutationPhase()) {
			System.out.println(++i);
			generator.generate(random, genStatus);
			generator.saveInput(random.nextInt(1000));
		}
		
		for(int j= 0; j< 10000; j++) {
			generator.generate(random, genStatus);
			System.out.println("-----------------------");
			System.out.printf("Queue size:      %d \n", genStatus.size());
			System.out.printf("Parent idx:      %d \n", genStatus.getCurrentParentIdx());
			System.out.printf(" Progress :      (%d/%d) \n", genStatus.getCurrentChildCount(), genStatus.getTargetNumChildren());
		}
		System.out.println("");
	}
}
