package de.hub.mse.emf.fuzz.junit.quickcheck.generator;

import java.util.function.Consumer;

import com.pholser.junit.quickcheck.generator.Generator;

/**
 * Generators of this type save their inputs for further processing.
 * A callback is provided to allow a guidance (e.g. {@link de.hub.mse.emf.fuzz.CoverageGuidance})
 * to notify the generator whenever a previously executed input should be saved
 * (e.g. if the input exercised new coverage).
 * 
 * @author Lam
 *
 * @param <T> type of values to be generated
 */
public abstract class InputSavingGenerator<T> extends Generator<T>{

	protected InputSavingGenerator(Class<T> type) {
		super(type);
	}
	
	/**
	 * Save the current input (e.g. to queue and/or disk).
	 * @param value some bookkeeping value, e.g. number of branch responsibilities.
	 */
	
	public abstract void saveInput(int value);
	
	/**
	 * Provides a callback to the saveInput() method.
	 * @return the saveInput() callback.
	 */
	public Consumer<Integer> generateSaveCallBack(){
		return this::saveInput;
	}

}
