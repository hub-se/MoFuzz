package de.hub.mse.emf.mutator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UMLPackage.Literals;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.junit.Before;
import org.junit.Test;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.generator.ModelGenerationConfigImpl;
import de.hub.mse.emf.generator.ModelGenerator;
import de.hub.mse.emf.generator.cgf.MetamodelCoverage;
import de.hub.mse.emf.generator.internal.EClassReferencePair;
import de.hub.mse.emf.generator.internal.MetamodelResource;
import de.hub.mse.emf.generator.internal.MetamodelUtil;
import de.hub.mse.emf.mutator.mutation.AddObjectMutation;
import de.hub.mse.emf.mutator.mutation.ChangeAttributesMutation;
import de.hub.mse.emf.mutator.mutation.ChangeCrossReferencesMutation;
import de.hub.mse.emf.mutator.mutation.DeleteObjectMutation;
import de.hub.mse.emf.mutator.mutation.Mutation;
import de.hub.mse.emf.mutator.mutation.ReplaceSubtreeMutation;
import de.hub.mse.emf.mutator.mutation.UnsetAttributesMutation;

public class UMLMutatorTest {
	
	private ResourceSetImpl resourceSet;
	private MetamodelResource metamodelResource;
	private HashSet<EClass> eClassWhitelist;
	private ModelGenerationConfigImpl config;
	private MetamodelUtil metamodelUtil;
	private MetamodelCoverage metamodelCoverage;
	private ModelGenerator modelGenerator;
		
	@Before
	public void setUp() {
		resourceSet = new ResourceSetImpl();
		resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
		UMLPackage.eINSTANCE.eClass();			
		EcoreUtil.resolveAll(UMLPackage.eINSTANCE);
		metamodelResource = new MetamodelResource(UMLPackage.eINSTANCE);
		eClassWhitelist = new HashSet<EClass>();
		config = new ModelGenerationConfigImpl(metamodelResource, Literals.MODEL, eClassWhitelist, 500,  10, 10 , 10);
		metamodelUtil = new MetamodelUtil(config.ePackages(), config.ignoredEClasses(), config.getEClassWhitelist());
		metamodelCoverage = new MetamodelCoverage(metamodelUtil.getAllEClasses(), metamodelUtil.getAllEContainmentRefs());
		modelGenerator = new ModelGenerator(config, metamodelUtil, metamodelCoverage);
	}
	
	@Test
	public void addMutationTest() {
		
		// Define mutation operators: AddObjectMutation only
		ArrayList<Mutation> mutationOperators = new ArrayList<Mutation>();
		mutationOperators.add(new AddObjectMutation());
		ModelMutator mutator = new ModelMutator(resourceSet, metamodelUtil, new Random(24), metamodelCoverage, mutationOperators);
		
		// Create a model consisting of only one 'Model' object
		Resource modelResource = resourceSet.createResource(URI.createFileURI("addMutationTest" + ".uml"));	
		EObject model = UMLFactory.eINSTANCE.createModel();
		modelResource.getContents().add(model);
		
		// Try to add 100 random objects
		int objectsAdded = 0;
		for(int i = 0; i < 100; i++) {			
			// Returns true if a new object was added
			int result = mutator.mutate(modelResource, null); 
			if(result != Integer.MIN_VALUE) {
				objectsAdded += result;
			}
		}
		assertEquals(getModelSize(modelResource), objectsAdded + 1);
		
		// Now undo everything again
		for(int i = 0; i < objectsAdded; i++) {
			mutator.undoLastMutation();
		}
		assertEquals(getModelSize(modelResource), 1);
	}
	
