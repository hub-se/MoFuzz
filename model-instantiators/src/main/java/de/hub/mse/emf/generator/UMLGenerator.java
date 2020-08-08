package de.hub.mse.emf.generator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.LogManager;

import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.provider.EcoreItemProviderAdapterFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UMLPackage.Literals;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import org.eclipse.uml2.common.edit.domain.UML2AdapterFactoryEditingDomain;
import org.eclipse.uml2.uml.edit.providers.UMLItemProviderAdapterFactory;
import org.eclipse.uml2.uml.edit.providers.UMLReflectiveItemProviderAdapterFactory;
import org.eclipse.uml2.uml.edit.providers.UMLResourceItemProviderAdapterFactory;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.generator.internal.MetamodelResource;
import de.hub.mse.emf.generator.internal.ModelGenerationStats;


//import de.hub.mse.emf.generator.benchmarks.*;

public class UMLGenerator extends Generator<Resource>{
	
	private ResourceSetImpl resourceSet;
	//public Resource modelResource;	
	private Set<EClass> eClassWhitelist;
	private ModelGenerationConfigImpl config;
	private ModelGenerator generator;
	
	public UMLGenerator() {
		super(Resource.class);
		
		// Disable logging
		LogManager.getLogManager().reset();
		
		this.resourceSet = new ResourceSetImpl();
		this.eClassWhitelist = new HashSet<EClass>();

		UMLResourcesUtil.init(this.resourceSet);
		
		// Might be redundant
		this.resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		this.resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
		
		/*
		eClassWhitelist.add(UMLPackage.Literals.CLASS);
		eClassWhitelist.add(UMLPackage.Literals.COMMENT);
		eClassWhitelist.add(UMLPackage.Literals.PACKAGE);
		eClassWhitelist.add(UMLPackage.Literals.PACKAGE_IMPORT);
		eClassWhitelist.add(UMLPackage.Literals.TEMPLATE_PARAMETER);
		eClassWhitelist.add(UMLPackage.Literals.TEMPLATE_SIGNATURE);
		eClassWhitelist.add(UMLPackage.Literals.TEMPLATE_BINDING);
		eClassWhitelist.add(UMLPackage.Literals.TEMPLATE_PARAMETER_SUBSTITUTION);
		eClassWhitelist.add(UMLPackage.Literals.ASSOCIATION);
		eClassWhitelist.add(UMLPackage.Literals.PROPERTY);
		eClassWhitelist.add(UMLPackage.Literals.DEPENDENCY);
		eClassWhitelist.add(UMLPackage.Literals.PARAMETER);
		eClassWhitelist.add(UMLPackage.Literals.OPERATION);
		eClassWhitelist.add(UMLPackage.Literals.DATA_TYPE);
		eClassWhitelist.add(UMLPackage.Literals.INTERFACE);
		eClassWhitelist.add(UMLPackage.Literals.OPERATION_TEMPLATE_PARAMETER);
		eClassWhitelist.add(UMLPackage.Literals.ENUMERATION);
		eClassWhitelist.add(UMLPackage.Literals.ENUMERATION_LITERAL);
		eClassWhitelist.add(UMLPackage.Literals.PRIMITIVE_TYPE);
		eClassWhitelist.add(UMLPackage.Literals.MODEL);
		eClassWhitelist.add(UMLPackage.Literals.GENERALIZATION);
		eClassWhitelist.add(UMLPackage.Literals.GENERALIZATION_SET);
		eClassWhitelist.add(UMLPackage.Literals.REDEFINABLE_TEMPLATE_SIGNATURE);
		eClassWhitelist.add(UMLPackage.Literals.SUBSTITUTION);
		eClassWhitelist.add(UMLPackage.Literals.REALIZATION);
		eClassWhitelist.add(UMLPackage.Literals.CLASSIFIER_TEMPLATE_PARAMETER);
		eClassWhitelist.add(UMLPackage.Literals.INTERFACE_REALIZATION);
		eClassWhitelist.add(UMLPackage.Literals.QUALIFIER_VALUE);
		eClassWhitelist.add(UMLPackage.Literals.INSTANCE_VALUE);
		eClassWhitelist.add(UMLPackage.Literals.ASSOCIATION_CLASS);
		eClassWhitelist.add(UMLPackage.Literals.LITERAL_INTEGER);
		eClassWhitelist.add(UMLPackage.Literals.LITERAL_UNLIMITED_NATURAL);

		// Supertypes, should not be required
		/*
		eClassWhitelist.add(UMLPackage.Literals.ELEMENT);
		eClassWhitelist.add(UMLPackage.Literals.NAMED_ELEMENT);
		eClassWhitelist.add(UMLPackage.Literals.NAMESPACE);
		eClassWhitelist.add(UMLPackage.Literals.REDEFINABLE_ELEMENT);
		eClassWhitelist.add(UMLPackage.Literals.PARAMETERABLE_ELEMENT);
		eClassWhitelist.add(UMLPackage.Literals.PACKAGEABLE_ELEMENT);
		eClassWhitelist.add(UMLPackage.Literals.TYPE);
		eClassWhitelist.add(UMLPackage.Literals.TEMPLATEABLE_ELEMENT);
		eClassWhitelist.add(UMLPackage.Literals.CLASSIFIER);
		eClassWhitelist.add(UMLPackage.Literals.STRUCTURED_CLASSIFIER);
		eClassWhitelist.add(UMLPackage.Literals.ENCAPSULATED_CLASSIFIER);
		eClassWhitelist.add(UMLPackage.Literals.BEHAVIORED_CLASSIFIER);
		eClassWhitelist.add(UMLPackage.Literals.RELATIONSHIP);
		eClassWhitelist.add(UMLPackage.Literals.DIRECTED_RELATIONSHIP);
		eClassWhitelist.add(UMLPackage.Literals.FEATURE);
		eClassWhitelist.add(UMLPackage.Literals.TYPED_ELEMENT);
		eClassWhitelist.add(UMLPackage.Literals.MULTIPLICITY_ELEMENT);
		eClassWhitelist.add(UMLPackage.Literals.STRUCTURAL_FEATURE);
		eClassWhitelist.add(UMLPackage.Literals.CONNECTABLE_ELEMENT);
		eClassWhitelist.add(UMLPackage.Literals.DEPLOYMENT_TARGET);
		eClassWhitelist.add(UMLPackage.Literals.BEHAVIORAL_FEATURE);
		eClassWhitelist.add(UMLPackage.Literals.DEPLOYED_ARTIFACT);
		eClassWhitelist.add(UMLPackage.Literals.INSTANCE_SPECIFICATION);
		eClassWhitelist.add(UMLPackage.Literals.ABSTRACTION);
		eClassWhitelist.add(UMLPackage.Literals.VALUE_SPECIFICATION);
		eClassWhitelist.add(UMLPackage.Literals.LITERAL_SPECIFICATION);
		*/
		
		// Initialize model
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
	}

