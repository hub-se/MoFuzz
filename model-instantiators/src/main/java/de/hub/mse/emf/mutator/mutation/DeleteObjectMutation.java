package de.hub.mse.emf.mutator.mutation;

import java.util.Collections;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.IdentityCommand;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.DeleteCommand;
import org.eclipse.emf.edit.domain.EditingDomain;

public class DeleteObjectMutation implements Mutation{
	
	@Override
	public Command getMutationCommand(EditingDomain editingDomain, MutationTargetSelector targetSelector){
		
		// Get random object from model
		EObject targetObject = targetSelector.selectRandomEObject();
				
		DeleteCommand deleteCommand = new DeleteCommand(editingDomain, Collections.singleton(targetObject));
		
		// No object can be added to the selected target, do nothing
		
		if(!deleteCommand.canExecute()) {
			return new IdentityCommand();
		}
		else {
			return deleteCommand;
		}
	}

}
