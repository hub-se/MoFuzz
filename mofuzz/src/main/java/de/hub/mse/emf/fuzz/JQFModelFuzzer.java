package de.hub.mse.emf.fuzz;

import de.hub.mse.emf.fuzz.junit.quickcheck.ModelFuzzStatement;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FuzzStatement;

public class JQFModelFuzzer extends JQF{
	
	public JQFModelFuzzer(Class<?> clazz) throws InitializationError {
		super(clazz);
	}
	
	@Override public Statement methodBlock(FrameworkMethod method) {
		if (method.getAnnotation(Fuzz.class) != null) {
            return new ModelFuzzStatement(method, getTestClass(), this.generatorRepository);
        }
        return super.methodBlock(method);
	}
}
