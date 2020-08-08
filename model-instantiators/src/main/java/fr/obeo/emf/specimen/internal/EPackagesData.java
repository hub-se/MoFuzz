/*******************************************************************************
 * Copyright (c) 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *******************************************************************************/
package fr.obeo.emf.specimen.internal;

import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Iterables.filter;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterators.concat;
import static com.google.common.collect.Iterators.filter;

/**
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 * 
 */
public class EPackagesData {

	private final ImmutableSet<EPackage> ePackages;
	private final ImmutableSet<EClass> ignoredEClasses;

	public EPackagesData(ImmutableSet<EPackage> ePackages, ImmutableSet<EClass> ignoredEClasses) {
		this.ePackages = ePackages;
		this.ignoredEClasses = ignoredEClasses;
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

					List<EClass> eAllConcreteSubTypeOrSelf = newArrayList(filter(eAllSubTypesOrSelf, CONCRETE_CLASS));
					eAllConcreteSubTypeOrSelf.removeAll(ignoredEClasses);

					return ImmutableList.copyOf(eAllConcreteSubTypeOrSelf);
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
}
