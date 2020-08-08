package de.hub.mse.emf.generator.serge;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.emf.henshin.model.Rule;
import org.eclipse.uml2.uml.UMLFactory;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.resource.UMLResource;
import org.eclipse.uml2.uml.resources.util.UMLResourcesUtil;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.generator.serge.EditRuleWrapper.OperationKind;

public class SERGeUMLGenerator {

	private int iterationCount; // for debugging

	private ResourceSetImpl resourceSet;
	
	private ResourceSetImpl copyResources;

	private EditRuleWrapper erWrapper;

	private Map<OperationKind, Set<Rule>> appliedByKind;
	private Set<Rule> appliedConstructRules;
	
	private Resource seedModel;

	public SERGeUMLGenerator() {
		//super(Resource.class);

		iterationCount = 0;

		// Disable logging
		// LogManager.getLogManager().reset();

		this.resourceSet = new ResourceSetImpl();
		UMLResourcesUtil.init(this.resourceSet);

		// Might be redundant
		this.resourceSet.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		this.resourceSet.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);

		this.copyResources = new ResourceSetImpl();
		UMLResourcesUtil.init(this.copyResources);
		this.copyResources.getPackageRegistry().put(UMLPackage.eNS_URI, UMLPackage.eINSTANCE);
		this.copyResources.getResourceFactoryRegistry().getExtensionToFactoryMap().put(UMLResource.FILE_EXTENSION, UMLResource.Factory.INSTANCE);
		
		// Initialize model
		UMLPackage.eINSTANCE.eClass();
		EcoreUtil.resolveAll(UMLPackage.eINSTANCE);

		// Create and init Edit Rule Wrapper
		erWrapper = new EditRuleWrapper("basic"); // cpeo or basic ?

		this.appliedByKind = new HashMap<OperationKind, Set<Rule>>();
		appliedByKind.put(OperationKind.CREATE, new HashSet<Rule>());
		appliedByKind.put(OperationKind.ADD, new HashSet<Rule>());
		appliedByKind.put(OperationKind.SET_REFERENCE, new HashSet<Rule>());
		appliedByKind.put(OperationKind.MOVE, new HashSet<Rule>());
		appliedByKind.put(OperationKind.SET_ATTRIBUTE, new HashSet<Rule>());
		appliedByKind.put(OperationKind.UNSET_REFERENCE, new HashSet<Rule>());
		appliedByKind.put(OperationKind.REMOVE, new HashSet<Rule>());
		appliedByKind.put(OperationKind.DELETE, new HashSet<Rule>());