	public UMLGenerator(int size, int depth, int breadth, int valuesize) {
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
		MetamodelResource metamodelResource = new MetamodelResource(UMLPackage.eINSTANCE);
		this.config = new ModelGenerationConfigImpl(metamodelResource, Literals.MODEL, eClassWhitelist, size,  depth, breadth, valuesize);
		this.generator = new ModelGenerator(config);
	}
	
	public Resource loadFromFile(String path) {
		URI modelURI = URI.createFileURI(path);
		Resource modelResource = resourceSet.getResource(modelURI, true);
		return modelResource;
	}
	
	public static void main(String[] args) {
		UMLGenerator generator = new UMLGenerator();
		boolean debug = false;
		if(debug) {
			long seed = 1583674167575L;
			System.out.println("Seed: " + seed);
			SourceOfRandomness rand = new SourceOfRandomness(new Random(seed));
			try {
				Resource resource = generator.generate(rand, null);
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
		else {
			int base = 1000; // print stats after this many models have been generated
			long startTime = System.currentTimeMillis();
			ModelGenerationStats.startTime = startTime;
			
			int generatedModels = 0;
			for(int i = 1; true; i++) {
				long seed = System.currentTimeMillis();
				//seed = 1583676448008L;
				SourceOfRandomness rand = new SourceOfRandomness(new Random(seed));
				try {
					Resource resource = generator.generate(rand, null);	
					//resource.save(null);
					generatedModels++;
					int size = ModelGenerationStats.addModelCount(resource);
					
					// Add counts to coverage map
					ModelGenerationStats.updateMetamodelCoverage(resource);
				}
				catch(Exception e) {
					System.out.println("Unhandled exception for seed " + seed);
					e.printStackTrace();
					return;
				}
				finally {
					// Clean up resources
					/*
					generator.resourceSet.getResources().get(0).unload();
					generator.resourceSet.getResources().remove(0);
					
					if(generator.resourceSet.getResources().size() > 1) {
						System.err.println("More than 1 resource:" + generator.resourceSet.getResources().size());
					}
					*/
					
					// Print stats after every 10% of progress
					if(generatedModels % base == 0) {
						ModelGenerationStats.printCompleteStats(i, generatedModels);
					}
					else if(generatedModels % (base/10) == 0) {	
						System.out.println("...");
					}

				}
			}
		}
	}
	
	// This method is invoked to generate a single test case
	private int id = 0;
	@Override
	public Resource generate(SourceOfRandomness random, GenerationStatus genStatus) {
		
		
		// Clean up resource from previous generation
		EList<Resource> resources = this.resourceSet.getResources();
		if(resources.size() > 0) {
			resources.get(0).unload();
			resources.remove(0);
			/*
			try {
				resources.get(0).delete(Collections.EMPTY_MAP);
			}
			catch(IOException e){
				
			}
			*/
			assert(resources.size() == 0);
		}
		
		Resource modelResource = resourceSet.createResource(URI.createFileURI("uml_model" +"_" + (++id) + ".uml"));	
		
		/**
		 * Test: create errornous model
		 */
		/*
		org.eclipse.uml2.uml.Model model = UMLFactory.eINSTANCE.createModel();
		org.eclipse.uml2.uml.StateMachine stateMachine = UMLFactory.eINSTANCE.createStateMachine();
		org.eclipse.uml2.uml.OpaqueBehavior opaqueBehavior = UMLFactory.eINSTANCE.createOpaqueBehavior();
		stateMachine.eSet(UMLPackage.Literals.BEHAVIORED_CLASSIFIER__OWNED_BEHAVIOR, opaqueBehavior);
		*/
		
		generator.generate(modelResource, random, genStatus);
		//System.out.println(status.getQueue());

		if(modelResource.isModified()) {
			return modelResource;
		}
		else {
			throw new RuntimeException("Unable to create model");
		}		
	}
	
	private static void registerPackage(EObject eObject) {
		if (eObject instanceof EPackage) {
			EPackage p = (EPackage) eObject;
			EPackage.Registry.INSTANCE.put(p.getNsURI(), p);
		}
		else {
			System.err.println("Warning (registerPackages): EObject " + eObject.toString() + " is not a package.");
		}
	}
}

