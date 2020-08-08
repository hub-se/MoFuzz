/*******************************************************************************
 * Copyright (c) 2015 Abel Gómez (AtlanMod) 
 * Copyright (c) 2019 Hoang Lam Nguyen (HU Berlin) TODO
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Abel Gómez (AtlanMod) - Original implementation (fr.inria.atlanmod.instantiator.GenericMetamodelConfig)     
 *     Hoang Lam Nguyen (HU Berlin) - Additional modifications TODO 
 *******************************************************************************/
package de.hub.mse.emf.generator;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

//import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import com.google.common.collect.ImmutableSet;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.generator.internal.MetamodelResource;

public class ModelGenerationConfigImpl implements IModelGenerationConfig{
	
	/** The wrapper providing an iterator of the metamodel contents */
	protected final MetamodelResource metamodelResource;
	
	/** The root of the metamodel. If not specified, possible roots are determined from the containment hierarchy. */
	protected EClass rootEClass;
	
	/** {@link EClass}es to solely include in the model generation. */
	protected Set<EClass> eClassWhitelist;
	
	/** The maximum number of objects per model. */
	protected final int MAX_OBJECT_COUNT;
	
	/** The maximum depth of the model containment tree. */
	protected final int MAX_DEPTH;
	
	/** The maximum breadth (number of contained objects) per object in the containment tree. */
	protected final int MAX_BREADTH;
	
	/** The maximum attribute value size, i.e. length of strings. */
	protected final int MAX_VALUE_SIZE;
	
	
	/**
	 * Constructor that also provides a root of the metamodel.
	 * @param metamodelResource the MetamodelResource wrapper object
	 * @param root the root of the metamodel
	 * @param whitelist a list of {@link EClass}es to exclusively include in the model generation
	 * @param max_object_count the maximum number of objects per model
	 * @param max_depth the maximum depth of the model containment tree
	 * @param max_breadth the maximum number of contained objects per object in the containment tree
	 * @param max_value_size the maximum size of attribute values
	 * 
	 * TODO: make sure config parameters are valid, e.g. counts are non-negative
	 */
	public ModelGenerationConfigImpl(MetamodelResource metamodelResource,
									 EClass root,
									 Set<EClass> whitelist,
									 int max_object_count,
									 int max_depth,
									 int max_breadth,
									 int max_value_size) {
		
		this.metamodelResource = metamodelResource;
		this.rootEClass = root;
		this.eClassWhitelist = whitelist;
		this.MAX_OBJECT_COUNT = max_object_count;
		this.MAX_DEPTH = max_depth;
		this.MAX_BREADTH = max_breadth;
		this.MAX_VALUE_SIZE = max_value_size;
		
	}
	
	/**
	 * 
	 * @param metamodelResource the MetamodelResource wrapper object
	 * @param whitelist a list of {@link EClass}es to exclusively include in the model generation
	 * @param max_object_count the maximum number of objects per model
	 * @param max_depth the maximum depth of the model containment tree
	 * @param max_breadth the maximum number of contained objects per object in the containment tree
	 * @param max_value_size the maximum size of attribute values
	 */
	public ModelGenerationConfigImpl(MetamodelResource metamodelResource,
									 Set<EClass> whitelist,
									 int max_object_count,
									 int max_depth,
									 int max_breadth,
									 int max_value_size) {
		
		this.metamodelResource = metamodelResource;
		this.rootEClass = null;
		this.eClassWhitelist = whitelist;
		this.MAX_OBJECT_COUNT = max_object_count;
		this.MAX_DEPTH = max_depth; 
		this.MAX_BREADTH = max_breadth;
		this.MAX_VALUE_SIZE = max_value_size;		
	}
	
	public EClass getRootEClass() {
		return this.rootEClass;
	}

	@Override
	public ImmutableSet<EClass> getEClassWhitelist() {
		return ImmutableSet.copyOf(this.eClassWhitelist);
	}

	@Override
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

	@Override
	public ImmutableSet<EClass> possibleRootEClasses() {
		List<EClass> eClasses = new LinkedList<EClass>();
		
		if(this.rootEClass != null) {
			eClasses.add(this.rootEClass);
			return ImmutableSet.copyOf(eClasses);
		}
		else {
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
			
			// copying the list of eClasses 
			List <EClass> result = new LinkedList<EClass>(eClasses);
			//Collections.copy(result , eClasses);
			
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

	@Override
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

	@Override
	public EClass getNextRootEClass(ImmutableSet<EClass> rootEClasses, SourceOfRandomness random) {
		return rootEClasses.asList().get(random.nextInt(rootEClasses.size()));
	}

	@Override
	public int getTotalObjectCount(SourceOfRandomness random) {
		return random.nextInt(this.MAX_OBJECT_COUNT);
	}

	@Override
	public int getWeightFor(EReference eReference, EClass eClass) {
		// TODO : implement
		return 1;
	}

	@Override
	public int getValueSizeFor(Class<?> clazz, SourceOfRandomness random) {
		return random.nextInt(this.MAX_VALUE_SIZE);
	}

	@Override
	public int getElementCountFor(EAttribute eAttribute, SourceOfRandomness random) {
		// TODO: add additional variable for max value count; make 0 more likely?
		int upperBound = eAttribute.getUpperBound() == EAttribute.UNBOUNDED_MULTIPLICITY ? Integer.MAX_VALUE : eAttribute.getUpperBound();
		upperBound = Math.min(this.MAX_BREADTH, upperBound);
		
		int lowerBound = eAttribute.getLowerBound();
		
		// return random value from [lowerBound, upperBound]
		return random.nextInt((upperBound - lowerBound) + 1) + lowerBound;
	}
	
	@Override
	public int getElementCountFor(EReference eReference, SourceOfRandomness random) {
		int upperBound = eReference.getUpperBound() == EReference.UNBOUNDED_MULTIPLICITY ? Integer.MAX_VALUE : eReference.getUpperBound();
		upperBound = Math.min(this.MAX_BREADTH, upperBound);
		
		int lowerBound = eReference.getLowerBound();
		
		// return random value from [lowerBound, upperBound]
		return random.nextInt((upperBound - lowerBound) + 1) + lowerBound;
	}

	@Override
	public int getDepthFor(EClass eClass, SourceOfRandomness random) {
		return random.nextInt(this.MAX_DEPTH);
	}
	
	@Override
	public int getBreadthFor(EClass eClass, SourceOfRandomness random) {
		return random.nextInt(this.MAX_BREADTH);
	}
	

}
