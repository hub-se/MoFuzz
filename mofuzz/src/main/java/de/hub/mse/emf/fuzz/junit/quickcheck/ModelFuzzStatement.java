package de.hub.mse.emf.fuzz.junit.quickcheck;

import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.junit.TrialRunner;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FuzzStatement;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.NonTrackingGenerationStatus;
import ru.vyarus.java.generics.resolver.GenericsResolver;

import static edu.berkeley.cs.jqf.fuzz.guidance.Result.FAILURE;
import static edu.berkeley.cs.jqf.fuzz.guidance.Result.INVALID;
import static edu.berkeley.cs.jqf.fuzz.guidance.Result.SUCCESS;
import static edu.berkeley.cs.jqf.fuzz.guidance.Result.TIMEOUT;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.internal.ParameterTypeContext;
import com.pholser.junit.quickcheck.internal.generator.CompositeGenerator;
import com.pholser.junit.quickcheck.internal.generator.GeneratorRepository;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;

import de.hub.mse.emf.fuzz.CoverageGuidance;
import de.hub.mse.emf.fuzz.junit.GuidedModelFuzzing;

import org.junit.AssumptionViolatedException;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;


public class ModelFuzzStatement extends Statement{
	private final FrameworkMethod method;
    private final TestClass testClass;
    private final Map<String, Type> typeVariables;
    private final GeneratorRepository generatorRepository;
    private final List<Class<?>> expectedExceptions;
    private final List<Throwable> failures = new ArrayList<>();
	
	public ModelFuzzStatement(FrameworkMethod method, TestClass testClass,
								GeneratorRepository generatorRepository) {
		//super(method, testClass, generatorRepository);
		this.method = method;
        this.testClass = testClass;
        this.typeVariables =
                GenericsResolver.resolve(testClass.getJavaClass())
                        .method(method.getMethod())
                        .genericsMap();
        this.generatorRepository = generatorRepository;
        this.expectedExceptions = Arrays.asList(method.getMethod().getExceptionTypes());
	}
	
	@Override
	public void evaluate() throws Throwable {
		 // Construct generators for each parameter
        List<Generator<?>> generators = Arrays.stream(this.method.getMethod().getParameters())
                .map(this::createParameterTypeContext)
                .map(this::produceGenerator)
                .collect(Collectors.toList());
	
		try {
			
			// TODO this is pretty ugly, solve differently
			CompositeGenerator compositeGen = (CompositeGenerator) generators.get(0);
			ModelGenerationStatus genStatus = new ModelGenerationStatus(compositeGen.composed(0));
			CoverageGuidance guidance = (CoverageGuidance) GuidedModelFuzzing.getCurrentGuidance();
	        
			
			// The main fuzzing loop, continue forever or until timeout
			while(guidance.hasInput()) {
				Throwable error = null;
				Result result = Result.INVALID;
				
				 guidance.setGenStatus(genStatus);

				try {
					Object[] args;

					try {
						guidance.getInput();
						SourceOfRandomness random = new SourceOfRandomness(new Random());
						
						args = generators.stream()
	                            .map(g -> g.generate(random, genStatus))
	                            .toArray();
	                            
						//args[0] = generator.generate(random, genStatus);
					} catch (Throwable e) {
						throw e;
					}
					
					// Attempt to run the trial
					new TrialRunner(testClass.getJavaClass(), method, args).run();	
					
					// If we reached here, then the trial must be a success
					result = Result.SUCCESS;
				} catch (GuidanceException e) {
					// Throw the guidance exception outside to stop fuzzing
					throw e;
				} catch (AssumptionViolatedException e) {
					result = INVALID;
					error = e;
				} catch (TimeoutException e) {
					result = TIMEOUT;
					error = e;
				} catch (Throwable e) {

					// Check if this exception was expected
					if (isExceptionExpected(e.getClass())) {
						result = SUCCESS; // Swallow the error
					} else {
						result = FAILURE;
						error = e;
						failures.add(e);
					}
				}
				
                // Inform guidance about the outcome of this trial
                guidance.handleResult(result, error);

				}
		} catch (Exception e) {
			throw e;
		}
		
		/*
		 if (failures.size() > 0) {
	            if (failures.size() == 1) {
	                throw failures.get(0);
	            } else {
	                // Not sure if we should report each failing run,
	                // as there may be duplicates
	                throw new MultipleFailureException(failures);
	            }
	     }
	    */	
	}
	
	
	/**
     * Returns whether an exception is expected to be thrown by a trial method
     *
     * @param e the class of an exception that is thrown
     * @return <tt>true</tt> if e is a subclass of any exception specified
     * in the <tt>throws</tt> clause of the trial method.
     */
    private boolean isExceptionExpected(Class<? extends Throwable> e) {
        for (Class<?> expectedException : expectedExceptions) {
            if (expectedException.isAssignableFrom(e)) {
                return true;
            }
        }
        return false;
    }

    private ParameterTypeContext createParameterTypeContext(Parameter parameter) {
        Executable exec = parameter.getDeclaringExecutable();
        String declarerName = exec.getDeclaringClass().getName() + '.' + exec.getName();
        return new ParameterTypeContext(
                        parameter.getName(),
                        parameter.getAnnotatedType(),
                        declarerName,
                        typeVariables)
                        .allowMixedTypes(true).annotate(parameter);
    }

    private Generator<?> produceGenerator(ParameterTypeContext parameter) {
        Generator<?> generator = generatorRepository.generatorFor(parameter);
        generator.provide(generatorRepository);
        generator.configure(parameter.annotatedType());
        return generator;
    }
}
