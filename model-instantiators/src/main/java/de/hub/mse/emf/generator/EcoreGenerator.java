package de.hub.mse.emf.generator;

import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.logging.LogManager;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.ecore.xmi.impl.EcoreResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceFactoryImpl;
import org.eclipse.emf.ecore.xmi.impl.XMIResourceImpl;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.generator.ModelGenerationConfigImpl;
import de.hub.mse.emf.generator.ModelGenerator;
import de.hub.mse.emf.generator.internal.MetamodelResource;

public class EcoreGenerator extends Generator<Resource>{
	
	private ResourceSetImpl resourceSet;
	private Set<EClass> eClassWhitelist;
	private ModelGenerationConfigImpl config;
	private ModelGenerator generator;
	
	public EcoreGenerator() {
		super(Resource.class);
		
		// Disable logging
		//LogManager.getLogManager().reset();
		
		{
			// Initialize the global registry
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
					EcorePackage.eNS_PREFIX, new EcoreResourceFactoryImpl());
			Resource.Factory.Registry.INSTANCE.getExtensionToFactoryMap().put(
					Resource.Factory.Registry.DEFAULT_EXTENSION, new XMIResourceFactoryImpl());
		}
		
		this.resourceSet = new ResourceSetImpl();
		this.eClassWhitelist = Collections.EMPTY_SET;
		
		try {
			String metamodelPath = "/Users/Lam/Studium/Masterarbeit/MetaModelFuzzing/implementation/atlanmod.instantiator/dist/model/TT.ecore";
			Resource metamodelResource = new XMIResourceImpl(URI.createFileURI(metamodelPath));
			metamodelResource.load(Collections.EMPTY_MAP);
			EcoreUtil.resolveAll(metamodelResource);
			
			registerPackages(metamodelResource);
						
			//GenericMetamodelConfig config = new GenericMetamodelConfig(metamodelResource, range, 12344, null, eClassWhitelist);
			//GenericMetamodelGenerator modelGen = new GenericMetamodelGenerator(config);
			
			MetamodelResource metamodelResourceWrapper = new MetamodelResource(metamodelResource);
			this.config = new ModelGenerationConfigImpl(metamodelResourceWrapper, eClassWhitelist, 500, 10, 10, 10);
			this.generator = new ModelGenerator(config);
			/*
			config.setValuesRange(5,10);
			
			config.setReferencesRange(5,10);
			
			config.setPropertiesRange(5,10);
			
			ResourceSetImpl resourceSet = new ResourceSetImpl();
			modelGen.runGeneration(resourceSet, 2, 100, 0.1f);*/

		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
	}
	
	
	public static void main(String[] args) {
		EcoreGenerator generator = new EcoreGenerator();
		SourceOfRandomness rand = new SourceOfRandomness(new Random());
				
		for(int i = 0; i < 2; i++) {
			
		Resource resource = generator.generate(rand, null);		
		try {
			resource.save(Collections.EMPTY_MAP);
		}
		catch(Exception e) {
			e.printStackTrace();
		}
		
		int count = 0;
		TreeIterator<EObject> iterator = resource.getAllContents();
		while(iterator.hasNext()) {
			iterator.next();
			count++;
		}
		System.out.println("Object count: " + count);
		}
	}
	
	static int modelCount = 0;
	// This method is invoked to generate a single test case
	@Override
	public Resource generate(SourceOfRandomness random, GenerationStatus __ignore__) {
		modelCount++;
		Resource modelResource = resourceSet.createResource(URI.createFileURI("out/model"+modelCount+".xmi"));	
		
		generator.generate(modelResource, random, __ignore__);

		if(modelResource.isModified()) {
			try {
				modelResource.save(Collections.EMPTY_MAP);
			}
			catch(Exception e) {
				e.printStackTrace();
			}
			return modelResource;
		}
		else {
			throw new RuntimeException("Unable to create model");
		}
	}
	
	private static void registerPackages(Resource resource) {
		EObject eObject = resource.getContents().get(0);
		if (eObject instanceof EPackage) {
			EPackage p = (EPackage) eObject;
			EPackage.Registry.INSTANCE.put(p.getNsURI(), p);
		}
	}
}


