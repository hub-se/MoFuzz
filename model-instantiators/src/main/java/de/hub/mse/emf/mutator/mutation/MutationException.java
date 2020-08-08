package de.hub.mse.emf.mutator.mutation;

/**
 * Mutation-specific exception in case a mutation fails.
 * @author Lam
 *
 */
public class MutationException extends Exception{

	private static final long serialVersionUID = -4681116602429887998L;

	/**
	 * Creates a new mutation exception.
	 */
	public MutationException() {
		super();
	}
	
	/**
	 * Creates a new mutation exception with a specific error {@code message}.
	 * @param message the error message
	 */
	public MutationException(String message) {
		super(message);
	}
}
