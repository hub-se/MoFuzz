package de.hub.mse.emf.generator.cgf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Random;
import java.util.logging.LogManager;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UMLPackage.Literals;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.fuzz.junit.quickcheck.ModelGenerationStatus;
import de.hub.mse.emf.generator.ModelGenerationConfigImpl;
import de.hub.mse.emf.generator.ModelGenerator;
import de.hub.mse.emf.generator.internal.MetamodelUtil;
import de.hub.mse.emf.generator.internal.MetamodelResource;
import de.hub.mse.emf.mutator.ModelMutator;
import de.hub.mse.emf.mutator.mutation.AddObjectMutation;
import de.hub.mse.emf.mutator.mutation.ChangeAttributesMutation;
import de.hub.mse.emf.mutator.mutation.ChangeCrossReferencesMutation;
import de.hub.mse.emf.mutator.mutation.DeleteObjectMutation;
import de.hub.mse.emf.mutator.mutation.Mutation;
import de.hub.mse.emf.mutator.mutation.ReplaceSubtreeMutation;
import de.hub.mse.emf.mutator.mutation.UnsetAttributesMutation;


/**
 * Provides UML models through generation or mutation of existing models.
 * @author Lam
 *
 */
public class UMLModelProvider {
	
	/** The resource set containing the model.*/
	private ResourceSetImpl resourceSet;
	
	/** The resource set containing the models of the input queue. */
	private ResourceSetImpl queueResourceSet;
	
	/** The metamodel resource */
	private MetamodelResource metamodelResource;
	
	/** Metamodel utility functions */
	private MetamodelUtil metamodelUtil;
	
	/** The model generation configuration. */
	private ModelGenerationConfigImpl generationConfig;
	
	/** The model mutation configuration. */
	//private ModelMutationConfig mutationConfig
	
	/** The EClasses to include in the generation. */
	HashSet<EClass> eClassWhitelist = new HashSet<EClass>(); // No whitelist for now

	/** The metamodel coverage */
	MetamodelCoverage metamodelCoverage;

	/** The model generator. */
	private ModelGenerator generator;
	
	/** The model mutator*/
	private ModelMutator mutator;
	
	public UMLModelProvider() {
		// Disable logging, TODO only log severe messages
		LogManager.getLogManager().reset();
		
		// Initialize resource set and UML package
		resourceSet = new ResourceSetImpl();
		resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
		UMLResourcesUtil.init(resourceSet);
		
		queueResourceSet = new ResourceSetImpl();
		queueResourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		queueResourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
		UMLResourcesUtil.init(resourceSet);
		
		UMLPackage.eINSTANCE.eClass();
		EcoreUtil.resolveAll(UMLPackage.eINSTANCE);
		
		metamodelResource = new MetamodelResource(UMLPackage.eINSTANCE);
		generationConfig = new ModelGenerationConfigImpl(metamodelResource, Literals.MODEL, eClassWhitelist, 500,  10, 10, 10);
		metamodelUtil = new MetamodelUtil(generationConfig.ePackages(), generationConfig.ignoredEClasses(), generationConfig.getEClassWhitelist());
		
		metamodelCoverage = new MetamodelCoverage(metamodelUtil.getAllEClasses(), metamodelUtil.getAllEContainmentRefs());
		generator = new ModelGenerator(generationConfig, metamodelUtil, metamodelCoverage);
		
		ArrayList<Mutation> mutationOperators = new ArrayList<Mutation>();
		mutationOperators.add(new AddObjectMutation(10));
		mutationOperators.add(new DeleteObjectMutation());
		mutationOperators.add(new ChangeAttributesMutation());
		mutationOperators.add(new UnsetAttributesMutation());
		mutationOperators.add(new ReplaceSubtreeMutation(generator));
		mutationOperators.add(new ChangeCrossReferencesMutation());
		
		double[] mutationWeights = {5,1,3,3,3,2};
		mutator = new ModelMutator(resourceSet, metamodelUtil, new Random(), metamodelCoverage, mutationOperators);
		mutator.setMutationWeights(mutationWeights);
	}
	
	public Resource generate(SourceOfRandomness random, GenerationStatus genStatus) {
		// Clean up resource from previous generation
		EList<Resource> resources = this.resourceSet.getResources();
		if(resources.size() > 0) {
			resources.get(0).unload();
			resources.remove(0);
			assert(resources.size() == 0);
		}
				
		Resource modelResource = resourceSet.createResource(URI.createFileURI("uml_model" + ".uml"));	
		generator.generate(modelResource, random, genStatus);
		
		ModelGenerationStatus status = (ModelGenerationStatus) genStatus;
		
		if(modelResource.isModified()) {
			return modelResource;
		}
		else {
			throw new RuntimeException("Unable to create model");
		}		
	}
	
	private int copyId = 0;
	
	public Resource createCopyResource(Resource modelResource) {
		Resource copyResource = queueResourceSet.createResource(URI.createFileURI("uml_copy" + (copyId++) + ".uml"));
		EObject sourceModel = EcoreUtil.copy(modelResource.getContents().get(0));
		copyResource.getContents().add(sourceModel);
		return copyResource;
	}
	
	public int mutate(Resource modelResource, SourceOfRandomness random, GenerationStatus genStatus) {
		return mutator.mutate(modelResource, genStatus);
	}
	
	public void undoLastMutation() {
		mutator.undoLastMutation();
		metamodelCoverage.discardTempCoveredEClasses();
	}
	
	public void commitTempCoveredEClasses() {
		metamodelCoverage.commitTempCoveredEClasses();
	}
	
	public void flushCommandStack() {
		// TODO add pending coverage
		mutator.flushCommandStack();
	}
	
	public boolean baseCoverageReached() {
		//System.out.println("EClass Coverage: " + metamodelCoverage.getEClassCoverage() + "ContainmentRef Coverage: " + metamodelCoverage.getEContainmentCoverage());
		return metamodelCoverage.getEClassCoverage() > 0.95 && metamodelCoverage.getEContainmentCoverage() > 0.8;
	}
	
	public static void main(String[] args) {
		UMLModelProvider modelProvider = new UMLModelProvider();
		SourceOfRandomness random = new SourceOfRandomness(new Random(25));
		modelProvider.generate(random, null);
		
		/*
		System.out.println(UMLPackage.Literals.PACKAGE__PACKAGED_ELEMENT.getEContainingClass().isSuperTypeOf(UMLPackage.Literals.MODEL));
		
		// This does not work
		System.out.println(UMLPackage.Literals.COMMENT.eContainingFeature().getEContainingClass().isSuperTypeOf(UMLPackage.Literals.MODEL));
		UMLPackage.eINSTANCE.getComment();
		System.out.println(UMLPackage.Literals.ELEMENT__OWNED_COMMENT.eContainer().eClass().isSuperTypeOf(UMLPackage.Literals.MODEL.eClass()));		
		System.out.println(UMLPackage.Literals.ELEMENT__OWNED_COMMENT.getEContainingClass().isSuperTypeOf(UMLPackage.Literals.MODEL));
		System.out.println(UMLPackage.eINSTANCE.getComment().equals(UMLPackage.Literals.COMMENT));
		*/

	}
	
}
