package de.hub.mse.emf.generator;

import static com.google.common.collect.Iterables.get;
import static com.google.common.primitives.Primitives.isWrapperType;
import static com.google.common.primitives.Primitives.unwrap;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.DeleteCommand;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.uml2.uml.UMLPackage;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ListMultimap;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.generator.cgf.MetamodelCoverage;
import de.hub.mse.emf.generator.internal.EClassReferencePair;
import de.hub.mse.emf.generator.internal.MetamodelUtil;
import de.hub.mse.emf.generator.internal.ModelGenerationStats;

/**
 * A generic instance model generator.
 * 
 * @author Lam
 *
 */
public class ModelGenerator {

	public final static Logger LOGGER = Logger.getLogger(ModelGenerator.class.getName());

	/**
	 * The model configuration parameters, such as target object count, reference
	 * counts, etc.
	 */
	protected final IModelGenerationConfig config;

	/**
	 * The metamodel data, e.g. classes, containment references, cross references,
	 * supertypes, etc.
	 */
	protected final MetamodelUtil metamodelUtil;

	/**
	 * The single-run configuration parameters for the generation of one instance
	 * model.
	 */
	protected int currentDepth;
	protected int currentMaxDepth;
	protected int currentObjectCount;
	protected int goalObjectCount;
	protected int maxBreadth = -1;

	/**
	 * The containment EReferences covered so far (for the current model). Used to
	 * prioritize uncovered references.
	 */
	protected HashSet<EReference> coveredEReferences;

	/**
	 * The classes covered so far (for the current model). Used to prioritize
	 * uncovered classes.
	 */
	protected HashSet<EClass> coveredEClasses;

	/** The metamodel coverage */
	MetamodelCoverage metamodelCoverage;

	/** The random source used to generate an instance model. */
	//protected SourceOfRandomness randomGenerator = new SourceOfRandomness(new Random(12));
	protected SourceOfRandomness randomGenerator = null;

	/**
	 * Caching of well-formedness constraints (valid EClasses for EReferences) to
	 * avoid exceptions during runtime.
	 */
	public static boolean useValidityCache = true;
	
	public static boolean trackMetamodelCoverage = true;
	
	/**
	 * Instantiate a generic model generator.
	 * 
	 * @param config
	 */
	public ModelGenerator(IModelGenerationConfig config) {
		this.config = config;
		this.metamodelUtil = new MetamodelUtil(config.ePackages(), config.ignoredEClasses(),
				config.getEClassWhitelist());
		this.metamodelCoverage = new MetamodelCoverage(metamodelUtil.getAllEClasses(),
				metamodelUtil.getAllEContainmentRefs());
	}

	/**
	 * This constructor is used when the model generator is used together with a
	 * model mutator to share information.
	 * 
	 * @param config         the model generation configuration parameters
	 * @param metamodelData  the metamodel data
	 * @param eRefCoverage   the set of covered {@link EReferences}
	 * @param eClassCoverage the set of covered {@link EClasses}
	 */
	public ModelGenerator(IModelGenerationConfig config, MetamodelUtil metamodelUtil,
			MetamodelCoverage metamodelCoverage) {
		this.config = config;
		this.metamodelUtil = metamodelUtil;
		this.metamodelCoverage = metamodelCoverage;
		// LOGGER.setLevel(Level.FINE);
	}

