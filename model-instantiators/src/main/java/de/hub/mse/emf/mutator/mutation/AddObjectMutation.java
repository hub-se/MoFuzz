package de.hub.mse.emf.mutator.mutation;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.command.IdentityCommand;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;

import com.google.common.collect.ImmutableList;

import de.hub.mse.emf.generator.internal.MetamodelUtil;

public class AddObjectMutation implements Mutation{
	
	private int numberOfObjects;
	
	/**
	 * Constructor
	 * @param numberOfObjects number of objects to add
	 */
	public AddObjectMutation(int numberOfObjects) {
		this.numberOfObjects = numberOfObjects;
	}
	
	public AddObjectMutation() {
		this.numberOfObjects = 1;
	}
		
	@Override
	public Command getMutationCommand(EditingDomain editingDomain, MutationTargetSelector targetSelector){
		
		// Get random object/s from model
		if(numberOfObjects == 1) {
			EObject sourceObject = targetSelector.selectWeightedEObjects(1, false).get(0);
			//EObject sourceObject = targetSelector.selectRandomEObject();
			return createMutationCommand(sourceObject, editingDomain, targetSelector);
		}
		else {
			ArrayList<EObject> sourceObjects = targetSelector.selectWeightedEObjects(numberOfObjects, false);
			//EObject[] sourceObjects = targetSelector.selectEObjects(numberOfObjects);
			CompoundCommand addAllCommand = new CompoundCommand();
			for(EObject sourceObject : sourceObjects) {
				addAllCommand.append(createMutationCommand(sourceObject, editingDomain, targetSelector));
			}
			return addAllCommand;
		}
						
	}
	
	private Command createMutationCommand(EObject sourceObject, EditingDomain editingDomain, MutationTargetSelector targetSelector) {
		ArrayList<EReference> candidateReferences = new ArrayList<EReference>();
		
		// We only want to add new containment refs or add to multi-valued ones
		for(EReference ref : targetSelector.getUtil().eAllContainment(sourceObject.eClass())){
			if(ref.isMany() || !sourceObject.eIsSet(ref)) {
				candidateReferences.add(ref);
			}
		}
		
		// No object can be added to the selected target, do nothing
		if(candidateReferences.isEmpty()) {
			return new IdentityCommand();
		}
		
		// Select a reference from one of the possible candidates, prefer uncovered ones
		EReference reference = targetSelector.selectUncoveredReference(candidateReferences);
		EClass targetClass = targetSelector.selectUncoveredReferenceType(sourceObject.eClass(), reference);
		EObject targetObject = targetClass != null ? targetClass.getEPackage().getEFactoryInstance().create(targetClass) : null;
		
		if(targetObject != null) {
			targetSelector.getMetamodelCoverage().addTempCoveredEClass(targetObject.eClass());
		}
		
		if(reference.isMany()) {
			return new AddCommand(editingDomain, sourceObject, reference, targetObject);
		}
		else {
			return new SetCommand(editingDomain, sourceObject, reference, targetObject);
		}
	}

}
