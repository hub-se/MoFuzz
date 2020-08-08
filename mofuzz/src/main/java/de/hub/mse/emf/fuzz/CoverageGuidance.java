package de.hub.mse.emf.fuzz;

import java.io.Console;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.GenerationStatus.Key;

import de.hub.mse.emf.fuzz.junit.quickcheck.ModelGenerationStatus;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance.Input;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.guidance.GuidanceException;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;
import edu.berkeley.cs.jqf.fuzz.guidance.TimeoutException;
import edu.berkeley.cs.jqf.fuzz.util.Coverage;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;

/**
 * A guidance that performs standard coverage-guided greybox fuzzing.
 * However, compared to the classic guidance interface, instances of this class
 * are only responsible for recording coverage and notifying whether an input
 * exercised any new coverage points.
 * 
 * The implementation is based on the ZestGuidance implementation.
 * 
 * @author Rohan Padhye - Original implementation of the ZestGuidance
 * @author Hoang Lam Nguyen - Minor adaptions
 */

public class CoverageGuidance implements Guidance {
	
	/** Currently, only single-threaded applications are supported, which is ensured by this field */
	protected Thread appThread;
	
	/** The name of the test for display purposes. */
	protected final String testName;
	
	// ---------- ALGORITHM BOOKKEEPING ----------
	/** Object keeping track of generated test inputs */
	ModelGenerationStatus genStatus;
	
	/** The max amount of time to run for, in milli-seconds */
	protected final long maxDurationMillis;
	
	/** The number of trials completed */
	protected long numTrials = 0;
	
	/** The number of valid inputs. */
	protected long numValid = 0;
	
	/** The directory where fuzzing results are written. */
	protected final File outputDirectory;
	
	/** The directory where saved inputs are written. */
	protected File savedInputsDirectory;
	
	/** The directory where saved (failing) inputs are written. */
	protected File savedFailuresDirectory;
	
	/** Blind fuzzing -- do we need this? */
	protected boolean blind;
	
	/** Number of saved inputs (or number of inputs that triggered new coverage). */
	protected int numSavedInputs = 0;
	
	/** Coverage statistics for a single run. */
	protected Coverage runCoverage = new Coverage();
	
	/** Cumulative coverage statistics. */
	protected Coverage totalCoverage = new Coverage();
	
	/** The maximum number of keys covered by any single input found so far. */
	protected int maxCoverage = 0;
	
	/** The set of unique failures found so far. */
	protected Set<List<StackTraceElement>> uniqueFailures = new HashSet<>();	
	
	
	// ---------- LOGGING / STATS OUTPUT ----------
	
	/** Whether to print log statements to stderr (debug option; manually edit). */
	protected final boolean verbose = true;
	
	/** A system console, which is non-null only if STDOUT is a console. */
	protected final Console console = System.console();
	
	/** Time since this guidance instance was created. */
	protected final Date startTime = new Date();
	
	/** Time at last refresh. */
	protected Date lastRefreshTime = startTime;
	
	/** Total execs at last stats refresh. */
	protected long lastNumTrials = 0;
	
	/** Minimum amount of time (in millis) in between two stats refreshes. */
	protected static final long STATS_REFRESH_TIME_PERIOD = 300;
	
	/** The file where log data is written. */
	protected File logFile;
	
	/** The file where saved plot data is written. */
	protected File statsFile;
	
	
	// ---------- TIMEOUT HANDLING ----------
	
	/** Timeout for an individual run */
	protected long singleRunTimeoutMillis;
	
	/** Date when last run was started. */
	protected Date runStart;
	
	/** Number of conditional jumps since last run was started. */
	protected long branchCount;
	
	/** Whether to stop/exit once a crash is found. **/
    static final boolean EXIT_ON_CRASH = Boolean.getBoolean("jqf.ei.EXIT_ON_CRASH");
	
	// ---------- FUZZING HEURISTICS ----------
	
	/** Whether to save inputs that only add new coverage bits (but no new responsibilities). */
	public static boolean SAVE_NEW_COUNTS = Boolean.getBoolean("jqf.ei.SAVE_NEW_COUNTS");
	