	@Test
	public void addMultiMutationTest() {
		
		// Define mutation operators: AddObjectMutation only
		ArrayList<Mutation> mutationOperators = new ArrayList<Mutation>();
		mutationOperators.add(new AddObjectMutation(10));
		ModelMutator mutator = new ModelMutator(resourceSet, metamodelUtil, new Random(24), metamodelCoverage, mutationOperators);
		
		// Create a model consisting of only one 'Model' object
		Resource modelResource = resourceSet.createResource(URI.createFileURI("addMutationTest" + ".uml"));	
		EObject model = UMLFactory.eINSTANCE.createModel();
		modelResource.getContents().add(model);
		
		// Try to add 100 random objects, 10 per mutation
		int objectsAdded = 0;
		for(int i = 0; i < 10; i++) {			
			// Returns true if a new object was added
			objectsAdded += mutator.mutate(modelResource, null);;
		}
		assertEquals(getModelSize(modelResource), objectsAdded + 1);
		
		// Now undo everything again
		for(int i = 0; i < 10; i++) {
			mutator.undoLastMutation();
		}
		assertEquals(getModelSize(modelResource), 1);
	}
	
	
	@Test
	public void deleteMutationTest() {
		
		// Define mutation operators: DeleteObjectMutation only
		ArrayList<Mutation> mutationOperators = new ArrayList<Mutation>();
		mutationOperators.add(new DeleteObjectMutation());
		
		// Using the initial seed 24 ensures that not the root of the model will be chosen for the deletion mutation
		ModelMutator mutator = new ModelMutator(resourceSet, metamodelUtil, new Random(24), metamodelCoverage, mutationOperators);
		
		/* Create our model:
		 *  
		 *  Model
		 *    |--- Class
		 *  		|--- Operation 1
		 *  		|--- Operation 2
		 *  		|--- Operation 3
		 */
		Resource modelResource = resourceSet.createResource(URI.createFileURI("deleteMutationTest" + ".uml"));	
		
		org.eclipse.uml2.uml.Model model = UMLFactory.eINSTANCE.createModel();
		
		org.eclipse.uml2.uml.Class clazz = UMLFactory.eINSTANCE.createClass();
		model.getPackagedElements().add(clazz);

		org.eclipse.uml2.uml.Operation op1 = UMLFactory.eINSTANCE.createOperation();
		org.eclipse.uml2.uml.Operation op2 = UMLFactory.eINSTANCE.createOperation();
		org.eclipse.uml2.uml.Operation op3 = UMLFactory.eINSTANCE.createOperation();
		clazz.getOwnedOperations().add(op1);
		clazz.getOwnedOperations().add(op2);
		clazz.getOwnedOperations().add(op3);

		modelResource.getContents().add(model);
		assertEquals(getModelSize(modelResource), 5);
		
		// This will delete one Operation object
		int diff = mutator.mutate(modelResource, null);
		assertEquals(getModelSize(modelResource), 5 + diff);
		
		// This will delete the Class object together with the remaining 2 Operations contained in it
		diff += mutator.mutate(modelResource, null);
		assertEquals(getModelSize(modelResource), 5 + diff);
		
		// Undo everthing
		mutator.undoLastMutation();
		assertEquals(getModelSize(modelResource), 4);
		
		mutator.undoLastMutation();
		assertEquals(getModelSize(modelResource), 5);
	}
	
	@Test
	public void changeAttributesTest() {
		// Define mutation operators: ChangeAttributesMutation only
		ArrayList<Mutation> mutationOperators = new ArrayList<Mutation>();
		mutationOperators.add(new ChangeAttributesMutation());
		
		ModelMutator mutator = new ModelMutator(resourceSet, metamodelUtil, new Random(24), metamodelCoverage, mutationOperators);
		
		Resource modelResource = resourceSet.createResource(URI.createFileURI("changeAttributesMutationTest" + ".uml"));	
		
		org.eclipse.uml2.uml.Model model = UMLFactory.eINSTANCE.createModel();
		modelResource.getContents().add(model);
		
		// No attributes are set yet
		for(EAttribute eAttribute : metamodelUtil.eAllAttributes(model.eClass())) {
			assertTrue(!model.eIsSet(eAttribute));
		}
		
		// Initialize attributes first
		mutator.mutate(modelResource, null);
		
		// All attributes are now set
		for(EAttribute eAttribute : metamodelUtil.eAllAttributes(model.eClass())) {
			assertTrue(model.eIsSet(eAttribute));
		}
		
		// Mutate all attributes again
		mutator.mutate(modelResource, null);
		
		// Undo both mutations
		mutator.undoLastMutation();
		mutator.undoLastMutation();
		
		// No attributes are set anymore
		for(EAttribute eAttribute : metamodelUtil.eAllAttributes(model.eClass())) {
			assertTrue(!model.eIsSet(eAttribute));
		}
	}
	
