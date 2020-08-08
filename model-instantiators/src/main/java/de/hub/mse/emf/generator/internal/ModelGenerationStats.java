package de.hub.mse.emf.generator.internal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.resource.Resource;

import de.hub.mse.emf.generator.ModelGenerator;

public class ModelGenerationStats {
	/*
	 * Possible exceptions when setting containment references:
	 * Single: IllegalArgumentException (Simply catching the exception should be sufficient)
	 * Many  : ArrayStoreException (Might need to restore the old value)
	 * 
	 * Possible exceptions when setting cross references:
	 * Single: IllegalArgumentException (Simply catching the exception shuold be sufficient), IllegalStateException (Might need to restore the old containments)
	 * Many  : ArrayStoreException (Might need to restore the old value)
	 * 
	 */
	public static long illegalArgumentExceptionCount = 0;
	public static long illegalStateExceptionCount = 0;
	public static long arrayStoreExceptionCount = 0;
	
	public static long singleContainmentRefCount = 0;
	public static long singleContainmentRefSuccess = 0;
	public static long singleContainmentRefFail = 0;
	public static long singleContainmentRefTrialCount = 0;
	
	public static long manyContainmentRefCount = 0;
	public static long manyContainmentRefSuccess = 0;
	public static long manyContainmentRefFail = 0;
	public static long manyContainmentRefTrialCount = 0;
	
	public static long singleCrossRefCount = 0;
	public static long singleCrossRefSuccess = 0;
	public static long singleCrossRefFail = 0;
	public static long singleCrossRefTrialCount = 0;
	
	public static long manyCrossRefCount = 0;
	public static long manyCrossRefSuccess = 0;
	public static long manyCrossRefFail = 0;
	public static long manyCrossRefTrialCount = 0;
	
	
	public static long startTime = System.currentTimeMillis();
	public static Runtime runtime = Runtime.getRuntime();
	
	public static HashMap<EClass, Integer> eClassCoverageMap;
	public static HashSet<EClass> allEClasses;
	
	
	private static int totalEReferences = 0;
	private static int coveredEReferences = 0;
	
	private static int totalEClasses = 0;
	private static int coveredEClasses = 0;
	public static String lastCoveredEClass;
	
	/*
	 *  Keep track of model sizes to make sure we generate models of diverse sizes.
	 *  
	 *  Buckets: 1 < 10 < 100 < 200 < 300 ... < 900 < 1000 < 1500
	 */
	public static int[] modelSizeDistribution = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
	
	public static int addModelCount(Resource resource) {
		int size = 0;
		for(TreeIterator<EObject> iter = resource.getAllContents(); iter.hasNext();) {
			iter.next();
			size++;
		}
		
		if(size == 1) {
			modelSizeDistribution[0]++;
		}
		else if(size < 10) {
			modelSizeDistribution[1]++;
		}
		else if(size < 100) {
			modelSizeDistribution[2]++;
		}
		else if(size < 200) {
			modelSizeDistribution[3]++;
		}
		else if(size < 300) {
			modelSizeDistribution[4]++;
		}
		else if(size < 400) {
			modelSizeDistribution[5]++;
		}
		else if(size < 500) {
			modelSizeDistribution[6]++;
		}
		else if(size < 600) {
			modelSizeDistribution[7]++;
		}
		else if(size < 700) {
			modelSizeDistribution[8]++;
		}
		else if(size < 800) {
			modelSizeDistribution[9]++;
		}
		else if(size < 900) {
			modelSizeDistribution[10]++;
		}
		else if(size < 1000) {
			modelSizeDistribution[11]++;
		}
		else if(size < 1500) {
			modelSizeDistribution[12]++;
		}
		return size;
	}
	
	public static void initEClassCoverageMap(Set<EPackage> ePackages) {
		if(eClassCoverageMap != null) {
			return;
		}
				
		eClassCoverageMap = new HashMap<EClass, Integer>();
		allEClasses = new HashSet<EClass>();
		
		for(EPackage ePackage : ePackages) {
			for(Iterator<EObject> it = ePackage.eAllContents(); it.hasNext();) {
				EObject eObject = it.next();
				if(eObject instanceof EClass){
					EClass eClass = (EClass) eObject;
					if(!eClass.isAbstract() && !eClass.isInterface()) {
						eClassCoverageMap.put(eClass, 0);
						allEClasses.add(eClass);
					}
				}
			}
		}
		ModelGenerationStats.totalEClasses = allEClasses.size();
	}
	
	public static void updateMetamodelCoverage(Resource resource) {
		for(TreeIterator<EObject> it = resource.getAllContents(); it.hasNext();) {
			EClass eClass = it.next().eClass();
			try {
				int currentCount = ModelGenerationStats.eClassCoverageMap.get(eClass);
				if(currentCount == 0) {
					ModelGenerationStats.coveredEClasses++;
					ModelGenerationStats.lastCoveredEClass = eClass.getName();
				}
				ModelGenerationStats.eClassCoverageMap.put(eClass, currentCount+1);
			}
			catch(NullPointerException e) {
				//System.out.println("Not in package: " + eClass.getName());
			}
		}
		
		// Compute package coverage
		/*
		int totalCount = 0;
		int covered = 0;
		for(EClass eClass : eClassCoverageMap.keySet()) {
			int currentCount = eClassCoverageMap.get(eClass);
			totalCount++;
			if(currentCount > 0) {
				covered++;
			}
		}
		System.out.println("Total: " + totalCount + "Covered" + covered + " Most recently covered: " + ModelGenerationStats.lastCoveredEClass);
		*/

	}
	
