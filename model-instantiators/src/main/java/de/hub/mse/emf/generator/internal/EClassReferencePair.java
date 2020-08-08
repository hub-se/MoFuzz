package de.hub.mse.emf.generator.internal;

import java.util.Objects;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EReference;

/*
 * Pair of EClass and Reference to use for caching purposes. 
 * See MetamodelData.eReferenceValidEClassesCache
 */
public class EClassReferencePair {
	private EClass eClass;
	private EReference eReference;
	
	public EClassReferencePair(EClass eClass, EReference eReference) {
		this.eClass = eClass;
		this.eReference = eReference;
	}
	
	public EClass getEClass() {
		return this.eClass;
	}
	
	public EReference getEReference() {
		return this.eReference;
	}
	
	public boolean referenceIsMany() {
		return this.eReference.isMany();
	}
	
	public boolean referenceIsContainment() {
		return this.eReference.isContainment();
	}
	
	@Override
	public int hashCode() {
	    return Objects.hash(eClass.getName(), eReference.getName());
	}
	
	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		
		if(obj == null) {
			return false;
		}
		
		if(this.getClass() != obj.getClass()) {
			return false;
		}
		
		EClassReferencePair eClassRefPair = (EClassReferencePair) obj;
		
		return this.eClass.equals(eClassRefPair.getEClass())
				&& this.eReference.equals(eClassRefPair.getEReference());
	}
}