	/**
	 * Generates one instance model according to the model generation configuration.
	 * 
	 * @param resource  the resource where the generated model should be added
	 * @param random    the source of randomness driving the generation process
	 * @param genStatus can be used to share information of the model generation
	 *                  process/history
	 */
	public void generate(Resource resource, SourceOfRandomness random, GenerationStatus genStatus) {

		resource.setModified(true);
		this.randomGenerator = random;

		// Map EClass to generated EObjects to generate cross-references later
		ListMultimap<EClass, EObject> indexByKind = ArrayListMultimap.create();

		// Reset generation stats
		currentDepth = 0;
		currentMaxDepth = 0;
		currentObjectCount = 0;
		coveredEReferences = new HashSet<EReference>();
		coveredEClasses = new HashSet<EClass>();

		ImmutableSet<EClass> possibleRootEClasses = config.possibleRootEClasses();

		// Create single-root model TODO: multi-root model
		boolean success = false;
		int trial = 0;
		while (!success) {
			if (trial++ > 10) {
				LOGGER.severe("Unable to generate complete model after 10 trials.");
				return;
			}

			// Sample parameters for the next model to be generated
			EClass root = config.getNextRootEClass(possibleRootEClasses, random);
			currentMaxDepth = config.getDepthFor(root, random) + 1; // min. depth = 1
			goalObjectCount = config.getTotalObjectCount(random);

			// Check presence of generated model, if present then generation was successful
			Optional<EObject> generatedEObject = generateEObject(root, indexByKind, random);
			if (generatedEObject.isPresent()) {
				resource.getContents().add(generatedEObject.get());
				success = true;
			}
		}

		// Generate cross-references
		LOGGER.fine("Generating cross-references");

		/*
		 * Instead of using an iterator, we have to add all elements to a temporary list
		 * since adding cross-references might result in changes in the containment
		 * tree.
		 */
		List<EObject> allEObjects = new ArrayList<EObject>();
		for (TreeIterator<EObject> it = resource.getAllContents(); it.hasNext();) {
			allEObjects.add(it.next());
		}

		for (EObject eobj : allEObjects) {
			generateCrossReferences(eobj, indexByKind, random);
		}
		
		// Update coverage
		if(trackMetamodelCoverage) {
			for(EClass eClass : coveredEClasses) {
				metamodelCoverage.addCoveredEClass(eClass);
			}
			
			for(EReference eReference : coveredEReferences) {
				metamodelCoverage.addCoveredEContainmentRef(eReference);
			}
		}
		
		// Clean up
		// indexByKind.clear();

		LOGGER.fine(MessageFormat.format("Generation finished for resource ''{0}''", resource.getURI()));
	}

	public EObject generateSubModel(Random random, EClass eClass, int depth, int breadth, int maxObjectCount, Set<EClass> containedEClasses) {
		this.randomGenerator = new SourceOfRandomness(random);
		this.coveredEReferences = new HashSet<EReference>();
		this.coveredEClasses = new HashSet<EClass>();
		
		// Set generation params
		currentDepth = 0;
		currentMaxDepth = depth;
		
		currentObjectCount = 0;
		goalObjectCount = maxObjectCount;
		
		maxBreadth = breadth;
		
		// The generation process will populate the coveredEReferences and coveredEClasses sets
		Optional<EObject> generatedEObject = generateEObject(eClass, null, randomGenerator);
		maxBreadth = -1;
		
		if (generatedEObject.isPresent()) {
			containedEClasses.addAll(coveredEClasses);
			return generatedEObject.get();
		}
		
		return null;
	}

	/**
	 * Attempts to generate an {@link EObject} of a given {@link EClass} by 1)
	 * instantiation of the {@link EClass}, 2) randomly setting the attributes, and
	 * 3) randomly generating the contained {@link EClass}es by recursively calling
	 * this method.
	 * 
	 * @param eClass      the {@link EClass} to instantiate
	 * @param indexByKind the map mapping the instantiated {@link EClass}es so far
	 *                    to the concrete objects in the model
	 * @param random      the random source driving the generation process
	 * @return
	 */
	protected Optional<EObject> generateEObject(EClass eClass, ListMultimap<EClass, EObject> indexByKind,
			SourceOfRandomness random) {
		final EObject eObject;
		currentObjectCount++;
		LOGGER.fine(MessageFormat.format("Generating EObject {0} / ~{1} (EClass={2})", currentObjectCount,
				goalObjectCount, eClass.getName()));
		eObject = createEObject(eClass, indexByKind);
		generateEAttributes(eObject, eClass, random);
		generateEContainmentReferences(eObject, eClass, indexByKind, random);
		return Optional.fromNullable(eObject);
	}

