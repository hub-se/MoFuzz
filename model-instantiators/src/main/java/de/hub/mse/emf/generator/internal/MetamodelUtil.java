/*******************************************************************************
 * Copyright (c) 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Hoang Lam Nguyen - additional modifications (added eclass whitelist and invalid reference map) TODO
 *******************************************************************************/
package de.hub.mse.emf.generator.internal;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Lists.newArrayList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.uml2.uml.UMLPackage.Literals;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
//import com.google.common.collect.Table;
//import com.google.common.collect.HashBasedTable;

/**
 * Provides utility methods to access information about the metamodel.
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a> original implementation
 * @author <a href="hoang.lam.nguyen@hu-berlin.de">Hoang Lam Nguyen</a> various extensions
 */
public class MetamodelUtil {
	
	/** The EPackages of the metamodel */
	private final ImmutableSet<EPackage> ePackages;
	
	/** The EClasses to ignore (blacklist) */
	private final ImmutableSet<EClass> ignoredEClasses;
	
	/** The EClasses to include (whitelist) */
	private final ImmutableSet<EClass> eClassWhitelist;
	private final boolean useWhitelist;
	
	/** The set of all containment EReferences */
	private ImmutableSet<EReference> allContainmentReferences;
	
	/** The set of all (concrete) EClasses */
	private ImmutableSet<EClass> allEClasses;
	
	/*
	public MetamodelUtil(ImmutableSet<EPackage> ePackages, ImmutableSet<EClass> ignoredEClasses) {
		this.ePackages = ePackages;
		this.ignoredEClasses = ignoredEClasses;
		this.eClassWhitelist = null; // TODO Or make it an empty set?
		this.useWhitelist = false;
		ModelGenerationStats.initEClassCoverageMap(ePackages);
		initSets();
	}
	*/
	
	public MetamodelUtil(ImmutableSet<EPackage> ePackages, ImmutableSet<EClass> ignoredEClasses, ImmutableSet<EClass> eClassWhitelist) {
		this.ePackages = ePackages;
		this.ignoredEClasses = ignoredEClasses;
		this.eClassWhitelist = eClassWhitelist;
		this.useWhitelist = eClassWhitelist.size() > 0 ? true : false;
		ModelGenerationStats.initEClassCoverageMap(ePackages);
		//this.buildContainmentTree();
		initSets();

	}
	
	/**
	 * Initiallizes the total EClass and EReference (containment) sets
	 */
	private void initSets(){
		HashSet<EReference> containmentRefs = new HashSet<EReference>();
		HashSet<EClass> eClasses = new HashSet<EClass>();
		
		for(EPackage ePackage : ePackages) {
			for(Iterator<EObject> it = ePackage.eAllContents(); it.hasNext();) {
				EObject eObject = it.next();
				if(eObject instanceof EClass){
					EClass eClass = (EClass) eObject;
					if(!eClass.isAbstract() && !eClass.isInterface()) {
						eClasses.add(eClass);
						for(EReference ref: filter(eClass.getEAllContainments(), VALID_EREFERENCE)) {
							containmentRefs.add(ref);
						}
					}
				}
			}
		}
		allContainmentReferences = ImmutableSet.copyOf(containmentRefs);
		allEClasses = ImmutableSet.copyOf(eClasses);
	}
	
	public ImmutableSet<EReference> getAllEContainmentRefs(){
		return allContainmentReferences;
	}
	
	public ImmutableSet<EClass> getAllEClasses(){
		return allEClasses;
	}
	
	public boolean useWhitelist() {
		return this.useWhitelist;
	}
	
	public ImmutableSet<EClass> getEClassWhitelist() {
		return this.eClassWhitelist;
	}
	
