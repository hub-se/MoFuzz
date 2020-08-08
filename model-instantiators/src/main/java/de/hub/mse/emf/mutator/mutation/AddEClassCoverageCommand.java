package de.hub.mse.emf.mutator.mutation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.ecore.EClass;

import de.hub.mse.emf.generator.cgf.MetamodelCoverage;

/**
 * A command that adds a collection of EClasses to the metamodel coverage set.
 * @author Lam
 *
 */
public class AddEClassCoverageCommand implements Command{
	private ArrayList<EClass> eClasses;
	private MetamodelCoverage metamodelCoverage;
	private HashSet<EClass> newEClasses;
	private boolean canRedo;
	
	public AddEClassCoverageCommand(Collection<EClass> eClasses, MetamodelCoverage metamodelCoverage) {
		this.eClasses = new ArrayList<EClass>(eClasses);
		this.metamodelCoverage = metamodelCoverage;
		this.canRedo = false;
		
		for(int i = 0; i < eClasses.size(); i++) {
			EClass eClass = this.eClasses.get(i);
			if(!metamodelCoverage.isCovered(eClass)) {
				newEClasses.add(eClass);
			}
		}
	}
	@Override
	public boolean canExecute() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void execute() {
		for(int i = 0; i < eClasses.size(); i++) {
			metamodelCoverage.addCoveredEClass(eClasses.get(i));
		}
	}

	@Override
	public boolean canUndo() {
		return true;
	}

	@Override
	public void undo() {
		// Decrement counts and remove any new covered eClasses
		Set<EClass> coveredEClasses = metamodelCoverage.getCoveredEClasses();
		for(int i = 0; i < eClasses.size(); i++) {
			EClass eClass = eClasses.get(i);
			metamodelCoverage.decrementEClassCount(eClass);
			if(newEClasses.contains(eClass)) {
				coveredEClasses.remove(eClass);
			}
		}
		canRedo = true;
		
	}

	@Override
	public void redo() {
		if(canRedo) {
			execute();
			canRedo = false;
		}
		
	}

	@Override
	public Collection<?> getResult() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<?> getAffectedObjects() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getLabel() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDescription() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Command chain(Command command) {
		// TODO Auto-generated method stub
		return null;
	}

}
