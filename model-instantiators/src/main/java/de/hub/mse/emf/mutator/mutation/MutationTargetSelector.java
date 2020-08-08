package de.hub.mse.emf.mutator.mutation;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Random;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.resource.Resource;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;

import de.hub.mse.emf.generator.cgf.MetamodelCoverage;
import de.hub.mse.emf.generator.internal.EClassReferencePair;
import de.hub.mse.emf.generator.internal.MetamodelUtil;

/**
 * The MutationTargetSelector is responsible for selecting suitable targets in
 * the model to mutate. This includes selecting source objects in the model as
 * well as possibly target locations (i.e. for move mutations).
 * 
 * @author Lam
 *
 */
public class MutationTargetSelector {

	private MetamodelUtil metamodelUtil;

	private Random random;

	private Resource modelResource;
	
	private ListMultimap<EClass, EObject> objectIndex;
	
	private boolean indexValid;

	private MetamodelCoverage metamodelCoverage;

	public MutationTargetSelector(Random random, Resource modelResource, MetamodelUtil metamodelUtil,
			MetamodelCoverage metamodelCoverage) {
		this.modelResource = modelResource;
		this.metamodelUtil = metamodelUtil;
		this.metamodelCoverage = metamodelCoverage;
		this.random = random;
		this.indexValid = false;
	}

	/**
	 * Sets the resource containing the model from which the targets should be
	 * selected from.
	 * 
	 * @param modelResource the resource containing the model
	 */
	public void setModelResource(Resource modelResource) {
		this.modelResource = modelResource;
	}
	
	public List<EObject> getAllObjects(){
		List<EObject> allEObjects = new ArrayList<EObject>();
		for(TreeIterator<EObject> it = modelResource.getAllContents(); it.hasNext();) {
			allEObjects.add(it.next());
		}
		return allEObjects;
	}

	/**
	 * Returns the {@link MetamodelUtil} instance
	 * 
	 * @return the {@link MetamodelUtil} instance
	 */
	public MetamodelUtil getUtil() {
		return this.metamodelUtil;
	}

	/**
	 * Returns the source of randomness
	 * 
	 * @return random generator
	 */
	public Random getRandom() {
		return this.random;
	}

	/**
	 * Returns the metamodel coverage isntance
	 * 
	 * @return the metamodel coverage instance
	 */
	public MetamodelCoverage getMetamodelCoverage() {
		return this.metamodelCoverage;
	}
	
	public ListMultimap<EClass, EObject> getObjectIndex(){
		if(!indexValid) {
			objectIndex = ArrayListMultimap.create();
			for(TreeIterator<EObject> it = modelResource.getAllContents(); it.hasNext();) {
				EObject eObject = it.next();
				EClass eClass = eObject.eClass();
				objectIndex.put(eClass, eObject);
				for (EClass eSuperType : eClass.getEAllSuperTypes()) {
					objectIndex.put(eSuperType, eObject);
				}
			}
			//indexValid = true; FIXME
		}
		return objectIndex;
	}
	
	public void invalidateIndex() {
		indexValid = false;
	}

	/**
	 * Select a random object from the whole model. Implements the reservoir
	 * sampling algorithm for space efficiency.
	 * 
	 * TODO Cache model sizes to draw a random number and get the object in a single
	 * pass.
	 * 
	 * @param modelResource
	 * @return the selected object
	 */
	public EObject selectRandomEObject() {
		TreeIterator<EObject> it = modelResource.getAllContents();
		EObject result = it.next();

		for (int i = 1; it.hasNext(); i++) {

			// Sample with decreasing probability
			// If the sampled number is 0, replace value
			int j = random.nextInt(i + 1);
			if (j == 0) {
				result = it.next();
			} else {
				it.next();
			}
		}
		return result;
	}

	/**
	 * Select multiple random objects from the model by using reservoir sampling.
	 * 
	 * @param n the number of objects to select
	 * @return the array containing the selected objects
	 */
	public EObject[] selectEObjects(int n) {
		EObject[] selectedEObjects = new EObject[n];

		TreeIterator<EObject> it = modelResource.getAllContents();
		for (int i = 0; i < n; i++) {
			if (it.hasNext()) {
				selectedEObjects[i] = it.next();
			} else {
				// No more elements to choose from, select a random existing one
				selectedEObjects[i] = selectedEObjects[random.nextInt(i)];
			}
		}

		// No more elements
		if (!it.hasNext()) {
			return selectedEObjects;
		}

		for (int i = n; it.hasNext(); i++) {

			// Pick random number from 0 to i
			int j = random.nextInt(i + 1);

			// If the randomly selected index is smaller than n,
			// replace with next object
			if (j < n) {
				selectedEObjects[j] = it.next();
			} else {
				it.next();
			}
		}
		return selectedEObjects;
	}

