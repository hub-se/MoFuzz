package de.hub.mse.emf.generator.benchmarks.runner;
import java.util.Random;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.fuzz.junit.quickcheck.ModelGenerationStatus;
import de.hub.mse.emf.generator.UMLGenerator;
import de.hub.mse.emf.generator.benchmarks.*;

/*
 * This class is used to test run benchmark subjects using a particular instance model generator.
 * Note that input generation is purely random in this case (i.e. no coverage feedback).
 */
public class BenchmarkRunner {

	public static void main(String[] args) {
		UMLGenerator generator = new UMLGenerator();
		ModelGenerationStatus genStatus = new ModelGenerationStatus(generator);
		AcceleoUML2JavaHeliosTest uml2java = new AcceleoUML2JavaHeliosTest();
		UML2ValidatorTest umlvalidator = new UML2ValidatorTest();
		EcoreUtilsTest ecoreutils = new EcoreUtilsTest();
		EMFCompareTest emfcompare = new EMFCompareTest();
		UML2OWLTest uml2owl = new UML2OWLTest();
		
		for(int i = 0; i < Integer.MAX_VALUE; i++) {
			long seed = System.currentTimeMillis();					
			SourceOfRandomness rand = new SourceOfRandomness(new Random(seed));
			Resource modelResource = null;
			
			try {
					
				generator.generate(rand, null);
				modelResource = generator.generate(rand, null);
				
				int size = 0;
				for(TreeIterator<EObject> iter = modelResource.getAllContents(); iter.hasNext(); iter.next()){
					size++;
				}
				System.out.println("Generated model of size: " + size);
			}
			catch(Exception e) {
				System.out.println("Exception for seed: " + seed);
				e.printStackTrace();
			}
		}
	}
}