	/**
	 * Creates a new guidance instance.
	 * @param testName the name of test to display on the status screen.
	 * @param duration the amount of time to run fuzzing for, where
	 * 					{@code null} indicates unlimited time.
	 * @param outputDirectory the directory where fuzzing results will be written.
	 * @throws IOException if the output directory could not be prepared
	 */
	public CoverageGuidance(String testName, Duration duration, File outputDirectory) throws IOException {
		this.testName = testName;
		this.maxDurationMillis = duration != null ? duration.toMillis() : Long.MAX_VALUE;
		this.outputDirectory = outputDirectory;
		this.prepareOutputDirectory();
		
		// Try to parse the single-run timeout
		String timeout = System.getProperty("jqf.ei.TIMEOUT");
		if (timeout != null && !timeout.isEmpty()) {
			try {
				// Interpret the timeout as milliseconds
				this.singleRunTimeoutMillis = Long.parseLong(timeout);
			} catch (NumberFormatException e1) {
				throw new IllegalArgumentException("Invalid timeout duration: " + timeout);
			}
		}
	}
	
	public void setGenStatus(ModelGenerationStatus status) {
		this.genStatus = status;
	}
	
	private void prepareOutputDirectory() throws IOException {

        // Create the output directory if it does not exist
        if (!outputDirectory.exists()) {
            if (!outputDirectory.mkdirs()) {
                throw new IOException("Could not create output directory" +
                        outputDirectory.getAbsolutePath());
            }
        }

        // Make sure we can write to output directory
        if (!outputDirectory.isDirectory() || !outputDirectory.canWrite()) {
            throw new IOException("Output directory is not a writable directory: " +
                    outputDirectory.getAbsolutePath());
        }

        // Name files and directories after AFL
        this.savedInputsDirectory = new File(outputDirectory, "corpus");
        this.savedInputsDirectory.mkdirs();
        this.savedFailuresDirectory = new File(outputDirectory, "failures");
        this.savedFailuresDirectory.mkdirs();
        this.statsFile = new File(outputDirectory, "plot_data");
        this.logFile = new File(outputDirectory, "fuzz.log");
        //this.currentInputFile = new File(outputDirectory, ".cur_input");


        // Delete everything that we may have created in a previous run.
        // Trying to stay away from recursive delete of parent output directory in case there was a
        // typo and that was not a directory we wanted to nuke.
        // We also do not check if the deletes are actually successful.
        statsFile.delete();
        logFile.delete();
        for (File file : savedInputsDirectory.listFiles()) {
            file.delete();
        }
        for (File file : savedFailuresDirectory.listFiles()) {
            file.delete();
        }

        appendLineToFile(statsFile,"# unix_time, cycles_done, cur_path, paths_total, pending_total, " +
                "branch_count, map_size, unique_crashes, unique_hangs, max_depth, execs_per_sec, valid_inputs, invalid_inputs, valid_cov");
    }
	

