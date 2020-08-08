package de.hub.mse.emf.serge.benchmarks;

import java.io.File;
import java.util.Random;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.emf.ecore.resource.Resource;
import org.emftools.emf2gv.graphdesc.GraphdescPackage;
import org.emftools.emf2gv.processor.core.StandaloneProcessor;
import org.junit.runner.RunWith;

import com.pholser.junit.quickcheck.From;

import de.hub.mse.emf.generator.cgf.GuidedSERGeUMLGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import de.hub.mse.emf.fuzz.JQFModelFuzzer;

@RunWith(JQFModelFuzzer.class)
public class EMF2GraphvizTest {

    @Fuzz
    public void test(@From(GuidedSERGeUMLGenerator.class) Resource resource) {
    	GraphdescPackage.eINSTANCE.eClass();
    	Random random = new Random();
    	String path = "workdirs/serge_emf2gvWorkDir";
    	String out_path = path + "/sample.jpg";
    	File workDir = new File(path);
    	
    	try {
    	StandaloneProcessor.process(resource.getContents(), // model
    			null, // gv figure description 
    			workDir, // work directory
    			out_path, // diagram file
    			null, // callback
    			null, // icon provider
    			null, // dot command
    			true, // add validation decorators?
    			false, // keep generated graphviz source file?
    			"UTF-8", // Graphviz source encoding,
    			null, // additional filters
    			null, // logger
    			null); // progress monitor
    	}
    	catch(CoreException e) {
    		e.printStackTrace();
    		throw new RuntimeException(e.toString());
    	}
    	finally {
    		removeDirectory(workDir);
    	}
    }
    
    public static boolean removeDirectory(File directory) {
		// System.out.println("removeDirectory " + directory);

		if (directory == null) {
			return false;
		}
		if (!directory.exists()) {
			return true;
		}
		if (!directory.isDirectory()) {
			return false;
		}

		String[] list = directory.list();

		// Some JVMs return null for File.list() when the
		// directory is empty.
		if (list != null) {
			for (int i = 0; i < list.length; i++) {
				File entry = new File(directory, list[i]);

				// System.out.println("\tremoving entry " + entry);

				if (entry.isDirectory()) {
					if (!removeDirectory(entry)) {
						return false;
					}
				} else {
					if (!entry.delete()) {
						return false;
					}
				}
			}
		}

		return directory.delete();
	}
}