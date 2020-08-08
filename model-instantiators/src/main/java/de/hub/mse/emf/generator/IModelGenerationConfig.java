/*******************************************************************************
 * Copyright (c) 2012 Obeo.
 * Copyright (c) 2019 Hoang Lam Nguyen. TODO
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Obeo - initial API and implementation
 *     Abel Gómez (AtlanMod) - Additional modifications
 *     Hoang Lam Nguyen - Additional modifications    
*******************************************************************************/

package de.hub.mse.emf.generator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EOperation;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import com.google.common.collect.ImmutableSet;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.generator.internal.MetamodelResource;

public interface IModelGenerationConfig{

	/**
	 * Returns the set of {@link EClass}es to consider in the model generation.
	 * 
	 * @return the set of whitelisted {@link EClass}es.
	 */
	ImmutableSet<EClass> getEClassWhitelist();
		
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
	EClass getNextRootEClass(ImmutableSet<EClass> rootEClasses, SourceOfRandomness random);

	/**
	 * Total object count (in number of {@link EObject}s) which may be sampled from
	 * a source of randomness.
	 * 
	 * 
	 * @return the sampled object count
	 */
	int getTotalObjectCount(SourceOfRandomness random);
		
	//void setSourceOfRandomness(SourceOfRandomness random);

	/**
	 * Returns how many elements have to be generated to fill an instance of the
	 * given {@link EReference}.
	 * 
	 * e.g: the {@link EReference} {@link EClass#getESuperTypes()}
	 * 
	 * 
	 * @param eReference
	 * @return
	 */
	int getElementCountFor(EReference eReference, SourceOfRandomness random);

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
	 * Returns the size for the values of the given {@link EAttribute}s
	 * if they may have a variable length (e.g., {@link String}s, arrays, etc.)
	 * 
	 * @param eAttribute
	 * @return
	 */
	int getValueSizeFor(Class<?> clazz, SourceOfRandomness random);

	/**
	 * Same as for {@link #getElementCountFor(EReference)}.
	 * 
	 * @param eAttribute
	 * @return
	 */
	int getElementCountFor(EAttribute eAttribute, SourceOfRandomness random);

	/**
	 * Returns the depth of an instance of
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
	int getDepthFor(EClass eClass, SourceOfRandomness random);
	
	int getBreadthFor(EClass eClass, SourceOfRandomness random);
	
	/**
	 * Returns the user-specified of root {@link EClass}, or null if none has been provided.
	 * 
	 * @return the root {@link EClass} or null. 
	 */
	EClass getRootEClass();
	
}