    /** Writes a line of text to a given log file. */
    protected void appendLineToFile(File file, String line) throws GuidanceException {
        try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
            out.println(line);
        } catch (IOException e) {
            throw new GuidanceException(e);
        }
    }
    
    /** Writes a line of text to the log file. */
    protected void infoLog(String str, Object... args) {
        if (verbose) {
            String line = String.format(str, args);
            if (logFile != null) {
                appendLineToFile(logFile, line);

            } else {
                System.err.println(line);
            }
        }
    }

	private String millisToDuration(long millis) {
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis % TimeUnit.MINUTES.toMillis(1));
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis % TimeUnit.HOURS.toMillis(1));
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        String result = "";
        if (hours > 0) {
            result = hours + "h ";
        }
        if (hours > 0 || minutes > 0) {
            result += minutes + "m ";
        }
        result += seconds + "s";
        return result;
    }
    
    // Call only if console exists
    private void displayStats() {
        assert (console != null);

        Date now = new Date();
        long intervalMilliseconds = now.getTime() - lastRefreshTime.getTime();
        if (intervalMilliseconds < STATS_REFRESH_TIME_PERIOD) {
            return;
        }
        long interlvalTrials = numTrials - lastNumTrials;
        long intervalExecsPerSec = interlvalTrials * 1000L / intervalMilliseconds;
        double intervalExecsPerSecDouble = interlvalTrials * 1000.0 / intervalMilliseconds;
        lastRefreshTime = now;
        lastNumTrials = numTrials;
        long elapsedMilliseconds = now.getTime() - startTime.getTime();
        long execsPerSec = numTrials * 1000L / elapsedMilliseconds;

        String currentParentInputDesc;
        if (genStatus.size() == 0) {
            currentParentInputDesc = "<seed>";
        } else {
            //Input currentParentInput = savedInputs.get(currentParentInputIdx);
            currentParentInputDesc = genStatus.getCurrentParentIdx() + " ";
            //currentParentInputDesc += currentParentInput.isFavored() ? "(favored)" : "(not favored)";
            currentParentInputDesc += " {" + genStatus.getCurrentChildCount() +
                    "/" + genStatus.getTargetNumChildren() + " mutations}";
        }

        int nonZeroCount = totalCoverage.getNonZeroCount();
        double nonZeroFraction = nonZeroCount * 100.0 / totalCoverage.size();
        //int nonZeroValidCount = validCoverage.getNonZeroCount();
        //double nonZeroValidFraction = nonZeroValidCount * 100.0 / validCoverage.size();

        console.printf("\033[2J");
        console.printf("\033[H");
        console.printf(this.getTitle() + "\n");
        if (this.testName != null) {
            console.printf("Test name:            %s\n", this.testName);
        }
        console.printf("Results directory:    %s\n", this.outputDirectory.getAbsolutePath());
        console.printf("Elapsed time:         %s (%s)\n", millisToDuration(elapsedMilliseconds),
                maxDurationMillis == Long.MAX_VALUE ? "no time limit" : ("max " + millisToDuration(maxDurationMillis)));
        console.printf("Number of executions: %,d\n", numTrials);
        console.printf("Valid inputs:         %,d (%.2f%%)\n", numValid, numValid * 100.0 / numTrials);
        console.printf("Cycles completed:     %d\n", genStatus.getNumCycles());
        console.printf("Unique failures:      %,d\n", uniqueFailures.size());
        //console.printf("Queue size:           %,d (%,d favored last cycle)\n", savedInputs.size(), numFavoredLastCycle);
        console.printf("Queue size:           %,d \n", genStatus.size());
        console.printf("Current parent input: %s\n", currentParentInputDesc);
        console.printf("Execution speed:      %,d/sec now | %,d/sec overall\n", intervalExecsPerSec, execsPerSec);
        console.printf("Total coverage:       %,d branches (%.2f%% of map)\n", nonZeroCount, nonZeroFraction);
        //console.printf("Valid coverage:       %,d branches (%.2f%% of map)\n", nonZeroValidCount, nonZeroValidFraction);
        

        String plotData = String.format("%d, %d, %d, %d, %d, %d, %.2f%%, %d, %d, %d, %.2f, %d, %d, %.2f%%",
                TimeUnit.MILLISECONDS.toSeconds(now.getTime()), genStatus.getNumCycles(), genStatus.getCurrentParentIdx(),
                numSavedInputs, 0, nonZeroCount, nonZeroFraction, uniqueFailures.size(), 0, 0, intervalExecsPerSecDouble,
                numValid, numTrials-numValid, 0.0);
        appendLineToFile(statsFile, plotData);

    }
    
    /** Returns the banner to be displayed on the status screen */
    protected String getTitle() {
    	return  "Coverage-guided Modelfuzzing\n" +
                    "--------------------------\n";
    }
    
    /** TODO Handles the end of fuzzing cycle (i.e., having gone through the entire queue) */
    /*
    protected void completeCycle() {
        // Increment cycle count
        cyclesCompleted++;
        infoLog("\n# Cycle " + cyclesCompleted + " completed.");

        // Go over all inputs and do a sanity check (plus log)
        infoLog("Here is a list of favored inputs:");
        int sumResponsibilities = 0;
        numFavoredLastCycle = 0;
        for (Input input : savedInputs) {
            if (input.isFavored()) {
                int responsibleFor = input.responsibilities.size();
                infoLog("Input %d is responsible for %d branches", input.id, responsibleFor);
                sumResponsibilities += responsibleFor;
                numFavoredLastCycle++;
            }
        }
        int totalCoverageCount = totalCoverage.getNonZeroCount();
        infoLog("Total %d branches covered", totalCoverageCount);
        if (sumResponsibilities != totalCoverageCount) {
            throw new AssertionError("Responsibilty mistmatch");
        }

        // Break log after cycle
        infoLog("\n\n\n");
    }
    */
	
	@Override
	public InputStream getInput() throws IllegalStateException, GuidanceException {
		// Clear coverage status for this run
		runCoverage.clear();
		
		// Write input to disk for debugging TODO
		
		// Start time-counting for timeout handling
		this.runStart = new Date();
		this.branchCount = 0;
		
		// We don't actually generate an input
		return null;
	}

	@Override
	public boolean hasInput() {
		Date now = new Date();
        long elapsedMilliseconds = now.getTime() - startTime.getTime();
        
        if (EXIT_ON_CRASH && uniqueFailures.size() >= 1) {
            // exit
            return false;
        }
        
        return elapsedMilliseconds < maxDurationMillis;
	}

	@Override
	public void handleResult(Result result, Throwable error) throws GuidanceException {
		// Stop timeout handling
		this.runStart = null;
		
		// Increment run count
		this.numTrials++;
		
		boolean valid = result == Result.SUCCESS;
		
		if (valid) {
			// Increment valid counter
			numValid++;
		}
		
		if (result == Result.SUCCESS || result == Result.INVALID) {
			
			// Coverage before
			int nonZeroBefore = totalCoverage.getNonZeroCount();
			
			// Compute a list of keys for which this input can assume responsibility,
			// i.e. the newly covered branches.
			Set<Object> responsibilities = computeResponsibilities(valid);
			
			boolean coverageBitsUpdated = totalCoverage.updateBits(runCoverage);
			
			// Coverage after
			int nonZeroAfter = totalCoverage.getNonZeroCount();
			if (nonZeroAfter > maxCoverage) {
				maxCoverage = nonZeroAfter;
			}
			
			// Possibly save input
			boolean toSave = false;
			String why = "";
			
			// Save if branch-hit count is incremented (but not necessarily a new branch is exercised)
			if (SAVE_NEW_COUNTS && coverageBitsUpdated) {
				toSave = true;
				why = why + "+count";
			}
			
			// Save if new total coverage is found, i.e. a new branch was taken
			if (nonZeroAfter > nonZeroBefore) {
				// Must be responsible for some branch
				assert(responsibilities.size() > 0);
				toSave = true;
				why = why + "cov";
			}
			
			if (toSave) {
				// Instruct input generator to save inputs
				genStatus.saveInput(responsibilities.size());
				numSavedInputs++;
				
				// Write to log file
				infoLog("Saving new input (at run %d): " +
                        "input #%d " +
                        "of size %d; " +
                        "total coverage = %d",
                numTrials,
                genStatus.size(),
                0,
                nonZeroAfter);
				
			}
		} else if (result == Result.FAILURE || result == Result.TIMEOUT) {
			String msg = error.getMessage();
			
			// Get the root cause of the failure
			Throwable rootCause = error;
			while (rootCause.getCause() != null) {
				rootCause = rootCause.getCause();
			}
			
			// Attempt to add this to the set of unique failures
			if (uniqueFailures.add(Arrays.asList(rootCause.getStackTrace()))) {
				
				// TODO Trim input
				
				// TODO Save crash to disk
				
				// Write to log
				long elapsed = new Date().getTime() - startTime.getTime();
                infoLog("%d %s", elapsed, "Found crash: " + error.getClass() + " - " + (msg != null ? msg : ""));

			}
		}
		
		// Display stats on every interval
		if (console != null) {
			displayStats();
		}	
	}
	
	// Compute a set of branches for which the current input may assume responsibility
	private Set<Object> computeResponsibilities(boolean valid) {
	    Set<Object> result = new HashSet<>();
    
	    // This input is responsible for all new coverage
	    Collection<?> newCoverage = runCoverage.computeNewCoverage(totalCoverage);
	    if (newCoverage.size() > 0) {
	    		result.addAll(newCoverage);
	    }
	    return result;
	}
    
    /* Saves an interesting input to the queue. */
	/*
    protected void saveCurrentInput(Set<Object> responsibilities, String why) throws IOException {
    }
    */
    
    
    @Override  
	public Consumer<TraceEvent> generateCallBack(Thread thread) {
    	if (appThread != null) {
    		throw new IllegalStateException(ZestGuidance.class +
    				" only supports single-threaded apps at the moment");
    	}
    	appThread = thread;
    	return this::handleEvent;
	}
    
	/** Handles a trace event generated during test execution */
	protected void handleEvent(TraceEvent e) {
		// Collect totalCoverage
		runCoverage.handleEvent(e);
		// Check for possible timeouts every so often
		if (this.singleRunTimeoutMillis > 0 &&
				this.runStart != null && (++this.branchCount) % 10_000 == 0) {
			long elapsed = new Date().getTime() - runStart.getTime();
			if (elapsed > this.singleRunTimeoutMillis) {
				throw new TimeoutException(elapsed, this.singleRunTimeoutMillis);
			}
		}
	}
    
	/**
     * Returns a reference to the coverage statistics.
     * @return a reference to the coverage statistics
     */
	public Coverage getTotalCoverage() {
		return totalCoverage;
	}

}