	/**
	 * Creates an instance of the given {@link EClass} and adds it to the instance
	 * map
	 * 
	 * @param eClass      the {@link EClass} to instantiate
	 * @param indexByKind the instance map of the model
	 * @return
	 */
	protected EObject createEObject(EClass eClass, ListMultimap<EClass, EObject> indexByKind) {
		EObject eObject = eClass.getEPackage().getEFactoryInstance().create(eClass);

		if (indexByKind != null) {
			indexByKind.put(eClass, eObject);
			for (EClass eSuperType : eClass.getEAllSuperTypes()) {
				indexByKind.put(eSuperType, eObject);
			}
		}

		return eObject;
	}

	/**
	 * Randomly sets all attributes for the given {@link EObject}
	 * 
	 * @param eObject the {@link EObject} where the attributes are set
	 * @param eClass  the respective {@link EClass}
	 * @param random  the random source
	 */
	protected void generateEAttributes(EObject eObject, EClass eClass, SourceOfRandomness random) {
		for (EAttribute eAttribute : metamodelUtil.eAllAttributes(eClass)) {
			// TODO: do not generate all attributes? set some attributes null?
			generateAttributes(eObject, eAttribute, random);
		}
	}

	protected void generateAttributes(EObject eObject, EAttribute eAttribute, SourceOfRandomness random) {
		EDataType eAttributeType = eAttribute.getEAttributeType();
		Class<?> instanceClass = eAttributeType.getInstanceClass();
		if (eAttribute.isMany()) {
			generateManyAttribute(eObject, eAttribute, instanceClass, random);
		} else {
			generateSingleAttribute(eObject, eAttribute, instanceClass, random);
		}
	}

	/**
	 * Generates a random number of containment {@link EReference}s for a given
	 * {@link EObject} in the model. Mandatory and previously uncovered references
	 * are prioritized.
	 * 
	 * @param eObject     the parent object
	 * @param eClass      the class of the parent object
	 * @param indexByKind the instance map
	 * @param random      the source of randomness
	 */
	protected void generateEContainmentReferences(EObject eObject, EClass eClass,
			ListMultimap<EClass, EObject> indexByKind, SourceOfRandomness random) {

		// Determine all (and uncovered) containment eReferences
		List<EReference> allContainmentReferences = new ArrayList<EReference>();
		List<EReference> uncoveredContainmentReferences = new ArrayList<EReference>();

		for (EReference eReference : metamodelUtil.eAllContainment(eClass)) {

			// Generate mandatory references right away
			if (eReference.isRequired()) {
				generateEContainmentReference(eObject, eReference, indexByKind, random);
				continue;
			} else if (metamodelUtil.useWhitelist()
					&& !metamodelUtil.isWhitelistedOrSuperType(eReference.getEReferenceType())) {
				continue;
			}

			// Keep track of references not covered yet, these will be prioritized
			// if(!this.coveredEReferences.contains(eReference)) {
			if (!metamodelCoverage.isCovered(eReference) || !coveredEReferences.contains(eReference)) {
				uncoveredContainmentReferences.add(eReference);
			}

			allContainmentReferences.add(eReference);
		}

		// Nothing to generate
		if (allContainmentReferences.size() == 0) {
			return;
		}

		// Sample the number of children (containment refs) to be generated (min. 1)
		// TODO: possibly allow more refs to be generated when there are still uncovered
		// containment refs
		int breadth = maxBreadth != -1 ? maxBreadth : config.getBreadthFor(eClass, random) + 1;

		for (int i = 0; i < breadth; i++) {
			if ((currentObjectCount < goalObjectCount && currentDepth <= currentMaxDepth)) {

				// If there are any uncovered containment references, generate one of them first
				if (!uncoveredContainmentReferences.isEmpty()) {
					int idx = random.nextInt(uncoveredContainmentReferences.size());
					EReference eReference = uncoveredContainmentReferences.get(idx);
					generateEContainmentReference(eObject, eReference, indexByKind, random);
				} else {
					int idx = random.nextInt(allContainmentReferences.size());
					EReference eReference = allContainmentReferences.get(idx);
					if(!eReference.isMany() && eObject.eIsSet(eReference)) {
						continue;
					}
					generateEContainmentReference(eObject, eReference, indexByKind, random);
				}
			}
		}

	}

