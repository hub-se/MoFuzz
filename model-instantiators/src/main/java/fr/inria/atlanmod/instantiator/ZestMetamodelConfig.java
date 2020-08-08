/*******************************************************************************
 * Copyright (c) 2015 Abel G�mez (AtlanMod) 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Abel G�mez (AtlanMod) - Additional modifications      
 *******************************************************************************/

package fr.inria.atlanmod.instantiator;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.Range;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;

import com.google.common.collect.ImmutableSet;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.generator.internal.MetamodelResource;
import fr.obeo.emf.specimen.ISpecimenConfiguration;
import fr.obeo.emf.specimen.SpecimenGenerator;

/**
 * @author <a href="mailto:abel.gomez-llana@inria.fr">Abel G�mez</a>
 *
 */
public class ZestMetamodelConfig {

	public static final int DEFAULT_AVERAGE_PROPERTIES_SIZE = 5;

	public static final int DEFAULT_AVERAGE_REFERENCES_SIZE = 5;

	public static final int DEFAULT_AVERAGE_VALUES_LENGTH = 5;
	
	public static final float DEFAULT_PROPERTIES_DEVIATION = 0.1f;
	
	public static final float DEFAULT_REFERENCES_DEVIATION = 0.1f;
	
	public static final float DEFAULT_VALUES_DEVIATION = 0.1f;
	
	protected final MetamodelResource metamodelResource;

	protected final Range<Integer> elementsRange;
	
	protected Range<Integer> propertiesRange = Range.between(
			Math.round(DEFAULT_AVERAGE_PROPERTIES_SIZE * (1 - DEFAULT_PROPERTIES_DEVIATION)), 
			Math.round(DEFAULT_AVERAGE_PROPERTIES_SIZE * (1 + DEFAULT_PROPERTIES_DEVIATION)));
	
	protected Range<Integer> referencesRange = Range.between(
			Math.round(DEFAULT_AVERAGE_REFERENCES_SIZE * (1 - DEFAULT_REFERENCES_DEVIATION)), 
			Math.round(DEFAULT_AVERAGE_REFERENCES_SIZE * (1 + DEFAULT_REFERENCES_DEVIATION)));
	
	protected Range<Integer> valuesRange = Range.between(
			Math.round(DEFAULT_AVERAGE_VALUES_LENGTH * (1 - DEFAULT_VALUES_DEVIATION)), 
			Math.round(DEFAULT_AVERAGE_VALUES_LENGTH * (1 + DEFAULT_VALUES_DEVIATION)));
	
	
	
	protected Map<Object, IntegerDistribution> distributions = new HashMap<Object, IntegerDistribution>();

	/**
	 * Creates a new {@link GenericMetamodelConfig}
	 * 
	 * @param metamodelResource
	 *            The resource containing the metamodel for which the
	 *            {@link SpecimenGenerator} will create instances
	 * 
	 * @param elementsRange
	 *            The minimum and maximum size allowed for resources created
	 *            using this {@link ISpecimenConfiguration}
	 * @param seed
	 *            The <code>seed</code> for the generator of {@link Random}
	 *            numbers. Useful to create models in a detirministic way.
	 */
	public ZestMetamodelConfig(MetamodelResource metamodelResource, Range<Integer> elementsRange) {
		super();
		this.metamodelResource = metamodelResource;
		this.elementsRange = elementsRange;
		//initReferencesMap();
	}

	/**
	 * Return the {@link Resource} of the metamodel for which this
	 * {@link ISpecimenConfiguration} will be used
	 * 
	 * @return the metamodel {@link Resource}
	 */
	public MetamodelResource getMetamodelResource() {
		return metamodelResource;
	}
	
	/**
	 * Returns the minimum and maximum size allowed for resources created using
	 * this {@link ISpecimenConfiguration}
	 * 
	 * @return The {@link Range}
	 */
	public Range<Integer> getElementsRange() {
		return elementsRange;
	}

	/**
	 * Returns the minimum and maximum number of properties allowed for
	 * {@link EObject}s created using this {@link ISpecimenConfiguration}
	 * 
	 * @return The {@link Range}
	 */
	public Range<Integer> getPropertiesRange() {
		return propertiesRange;
	}

	/**
	 * Sets the minimum and maximum number of properties allowed for
	 * {@link EObject}s created using this {@link ISpecimenConfiguration}
	 * 
	 * @param valuesRange
	 *            The {@link Range} with minimum and maximum values
	 */
	public void setPropertiesRange(Range<Integer> propertiesRange) {
		this.propertiesRange = propertiesRange;
	}

