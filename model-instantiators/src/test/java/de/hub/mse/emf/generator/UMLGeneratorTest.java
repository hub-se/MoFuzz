package de.hub.mse.emf.generator;
import static org.junit.Assert.assertEquals;

import java.util.Random;

import org.eclipse.emf.ecore.resource.Resource;
import org.junit.*;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

public class UMLGeneratorTest {
	UMLGenerator generator;
	
	@Before
	public void setUp() {
		generator = new UMLGenerator();
	}
	
	// Test if resources are being properly unloaded
	@Test
	public void resourceTest() {
		SourceOfRandomness rand = new SourceOfRandomness(new Random(1001));
		
		Resource resource = generator.generate(rand, null);
		for(int i = 0; i < 10; i++) {
			resource = generator.generate(rand, null);
		}
		assertEquals(resource.getResourceSet().getResources().size(), 1);
	}
	
}