	protected void generateEContainmentReference(EObject eObject, EReference eReference,
			ListMultimap<EClass, EObject> indexByKind, SourceOfRandomness random) {

		currentDepth++;

		ImmutableList<EClass> eAllConcreteSubTypeOrSelf;
		if (useValidityCache) {
			EClassReferencePair eClassRefPair = new EClassReferencePair(eObject.eClass(), eReference);
			eAllConcreteSubTypeOrSelf = metamodelUtil.eReferenceValidEClasses(eClassRefPair);
		} else {
			eAllConcreteSubTypeOrSelf = metamodelUtil.eAllConcreteSubTypeOrSelf(eReference);
		}

		if (!eAllConcreteSubTypeOrSelf.isEmpty()) {
			if (eReference.isMany()) {
				generateManyContainmentReference(eObject, eReference, indexByKind, eAllConcreteSubTypeOrSelf, random);
			} else {
				generateSingleContainmentReference(eObject, eReference, indexByKind, eAllConcreteSubTypeOrSelf, random);
			}
		}

		currentDepth--;
	}

	protected void generateSingleContainmentReference(EObject eObject, EReference eReference,
			ListMultimap<EClass, EObject> indexByKind, ImmutableList<EClass> eAllConcreteSubTypesOrSelf,
			SourceOfRandomness random) {

		// Check if there are any uncovered EClasses
		List<EClass> uncoveredEClasses = new ArrayList<EClass>();
		for (EClass eClass : eAllConcreteSubTypesOrSelf) {
			if (!metamodelCoverage.isCovered(eClass)) {
				uncoveredEClasses.add(eClass);
			}
		}

		// Randomly skip generation of the reference, but only if it has already been
		// covered before and all EClasses have been covered
		if (!eReference.isRequired() && coveredEReferences.contains(eReference) && uncoveredEClasses.isEmpty()
				&& random.nextBoolean()) {
			return;
		}

		LOGGER.fine(MessageFormat.format("Generating EReference ''{0}'' in EObject {1}", eReference.getName(),
				eObject.toString()));
		ModelGenerationStats.singleContainmentRefCount++;

		try {
			// If there are any uncovered EClasses, prioritize them
			EClass nextEClass;
			if (!uncoveredEClasses.isEmpty()) {
				int idx = random.nextInt(uncoveredEClasses.size());
				nextEClass = get(uncoveredEClasses, idx);
			} else {
				int idx = random.nextInt(eAllConcreteSubTypesOrSelf.size());
				nextEClass = get(eAllConcreteSubTypesOrSelf, idx);
			}

			final Optional<EObject> nextEObject = generateEObject(nextEClass, indexByKind, random);

			if (nextEObject.isPresent()) {
				eObject.eSet(eReference, nextEObject.get());
				ModelGenerationStats.singleContainmentRefSuccess++;

				// Add to set of covered references
				//coveredEClasses.add(nextEClass);
				//coveredEReferences.add(eReference);
				//metamodelCoverage.addCoveredEClass(nextEClass);
				//metamodelCoverage.addCoveredEContainmentRef(eReference);
			} else {
				ModelGenerationStats.singleContainmentRefFail++;
			}
			// Success
			return;

		} catch (IllegalArgumentException e) {
			/*
			 * Cause: Actual implementation only allows to set instances of specific
			 * subclasses of the reference type
			 */

			// Should not happen anymore, just in case
			LOGGER.severe(e.getStackTrace().toString());

			ModelGenerationStats.illegalArgumentExceptionCount++;
		}
		ModelGenerationStats.singleContainmentRefFail++;
		LOGGER.warning(
				MessageFormat.format("Unable to generate single containment reference ''{0}'' for ''{1}'' object",
						eReference.getName(), eObject.eClass().getName()));
		return;
	}
	
	private boolean isContained(EObject eObject) {
		HashSet<EObject> visited = new HashSet<EObject>();
		EObject current = eObject;
		while(current.eContainer() != null) {
			visited.add(current);
			current = current.eContainer();
			if(visited.contains(current)) {
				return false;
			}
		}
		if(current.eClass().equals(UMLPackage.Literals.MODEL)) {
			return true;
		}
		else {
			return false;
		}
	}