	/**
	 * Sets the minimum and maximum number of properties allowed for
	 * {@link EObject}s created using this {@link ISpecimenConfiguration}
	 * 
	 * @param min
	 *            The minimum
	 * @param max
	 *            The maximum
	 */
	public void setPropertiesRange(int min, int max) {
		this.propertiesRange = Range.between(min, max);
	}

	/**
	 * Returns the minimum and maximum number of references allowed for
	 * {@link EObject}s created using this {@link ISpecimenConfiguration}
	 * 
	 * @return The {@link Range}
	 */
	public Range<Integer> getReferencesRange() {
		return referencesRange;
	}

	/**
	 * Sets the minimum and maximum number of references allowed for
	 * {@link EObject}s created using this {@link ISpecimenConfiguration}
	 * 
	 * @param valuesRange
	 *            The {@link Range} with minimum and maximum values
	 */
	public void setReferencesRange(Range<Integer> referencesRange) {
		this.referencesRange = referencesRange;
	}

	/**
	 * Sets the minimum and maximum number of references allowed for
	 * {@link EObject}s created using this {@link ISpecimenConfiguration}
	 * 
	 * @param min
	 *            The minimum
	 * @param max
	 *            The maximum
	 */
	public void setReferencesRange(int min, int max) {
		this.referencesRange = Range.between(min, max);
	}
	
	/**
	 * Returns the minimum and maximum length of non-fixed-sized properties (e.g.,
	 * {@link String}s, arrays, etc).

	 * @return The {@link Range}
	 */
	public Range<Integer> getValuesRange() {
		return valuesRange;
	}

	/**
	 * Sets the minimum and maximum length of non-fixed-sized properties (e.g.,
	 * {@link String}s, arrays, etc).
	 * 
	 * @param valuesRange
	 *            The {@link Range} with minimum and maximum values
	 */
	public void setValuesRange(Range<Integer> valuesRange) {
		this.valuesRange = valuesRange;
	}

	/**
	 * Sets the minimum and maximum length of non-fixed-sized properties (e.g.,
	 * {@link String}s, arrays, etc).
	 * 
	 * @param min
	 *            The minimum
	 * @param max
	 *            The maximum
	 */
	public void setValuesRange(int min, int max) {
		this.valuesRange = Range.between(min, max);
	}
	
	public ImmutableSet<EPackage> ePackages() {
		Set<EPackage> ePackages = new HashSet<EPackage>();
		for (Iterator<EObject> it = metamodelResource.getAllContents(); it.hasNext();) {
			EObject eObject = (EObject) it.next();
			if (eObject instanceof EPackage) {
				ePackages.add((EPackage) eObject);
			}
		}
		return ImmutableSet.copyOf(ePackages);
	}

	public ImmutableSet<EClass> possibleRootEClasses() {
		List<EClass> eClasses = new LinkedList<EClass>();
		// creating a subtypes map
		Map<EClass,Set<EClass>> eSubtypesMap = computeSubtypesMap();
		
		// Eclasses.filter( instance of EClass && not abstract && not interface) 
		for (Iterator<EObject> it = metamodelResource.getAllContents(); it.hasNext();) {
			EObject eObject = (EObject) it.next();
			if (eObject instanceof EClass) {
				EClass eClass = (EClass) eObject;
				if (!eClass.isAbstract() && !eClass.isInterface()) {
					eClasses.add(eClass);
					}
			}
		}
		
		//copying the list of eClasses 
		List <EClass> result = new LinkedList<EClass>(eClasses);
//		Collections.copy(result , eClasses);
		
		// iterating eClasses and removing elements (along with subtypes) being 
		// subject to a container reference 
		for (EClass cls : eClasses ) {
			for (EReference cont : cls.getEAllContainments()) {
				Set<EClass> list = eSubtypesClosure(eSubtypesMap, (EClass)cont.getEType());
				if (list.size() == 0) {
					result.remove((EClass)cont.getEType());
				} else {
					result.removeAll(list); 
					}
			}
		}
		
		return ImmutableSet.copyOf(result);
	}

	@SuppressWarnings("unchecked")
	private Set<EClass> eSubtypesClosure(Map<EClass, Set<EClass>> eSubtypesMap, EClass eType) {
		Set<EClass> result = new LinkedHashSet<EClass> ();
			if (!eSubtypesMap.containsKey(eType)) {
				return Collections.EMPTY_SET;
			} else {
				result.addAll(eSubtypesMap.get(eType));
				for (EClass eSubType : eSubtypesMap.get(eType)) {
					if (! eSubType.equals(eType)) 
						result.addAll(eSubtypesClosure(eSubtypesMap, eSubType));
				}
			}
		return result;
	}


