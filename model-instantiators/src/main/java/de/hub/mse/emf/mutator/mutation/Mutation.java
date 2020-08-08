package de.hub.mse.emf.mutator.mutation;

import java.util.Random;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.domain.EditingDomain;

import de.hub.mse.emf.generator.internal.MetamodelUtil;

/**
 * Mutation interface
 * @author Lam
 *
 */
public interface Mutation {
	
	/**
	 * Returns a {@link Command} that can be executed to perform the mutation.
	 * 
	 * @param editingDomain EditingDomain to execute mutation on
	 * @param targetSelector MutationTargetSelector to select mutation targets
	 */
	public abstract Command getMutationCommand(EditingDomain editingDomain, MutationTargetSelector targetSelector);
}