	protected void generateManyContainmentReference(EObject eObject, EReference eReference,
			ListMultimap<EClass, EObject> indexByKind, ImmutableList<EClass> eAllConcreteSubTypesOrSelf,
			SourceOfRandomness random) {
		@SuppressWarnings("unchecked")

		List<EObject> values = (List<EObject>) eObject.eGet(eReference);		
		int childCount = maxBreadth != -1 ? maxBreadth : config.getElementCountFor(eReference, random);
		
		LOGGER.fine(MessageFormat.format("Generating {0} values for EReference ''{1}'' in EObject {2}", childCount,
				eReference.getName(), eObject.toString()));

		// Check if there are any uncovered EClasses
		List<EClass> uncoveredEClasses = new ArrayList<EClass>();
		for (EClass eClass : eAllConcreteSubTypesOrSelf) {
			if (!metamodelCoverage.isCovered(eClass)) {
				uncoveredEClasses.add(eClass);
			}
		}

		// Try to cover at least one new uncovered EClass
		childCount = Math.max(Math.min(uncoveredEClasses.size(), 1), childCount);

		for (int i = 0; i < childCount; i++) {
			ModelGenerationStats.manyContainmentRefCount++;

			// If there are any uncovered EClasses, prioritize them
			EClass nextEClass;
			if (!uncoveredEClasses.isEmpty()) {
				int idx = randomGenerator.nextInt(uncoveredEClasses.size());
				nextEClass = get(uncoveredEClasses, idx);
			} else {
				int idx = randomGenerator.nextInt(eAllConcreteSubTypesOrSelf.size());
				nextEClass = get(eAllConcreteSubTypesOrSelf, idx);
			}

			final Optional<EObject> nextEObject = generateEObject(nextEClass, indexByKind, random);

			if (nextEObject.isPresent()) {
				try {
					values.add(nextEObject.get());
					ModelGenerationStats.manyContainmentRefSuccess++;

					// Add to set of covered references/classes
					coveredEClasses.add(nextEClass);
					coveredEReferences.add(eReference);
					//metamodelCoverage.addCoveredEClass(nextEClass);
					//metamodelCoverage.addCoveredEContainmentRef(eReference);
					// Remove from uncovered EClasses for next iterations
					uncoveredEClasses.remove(nextEClass);
				} catch (ArrayStoreException e) {
					/*
					 * Cause: Actual list type is only a subclass of the reference type, trying to
					 * store a different subclass (e.g. retrieved from the indexByKind map) will
					 * result in the exception TODO: handle/store free eobjects Note: This happens
					 * very unfrequently for the UML metamodel (0,008%)
					 */
					// Should not happen anymore, just in case
					LOGGER.severe(e.getStackTrace().toString());
					ModelGenerationStats.arrayStoreExceptionCount++;
				}
			} else {
				ModelGenerationStats.manyContainmentRefFail++;
			}
		}
	}

