package de.hub.mse.emf.mutator.mutation;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;

public class UnsetAttributesMutation implements Mutation{

	@Override
	public Command getMutationCommand(EditingDomain editingDomain, MutationTargetSelector targetSelector) {
		// Get random object from model
		EObject targetObject = targetSelector.selectRandomEObject();
		
		CompoundCommand command = new CompoundCommand();
		
		for(EAttribute eAttribute : targetSelector.getUtil().eAllAttributes(targetObject.eClass())) {
			command.append(new SetCommand(editingDomain, targetObject, eAttribute, SetCommand.UNSET_VALUE));
		}
		return command;
	}
}
