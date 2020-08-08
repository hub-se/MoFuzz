package de.hub.mse.emf.mutator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.logging.Logger;

import org.eclipse.emf.common.command.BasicCommandStack;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CommandStack;
import org.eclipse.emf.common.command.CommandWrapper;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.common.command.IdentityCommand;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.provider.EcoreItemProviderAdapterFactory;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.edit.command.AddCommand;
import org.eclipse.emf.edit.command.DeleteCommand;
import org.eclipse.uml2.uml.edit.providers.UMLResourceItemProvider;
import org.eclipse.emf.edit.command.RemoveCommand;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.AdapterFactoryEditingDomain;
import org.eclipse.emf.edit.domain.EditingDomain;
import org.eclipse.emf.edit.provider.ComposedAdapterFactory;
import org.eclipse.emf.edit.provider.ItemProviderAdapter;
import org.eclipse.uml2.common.edit.domain.UML2AdapterFactoryEditingDomain;
import org.eclipse.uml2.uml.edit.providers.UMLItemProviderAdapterFactory;
import org.eclipse.uml2.uml.edit.providers.UMLReflectiveItemProviderAdapterFactory;
import org.eclipse.uml2.uml.edit.providers.UMLResourceItemProviderAdapterFactory;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.generator.ModelGenerator;
import de.hub.mse.emf.generator.cgf.MetamodelCoverage;
import de.hub.mse.emf.generator.internal.MetamodelUtil;
import de.hub.mse.emf.mutator.mutation.AddObjectMutation;
import de.hub.mse.emf.mutator.mutation.ChangeAttributesMutation;
import de.hub.mse.emf.mutator.mutation.ChangeCrossReferencesMutation;
import de.hub.mse.emf.mutator.mutation.DeleteObjectMutation;
import de.hub.mse.emf.mutator.mutation.Mutation;
import de.hub.mse.emf.mutator.mutation.MutationException;
import de.hub.mse.emf.mutator.mutation.MutationTargetSelector;
import de.hub.mse.emf.mutator.mutation.UnsetAttributesMutation;
/**
 * A generic instance model mutator.
 * @author Lam
 *
 */
public class ModelMutator {

	public final static Logger LOGGER = Logger.getLogger(ModelMutator.class.getName());
	
	/** The model mutation parameters. */
	//protected final ModelMutationConfig config;
	
	/** Metamodel utility functions, e.g. getters for classes, containment references, cross references, supertypes, etc. */
	protected final MetamodelUtil metamodelUtil;
	
	/** The metamodel coverage */
	MetamodelCoverage metamodelCoverage;
	
	private EditingDomain editingDomain;
	
	private MutationTargetSelector mutationTargetSelector;
	
	private ArrayList<Mutation> mutationOperators;
	
	private double[] mutationWeights;
	private double mutationWeightsSum;
	
	private Random random;
		
	public ModelMutator(ResourceSet resourceSet, MetamodelUtil metamodelUtil, Random random, MetamodelCoverage metamodelCoverage, ArrayList<Mutation> mutationOperators) {
		this.metamodelUtil = metamodelUtil;
		this.random = random;
		this.metamodelCoverage = metamodelCoverage;
		this.mutationTargetSelector = new MutationTargetSelector(random, null, metamodelUtil, metamodelCoverage);
		
		// Setup editing domain
		ComposedAdapterFactory adapterFactory = new ComposedAdapterFactory(ComposedAdapterFactory.Descriptor.Registry.INSTANCE);
		adapterFactory.addAdapterFactory(new UMLResourceItemProviderAdapterFactory());
		adapterFactory.addAdapterFactory(new UMLItemProviderAdapterFactory());
		adapterFactory.addAdapterFactory(new EcoreItemProviderAdapterFactory());
		adapterFactory.addAdapterFactory(new UMLReflectiveItemProviderAdapterFactory());

		this.editingDomain = new UML2AdapterFactoryEditingDomain(adapterFactory, new BasicCommandStack(), resourceSet);
		
		this.mutationOperators = (mutationOperators == null) ? getDefaultMutationOperators() : mutationOperators;
	}
	
	private ArrayList<Mutation> getDefaultMutationOperators() {
		assert(editingDomain != null && mutationTargetSelector != null);
		ArrayList<Mutation> mutationOperators = new ArrayList<Mutation>();
		mutationOperators.add(new AddObjectMutation(10));
		mutationOperators.add(new DeleteObjectMutation());
		mutationOperators.add(new ChangeAttributesMutation());
		mutationOperators.add(new UnsetAttributesMutation());
		return mutationOperators;
	}
	
	public void setMutationWeights(double[] weights) {
		assert(weights.length == mutationOperators.size());
		mutationWeights = weights;
		int sum = 0;
		for(int i = 0; i < weights.length; i++) {
			sum += weights[i];
		}
		mutationWeightsSum = sum;
	}
		
