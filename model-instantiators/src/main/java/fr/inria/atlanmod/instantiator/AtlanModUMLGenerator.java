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

import java.io.IOException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.logging.LogManager;

import org.apache.commons.lang3.Range;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.generator.internal.MetamodelResource;
import fr.obeo.emf.specimen.SpecimenGenerator;


public class AtlanModUMLGenerator extends Generator<Resource>{
	

	private final int DEFAULT_AVERAGE_MODEL_SIZE = 1000;
	private final float DEFAULT_DEVIATION = 0.1f;
	private final int ERROR = 1;
	
	private ResourceSetImpl resourceSet;
	private GenericMetamodelConfig config;
	private SpecimenGenerator generator;
	
	private int size;
	private float variation;
	private float propVariation;
	private Range<Integer> range;
	private long seed;
	int valuesSize;
	int referencesSize;
	
	private Set<EClass> allEClasses;
	private Set<EClass> coveredEClasses;
	
	static boolean debug = false;
	
	public AtlanModUMLGenerator() {
		super(Resource.class);
		
		// Disable logging
		LogManager.getLogManager().reset();
		
		// UML initialization stuff
		resourceSet = new ResourceSetImpl();
		
		UMLResourcesUtil.init(this.resourceSet);
		
		resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
		
		UMLPackage.eINSTANCE.eClass();
		EcoreUtil.resolveAll(UMLPackage.eINSTANCE);
		MetamodelResource metamodelResource = new MetamodelResource(UMLPackage.eINSTANCE);
		
		// Generation parameters, default
		size = DEFAULT_AVERAGE_MODEL_SIZE;
		variation = DEFAULT_DEVIATION;		
		propVariation = DEFAULT_DEVIATION;	
		valuesSize = GenericMetamodelConfig.DEFAULT_AVERAGE_VALUES_LENGTH;
		referencesSize = GenericMetamodelConfig.DEFAULT_AVERAGE_REFERENCES_SIZE;
		
		seed = System.currentTimeMillis();
		range = Range.between(
				Math.round(size * (1 - variation)), 
				Math.round(size * (1 + variation)));
		
		config = new GenericMetamodelConfig(metamodelResource, range, seed);
		generator = new SpecimenGenerator(config, config.getSeed());
		
		config.setValuesRange(
				Math.round(valuesSize * (1 - propVariation)), 
				Math.round(valuesSize * (1 + propVariation)));
		
		config.setReferencesRange(
				Math.round(referencesSize * (1 - propVariation)), 
				Math.round(referencesSize * (1 + propVariation)));
		
		config.setPropertiesRange(
				Math.round(referencesSize * (1 - propVariation)), 
				Math.round(referencesSize * (1 + propVariation)));
		
		if(AtlanModUMLGenerator.debug) {
			coveredEClasses = new HashSet<EClass>();
			allEClasses = new HashSet<EClass>();
			Set<EPackage> ePackages = config.ePackages();
			
			for(EPackage ePackage : ePackages) {
				for(Iterator<EObject> it = ePackage.eAllContents(); it.hasNext();) {
					EObject eObject = it.next();
					if(eObject instanceof EClass){
						EClass eClass = (EClass) eObject;
						if(!eClass.isAbstract() && !eClass.isInterface()) {
							allEClasses.add(eClass);
						}
					}
				}
			}
		}

	}
	
	// This method is invoked to generate a single test case
	@Override
	public Resource generate(SourceOfRandomness random, GenerationStatus genStatus) {
		
		// Clean up resource from previous generation
		EList<Resource> resources = this.resourceSet.getResources();
		if(resources.size() > 0) {
			resources.get(0).unload();
			resources.remove(0);
			assert(resources.size() == 0);
		}
		
		Resource modelResource = resourceSet.createResource(URI.createFileURI("atlanmod_model" + ".uml"));	
		try {
			generator.generate(modelResource,random, UMLPackage.Literals.MODEL);
			if(modelResource.isModified()) {
				if(AtlanModUMLGenerator.debug) {
					int modelSize = 0;
					for(TreeIterator<EObject> it = modelResource.getAllContents(); it.hasNext();) {
						EClass eClass = it.next().eClass();
						coveredEClasses.add(eClass);
						modelSize++;
					}
					System.out.printf("Last model size: %d Metaclass coverage: %.15f \n", modelSize, (double)coveredEClasses.size() / allEClasses.size());
				}
				return modelResource;
			}
			else {
				modelResource.getContents().clear();
				modelResource.getContents().add(UMLFactory.eINSTANCE.createModel());
				return modelResource;
			}
		}
		catch(Exception e) {
			if(AtlanModUMLGenerator.debug) e.printStackTrace();
			modelResource.getContents().clear();
			modelResource.getContents().add(UMLFactory.eINSTANCE.createModel());
			return modelResource;
		}
		//System.out.println(status.getQueue());	
	}
	
	
	public static void main(String[] args) throws GenerationException, IOException {
		AtlanModUMLGenerator.debug = true;
		AtlanModUMLGenerator generator = new AtlanModUMLGenerator();
		int attempts = 10000;
		int success = 0;
		for(int i = 0; i < attempts; i++) {
			long seed = System.currentTimeMillis();
			SourceOfRandomness random = new SourceOfRandomness(new Random(seed));
			try {
				Resource resource = generator.generate(random, null);	
				if(resource != null) success++;
			}
			catch(Exception e) {
				System.out.println("Unhandled exception for seed " + seed);
				e.printStackTrace();
				return;
			}
		}
		float successRate = (float) success / attempts;
		System.out.println("Model Generation success rate: " + successRate);
	}
}
