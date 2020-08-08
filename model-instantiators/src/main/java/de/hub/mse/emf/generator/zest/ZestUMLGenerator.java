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

package de.hub.mse.emf.generator.zest;

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
import org.eclipse.uml2.uml.UMLPackage.Literals;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.generator.ModelGenerationConfigImpl;
import de.hub.mse.emf.generator.ModelGenerator;
import de.hub.mse.emf.generator.internal.MetamodelResource;
import fr.inria.atlanmod.instantiator.GenerationException;
import fr.inria.atlanmod.instantiator.ZestMetamodelConfig;
import fr.obeo.emf.specimen.SpecimenGenerator;
import fr.obeo.emf.specimen.ZestSpecimenGenerator;


public class ZestUMLGenerator extends Generator<Resource>{
	

	private ResourceSetImpl resourceSet;
	//public Resource modelResource;	
	private Set<EClass> eClassWhitelist;
	private ModelGenerationConfigImpl config;
	private ModelGenerator generator;
	
	public ZestUMLGenerator() {
		super(Resource.class);
		
		// Disable logging
		LogManager.getLogManager().reset();
		
		this.resourceSet = new ResourceSetImpl();
		this.eClassWhitelist = new HashSet<EClass>();

		UMLResourcesUtil.init(this.resourceSet);
		
		// Might be redundant
		this.resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		this.resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);

		UMLPackage.eINSTANCE.eClass();
		
		EcoreUtil.resolveAll(UMLPackage.eINSTANCE);
		
		// Might be redundant too
		//registerPackage(UMLPackage.eINSTANCE);
		MetamodelResource metamodelResource = new MetamodelResource(UMLPackage.eINSTANCE);
		
		/*
		 * Numeric parameters:
		 * 1. maxObjectCount
		 * 2. maxDepth
		 * 3. maxBreadth
		 * 4. maxValueSize
		 */
		this.config = new ModelGenerationConfigImpl(metamodelResource, Literals.MODEL, eClassWhitelist, 500,  10, 10, 10);
		this.generator = new ModelGenerator(config);
		ModelGenerator.trackMetamodelCoverage = false;
	}
	
		private int id = 0;
		@Override
		public Resource generate(SourceOfRandomness random, GenerationStatus genStatus) {
			
			// Clean up resource from previous generation
			EList<Resource> resources = this.resourceSet.getResources();
			if(resources.size() > 0) {
				resources.get(0).unload();
				resources.remove(0);

				assert(resources.size() == 0);
			}
			
			Resource modelResource = resourceSet.createResource(URI.createFileURI("uml_model" +"_" + (++id) + ".uml"));	
			
			generator.generate(modelResource, random, genStatus);
						
			if(modelResource.isModified()) {
				return modelResource;
			}
			else {
				throw new RuntimeException("Unable to create model");
			}		
		}
	
	
	public static void main(String[] args) throws GenerationException, IOException {
		ZestUMLGenerator generator = new ZestUMLGenerator();
		int attempts = 10000;
		int success = 0;
		for(int i = 0; i < attempts; i++) {
			long seed = System.currentTimeMillis();
			//System.out.println(seed);
			SourceOfRandomness random = new SourceOfRandomness(new Random(seed));
			try {
				Resource resource = generator.generate(random, null);	
				if(resource != null) success++;
				int size = 0;
				for(TreeIterator<EObject> it = resource.getAllContents(); it.hasNext();) {
					it.next();
					size++;
				}
				System.out.println(size);
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
