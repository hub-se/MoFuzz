/*******************************************************************************
 * Copyright (c) 2012 Obeo.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Abel Gómez (AtlanMod) - Additional modifications      
*******************************************************************************/

package fr.obeo.emf.specimen;

import org.apache.commons.math3.distribution.BinomialDistribution;
import org.apache.commons.math3.distribution.IntegerDistribution;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import com.google.common.collect.ImmutableSet;

/**
 * @author <a href="mailto:mikael.barbero@obeo.fr">Mikael Barbero</a>
 * @author <a href="mailto:abel.gomez-llana@inria.fr">Abel Gómez</a>
 */
public interface ISpecimenConfiguration {

	/**
	 * Returns the set of {@link EPackage}s to consider as the metamodel of the
	 * model to be generated
	 * 
	 * @return
	 */
	ImmutableSet<EPackage> ePackages();

	/**
	 * Returns the list of {@link EClass}es to be instantiated as root objects.
	 * 
	 * @return
	 */
	ImmutableSet<EClass> possibleRootEClasses();

	/**
	 * Those {@link EClass}es will never be generated.
	 * 
	 * @return
	 */
	ImmutableSet<EClass> ignoredEClasses();

	/**
	 *
	 * Selects an {@link EClass} from {@code rootEClasses} to be used as the
	 * type for the next root {@link EObject}.
	 * 
	 * Implementors may use different distributions to balance the proportion of
	 * {@link EClass}es of each type in the root of the {@link Resource}
	 * 
	 * @param rootEClasses
	 *            The list of possible root {@link EObjects}es
	 * @return The selected {@link EClass} according to the desired distribution
	 */
	EClass getNextRootEClass(ImmutableSet<EClass> rootEClasses);

	/**
	 * Size of resources (in number of {@link EObject}s) will be distributed
	 * following the returned the distribution.
	 * 
	 * Note that the real distribution may be shifted up in some
	 * {@link SpecimenGenerator}.
	 * 
	 * @return the distribution
	 */
	IntegerDistribution getResourceSizeDistribution();

	/**
	 * Returns how many element has to be generated to fill an instance of the
	 * given {@link EReference}.
	 * 
	 * e.g: the {@link EReference} {@link EClass#getESuperTypes()}
	 * 
	 * return a {@link BinomialDistribution}(3,0.5) and you will get a model
	 * with the eSuperType reference filled with a mean of 1.5 elements.
	 * 
	 * @param eReference
	 * @return
	 */
	IntegerDistribution getDistributionFor(EReference eReference);

	/**
	 * Returns the weight of the possibles concrete {@link EClass} for the given
	 * {@link EReference}.
	 * 
	 * e.g.: the {@link EReference} {@link EClass#getEStructuralFeatures()}
	 * 
	 * returns the weight: {@link EAttribute}=10, {@link EReference}=8 and
	 * {@link EOperation}=2 to have those ratio in the eStructuralFeatures
	 * references.
	 * 
	 * @param eReference
	 * @param eClass
	 * @return
	 */
	int getWeightFor(EReference eReference, EClass eClass);

	/**
	 * Returns the distribution for the values of the given {@link EAttribute}s
	 * if they may have a variable length (e.g., {@link String}s, arrays, etc.)
	 * 
	 * @param eAttribute
	 * @return
	 */
	IntegerDistribution getValueDistributionFor(Class<?> clazz);

	/**
	 * Same as for {@link #getDistributionFor(EReference)}.
	 * 
	 * @param eAttribute
	 * @return
	 */
	IntegerDistribution getDistributionFor(EAttribute eAttribute);

	/**
	 * Returns the distribution to follow describing the depth of an instance of
	 * the given ECLass.
	 * 
	 * e.g: the {@link EClass} {@link EPackage} is a
	 * {@link #possibleRootEClasses()}. The specimen generator will generate a
	 * number of instance following {@link #getRootDistributionFor(EClass)}. The
	 * depth from those objects to leaves will follow the returned distribution.
	 * 
	 * @param eClass
	 * @return
	 */
	IntegerDistribution getDepthDistributionFor(EClass eClass);

}