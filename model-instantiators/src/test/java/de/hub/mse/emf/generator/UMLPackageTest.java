package de.hub.mse.emf.generator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import util.Tree;
import util.Tree.Node;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.junit.*;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.uml2.uml.UMLPackage;
import org.eclipse.uml2.uml.UMLPackage.Literals;

import de.hub.mse.emf.generator.internal.MetamodelUtil;
import de.hub.mse.emf.generator.internal.MetamodelResource;

public class UMLPackageTest {
	MetamodelResource metamodelResource;
	HashSet<EClass> eClassWhitelist;
	ModelGenerationConfigImpl config;
	MetamodelUtil metamodelData;

	// The containment tree
	// To ensure termination, EClasses are only added once (when they are first discovered through BFS)
	Tree<EClass> containmentTree;
	
	// Maps EClasses to their direct (not transitive) child/parent containment EClasses
	HashMap<EClass, HashSet<EClass>> childMap;
	HashMap<EClass, HashSet<EClass>> parentMap;
	HashMap<EClass, EClass> directParentMap;
		
		
	@Before
	public void setUp() {
		metamodelResource = new MetamodelResource(UMLPackage.eINSTANCE);
		eClassWhitelist = new HashSet<EClass>();
		config = new ModelGenerationConfigImpl(metamodelResource, Literals.MODEL, eClassWhitelist, 1500,  15, 15 , 15);
		metamodelData = new MetamodelUtil(config.ePackages(), config.ignoredEClasses(), config.getEClassWhitelist());
		
		childMap = new HashMap<EClass, HashSet<EClass>>();
		parentMap = new HashMap<EClass, HashSet<EClass>>();
		directParentMap = new HashMap<EClass, EClass>();
		
		containmentTree = new Tree<EClass>(Literals.MODEL);
		buildContainmentTree();
	}

	private void buildContainmentTree() {		
		// EClasses un/reachable from the root through containment references
   		HashSet<EClass> reachableEClasses = new HashSet<EClass>();
		HashSet<EClass> unreachableEClasses = new HashSet<EClass>();
		
		// For BFS
		Queue<Node<EClass>> eClassQueue = new LinkedList<Node<EClass>>();
		eClassQueue.add(containmentTree.getRoot());
		reachableEClasses.add(Literals.MODEL);
		
		while(!eClassQueue.isEmpty()) {
			Node<EClass> parentNode = eClassQueue.remove();
			EClass curEClass = parentNode.getData();
			EObject parent = curEClass.getEPackage().getEFactoryInstance().create(curEClass);
			
			for(EReference eReference : metamodelData.eAllContainment(curEClass)) {
				
				// Counting how many of the concrete classes of the reference type are actually valid/invalid
				int valid = 0;
				int invalid = 0;
				
				for(EClass childClass : metamodelData.eAllConcreteSubTypeOrSelf(eReference)) {
					EObject child = childClass.getEPackage().getEFactoryInstance().create(childClass);
					if(eReference.isMany()) {
						List<EObject> values = (List<EObject>) parent.eGet(eReference);
						try {
							// This might throw an exception if setting the feature violates internal well-formedness rules
							values.add(child);
							parent.eSet(eReference, values);
							
							// If we reach this statement, no exception has occured and the reference is valid
							// Add the child class to the end of the queue (BFS)
							if(!reachableEClasses.contains(childClass)) {
								reachableEClasses.add(childClass);	
								Node<EClass> childNode = new Node<EClass>(childClass,parentNode);
								directParentMap.put(childClass, curEClass);
								eClassQueue.add(childNode);
							}
							else {
								//Node<EClass> childNode = new Node<EClass>(childClass,parentNode);
							}
							valid++;
							
							// Update child/parent maps with this new parent-child relationship
							if(childMap.containsKey(curEClass)) {
								childMap.get(curEClass).add(childClass);
							}
							else {
								HashSet<EClass> children = new HashSet<EClass>();
								children.add(childClass);
								childMap.put(curEClass, children);
							}
							
							if(parentMap.containsKey(childClass)) {
								parentMap.get(childClass).add(curEClass);
							}
							else {
								HashSet<EClass> parents = new HashSet<EClass>();
								parents.add(curEClass);
								parentMap.put(childClass, parents);
							}
							
						}
						catch(ArrayStoreException e) {
							unreachableEClasses.add(childClass);
							invalid++;
						}
						catch(UnsupportedOperationException e) {
							invalid++;
							System.out.println(parent.eClass().getName() + "--" + eReference.getName() + "--" + childClass.getName());
						}
					}
					else {
						try {
							parent.eSet(eReference, child);
							if(!reachableEClasses.contains(childClass)) {
								reachableEClasses.add(childClass);
								Node<EClass> childNode = new Node<EClass>(childClass, parentNode);
								directParentMap.put(childClass, curEClass);
								eClassQueue.add(childNode);
							}
							else {
								//Node<EClass> childNode = new Node<EClass>(childClass,parentNode);
							}
							valid++;
							
							if(childMap.containsKey(curEClass)) {
								childMap.get(curEClass).add(childClass);
							}
							else {
								HashSet<EClass> children = new HashSet<EClass>();
								children.add(childClass);
								childMap.put(curEClass, children);
							}
							
							if(parentMap.containsKey(childClass)) {
								parentMap.get(childClass).add(curEClass);
							}
							else {
								HashSet<EClass> parents = new HashSet<EClass>();
								parents.add(curEClass);
								parentMap.put(childClass, parents);
							}
						}
						catch(IllegalArgumentException e) {
							unreachableEClasses.add(childClass);
							invalid++;
						}
					}
				}
				
				if(invalid != 0) {
					//System.out.println(parent.eClass().getName() + "--" + eReference.getName() + ": (" + valid + "/" + invalid + ")" );
				}
			}
		}
		
		// Remove all EClasses that ended up being reachable through some other reference
		//for(EClass valid : reachableEClasses) {
		//	unreachableEClasses.remove(valid);
		//}
	}
	