	public static void printCompleteStats(int attempts, int generatedModels) {
		long elapsedTimeSeconds = (System.currentTimeMillis() - startTime)/1000;
		long elapsedTimeMinutes = elapsedTimeSeconds/60;
        long memory = (runtime.totalMemory() - runtime.freeMemory()) / (1024L * 1024L);
        
		//Print stats
		System.out.println("\n");
		System.out.println("================================================");
		System.out.println("Elapsed Time: 					" + elapsedTimeSeconds + " seconds (" + elapsedTimeMinutes + " minutes)");
		System.out.println("Models generated 				" + generatedModels + "(Attempts: " + attempts + ", Success rate: " + addPercentage(generatedModels, attempts) + ")");
		System.out.println("Memory consumption: 			" + memory + "mb");
		System.out.println("EClass coverage:	 			" + "(" + coveredEReferences + "/"+ totalEClasses + ")");
		System.out.println("EClass coverage:	 			" + "(" + coveredEClasses + "/"+ totalEClasses + ")");
		System.out.println("Most recently covered EClass:	" + lastCoveredEClass);
		System.out.println("IllegalArgumentExceptions:		" + illegalArgumentExceptionCount);
		System.out.println("IllegalStateExceptions:			" + illegalStateExceptionCount);
		System.out.println("ArrayStoreExceptions:			" + arrayStoreExceptionCount);
		System.out.println("------------------------------------------------");
		System.out.println("Containment References (single):" + singleContainmentRefCount);
		System.out.println("Successful:						" + addPercentage(singleContainmentRefSuccess, singleContainmentRefCount));
		System.out.println("Avg. number of trials:			" + String.format("%.3f", ((double) singleContainmentRefTrialCount) / singleContainmentRefSuccess));
		System.out.println("Failed:							" + addPercentage(singleContainmentRefFail, singleContainmentRefCount));
		System.out.println();
		System.out.println("Containment References (many):	" + manyContainmentRefCount);
		System.out.println("Successful:						" + addPercentage(manyContainmentRefSuccess, manyContainmentRefCount));
		System.out.println("Avg. number of trials:			" + String.format("%.2f", ((double) manyContainmentRefTrialCount) / manyContainmentRefSuccess));
		System.out.println("Failed:							" + addPercentage(manyContainmentRefFail, manyContainmentRefCount));
		System.out.println();
		System.out.println("Containment References (total):	" + singleContainmentRefCount + manyContainmentRefCount);
		System.out.println("Successful:						" + addPercentage(singleContainmentRefSuccess + manyContainmentRefSuccess,
																	singleContainmentRefCount + manyContainmentRefCount));
		System.out.println("Avg. number of trials:			" + String.format("%.2f", ((double) (singleContainmentRefTrialCount + manyContainmentRefTrialCount)) 
																						/ (singleContainmentRefSuccess + manyContainmentRefSuccess)));
		System.out.println("Failed:							" + addPercentage(singleContainmentRefFail + manyContainmentRefFail,
																	singleContainmentRefCount + manyContainmentRefCount));
		System.out.println("------------------------------------------------");
		System.out.println("Cross References (single):		" + singleCrossRefCount);
		System.out.println("Successful:						" + addPercentage(singleCrossRefSuccess, singleCrossRefCount));
		System.out.println("Avg. number of trials:			" + String.format("%.2f", ((double) singleCrossRefTrialCount) / singleCrossRefSuccess));
		System.out.println("Failed:							" + addPercentage(singleCrossRefFail, singleCrossRefCount));
		System.out.println();
		System.out.println("Cross References (many):		" + manyCrossRefCount);
		System.out.println("Successful:						" + addPercentage(manyCrossRefSuccess, manyCrossRefCount));
		System.out.println("Avg. number of trials:			" + String.format("%.2f", ((double) manyCrossRefTrialCount) / manyCrossRefSuccess));
		System.out.println("Failed:							" + addPercentage(manyCrossRefFail, manyCrossRefCount));
		System.out.println();
		System.out.println("Cross References (total):		" + singleCrossRefCount + manyCrossRefCount);
		System.out.println("Successful:						" + addPercentage(singleCrossRefSuccess + manyCrossRefSuccess,
																	singleCrossRefCount + manyCrossRefCount));
		System.out.println("Avg. number of trials:			" + String.format("%.2f", ((double) (singleCrossRefTrialCount + manyCrossRefTrialCount)) 
																						/ (singleCrossRefSuccess + manyCrossRefSuccess)));
		System.out.println("Failed:							" + addPercentage(singleCrossRefFail + manyCrossRefFail,
																	singleCrossRefCount + manyCrossRefCount));
		printModelSizeDistribution();
		System.out.println("================================================");
	}
	
	public static String addPercentage(long value, long total) {
		double perc = (((double) value) / total) * 100;
		return value + " (" + String.format("%.2f", perc) + "%)";
	}
	
	public static void printModelSizeDistribution() {
		System.out.println(modelSizeDistribution[0] + " | " + modelSizeDistribution[1] + " | " + modelSizeDistribution[2] + " | " + modelSizeDistribution[3] + " | " + 
				modelSizeDistribution[4] + " | " + modelSizeDistribution[5] + " | " + modelSizeDistribution[6] + " | " + modelSizeDistribution[7] + " | " + 
				+ modelSizeDistribution[8] + " | " + modelSizeDistribution[9] + " | " + modelSizeDistribution[10] + " | "
				+ modelSizeDistribution[11] + " | " + modelSizeDistribution[12]);
	}
	
}