	/**
	 * Generates the cross {@link EReference} for an object in the model. References
	 * are generated by iterating through the available references for the class of
	 * the object and searching for suitable objects (w.r.t the reference type) in
	 * the model.
	 * 
	 * @param eObject     the object for which the cross references are generated
	 * @param indexByKind the instance map of the model mapping {@link EClass}es to
	 *                    objects in the model for lookup
	 * @param random      the source of randomness
	 */
	protected void generateCrossReferences(EObject eObject, ListMultimap<EClass, EObject> indexByKind,
			SourceOfRandomness random) {
		Iterable<EReference> eAllNonContainment = metamodelUtil.eAllNonContainment(eObject.eClass());
		for (EReference eReference : eAllNonContainment) {

			EClass eReferenceType = eReference.getEReferenceType();
			List<EObject> allValues = indexByKind.get(eReferenceType);
			List<EObject> availableValues = new LinkedList<EObject>();

			// Add only valid values
			if (useValidityCache) {
				ImmutableList<EClass> validEClasses = metamodelUtil
						.eReferenceValidEClasses(new EClassReferencePair(eObject.eClass(), eReference));
				for (EObject e : allValues) {
					if (validEClasses.contains(e.eClass()) && (isContained(e))) {
						availableValues.add(e);
					}
				}
				availableValues.remove(eObject); // Forbid self-referencing
			} else {
				availableValues.addAll(allValues);
				availableValues.remove(eObject); // Forbid self-referencing
			}

			if (availableValues.isEmpty()) {
				continue;
			}

			if (eReference.isMany()) {
				@SuppressWarnings("unchecked")
				List<Object> values = (List<Object>) eObject.eGet(eReference);
				int elementCount = config.getElementCountFor(eReference, random);
				LOGGER.fine(MessageFormat.format("Generating {0} values for EReference ''{1}'' in EObject {2}",
						elementCount, eReference.getName(), eObject.toString()));

				//elementLoop: 
				for (int i = 0; i < elementCount; i++) {
					ModelGenerationStats.manyCrossRefCount++;
					if (!availableValues.isEmpty()) {
						int idx = randomGenerator.nextInt(availableValues.size());
						final EObject nextEObject = availableValues.get(idx);
						try {
							values.add(nextEObject);
							availableValues.remove(idx); // Forbid duplicate referencing
							ModelGenerationStats.manyCrossRefSuccess++;
							// metamodelCoverage.addCoveredEReference(eReference);
							//break; // success, continue with next element
						} catch (ArrayStoreException e) {
							/*
							 * Cause: Actual list type is only a subclass of the reference type, trying to
							 * store a different subclass (e.g. retrieved from the indexByKind map) will
							 * result in the exception
							 */
							// e.printStackTrace(); // Should not happen anymore, just in case
							ModelGenerationStats.manyCrossRefFail++;
							ModelGenerationStats.arrayStoreExceptionCount++;
							availableValues.remove(idx);
						} catch (IllegalStateException e) {
							// Cause: Cross-references can change the containment tree structure, possibly
							// resulting in cycles
							// This can still happen even with precomputing the possible eclasses
							ModelGenerationStats.manyCrossRefFail++;
							ModelGenerationStats.illegalStateExceptionCount++;
							availableValues.remove(idx);
							// TODO: throw new IllegalStateException (cancel generation?)
						}
					} else {
						//break elementLoop; // no more suitable elements left, continue with next reference
					}
				}
			} else {
				if (eReference.isRequired() || randomGenerator.nextBoolean()) {
					LOGGER.fine(MessageFormat.format("Generating EReference ''{0}'' in EObject {1}",
							eReference.getName(), eObject.toString()));

					ModelGenerationStats.singleCrossRefCount++;
					if (!availableValues.isEmpty()) {
						int idx = randomGenerator.nextInt(availableValues.size());
						final EObject nextEObject = availableValues.get(idx);
						try {
							eObject.eSet(eReference, nextEObject);
							ModelGenerationStats.singleCrossRefSuccess++;
							// metamodelCoverage.addCoveredEReference(eReference);
							break; // Continue with next reference
						} catch (IllegalArgumentException e) {
							/*
							 * Cause: Actual implementation only allows to set instances of specific
							 * subclasses of the reference type
							 */
							e.printStackTrace(); // Should not happen...
							ModelGenerationStats.singleCrossRefFail++;
							ModelGenerationStats.illegalArgumentExceptionCount++;
							availableValues.remove(idx);
						} catch (IllegalStateException e) {
							/*
							 * Cause: Some cross references change the structure of the containment-tree,
							 * which can possibly result in a cycle
							 */
							ModelGenerationStats.singleCrossRefFail++;
							ModelGenerationStats.illegalStateExceptionCount++;
							availableValues.remove(idx);
							// TODO: throw new IllegalStateException (cancel generation?)

						}
					} else {
						break;
					}
				}
			}
		}
	}

