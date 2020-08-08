package de.hub.mse.emf.mutator.mutation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UMLPackage.Literals;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.junit.Before;
import org.junit.Test;

import de.hub.mse.emf.generator.ModelGenerationConfigImpl;
import de.hub.mse.emf.generator.cgf.MetamodelCoverage;
import de.hub.mse.emf.generator.internal.MetamodelResource;
import de.hub.mse.emf.generator.internal.MetamodelUtil;
import de.hub.mse.emf.mutator.mutation.MutationTargetSelector;

public class MutationTargetSelectorTest {
	private ResourceSetImpl resourceSet;
	private MetamodelResource metamodelResource;
	private HashSet<EClass> eClassWhitelist;
	private ModelGenerationConfigImpl config;
	private MetamodelUtil metamodelUtil;
	private MetamodelCoverage metamodelCoverage;
	private MutationTargetSelector targetSelector;
		
	@Before
	public void setUp() {
		resourceSet = new ResourceSetImpl();
		resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
		UMLPackage.eINSTANCE.eClass();			
		EcoreUtil.resolveAll(UMLPackage.eINSTANCE);
		metamodelResource = new MetamodelResource(UMLPackage.eINSTANCE);
		eClassWhitelist = new HashSet<EClass>();
		config = new ModelGenerationConfigImpl(metamodelResource, Literals.MODEL, eClassWhitelist, 1500,  15, 15 , 15);
		metamodelUtil = new MetamodelUtil(config.ePackages(), config.ignoredEClasses(), config.getEClassWhitelist());
		
		HashSet<EClass> allEClasses = new HashSet<EClass>();
		allEClasses.add(UMLPackage.Literals.MODEL);
		allEClasses.add(UMLPackage.Literals.CLASS);
		allEClasses.add(UMLPackage.Literals.OPERATION);
		
		HashSet<EReference> allEContainmentRefs = new HashSet<EReference>();
		allEContainmentRefs.add(UMLPackage.Literals.PACKAGE__PACKAGED_ELEMENT);
		allEContainmentRefs.add(UMLPackage.Literals.CLASS__OWNED_OPERATION);
		
		metamodelCoverage = new MetamodelCoverage(allEClasses, allEContainmentRefs);
		targetSelector = new MutationTargetSelector(new Random(24), null, metamodelUtil, metamodelCoverage);
	}
	
