package de.hub.mse.emf.generator.serge;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import org.eclipse.emf.ecore.EEnum;

public class RandomUtils {

	private static Random random = new Random();

	// TODO also other characters
	public static String randomlyGetString() {
		int leftLimit = 97; // letter 'a'
		int rightLimit = 122; // letter 'z'
		int targetStringLength = (int) (Math.random() * 10) + 1; // length between 1 and 10;
		StringBuilder buffer = new StringBuilder(targetStringLength);
		for (int i = 0; i < targetStringLength; i++) {
			int randomLimitedInt = leftLimit + (int) (random.nextFloat() * (rightLimit - leftLimit + 1));
			buffer.append((char) randomLimitedInt);
		}
		String generatedString = buffer.toString();
		
		return generatedString;
	}

	public static boolean randomlyGetBoolean() {
		return random.nextBoolean();
	}

	public static int randomlyGetInteger() {
		return random.nextInt();
	}

	public static double randomlyGetDouble() {
		return random.nextDouble();
	}
	
	public static String randomlyGetEnumLiteral(EEnum enumType) {
		int index = (int) (Math.random() * enumType.getELiterals().size());
		return enumType.getELiterals().get(index).toString();
	}

	public static Object randomlyGetFromList(List list) {
		if (list.isEmpty()) {
			return null;
		} else {
			int index = (int) (Math.random() * list.size());
			return list.get(index);
		}
	}

	public static Object randomlyGetFromSet(Set set) {
		if (set.isEmpty()) {
			return null;
		} else {
			Object[] setAsArray = set.toArray();
			int index = (int) (Math.random() * setAsArray.length);
			return setAsArray[index];
		}
	}

}