	public Mutation getRandomMutation() {
		if(mutationWeights != null) {
			double sum = 0;
			double x = random.nextDouble() * mutationWeightsSum;
	
			for (int i = 0; i < mutationWeights.length; i++) {
				sum += mutationWeights[i];
				if (x < sum) {
					return mutationOperators.get(i);
				}
			}
			
			return mutationOperators.get(0); // should not happen
		}
		else {
			int idx = random.nextInt(mutationOperators.size());
			return mutationOperators.get(idx);
		}
	}
	
	public int mutate(Resource resource, GenerationStatus genStatus) {
		/*
		 * Possible mutation operators:
		 * 1. Instantiate a given EClass and add it to the Model (possible creating additional objects along the containment hierarchy to an object of the model)
		 * 2. Delete a subtree (including dangling references)
		 * 3. Replace an object of a model with an instance of the same supertype (but possibly different concrete type)
		 * 4. Move one object/subtree to a different location in the model
		 * 5. Mutate the primitive attributes of an object
		 * 6. Select a cross reference and change either side of it
		 */
		
		// Set model to be mutated
		mutationTargetSelector.setModelResource(resource);
		
		// Select random mutation to perform
		//int idx = random.nextInt(mutationOperators.size());
		//Mutation selectedMutation = mutationOperators.get(idx);
		Mutation selectedMutation = getRandomMutation();
		Command mutationCommand = selectedMutation.getMutationCommand(editingDomain, mutationTargetSelector);
		
		if(!(mutationCommand instanceof IdentityCommand) && mutationCommand.canExecute()) {
			try {
				editingDomain.getCommandStack().execute(mutationCommand);
			}
			catch(Exception e) {
				System.err.println(e);
			}
			
			return modelSizeDiff(mutationCommand);
		}
		else {
			return Integer.MIN_VALUE;
		}
	}
	
	public boolean undoLastMutation() {
		if(editingDomain.getCommandStack().canUndo()) {
			editingDomain.getCommandStack().undo();
			return true;
		}
		return false;
	}
	
	public void flushCommandStack() {
		editingDomain.getCommandStack().flush();
	}
	
	private int modelSizeDiff(Command command) {
		
		// Delete Commands (RemoveObjectMutation, ReplaceSubTreeMutation)
		if(command instanceof CompoundCommand) {
			
			int sizeDiff = 0;
			for(Command cmd : ((CompoundCommand) command).getCommandList()) {
				sizeDiff += modelSizeDiff(cmd);
			}
			return sizeDiff;
		}
		
		// AddObjectMutation, ReplaceSubTreeMutation
		if(command instanceof AddCommand) {
			int size = 0; // The object itself, plus the contents as follows
			for(Object obj : ((AddCommand) command).getCollection()) {
				assert(obj instanceof EObject);
				size++;
				EObject value = (EObject) obj;
				for(TreeIterator<EObject> it = value.eAllContents(); it.hasNext();) {
					it.next();
					size++;
				}
				return size;
			}
			return 0; // Empty collection?
		}
		
		// AddObjectMutation (single containment refs), ChangeAttributesMutation, UnsetAttributesMutation
		if(command instanceof SetCommand) {
			int size = 0;
			SetCommand setCmd = (SetCommand) command;
			EStructuralFeature feature = setCmd.getFeature();
			if(feature instanceof EReference && !((EReference) feature).isContainment()) {
				return 0;
			}
			if(((SetCommand) command).getValue() instanceof EObject) { // AddObjectMutation
				EObject value = (EObject) ((SetCommand) command).getValue();
				size++; // the object itself, plus the contents as follows
				for(TreeIterator<EObject> it = value.eAllContents(); it.hasNext();) {
					it.next();
					size++;
				}
				return size;
			}
			return 0; // Model size unchanged
		}
		
		// Part of DeleteCommand (Remove + Set/Add)
		if(command instanceof RemoveCommand) {
			for(Object obj : ((RemoveCommand) command).getCollection()) {
				assert(obj instanceof EObject);
				EObject value = (EObject) obj;
				int size = 1; // the object itself, plus the contents as follows
				for(TreeIterator<EObject> it = value.eAllContents(); it.hasNext();) {
					it.next();
					size++;
				}
				return -size; // Objects are removed, so size diff is negative
			}
		}
		
		if(command instanceof CommandWrapper) {
			Command wrappedCommand = ((CommandWrapper) command).getCommand();
			if(wrappedCommand instanceof SetCommand) {
				SetCommand setCommand = (SetCommand) wrappedCommand;
				if(setCommand.getValue().equals(SetCommand.UNSET_VALUE)) {
					Object oldValue = setCommand.getOldValue();
					if(oldValue instanceof EObject) {
						EObject removedEObject = (EObject) oldValue;
						int size = 1; // the object itself, plus the contents as follows
						for(TreeIterator<EObject> it = removedEObject.eAllContents(); it.hasNext();) {
							it.next();
							size++;
						}
						return -size;
					}	
				}
				System.err.println("!!!");
			}
			else {
				System.err.println("???");
				// TODO: HANDLE SUBSETSUPERSET COMMANDS
			}
		}
		
		// All other mutation operators don't modify the size
		return 0;
	}
}