		this.appliedConstructRules = new HashSet<Rule>();
	}

	// This method is invoked to generate a single test case
	/*
	@Override
	public Resource generate(SourceOfRandomness random, GenerationStatus __ignore__) {
		if (getConstructCoverage() < 0.5) {
			return generatePhase1(random, __ignore__);
		} else {
			return generatePhase2(random, __ignore__);
		}
	}
	*/
	public Resource getSeedModel(SourceOfRandomness random) {
		this.appliedConstructRules = new HashSet<Rule>();
		seedModel = generateSeedModel(random);
		return this.seedModel;
	}

	//public Resource generatePhase1(SourceOfRandomness random, GenerationStatus __ignore__) {
	private Resource generateSeedModel(SourceOfRandomness random) {
		/*
		System.out.println("----");
		System.out.println("Phase 1, Iteration: " + iterationCount);
		System.out.println("Construct Coverage: " + getConstructCoverage());
		System.out.println("CREATE Coverage: " + getCoverageByKind(OperationKind.CREATE));
		System.out.println("ADD Coverage: " + getCoverageByKind(OperationKind.ADD));
		System.out.println("SET_REFERENCE Coverage: " + getCoverageByKind(OperationKind.SET_REFERENCE));
		System.out.println("----");
		*/
		System.out.println("==> Generating seed model");
		EList<Resource> resources = this.resourceSet.getResources();
		Resource modelResource = null;
		String resourcePath = (new File("").getAbsolutePath() + "/models/uml_model_" + String.format("%04d", iterationCount) + ".uml");
		if (resources.isEmpty()) {
			modelResource = resourceSet.createResource(URI.createFileURI(resourcePath));
			modelResource.getContents().add(UMLFactory.eINSTANCE.createModel());
		} else {
			assert (resources.size() == 1);
			modelResource = resources.get(0);
			modelResource.setURI(URI.createFileURI(resourcePath));
		}

		// Get most suitable rules to increase rule coverage
		Set<Rule> allConstructRules = new HashSet<Rule>(erWrapper.getRulesByKind(OperationKind.CREATE));
		allConstructRules.removeAll(appliedConstructRules);

		// Apply 10 random rules
		for(int appliedRules = 0; appliedRules < 10; appliedRules++) {
			Rule rule = (Rule) RandomUtils.randomlyGetFromSet(allConstructRules);
			//System.out.println("Try " + rule.getModule().getName());
			boolean success = erWrapper.applyRule(modelResource, rule, 1);
			if (success) {
				appliedConstructRules.add(rule);
				allConstructRules.remove(rule);
				appliedByKind.get(erWrapper.getOperationKind(rule)).add(rule);
			}
		}
		
		//System.out.println(success);

		// try {
		// modelResource.save(null);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }

		iterationCount++;

		return modelResource;
	}

	//public Resource generatePhase2(SourceOfRandomness random, GenerationStatus __ignore__) {
	public Resource mutateModel(SourceOfRandomness random, Resource modelResource, int numMutations) {
		// TODO: Mutations exploiting coverage from grey box fuzzing
		/*
		System.out.println("----");
		System.out.println("Phase 2, Iteration: " + iterationCount);
		System.out.println("CREATE Coverage: " + getCoverageByKind(OperationKind.CREATE));
		System.out.println("ADD Coverage: " + getCoverageByKind(OperationKind.ADD));
		System.out.println("SET_REFERENCE Coverage: " + getCoverageByKind(OperationKind.SET_REFERENCE));
		System.out.println("SET_ATTRIBUTE Coverage: " + getCoverageByKind(OperationKind.SET_ATTRIBUTE));
		System.out.println("MOVE Coverage: " + getCoverageByKind(OperationKind.MOVE));
		System.out.println("UNSET_REFERENCE Coverage: " + getCoverageByKind(OperationKind.UNSET_REFERENCE));
		System.out.println("REMOVE Coverage: " + getCoverageByKind(OperationKind.REMOVE));
		System.out.println("DELETE Coverage: " + getCoverageByKind(OperationKind.DELETE));
		System.out.println("----");
		*/
		// Just to showcase, some mutations, select randomly from all operation kinds?
		/*
		EList<Resource> resources = this.resourceSet.getResources();
		Resource modelResource = null;
		String resourcePath = (new File("").getAbsolutePath() + "/models/uml_model_" + String.format("%04d", iterationCount) + ".uml");
		if (resources.isEmpty()) {
			modelResource = resourceSet.createResource(URI.createFileURI(resourcePath));
			modelResource.getContents().add(UMLFactory.eINSTANCE.createModel());
		} else {
			assert (resources.size() == 1);
			modelResource = resources.get(0);
			modelResource.setURI(URI.createFileURI(resourcePath));
		}
		*/

		// Select and apply rule
		for(int i = 0; i < numMutations; i++) {
			Rule rule = (Rule) RandomUtils.randomlyGetFromSet(erWrapper.getAllRules());
			//System.out.println("Try " + rule.getModule().getName());
			boolean success = erWrapper.applyRule(modelResource, rule, 1);
			if (success) {
				appliedConstructRules.add(rule);
				appliedByKind.get(erWrapper.getOperationKind(rule)).add(rule);
			}
			else {
				i--;
			}
		}
		
		//iterationCount++;
		
		return modelResource;
	}
	
	private int mutantId = 0;
	public Resource copyModel(Resource originalModel) {
		String resourcePath = (new File("").getAbsolutePath() + "/models/uml_model_mutant" + String.format("%04d", mutantId++) + ".uml");
		Resource modelResource = copyResources.createResource(URI.createFileURI(resourcePath));
		EObject model = originalModel.getContents().get(0);
		modelResource.getContents().add(EcoreUtil.copy(model));
		return modelResource;
	}

	public double getConstructCoverage() {
		return (double) appliedConstructRules.size() / (double) erWrapper.getAllConstructRules().size();
	}

	public double getCoverageByKind(OperationKind kind) {
		return (double) appliedByKind.get(kind).size() / (double) erWrapper.getRulesByKind(kind).size();
	}

	public static void main(String[] args) {
		System.out.println("==> start");

		SERGeUMLGenerator generator = new SERGeUMLGenerator();
		for (int i = 0; i < 10000; i++) {
			//generator.generate(null, null);
		}

		System.out.println("==> finished");
	}
}