	private HashSet<EClass> getAllConcreteEClasses(EPackage ePackage) {
		HashSet<EClass> eClasses = new HashSet<EClass>();
		for(TreeIterator<EObject> iter = ePackage.eAllContents(); iter.hasNext();) {
			EObject eObject = iter.next();
			if(eObject instanceof EClass) {
				EClass eClass = (EClass) eObject;
				if(!eClass.isAbstract() && !eClass.isInterface()) {
					eClasses.add(eClass);
				}
			}
		}
		return eClasses;
	}
	
	// Determines the classes that are reachable from the root class 'Model' through containment references
	@Test
	public void eClassReachabilityTest() {
		HashSet<EClass> allConcreteClasses = getAllConcreteEClasses(UMLPackage.eINSTANCE);
		
		// Reachability by construction of the parent map
		assertEquals(allConcreteClasses.size(), 193);
		for(EClass eClass : allConcreteClasses) {
			assertTrue(parentMap.keySet().contains(eClass));
			
			Deque<EClass> pathToRoot = getPathToRoot(eClass);
			assertTrue(!pathToRoot.isEmpty());
			assertTrue(pathToRoot.getFirst().equals(UMLPackage.Literals.MODEL));
		}
		
		// Reachability by finding a path from the root of the containment tree to the eClass via DFS
		HashSet<EClass> visited = new HashSet<EClass>();
		Node<EClass> root = containmentTree.getRoot();
		DFS(root, visited);
		
		for(EClass eClass : allConcreteClasses) {
			assertTrue(visited.contains(eClass));
		}
	}
	
	private Deque<EClass> getPathToRoot(EClass eClass){
		
		Deque<EClass> path = new ArrayDeque<EClass>();
		path.addFirst(eClass);
		
		EClass currentEClass = eClass;
		while(!currentEClass.equals(UMLPackage.Literals.MODEL)) {
			currentEClass = directParentMap.get(currentEClass);			
			// Cycle???
			if(path.contains(currentEClass)) {
				break;
			}
			path.addFirst(currentEClass);

		}
		return path;
	}
	
	private void DFS(Node<EClass> currentNode, HashSet<EClass> visited) {
		// Sanity check
		assertTrue(!visited.contains(currentNode.getData()));
		
		visited.add(currentNode.getData());
		List<Node<EClass>> children = currentNode.getChildren();
		for(Node<EClass> child : children) {
			if(!visited.contains(child.getData())) {
				DFS(child, visited);
			}
		}
	}
	
	@Test
	public void coverageTest() {
		for(EClass eClass : parentMap.keySet()) {
			//System.out.println(eClass.getName() + ": \t\t\t\t" + parentMap.get(eClass).size());
		}
	}
}
