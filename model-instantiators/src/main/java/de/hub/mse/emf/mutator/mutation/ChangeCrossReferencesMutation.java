package de.hub.mse.emf.mutator.mutation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.command.IdentityCommand;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.util.EContentsEList;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;

import com.google.common.collect.ListMultimap;

import de.hub.mse.emf.generator.internal.EClassReferencePair;

public class ChangeCrossReferencesMutation implements Mutation{
		
	private Random random;
	
	@Override
	public Command getMutationCommand(EditingDomain editingDomain, MutationTargetSelector targetSelector) {
		this.random = targetSelector.getRandom();
		
		List<EObject> allEObjects = targetSelector.getAllObjects();
		ListMultimap<EClass, EObject> objectIndex = targetSelector.getObjectIndex();
		CompoundCommand command = new CompoundCommand();

		for(int i = 0; i < allEObjects.size(); i++) {
			//if(random.nextBoolean()) continue;
			Command nextCmd = createMutationCommand(allEObjects.get(i),objectIndex, editingDomain, targetSelector);
			if(nextCmd instanceof IdentityCommand) continue;
			if(!nextCmd.canUndo()) continue;
			command.appendIfCanExecute(nextCmd);
		}
		
		if(command.getCommandList().size() > 0) {
			return command;
		}
		
		return new IdentityCommand();
	}
	
	private Command createMutationCommand(EObject sourceObject, ListMultimap<EClass, EObject> objectIndex, EditingDomain editingDomain, MutationTargetSelector targetSelector) {
		
		// If the object has some set references, change them
		/*
		CompoundCommand command = new CompoundCommand();
		for(EContentsEList.FeatureIterator<EObject> iterator = 
				(EContentsEList.FeatureIterator<EObject>)sourceObject.eCrossReferences().iterator();
				iterator.hasNext(); ) {
			
			EObject currentTargetObject = (EObject) iterator.next();
			EReference crossReference = (EReference) iterator.feature();
			
			if (!crossReference.isChangeable()) continue;
			
			List<EObject> allObjects = new LinkedList<EObject>(objectIndex.get(crossReference.getEReferenceType()));

			if(allObjects.isEmpty()) continue;
			
			// Determine valid eClasses
			EClassReferencePair eClassRefPair = new EClassReferencePair(sourceObject.eClass(), crossReference);
			HashSet<EClass> validEClassSet = 
					new HashSet<EClass>(targetSelector.getUtil().eReferenceValidEClasses(eClassRefPair));
			
			ArrayList<EObject> candidateObjects = new ArrayList<EObject>();
			for(EObject eObject : allObjects) {
				if(validEClassSet.contains(eObject.eClass())
						) {
					candidateObjects.add(eObject);
				}
			}
			candidateObjects.remove(sourceObject); // Avoid self-referencing
			candidateObjects.remove(currentTargetObject); // To choose new target
			
			if(!candidateObjects.isEmpty()) {
				int idx = random.nextInt(candidateObjects.size());
				EObject target = candidateObjects.get(idx);
				command.append(new SetCommand(editingDomain, sourceObject, crossReference, target));
			}
			
			return command;
		}
		*/
		CompoundCommand command = new CompoundCommand();
		for(EReference crossReference : targetSelector.getUtil().eAllNonContainment(sourceObject.eClass())) {
			/*
			if (!crossReference.isChangeable() || sourceObject.eIsSet(crossReference)) {
				continue;
			}
			*/
			if (!crossReference.isChangeable() && !crossReference.isUnique()) {
				continue;
			}
			
			List<EObject> allObjects = new LinkedList<EObject>(objectIndex.get(crossReference.getEReferenceType()));

			if(allObjects.isEmpty()) continue;
			
			// Determine valid eClasses
			EClassReferencePair eClassRefPair = new EClassReferencePair(sourceObject.eClass(), crossReference);
			HashSet<EClass> validEClassSet = 
					new HashSet<EClass>(targetSelector.getUtil().eReferenceValidEClasses(eClassRefPair));
			
			ArrayList<EObject> candidateObjects = new ArrayList<EObject>();
			for(EObject eObject : allObjects) {
				if(validEClassSet.contains(eObject.eClass())) {
					candidateObjects.add(eObject);
				}
			}
			candidateObjects.remove(sourceObject); // Avoid self-referencing
			
			if(!candidateObjects.isEmpty()) {
				int idx = random.nextInt(candidateObjects.size());
				EObject target = candidateObjects.get(idx);
				command.append(new SetCommand(editingDomain, sourceObject, crossReference, target));
			}
		}
		
		if(command.getCommandList().size() > 0) {
			return command;
		}
		else {
			return new IdentityCommand();
		}
	}
}
