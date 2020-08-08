package de.hub.mse.emf.mutator.mutation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.command.IdentityCommand;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.DeleteCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;

import de.hub.mse.emf.generator.ModelGenerator;

public class ReplaceSubtreeMutation implements Mutation{
	ModelGenerator modelGenerator;
	int maxDepth;
	int maxBreadth;
	int maxObjectCount;
	
	public ReplaceSubtreeMutation(ModelGenerator modelGenerator) {
		this.modelGenerator = modelGenerator;
		this.maxDepth = 3;
		this.maxBreadth = 3;
		this.maxObjectCount = 30;
	}
	
	public ReplaceSubtreeMutation(ModelGenerator modelGenerator, int maxDepth, int maxObjectCount) {
		this.modelGenerator = modelGenerator;
		this.maxDepth = maxDepth;
		this.maxObjectCount = maxObjectCount;
	}
	
	@Override
	public Command getMutationCommand(EditingDomain editingDomain, MutationTargetSelector targetSelector) {
				
		// Select target to replace
		EObject targetObject = targetSelector.selectWeightedEObjects(1, false).get(0);
		EObject container = targetObject.eContainer();
		
		if(container == null) {
			return new IdentityCommand();
		}
		
		DeleteCommand deleteCommand = new DeleteCommand(editingDomain, Collections.singleton(targetObject));
		
		if(!deleteCommand.canExecute()) {
			return new IdentityCommand();
		}
		
		/*
		int deleted = 1;
		for(TreeIterator<EObject> it = targetObject.eAllContents(); it.hasNext();) {
			it.next();
			deleted++;
		}
				System.out.println("Model size to be deleted: " + deleted);
		*/
		
		EReference containmentRef = targetObject.eContainmentFeature();
		EClass childClass = targetSelector.selectUncoveredReferenceType(container.eClass(), containmentRef);
		
		Set<EClass> containedEClasses = new HashSet<EClass>();
		EObject subModel = modelGenerator.generateSubModel(targetSelector.getRandom(), childClass, maxDepth, maxBreadth, maxObjectCount, containedEClasses);
		
		/*
		int added = 1;
		for(TreeIterator<EObject> it = subModel.eAllContents(); it.hasNext();) {
			it.next();
			added++;
		}
		System.out.println("Model size to be added: " + added);
		System.out.println("Diff: " + (added - deleted));
		*/

		
		if(subModel == null) {
			return new IdentityCommand();
		}
		
		// We have all ingredients, now compose command
		CompoundCommand command = new CompoundCommand();
		command.append(deleteCommand);
		
		if(containmentRef.isMany()) {
			command.append(new AddCommand(editingDomain, container, containmentRef, subModel));
		}
		else {
			command.append(new SetCommand(editingDomain, container, containmentRef, subModel));
		}
		
		for(EClass eClass : containedEClasses) {
			targetSelector.getMetamodelCoverage().addTempCoveredEClass(eClass);
		}
		return command;
	}

}