	@Test
	public void unsetAttributesTest() {
		// Define mutation operators: DeleteObjectMutation only
		ArrayList<Mutation> mutationOperators = new ArrayList<Mutation>();
		mutationOperators.add(new UnsetAttributesMutation());
		
		ModelMutator mutator = new ModelMutator(resourceSet, metamodelUtil, new Random(24), metamodelCoverage, mutationOperators);
		
		// Create a model and set some attributes
		Resource modelResource = resourceSet.createResource(URI.createFileURI("unsetAttributesMutationTest" + ".uml"));	
		org.eclipse.uml2.uml.Model model = UMLFactory.eINSTANCE.createModel();
		model.setName("model");
		model.setURI("uri");
		model.setViewpoint("viewpoint");
		org.eclipse.uml2.uml.VisibilityKind vkind = (org.eclipse.uml2.uml.VisibilityKind) UMLPackage.Literals.VISIBILITY_KIND.getELiterals().get(0).getInstance();
		model.setVisibility(vkind);
		modelResource.getContents().add(model);
		
		// Unsets all attributes
		mutator.mutate(modelResource, null);
		
		// No attributes are set yet
		for(EAttribute eAttribute : metamodelUtil.eAllAttributes(model.eClass())) {
			assertTrue(!model.eIsSet(eAttribute));
		}
		
		// Undo changes
		mutator.undoLastMutation();
		
		assertTrue(model.getName().equals("model"));
		assertTrue(model.getURI().equals("uri"));
		assertTrue(model.getViewpoint().equals("viewpoint"));
		assertTrue(model.getVisibility().equals(vkind));
	}
	
	private int getModelSize(Resource modelResource) {
		int size = 0;
		for(TreeIterator<EObject> it = modelResource.getAllContents(); it.hasNext(); it.next()) {
			size++;
		}
		return size;
	}
	
	@Test
	public void replaceSubTreeMutation() {
		
		// Define mutation operators: DeleteObjectMutation only
		ArrayList<Mutation> mutationOperators = new ArrayList<Mutation>();
		mutationOperators.add(new ReplaceSubtreeMutation(modelGenerator));
		
		// Using the initial seed 24 ensures that not the root of the model will be chosen for the deletion mutation
		ModelMutator mutator = new ModelMutator(resourceSet, metamodelUtil, new Random(24), metamodelCoverage, mutationOperators);
		
		/* Create our model:
		 *  
		 *  Model
		 *    |--- Class
		 *  		|--- Operation 1
		 *  		|--- Operation 2
		 *  		|--- Operation 3
		 */
		Resource modelResource = resourceSet.createResource(URI.createFileURI("replaceSubTreeMutationTest" + ".uml"));	
		
		org.eclipse.uml2.uml.Model model = UMLFactory.eINSTANCE.createModel();
		
		org.eclipse.uml2.uml.Class clazz = UMLFactory.eINSTANCE.createClass();
		model.getPackagedElements().add(clazz);

		org.eclipse.uml2.uml.Operation op1 = UMLFactory.eINSTANCE.createOperation();
		org.eclipse.uml2.uml.Operation op2 = UMLFactory.eINSTANCE.createOperation();
		org.eclipse.uml2.uml.Operation op3 = UMLFactory.eINSTANCE.createOperation();
		clazz.getOwnedOperations().add(op1);
		clazz.getOwnedOperations().add(op2);
		clazz.getOwnedOperations().add(op3);

		modelResource.getContents().add(model);
		
		int totalSizeDiff = 0;
		for(int i = 0; i < 100; i++) {
			int sizeDiff = mutator.mutate(modelResource, null);
			if(sizeDiff != Integer.MIN_VALUE) totalSizeDiff += sizeDiff;
			//assertEquals(getModelSize(modelResource), 5 + totalSizeDiff);
		}
		
		//assertEquals(getModelSize(modelResource), 5 + totalSizeDiff);
	}
	
