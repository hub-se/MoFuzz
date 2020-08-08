package de.hub.mse.emf.mutator.mutation;

import static com.google.common.primitives.Primitives.isWrapperType;
import static com.google.common.primitives.Primitives.unwrap;

import java.text.MessageFormat;
import java.util.Random;

import org.apache.commons.lang3.RandomStringUtils;
import org.eclipse.emf.common.command.Command;
import org.eclipse.emf.common.command.CompoundCommand;
import org.eclipse.emf.ecore.EAttribute;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EEnum;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.edit.command.SetCommand;
import org.eclipse.emf.edit.domain.EditingDomain;

public class ChangeAttributesMutation implements Mutation{
	
	@Override
	public Command getMutationCommand(EditingDomain editingDomain, 
			MutationTargetSelector targetSelector) {
		
		// Get random object from model
		EObject targetObject = targetSelector.selectRandomEObject();
		
		CompoundCommand command = new CompoundCommand();
		
		for(EAttribute eAttribute : targetSelector.getUtil().eAllAttributes(targetObject.eClass())) {
			command.append(getAttributeMutationCommand(editingDomain, targetSelector.getRandom(), targetObject, eAttribute));
		}
		return command;
	}
	
	/**
	 * Creates a mutation command to mutate a single attribute.
	 * @param eObject the object to mutate
	 * @param eAttribute the attribute to mutate
	 * @return the mutation command
	 */
	private Command getAttributeMutationCommand(EditingDomain editingDomain, Random random, 
			EObject eObject, EAttribute eAttribute) {
		
		EDataType eAttributeType = eAttribute.getEAttributeType();
		Class<?> instanceClass = eAttributeType.getInstanceClass();
		final Object value;
		
		if (eAttributeType instanceof EEnum) {
			EEnum eEnum = (EEnum) eAttributeType;
			int size = eEnum.getELiterals().size();
			if(instanceClass == null) {
				// Initial implementation TODO: review
				instanceClass = int.class;
				int randomValue = Math.abs((Integer) nextValue(instanceClass, random));
				value = eEnum.getELiterals().get(randomValue % size); 
			}
			else {
				int idx = random.nextInt(size);
				value = eEnum.getELiterals().get(idx).getInstance();
			}
		} else {
			value = nextValue(instanceClass, random);
		}

		return new SetCommand(editingDomain, eObject, eAttribute, value);
	}
	
	protected Object nextValue(Class<?> instanceClass, Random random) {
		final Object value;
		if (instanceClass.isPrimitive() || isWrapperType(instanceClass)) {
			value = nextPrimitive(unwrap(instanceClass), random);
		} else {
			value = nextObject(instanceClass, random);
		}
		return value;
	}
	
	protected Object nextObject(Class<?> instanceClass, Random random) {
		if (instanceClass == String.class) {
			return RandomStringUtils.random(
					10, 
					0, 0, true, true, null, random);
		} else {
			System.err.println("Unable to generate instance of: " + instanceClass.getName());
		}
		return null;
	}

	/**
	 * @param eObject
	 * @param eAttribute
	 * @param instanceClass
	 */
	protected Object nextPrimitive(Class<?> instanceClass, Random random) {
		if (instanceClass == boolean.class) {
			return random.nextBoolean();
		} else if (instanceClass == byte.class) {
			byte[] buff = new byte[1];
			random.nextBytes(buff);
			return buff[0];
		} else if (instanceClass == char.class) {
			char nextChar = (char) random.nextInt();
			return nextChar;
		} else if (instanceClass == double.class) {
			return random.nextDouble();
		} else if (instanceClass == float.class) {
			return random.nextFloat();
		} else if (instanceClass == int.class) {
			return random.nextInt();
		} else if (instanceClass == long.class) {
			return random.nextLong();
		} else if (instanceClass == short.class) {
			short nextShort = (short) random.nextInt();
			return nextShort;
		} else {
			throw new IllegalArgumentException();
		}
	}

}