	/**
	 * Selects n {@link EObject}s from the model based on a global
	 * Metaclass-to-count-map. In particular, it favors objects that have been
	 * observed less frequently over all models.
	 * 
	 * @param numObjects             the number of objects to draw
	 * @param eClassCountMap         a map mapping {@link EClass}es to frequency
	 *                               counts
	 * @param prioritizeHigherCounts whether to prioritize objects from classes that
	 *                               have higher frequency counts
	 * @return the list of selected objects
	 */
	public ArrayList<EObject> selectWeightedEObjects(int numObjects, boolean prioritizeHigherCounts) {
		// 1. Determine the EClasses that occur in the model
		// LinkedHashSet<EClass> eClassesInModel = new LinkedHashSet<EClass>();
		// TODO this should be precomputed
		LinkedHashMap<EClass, ArrayList<EObject>> classIndex = new LinkedHashMap<EClass, ArrayList<EObject>>();

		for (TreeIterator<EObject> it = modelResource.getAllContents(); it.hasNext();) {
			EObject curEObject = it.next();
			if (classIndex.containsKey(curEObject.eClass())) {
				classIndex.get(curEObject.eClass()).add(curEObject);
			} else {
				ArrayList<EObject> eObjectList = new ArrayList<EObject>();
				eObjectList.add(curEObject);
				classIndex.put(curEObject.eClass(), eObjectList);
			}
		}

		// 2. Get counts for each EClass and compute absolute frequency
		// as a representation of the discrete cumulative density function (CDF)
		double[] absoluteCounts = new double[classIndex.size()];
		double[] cumulativeCounts = new double[classIndex.size()];
		EClass[] eClasses = new EClass[classIndex.size()];

		int i = 0;
		double sum = 0;

		// Compute cumulative counts and store EClasses for fast access later
		// LinkedHashSet guarantees that the iteration order is the same as the
		// insertion order
		for (EClass eClass : classIndex.keySet()) {
			eClasses[i] = eClass;

			double count = metamodelCoverage.getEClassCount(eClass);

			// Prioritize lower counts by inverting them, assign 1 to 0-counts
			// TODO: Actually we should never encounter any 0-counts at all
			if (!prioritizeHigherCounts) {
				count = count != 0 ? 1 / count : 1;
			}

			absoluteCounts[i] = count;
			cumulativeCounts[i] = sum + count;
			sum += count;
			i++;
		}

		// 3. Now that we have our frequency distribution, draw objects randomly
		ArrayList<EObject> selectedEObjects = new ArrayList<EObject>();
		for (int j = 0; j < numObjects; j++) {
			// Draw random number and select EClass based on CDF
			// int x = random.nextInt(sum);
			double x = random.nextDouble() * sum;

			// To find our sampled EClass, we have to find the first index i
			// in our CDF where CDF[i] > x, since x lies in that interval
			// We do that with binary search
			int lower = 0;
			int upper = classIndex.size() - 1;

			int idx = 0;
			while (upper >= lower) {
				idx = (lower + upper) / 2;
				if (cumulativeCounts[idx] < x) {
					// Too low, search in upper half
					lower = idx + 1;
				} else if (cumulativeCounts[idx] - absoluteCounts[idx] > x) {
					// Too high, search in lower half
					upper = idx - 1;
				} else {
					break;
				}
			}

			ArrayList<EObject> eObjectsForClass = classIndex.get(eClasses[idx]);
			EObject selectedEObject = eObjectsForClass.get(random.nextInt(eObjectsForClass.size()));
			selectedEObjects.add(selectedEObject);
		}
		return selectedEObjects;
	}

	/**
	 * Selects one uncovered reference from the given list of references.
	 * 
	 * @param eReferences the selection of EReferences to choose from
	 * @return an uncovered reference if there exists one, otherwise a random one
	 */
	public EReference selectUncoveredReference(List<EReference> eReferences) {
		ArrayList<EReference> uncoveredEReferences = new ArrayList<EReference>();
		for (EReference ref : eReferences) {
			if (!metamodelCoverage.isCovered(ref)) {
				uncoveredEReferences.add(ref);
			}
		}

		// There exist uncovered references
		EReference result;
		if (!uncoveredEReferences.isEmpty()) {
			result = uncoveredEReferences.get(random.nextInt(uncoveredEReferences.size()));
			metamodelCoverage.addCoveredEContainmentRef(result);
		} else {
			result = eReferences.get(random.nextInt(eReferences.size()));
		}
		return result;
	}

	public EClass selectUncoveredReferenceType(EClass eClass, EReference eReference) {
		ArrayList<EClass> uncoveredEClasses = new ArrayList<EClass>();

		EClassReferencePair pair = new EClassReferencePair(eClass, eReference);
		ImmutableList<EClass> allEClasses = metamodelUtil.eReferenceValidEClasses(pair);

		if (allEClasses.isEmpty()) {
			return null;
		}

		for (EClass clazz : allEClasses) {
			if (!metamodelCoverage.isCovered(clazz)) {
				uncoveredEClasses.add(clazz);
			}
		}

		EClass result;
		if (!uncoveredEClasses.isEmpty()) {
			result = uncoveredEClasses.get(random.nextInt(uncoveredEClasses.size()));
		} else {
			result = allEClasses.get(random.nextInt(allEClasses.size()));
		}

		// FIXME this should actually be done after the command has been successfully
		// executed
		// metamodelCoverage.addCoveredEClass(result);
		return result;
	}
}