	protected void generateSingleAttribute(EObject eObject, EAttribute eAttribute, Class<?> instanceClass,
			SourceOfRandomness random) {
		if (eAttribute.isRequired() || randomGenerator.nextBoolean()) {
			final Object value;
			EDataType eAttributeType = eAttribute.getEAttributeType();
			if (eAttributeType instanceof EEnum) {
				EEnum eEnum = (EEnum) eAttributeType;
				int size = eEnum.getELiterals().size();
				if (instanceClass == null) {
					// Initial implementation TODO: review
					instanceClass = int.class;
					int randomValue = Math.abs((Integer) nextValue(instanceClass, random));
					value = eEnum.getELiterals().get(randomValue % size);
				} else {
					int idx = random.nextInt(size);
					value = eEnum.getELiterals().get(idx).getInstance();
				}
			} else {
				value = nextValue(instanceClass, random);
			}
			eObject.eSet(eAttribute, value);
		}
	}

	protected void generateManyAttribute(EObject eObject, EAttribute eAttribute, Class<?> instanceClass,
			SourceOfRandomness random) {
		@SuppressWarnings("unchecked")
		List<Object> values = (List<Object>) eObject.eGet(eAttribute);
		for (int i = 0; i < config.getElementCountFor(eAttribute, random); i++) {
			final Object value;
			EDataType eAttributeType = eAttribute.getEAttributeType();
			if (eAttributeType instanceof EEnum) {
				EEnum eEnum = (EEnum) eAttributeType;
				int size = eEnum.getELiterals().size();
				if (instanceClass == null) {
					instanceClass = int.class;
					int randomValue = Math.abs((Integer) nextValue(instanceClass, random));
					value = eEnum.getELiterals().get(randomValue % size);
				} else {
					int idx = random.nextInt(size);
					value = eEnum.getELiterals().get(idx).getInstance();
				}
			} else {
				value = nextValue(instanceClass, random);
			}
			values.add(value);
		}
	}

	protected Object nextValue(Class<?> instanceClass, SourceOfRandomness random) {
		final Object value;
		if (instanceClass.isPrimitive() || isWrapperType(instanceClass)) {
			value = nextPrimitive(unwrap(instanceClass));
		} else {
			value = nextObject(instanceClass, random);
		}
		return value;
	}

	protected Object nextObject(Class<?> instanceClass, SourceOfRandomness random) {
		if (instanceClass == String.class) {
			return RandomStringUtils.random(config.getValueSizeFor(instanceClass, random), 0, 0, true, true, null,
					randomGenerator.toJDKRandom());
		} else {
			LOGGER.warning(MessageFormat.format("Do not know how to randomly generate ''{0}'' object",
					instanceClass.getName()));
		}
		return null;
	}

	/**
	 * @param eObject
	 * @param eAttribute
	 * @param instanceClass
	 */
	protected Object nextPrimitive(Class<?> instanceClass) {
		if (instanceClass == boolean.class) {
			return randomGenerator.nextBoolean();
		} else if (instanceClass == byte.class) {
			byte[] buff = new byte[1];
			randomGenerator.nextBytes(buff);
			return buff[0];
		} else if (instanceClass == char.class) {
			char nextChar = (char) randomGenerator.nextInt();
			return nextChar;
		} else if (instanceClass == double.class) {
			return randomGenerator.nextDouble();
		} else if (instanceClass == float.class) {
			return randomGenerator.nextFloat();
		} else if (instanceClass == int.class) {
			return randomGenerator.nextInt();
		} else if (instanceClass == long.class) {
			return randomGenerator.nextLong();
		} else if (instanceClass == short.class) {
			short nextShort = (short) randomGenerator.nextInt();
			return nextShort;
		} else {
			throw new IllegalArgumentException();
		}
	}

	protected ImmutableMultiset<EClass> getEReferenceTypesWithWeight(EReference eReference,
			ImmutableList<EClass> eAllSubTypesOrSelf) {
		ImmutableMultiset.Builder<EClass> eAllSubTypesOrSelfWithWeights = ImmutableMultiset.builder();
		for (EClass eClass : eAllSubTypesOrSelf) {
			eAllSubTypesOrSelfWithWeights.addCopies(eClass, config.getWeightFor(eReference, eClass));
		}
		return eAllSubTypesOrSelfWithWeights.build();
	}
}
