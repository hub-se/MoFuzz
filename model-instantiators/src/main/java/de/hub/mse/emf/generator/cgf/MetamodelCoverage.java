package de.hub.mse.emf.generator.cgf;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;

/**
 * Class to save metamodel coverage information.
 * Coverage information may be used to guide the mutation process.
 * @author Lam
 *
 */
public class MetamodelCoverage {
	
	/** The total classes */
	private Set<EClass> allEClasses;
	
	/** The overall covered classes. */
	private Set<EClass> coveredEClasses;
	
	/** The tentatively covered classes. */
	private Set<EClass> tempCoveredEClasses;
	
	/** The global number of instances per EClass */
	private Map<EClass, Integer> eClassCounts;
	
	/** The total containment references. */
	private Set<EReference> allEContainmentRefs;
	
	/** The covered containment references. */
	private Set<EReference> coveredEContainmentRefs;
	
	public MetamodelCoverage(Collection<EClass> allClasses, Collection<EReference> allContainmentRefs) {
		allEClasses = new HashSet<EClass>();
		allEClasses.addAll(allClasses);
		
		allEContainmentRefs = new HashSet<EReference>();
		allEContainmentRefs.addAll(allContainmentRefs);
		
		coveredEContainmentRefs = new HashSet<EReference>();
		coveredEClasses = new HashSet<EClass>();
		tempCoveredEClasses = new HashSet<EClass>();
		
		eClassCounts = new HashMap<EClass, Integer>();
	}
	
	// --- ECLASS COVERAGE ---
	
	public Set<EClass> getCoveredEClasses(){
		return coveredEClasses;
	}
	
	public boolean isCovered(EClass eClass) {
		return coveredEClasses.contains(eClass);
	}
	
	public void addCoveredEClass(EClass eClass) {
		coveredEClasses.add(eClass);
		
		// Increment count
		// If there exists a value for the given key, add 1
		// otherwise, set as 1
		eClassCounts.merge(eClass, 1, Integer::sum);
	}
	
	public void addTempCoveredEClass(EClass eClass) {
		tempCoveredEClasses.add(eClass);
	}
	
	public void discardTempCoveredEClasses() {
		tempCoveredEClasses.clear();
	}
	
	public void commitTempCoveredEClasses() {
		for(EClass eClass : tempCoveredEClasses) {
			coveredEClasses.add(eClass);
			eClassCounts.merge(eClass, 1, Integer::sum);
		}
		tempCoveredEClasses.clear();
	}
	
	public void decrementEClassCount(EClass eClass) {
		assert(eClassCounts.containsKey(eClass));
		int oldValue = eClassCounts.get(eClass);
		eClassCounts.put(eClass, oldValue - 1);
	}
	
	public int getEClassCount(EClass eClass) {
		return eClassCounts.getOrDefault(eClass, 0);
	}
	
	// For debugging/testing purposes only
	public void setCount(EClass eClass, int count) {
		eClassCounts.put(eClass, count);
	}
	
	public float getEClassCoverage() {
		return (float) coveredEClasses.size() / allEClasses.size();
	}
	
	// --- ECONTAINMENT REFERENCE COVERAGE ---
	
	public Set<EReference> getCoveredEContainmentRefs() {
		return coveredEContainmentRefs;
	}
	
	public boolean isCovered(EReference eReference) {
		return coveredEContainmentRefs.contains(eReference);
	}
	
	public void addCoveredEContainmentRef(EReference eReference) {
		coveredEContainmentRefs.add(eReference);
	}
	
	public float getEContainmentCoverage() {
		return (float) coveredEContainmentRefs.size() / allEContainmentRefs.size();
	}

}
