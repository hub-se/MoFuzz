package de.hub.mse.emf.fuzz.junit;

import java.io.PrintStream;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.Runner;

import de.hub.mse.emf.fuzz.JQFModelFuzzer;
import edu.berkeley.cs.jqf.fuzz.JQF;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.instrument.tracing.SingleSnoop;


public class GuidedModelFuzzing extends GuidedFuzzing{
	
	private static Guidance guidance;

    public static long DEFAULT_MAX_TRIALS = 100;

    private static void setGuidance(Guidance g) {
        if (guidance != null) {
            throw new IllegalStateException("Can only set guided once.");
        }
        guidance = g;
    }

    /**
     * Returns the currently registered Guidance instance.
     *
     * @return the currently registered Guidance instance
     */
    public static Guidance getCurrentGuidance() {
        return guidance;
    }

    private static void unsetGuidance() {
        guidance = null;
    }


    /**
     * Runs the guided fuzzing loop, using the system class loader to load
     * test-application classes.
     *
     * <p>The test class must be annotated with <tt>@RunWith(JQF.class)</tt>
     * and the test method must be annotated with <tt>@Fuzz</tt>.</p>
     *
     * <p>Once this method is invoked, the guided fuzzing loop runs continuously
     * until the guidance instance decides to stop by returning <tt>false</tt>
     * for {@link Guidance#hasInput()}. Until the fuzzing stops, this method
     * cannot be invoked again (i.e. at most one guided fuzzing can be running
     * at any time in a single JVM instance).</p>
     *
     * @param testClassName the test class containing the test method
     * @param testMethod    the test method to execute in the fuzzing loop
     * @param guidance      the fuzzing guidance
     * @param out           an output stream to log Junit messages
     * @throws ClassNotFoundException if testClassName cannot be loaded
     * @throws IllegalStateException if a guided fuzzing run is currently executing
     * @return the Junit-style test result
     */
    public synchronized static Result run(String testClassName, String testMethod,
                                        Guidance guidance, PrintStream out) throws ClassNotFoundException, IllegalStateException {

        // Run with the system class loader
        return run(testClassName, testMethod, ClassLoader.getSystemClassLoader(), guidance, out);
    }

    /**
     * Runs the guided fuzzing loop, using a provided classloader to load
     * test-application classes.
     *
     * <p>The test class must be annotated with <tt>@RunWith(JQF.class)</tt>
     * and the test method must be annotated with <tt>@Fuzz</tt>.</p>
     *
     * <p>Once this method is invoked, the guided fuzzing loop runs continuously
     * until the guidance instance decides to stop by returning <tt>false</tt>
     * for {@link Guidance#hasInput()}. Until the fuzzing stops, this method
     * cannot be invoked again (i.e. at most one guided fuzzing can be running
     * at any time in a single JVM instance).</p>
     *
     * @param testClassName the test class containing the test method
     * @param testMethod    the test method to execute in the fuzzing loop
     * @param loader        the classloader to load the test class with
     * @param guidance      the fuzzing guidance
     * @param out           an output stream to log Junit messages
     * @throws ClassNotFoundException if testClassName cannot be loaded
     * @throws IllegalStateException if a guided fuzzing run is currently executing
     * @return the Junit-style test result
     */
    public synchronized static Result run(String testClassName, String testMethod,
                                        ClassLoader loader,
                                        Guidance guidance, PrintStream out) throws ClassNotFoundException, IllegalStateException {
        Class<?> testClass =
                java.lang.Class.forName(testClassName, true, loader);

        return run(testClass, testMethod, guidance, out);
    }


    /**
     * Runs the guided fuzzing loop for a resolved class.
     *
     * <p>The test class must be annotated with <tt>@RunWith(JQF.class)</tt>
     * and the test method must be annotated with <tt>@Fuzz</tt>.</p>
     *
     * <p>Once this method is invoked, the guided fuzzing loop runs continuously
     * until the guidance instance decides to stop by returning <tt>false</tt>
     * for {@link Guidance#hasInput()}. Until the fuzzing stops, this method
     * cannot be invoked again (i.e. at most one guided fuzzing can be running
     * at any time in a single JVM instance).</p>
     *
     * @param testClass     the test class containing the test method
     * @param testMethod    the test method to execute in the fuzzing loop
     * @param guidance      the fuzzing guidance
     * @param out           an output stream to log Junit messages
     * @throws IllegalStateException if a guided fuzzing run is currently executing
     * @return the Junit-style test result
     */
    public synchronized static Result run(Class<?> testClass, String testMethod,
                                          Guidance guidance, PrintStream out) throws IllegalStateException {    	
    	
        // Ensure that the class uses the right test runner
        RunWith annotation = testClass.getAnnotation(RunWith.class);
        if (annotation == null || !annotation.value().equals(JQFModelFuzzer.class)) {
            throw new IllegalArgumentException(testClass.getName() + " is not annotated with @RunWith(JQFModelFuzzer.class)");
        }


        // Set the static guided instance
        setGuidance(guidance);

        // Register callback
        SingleSnoop.setCallbackGenerator(guidance::generateCallBack);

        // Create a JUnit Request
        Request testRequest = Request.method(testClass, testMethod);

        // Instantiate a runner (may return an error)
        Runner testRunner = testRequest.getRunner();

        // Start tracing for the test method
        SingleSnoop.startSnooping(testClass.getName() + "#" + testMethod);

        // Run the test and make sure to de-register the guidance before returning
        try {
            JUnitCore junit = new JUnitCore();
            if (out != null) {
                junit.addListener(new TextListener(out));
            }
            return junit.run(testRunner);
        } finally {
            unsetGuidance();
        }
   
    }
}