	@Test
	public void mutationWeightsTest() {
		// Generate random model
		Resource modelResource = resourceSet.createResource(URI.createFileURI("modelMutationTest" + ".uml"));
		SourceOfRandomness random = new SourceOfRandomness(new Random(24));
		modelGenerator.generate(modelResource, random, null);
		
		ArrayList<Mutation> mutationOperators = new ArrayList<Mutation>();
		mutationOperators.add(new AddObjectMutation(10));
		mutationOperators.add(new DeleteObjectMutation());
		mutationOperators.add(new ChangeAttributesMutation());
		mutationOperators.add(new UnsetAttributesMutation());
		mutationOperators.add(new ReplaceSubtreeMutation(modelGenerator));
		mutationOperators.add(new ChangeCrossReferencesMutation());
		
		double[] mutationWeights = {5,1,3,3,3,2};
		ModelMutator mutator = new ModelMutator(resourceSet, metamodelUtil, new Random(), metamodelCoverage, mutationOperators);
		mutator.setMutationWeights(mutationWeights);
		
		int addMutationCount = 0;
		int deleteObjectMutationCount = 0;
		int changeAttrMutationCount = 0;
		int unsetAttrMutationCount = 0;
		int replaceSubTreeMutationCount = 0;
		int changeCrossRefsMutationCount = 0;
		
		for(int i = 0; i < 10000; i++) {
			Mutation mutation = mutator.getRandomMutation();
			if(mutation instanceof AddObjectMutation) {
				addMutationCount++;
			}
			if(mutation instanceof DeleteObjectMutation) {
				deleteObjectMutationCount++;
			}
			if(mutation instanceof ChangeAttributesMutation) {
				changeAttrMutationCount++;
			}
			if(mutation instanceof UnsetAttributesMutation) {
				unsetAttrMutationCount++;
			}
			if(mutation instanceof ReplaceSubtreeMutation) {
				replaceSubTreeMutationCount++;
			}
			if(mutation instanceof ChangeCrossReferencesMutation) {
				changeCrossRefsMutationCount++;
			}
		}
		System.out.println(addMutationCount);
		System.out.println(deleteObjectMutationCount);
		System.out.println(changeAttrMutationCount);
		System.out.println(unsetAttrMutationCount);
		System.out.println(replaceSubTreeMutationCount);
		System.out.println(changeCrossRefsMutationCount);

		
		
	}
	
	// Robustness test
	@Test
	public void completeMutationTest() {
		
		// Generate random model
		Resource modelResource = resourceSet.createResource(URI.createFileURI("modelMutationTest" + ".uml"));
		SourceOfRandomness random = new SourceOfRandomness(new Random(24));
		modelGenerator.generate(modelResource, random, null);
		
		//org.eclipse.uml2.uml.SendObjectAction sendObjectAction = UMLFactory.eINSTANCE.createSendObjectAction();
		//org.eclipse.uml2.uml.InputPin inputPin = UMLFactory.eINSTANCE.createInputPin();
		//sendObjectAction.eSet(UMLPackage.Literals.INVOCATION_ACTION__ARGUMENT, Collections.singleton(inputPin));
		
		EClassReferencePair classRefPair = new EClassReferencePair(UMLPackage.Literals.SEND_OBJECT_ACTION, UMLPackage.Literals.INVOCATION_ACTION__ARGUMENT);
		metamodelUtil.eReferenceValidEClasses(classRefPair);
		
		// Setup model mutator
		ArrayList<Mutation> mutationOperators = new ArrayList<Mutation>();
		mutationOperators.add(new AddObjectMutation());
		mutationOperators.add(new DeleteObjectMutation());
		mutationOperators.add(new ChangeAttributesMutation());
		mutationOperators.add(new UnsetAttributesMutation());
		mutationOperators.add(new ReplaceSubtreeMutation(modelGenerator));
		mutationOperators.add(new ChangeCrossReferencesMutation());
		ModelMutator mutator = new ModelMutator(resourceSet, metamodelUtil, new Random(24), metamodelCoverage, mutationOperators);
		
		// Number of successfully executed mutations
		int success = 0;
		for(int i = 0; i < 10000; i++) {
			//Only for cross-reference test
			EList<Resource> resources = this.resourceSet.getResources();
			if(resources.size() > 0) {
				resources.get(0).unload();
				resources.remove(0);
				assert(resources.size() == 0);
			}
			
			Resource model = resourceSet.createResource(URI.createFileURI("uml_model"+ i + ".uml"));	
			modelGenerator.generate(model, random, null);
			
			// Returns true if a new object was added
			int result = mutator.mutate(model, null);
			if(result != Integer.MIN_VALUE) success++;
			//System.out.println(i + " - " + success);
		}
		System.out.println(success);
		// Undo each mutation
		for(int i = 0; i < success; i++) {
			//assertTrue(mutator.undoLastMutation());
		}
	}
	
}