	private Map<EClass, Set <EClass>> computeSubtypesMap() {
		Map<EClass, Set<EClass>> result = new HashMap<EClass, Set<EClass>> (); 
		Iterator<EObject> iter = metamodelResource.getAllContents();
		
		 for (EObject ecls = null ;  iter.hasNext(); ) {
			 ecls = iter.next();
			 if (ecls instanceof EClass) {
				 EClass clazz = (EClass) ecls;
				 for (EClass cls : clazz.getEAllSuperTypes()) {
					 if (result.containsKey(cls)) {
						 result.get(cls).add(clazz);
					 } else {
						 Set<EClass> list = new HashSet<EClass>();
						 list.add(cls);
						 list.add(clazz);
						 result.put(cls, list);
					 }
				 }
			 }
		 }
			 
		return result;
	}


	/**
	 * Returns whether instances of this {@link EClass} need a container, i.e.,
	 * they have a <code>required</code> <code>container</code>
	 * {@link EReference} (see {@link EReference#isRequired()} and
	 * {@link EReference#isContainer()})
	 * 
	 * @param eClass
	 *            The {@link EClass}
	 * @return Whether the {@link EClass} needs a <code>container</code>
	 */
	@SuppressWarnings("unused")
	private static boolean needsContainer(EClass eClass) {
		for (EReference eReference : eClass.getEAllReferences()) {
			if (eReference.isContainer() && eReference.isRequired()) {
				return true;
			}
		}
		return false;
	}

	public ImmutableSet<EClass> ignoredEClasses() {
		Set<EClass> eClasses = new HashSet<EClass>();
		for (Iterator<EObject> it = metamodelResource.getAllContents(); it.hasNext();) {
			EObject eObject = (EObject) it.next();
			if (eObject instanceof EClass) {
				EClass eClass = (EClass) eObject;
				if (eClass.isAbstract() || eClass.isInterface()) {
					// Abstract EClasses and Interfaces can't be instantiated
					eClasses.add(eClass);
				}
			}
		}
		return ImmutableSet.copyOf(eClasses);
	}

	public int getResourceSizeDistribution(SourceOfRandomness random) {
		return random.nextInt(elementsRange.getMaximum() - elementsRange.getMinimum() + 1) + elementsRange.getMinimum();
	}

	public int getWeightFor(EReference eReference, EClass eClass) {
		return 1;
	}

	public int getDistributionFor(EAttribute eAttribute, SourceOfRandomness random) {
		int upperBound = eAttribute.getUpperBound() == EAttribute.UNBOUNDED_MULTIPLICITY ? Integer.MAX_VALUE : eAttribute.getUpperBound();
		upperBound = Math.min(upperBound, valuesRange.getMaximum());
		int lowerBound = eAttribute.getLowerBound();
		
		// return random value from [lowerBound, upperBound]
		return random.nextInt((upperBound - lowerBound) + 1) + lowerBound;
	}

	public int getValueDistributionFor(Class<?> clazz, SourceOfRandomness random) {

		return random.nextInt(valuesRange.getMaximum() - valuesRange.getMinimum()  + 1) + valuesRange.getMinimum();
	}

	public int getDistributionFor(EReference eReference, SourceOfRandomness random) {
		int upperBound = eReference.getUpperBound() == EAttribute.UNBOUNDED_MULTIPLICITY ? Integer.MAX_VALUE : eReference.getUpperBound();
		upperBound = Math.min(upperBound, referencesRange.getMaximum());
		int lowerBound = eReference.getLowerBound();
		
		// return random value from [lowerBound, upperBound]
		return random.nextInt((upperBound - lowerBound) + 1) + lowerBound;
	}

	public int getDepthDistributionFor(EClass eClass, SourceOfRandomness random) {
		return random.nextInt((referencesRange.getMaximum() - referencesRange.getMinimum()) + 1) + referencesRange.getMinimum();
	}
	
	public int getBreadthDistributionFor(EClass eClass, SourceOfRandomness random) {
		return random.nextInt((referencesRange.getMaximum() - referencesRange.getMinimum()) + 1) + referencesRange.getMinimum();
	}

	public EClass getNextRootEClass(ImmutableSet<EClass> rootEClasses, SourceOfRandomness random) {
		return rootEClasses.asList().get(random.nextInt(rootEClasses.size()));
	}
}
