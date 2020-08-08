package de.hub.mse.emf.fuzz;

import edu.berkeley.cs.jqf.instrument.InstrumentingClassLoader;
import org.junit.runner.Result;

import de.hub.mse.emf.fuzz.junit.GuidedModelFuzzing;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;

/**
 *
 * @author Yevgeny Pats - Original author for Zest based guidance on CLI
 * @author Hoang Lam Nguyen - Minor modifications/adaptions
 */

@CommandLine.Command(name = "ModelFuzzerCLI", mixinStandardHelpOptions = true, version = "1.3")
public class ModelFuzzerCLI implements Runnable{

    @CommandLine.ArgGroup(exclusive = false, multiplicity = "0..2")
    Dependent dependent;

    static class Dependent {
        @Option(names = { "-e", "--exit-on-crash" },
                description = "Exit fuzzer on first crash (default: false)")
        boolean exitOnCrash = false;

        @Option(names = { "--exact-crash-path" },
                description = "exact path for the crash")
        String exactCrashPath;
    }

    @Option(names = { "-l", "--libfuzzer-compat-output" },
            description = "Use libFuzzer compat output instead of AFL like stats screen (default: false)")
    private boolean libFuzzerCompatOutput = false;

    @Option(names = { "-i", "--input" },
            description = "Input directory containing seed test cases (default: none)")
    private File inputDirectory;

    @Option(names = { "-o", "--output" },
            description = "Output Directory containing results (default: fuzz_results)")
    private File outputDirectory = new File("fuzz-results");

    @Option(names = { "-d", "--duration" },
            description = "Total fuzz duration (e.g. PT5s or 5s)")
    private Duration duration;

    @Option(names = { "-b", "--blind" },
            description = "Blind fuzzing: do not use coverage feedback (default: false)")
    private boolean blindFuzzing;

    @Parameters(index = "0", paramLabel = "PACKAGE", description = "package containing the fuzz target and all dependencies")
    private String testPackageName;

    @Parameters(index="1", paramLabel = "TEST_CLASS", description = "full class name where the fuzz function is located")
    private String testClassName;

    @Parameters(index="2", paramLabel = "TEST_METHOD", description = "fuzz function name")
    private String testMethodName;


    private File[] readSeedFiles() {
        if (this.inputDirectory == null) {
            return new File[0];
        }

        ArrayList<File> seedFilesArray = new ArrayList<>();
        File[] allFiles = this.inputDirectory.listFiles();
        if (allFiles == null) {
            // this means the directory doesn't exist
            return new File[0];
        }
        for (int i = 0; i < allFiles.length; i++) {
            if (allFiles[i].isFile()) {
                seedFilesArray.add(allFiles[i]);
            }
        }
        File[] seedFiles = seedFilesArray.toArray(new File[seedFilesArray.size()]);
        return seedFiles;
    }

    public void run() {

        File[] seedFiles = readSeedFiles();

        if (this.dependent != null) {
            if (this.dependent.exitOnCrash) {
                System.setProperty("jqf.ei.EXIT_ON_CRASH", "true");
            }

            if (this.dependent.exactCrashPath != null) {
                System.setProperty("jqf.ei.EXACT_CRASH_PATH", this.dependent.exactCrashPath);
            }
        }

        if (this.libFuzzerCompatOutput) {
            System.setProperty("jqf.ei.LIBFUZZER_COMPAT_OUTPUT", "true");
        }


        try {
            ClassLoader loader = new InstrumentingClassLoader(
                    this.testPackageName.split(File.pathSeparator),
                    ModelFuzzerCLI.class.getClassLoader());

            // Load the guidance
            String title = this.testClassName+"#"+this.testMethodName;
            /*
            ZestGuidance guidance = seedFiles.length > 0 ?
                    new ZestGuidance(title, duration, this.outputDirectory, seedFiles) :
                    new ZestGuidance(title, duration, this.outputDirectory);
                    */
            CoverageGuidance guidance = new CoverageGuidance(title, duration, this.outputDirectory);
            //guidance.setBlind(blindFuzzing);
            
            // Run the Junit test
            Result res = GuidedModelFuzzing.run(testClassName, testMethodName, loader, guidance, System.out);
            if (Boolean.getBoolean("jqf.logCoverage")) {
                System.out.println(String.format("Covered %d edges.",
                        guidance.getTotalCoverage().getNonZeroCount()));
            }
            if (Boolean.getBoolean("jqf.ei.EXIT_ON_CRASH") && !res.wasSuccessful()) {
                System.exit(3);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.exit(2);
        }

    }
    public static void main(String[] args) {
        int exitCode = new CommandLine(new ModelFuzzerCLI())
                .registerConverter(Duration.class, v -> {
                    try {
                        return Duration.parse(v);
                    } catch (DateTimeParseException e) {
                        return Duration.parse("PT" + v);
                    }
                })
                .execute(args);
        System.exit(exitCode);
    }
}