package de.hub.mse.emf.generator.cgf;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.uml2.uml.UMLPackage;
import org.junit.Before;
import org.junit.Test;

public class MetamodelCoverageTest {
	
	private MetamodelCoverage metamodelCoverage;
	
	@Before
	public void setUp() {
		HashSet<EClass> allEClasses = new HashSet<EClass>();
		allEClasses.add(UMLPackage.Literals.MODEL);
		allEClasses.add(UMLPackage.Literals.CLASS);
		allEClasses.add(UMLPackage.Literals.OPERATION);
		
		HashSet<EReference> allEContainmentRefs = new HashSet<EReference>();
		allEContainmentRefs.add(UMLPackage.Literals.PACKAGE__PACKAGED_ELEMENT);
		allEContainmentRefs.add(UMLPackage.Literals.CLASS__OWNED_OPERATION);
		
		metamodelCoverage = new MetamodelCoverage(allEClasses, allEContainmentRefs);
	}
	
	
	@Test
	public void coverageTest() {
		
		EClass modelClass = UMLPackage.Literals.MODEL;
		
		// Not covered yet
		assertTrue(!metamodelCoverage.isCovered(modelClass));
		assertEquals(metamodelCoverage.getEClassCount(modelClass), 0);
		
		// Add coverage and increment count
		metamodelCoverage.addCoveredEClass(modelClass);
		
		assertTrue(metamodelCoverage.isCovered(modelClass));
		assertEquals(metamodelCoverage.getEClassCount(modelClass), 1);
		
		// Add and increment count again
		metamodelCoverage.addCoveredEClass(modelClass);
		assertEquals(metamodelCoverage.getEClassCount(modelClass), 2);
		
		// Coverage is 1/3
		assertTrue(Math.abs(metamodelCoverage.getEClassCoverage() - (float) 1/3) < 0.0001);
		
		// Test containment ref coverage
		metamodelCoverage.addCoveredEContainmentRef(UMLPackage.Literals.PACKAGE__PACKAGED_ELEMENT);
		assertTrue(Math.abs(metamodelCoverage.getEContainmentCoverage() - (float) 1/2) < 0.0001);
	}
}