	@Test
	public void testWeightedSelection() {
		/* Create our model:
		 *  
		 *  Model
		 *    |--- Class
		 *  		|--- Operation 1
		 *  		|--- Operation 2
		 *  		|--- Operation 3
		 */
		Resource modelResource = resourceSet.createResource(URI.createFileURI("deleteMutationTest" + ".uml"));	
		
		org.eclipse.uml2.uml.Model model = UMLFactory.eINSTANCE.createModel();
		
		org.eclipse.uml2.uml.Class clazz = UMLFactory.eINSTANCE.createClass();
		model.getPackagedElements().add(clazz);

		org.eclipse.uml2.uml.Operation op1 = UMLFactory.eINSTANCE.createOperation();
		org.eclipse.uml2.uml.Operation op2 = UMLFactory.eINSTANCE.createOperation();
		org.eclipse.uml2.uml.Operation op3 = UMLFactory.eINSTANCE.createOperation();
		clazz.getOwnedOperations().add(op1);
		clazz.getOwnedOperations().add(op2);
		clazz.getOwnedOperations().add(op3);

		modelResource.getContents().add(model);
		targetSelector.setModelResource(modelResource);
		
		// Setup global eClass count map:
		HashMap<EClass, Integer> eClassCountMap = new HashMap<EClass, Integer>();
		/*
		eClassCountMap.put(UMLPackage.Literals.MODEL, 600);
		eClassCountMap.put(UMLPackage.Literals.CLASS, 100);
		eClassCountMap.put(UMLPackage.Literals.OPERATION, 300);
		eClassCountMap.put(UMLPackage.Literals.ACTIVITY, 500); // not included in model
		*/
		metamodelCoverage.setCount(UMLPackage.Literals.MODEL, 600);
		metamodelCoverage.setCount(UMLPackage.Literals.CLASS, 100);
		metamodelCoverage.setCount(UMLPackage.Literals.OPERATION, 300);
		metamodelCoverage.setCount(UMLPackage.Literals.ACTIVITY, 500); // not included in model
		
		
		
		// Now lets draw a random object from the model 10000 times and count
		// How often each class has been drawn
		int modelCount = 0;
		int classCount = 0;
		int operationCount = 0;
		
		int totalSamples = 10000;
		
		ArrayList<EObject> selectedEObjects = targetSelector.selectWeightedEObjects(totalSamples, true);
		
		
		for(int i = 0; i < selectedEObjects.size(); i++) {
			EObject currentEObject = selectedEObjects.get(i);
			if(currentEObject instanceof org.eclipse.uml2.uml.Model) {
				modelCount++;
				continue;
			}
			
			if(currentEObject instanceof org.eclipse.uml2.uml.Class) {
				classCount++;
				continue;
			}
			
			if(currentEObject instanceof org.eclipse.uml2.uml.Operation) {
				operationCount++;
				continue;
			}
			
			// Some other object has been drawn?
			assertTrue(false);
		}
		
		double error = 0.05;
		assertEquals(modelCount + classCount + operationCount, totalSamples);
		
		double expectedModelCount = totalSamples * 6/10;
		double expectedClassCount = totalSamples * 1/10;
		double expectedOperationCount = totalSamples * 3/10;
		assertTrue(Math.abs(1 - modelCount / expectedModelCount) < error);
		assertTrue(Math.abs(1 - classCount / expectedClassCount) < error);
		assertTrue(Math.abs(1 - operationCount / expectedOperationCount) < error);
		
		modelCount = 0;
		classCount = 0;
		operationCount = 0;
		
		// Now let us prioritize less-frequent EClasses instead, so the results should be reversed
		selectedEObjects = targetSelector.selectWeightedEObjects(totalSamples, false);

		for(int i = 0; i < selectedEObjects.size(); i++) {
			EObject currentEObject = selectedEObjects.get(i);
			
			if(currentEObject instanceof org.eclipse.uml2.uml.Model) {
				modelCount++;
				continue;
			}
			
			if(currentEObject instanceof org.eclipse.uml2.uml.Class) {
				classCount++;
				continue;
			}
			
			if(currentEObject instanceof org.eclipse.uml2.uml.Operation) {
				operationCount++;
				continue;
			}
			
			// Some other object has been drawn?
			assertTrue(false);
		}
		
		/* When prioritizing more frequent EClasses, the selection probabilities for the
		 * EClasses that occur in the model look as follows (see eClassCountMap):
		 * Models: 60%, Classes: 10%, Operations: 30%, Activity: 0% (does not appear in model)
		 * 
		 * That is, a Model-Object is picked 6-times more often than a Class-Object and
		 * 2-times more often than an Operation-Object. Similarly, the Operation-Object has 
		 * a 3-times higher chance to be selected than the Class-Object.
		 * 
		 * When prioritizing less frequent EClasses, the distribution looks as follows instead:
		 * Models: 1/9 (~11,11%), Classes: 2/3 (~66,67%), Operations: 2/9 (~22,22%)
		 * 
		 * This means that now a Class-Object has a 6-times higher probability to be selected
		 * than a Model-Object.
		 */
		
		expectedModelCount = totalSamples * 1/9;
		expectedClassCount = totalSamples * 2/3;
		expectedOperationCount = totalSamples * 2/9;
		
		assertEquals(modelCount + classCount + operationCount, 10000);
		assertTrue(Math.abs(1 - modelCount / expectedModelCount) < error);
		assertTrue(Math.abs(1 - classCount / expectedClassCount) < error);
		assertTrue(Math.abs(1 - operationCount / expectedOperationCount) < error);
		
	}
}