	public boolean isWhitelistedOrSuperType(EClass eClass) {
		if(this.eClassWhitelist.contains(eClass)) {
			return true;
		}
		else {
			for(EClass whitelistedEClass : eClassWhitelist) {
				if(eClass.isSuperTypeOf(whitelistedEClass)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public boolean isWhitelisted(EClass eClass) {
		return this.eClassWhitelist.contains(eClass);
	}
	

	private Cache<EClass, ImmutableList<EAttribute>> eAllAttributesCache = CacheBuilder.newBuilder().build(
			new CacheLoader<EClass, ImmutableList<EAttribute>>() {
				@Override
				public ImmutableList<EAttribute> load(EClass eClass) throws Exception {
					EList<EAttribute> eAllAttributes = eClass.getEAllAttributes();
					return ImmutableList.copyOf(filter(eAllAttributes, VALID_EATTRIBUTE));
				}
			});

	public ImmutableList<EAttribute> eAllAttributes(EClass eClass) {
		return ((LoadingCache<EClass, ImmutableList<EAttribute>>) eAllAttributesCache).getUnchecked(eClass);
	}

	private Cache<EClass, ImmutableList<EReference>> eAllNonContainmentCache = CacheBuilder.newBuilder().build(
			new CacheLoader<EClass, ImmutableList<EReference>>() {
				@Override
				public ImmutableList<EReference> load(EClass eClass) throws Exception {
					EList<EReference> eAllReferences = eClass.getEAllReferences();
					return ImmutableList.copyOf(filter(eAllReferences, NON_CONTAINMENT_EREFERENCE));
				}
			});

	public ImmutableList<EReference> eAllNonContainment(EClass eClass) {
		return ((LoadingCache<EClass, ImmutableList<EReference>>) eAllNonContainmentCache).getUnchecked(eClass);
	}
	
	private Cache<EClassReferencePair, ImmutableList<EClass>> eReferenceValidEClassesCache = CacheBuilder.newBuilder().build(
			new CacheLoader<EClassReferencePair, ImmutableList<EClass>>() {
				@Override
				public ImmutableList<EClass> load(EClassReferencePair eClassRefPair) throws Exception {
					EClass eClass = eClassRefPair.getEClass();
					EReference eReference = eClassRefPair.getEReference();
					List<EClass> validEClasses = new ArrayList<EClass>();

					// Instantiate EClass
					EObject instance = eClass.getEPackage().getEFactoryInstance().create(eClass);
					
					// Now try to set each concrete EClass of the EReferenceType and check for thrown Exceptions
					ImmutableList<EClass> eAllConcreteSubTypeOrSelf = eAllConcreteSubTypeOrSelf(eReference);
					for(EClass refEClass : eAllConcreteSubTypeOrSelf) {
						
						// Instantiate child class
						EObject refEObject = refEClass.getEPackage().getEFactoryInstance().create(refEClass);
						
						if(eReference.isMany()) {
							List<EObject> values = (List<EObject>) instance.eGet(eReference);
							try {
								values.add(refEObject);
								instance.eSet(eReference, values);

								// If no exception is thrown, then the eclass is actually valid
								validEClasses.add(refEClass);
							}
							catch(ArrayStoreException e) {
							}
							catch(UnsupportedOperationException e) {
								
							}
						}
						else {
							try {
								instance.eSet(eReference, refEObject);
								validEClasses.add(refEClass);
							}
							catch(IllegalArgumentException e) {
							}
						}
					}
					return ImmutableList.copyOf(validEClasses);
				}
			}
	);
	
	public ImmutableList<EClass> eReferenceValidEClasses(EClassReferencePair eClassRefPair) {
		return ((LoadingCache<EClassReferencePair, ImmutableList<EClass>>) eReferenceValidEClassesCache).getUnchecked(eClassRefPair);
	}
	
	/*
	private HashMap<EClassReferencePair, ImmutableList<EClass>> eReferenceValidEClassesCache = new HashMap<EClassReferencePair, ImmutableList<EClass>>();

	public ImmutableList<EClass> eReferenceValidEClasses(EClassReferencePair eClassRefPair) {
		if(eReferenceValidEClassesCache.containsKey(eClassRefPair)) {
			return eReferenceValidEClassesCache.get(eClassRefPair);
		}
			
		EClass eClass = eClassRefPair.getEClass();
		EReference eReference = eClassRefPair.getEReference();
		List<EClass> validEClasses = new ArrayList<EClass>();
		System.out.println("Miss: " + eClass.getName() + "--" + eReference.getName());
				
		// Instantiate EClass
		EObject instance = eClass.getEPackage().getEFactoryInstance().create(eClass);
				
		// Now try to set each concrete EClass of the EReferenceType and check for thrown Exceptions
		ImmutableList<EClass> eAllConcreteSubTypeOrSelf = eAllConcreteSubTypeOrSelf(eReference);
		for(EClass refEClass : eAllConcreteSubTypeOrSelf) {
			
			// Instantiate child class
			EObject refEObject = refEClass.getEPackage().getEFactoryInstance().create(refEClass);
			
			if(eReference.isMany()) {
				List<EObject> values = (List<EObject>) instance.eGet(eReference);
				try {
					values.add(refEObject);
					
					// If no exception is thrown, then the eclass is actually valid
					validEClasses.add(refEClass);
				}
				catch(ArrayStoreException e) {
					
				}
			}
			else {
				try {
					instance.eSet(eReference, refEObject);
					validEClasses.add(refEClass);
				}
				catch(IllegalArgumentException e) {
				}
			}
		}
		
		ImmutableList<EClass> result = ImmutableList.copyOf(validEClasses);
		eReferenceValidEClassesCache.put(eClassRefPair, result);
		return ImmutableList.copyOf(validEClasses);
	}
	*/
	
	private Cache<EClass, ImmutableList<EReference>> eAllContainmentCache = CacheBuilder.newBuilder().build(
			new CacheLoader<EClass, ImmutableList<EReference>>() {
				@Override
				public ImmutableList<EReference> load(EClass eClass) throws Exception {
					EList<EReference> eAllContainments = eClass.getEAllContainments();
					return ImmutableList.copyOf(filter(eAllContainments, VALID_EREFERENCE));
				}
			});

	public Iterable<EReference> eAllContainment(EClass eClass) {
		return ((LoadingCache<EClass, ImmutableList<EReference>>) eAllContainmentCache).getUnchecked(eClass);
	}

	private Cache<EReference, ImmutableList<EClass>> eAllConcreteSubTypeOrSelfCache = CacheBuilder.newBuilder().build(
			new CacheLoader<EReference, ImmutableList<EClass>>() {
				@Override
				public ImmutableList<EClass> load(EReference eReference) throws Exception {
					EClass eReferenceType = eReference.getEReferenceType();
					ImmutableList<EClass> eAllSubTypesOrSelf = eAllSubTypesOrSelf(eReferenceType);
					
					
					if(useWhitelist()) {
						List<EClass> eAllConcreteSubTypeOrSelf = newArrayList(filter(eAllSubTypesOrSelf, whitelisted(getEClassWhitelist())));
						eAllConcreteSubTypeOrSelf.removeAll(ignoredEClasses);
						return ImmutableList.copyOf(eAllConcreteSubTypeOrSelf);

					}
					else {
						List<EClass> eAllConcreteSubTypeOrSelf = newArrayList(filter(eAllSubTypesOrSelf, CONCRETE_CLASS));
						eAllConcreteSubTypeOrSelf.removeAll(ignoredEClasses);
						
						/*
						// Lam: Only include whitelisted eclasses
						if(useWhitelist()) {
							List<EClass> eClassBlacklist = new ArrayList<EClass>();
							for(EClass eClass : eAllConcreteSubTypeOrSelf) {
								if(!isWhitelisted(eClass)) {
									eClassBlacklist.add(eClass);
								}
							}					
							eAllConcreteSubTypeOrSelf.removeAll(eClassBlacklist);
						}
						*/
	
						return ImmutableList.copyOf(eAllConcreteSubTypeOrSelf);
					}
				}
			});

	public ImmutableList<EClass> eAllConcreteSubTypeOrSelf(EReference eReference) {
		return ((LoadingCache<EReference, ImmutableList<EClass>>) eAllConcreteSubTypeOrSelfCache).getUnchecked(eReference);
	}

	private ImmutableList<EClass> eAllSubTypesOrSelf(final EClass eClass) {
		return ImmutableList.copyOf(concat(eAllSubTypes(eClass), ImmutableList.of(eClass)));
	}

	private ImmutableList<EClass> eAllSubTypes(final EClass eClass) {
		Iterator<EClass> eAllClasses = filter(eAllContents(ePackages), EClass.class);
		return ImmutableList.copyOf(filter(eAllClasses, new Predicate<EClass>() {
			public boolean apply(EClass aClass) {
				return aClass.getEAllSuperTypes().contains(eClass);
			}
		}));
	}

	private Iterator<EObject> eAllContents(Iterable<? extends EObject> eObjects) {
		Iterable<TreeIterator<EObject>> eAllContents = transform(eObjects,
				new Function<EObject, TreeIterator<EObject>>() {
					public TreeIterator<EObject> apply(EObject eObject) {
						return eObject.eAllContents();
					}
				});
		return concat(eAllContents.iterator());
	}

	private static final Predicate<EClass> CONCRETE_CLASS = new Predicate<EClass>() {
		public boolean apply(EClass eClass) {
			return !eClass.isAbstract() && !eClass.isInterface();
		}
	};

	private static final Predicate<EReference> VALID_EREFERENCE = new Predicate<EReference>() {
		public boolean apply(EReference eReference) {
			return eReference.isChangeable() && !eReference.isTransient() && !eReference.isDerived();
		}
	};

	private static final Predicate<EAttribute> VALID_EATTRIBUTE = new Predicate<EAttribute>() {
		public boolean apply(EAttribute eAttribute) {
			return eAttribute.isChangeable() && !eAttribute.isTransient() && !eAttribute.isDerived();
		}
	};

	private static final Predicate<EReference> NON_CONTAINMENT_EREFERENCE = new Predicate<EReference>() {
		public boolean apply(EReference eReference) {
			return !eReference.isContainment() && !eReference.isContainer() && eReference.isChangeable()
					&& !eReference.isTransient() && !eReference.isDerived();
		}
	};
	
	public static Predicate<EClass> whitelisted(ImmutableSet<EClass> eClassWhitelist) {
	    return new Predicate<EClass>() {
	        @Override
	        public boolean apply(EClass eClass) {
	        	if(eClass.isAbstract() || eClass.isInterface()) {
	        		return false;
	        	}
	        	
	            return eClassWhitelist.contains(eClass);
	        }
	    };
	}
	
	public static Predicate<EClass> whitelistedOrSuperType(ImmutableSet<EClass> eClassWhitelist) {
	    return new Predicate<EClass>() {
	        @Override
	        public boolean apply(EClass eClass) {
	        	if(eClass.isAbstract() || eClass.isInterface()) {
	        		return false;
	        	}
	        	if(eClassWhitelist.contains(eClass)) {
	    			return true;
	    		}
	    		else {
	    			for(EClass whitelistedEClass : eClassWhitelist) {
	    				if(eClass.isSuperTypeOf(whitelistedEClass)) {
	    					return true;
	    				}
	    			}
	    		}
	    		return false;
	        }
	    };
	}
	
	
}
