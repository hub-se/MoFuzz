package de.hub.mse.emf.generator.benchmarks;

import java.io.File;
import java.io.IOException;
import java.util.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLPackage;
import org.junit.runner.RunWith;
import com.pholser.junit.quickcheck.*;
import com.pholser.junit.quickcheck.generator.*;
import edu.berkeley.cs.jqf.fuzz.Fuzz;

import de.hub.mse.emf.fuzz.JQFModelFuzzer;
import de.hub.mse.emf.generator.UMLGenerator;


//import org.eclipse.acceleo.examples.uml2java.main.Uml2java; // To re-add: Add org.eclipse.acceleo.examples.uml2java project to build path
@RunWith(JQFModelFuzzer.class)
public class AcceleoUML2JavaTest {

    @Fuzz
    public void simpleGeneratorTest(@From(UMLGenerator.class) Resource resource) {
    	Model model = (Model) EcoreUtil.getObjectByType(resource.getContents(), UMLPackage.Literals.MODEL);
    	ResourceSet set = resource.getResourceSet();
		File targetFolder = new File("workdirs/uml2java_out/");
		
		/*
		try {
			
			Uml2java generator = new Uml2java(model, targetFolder, new ArrayList<String>());
			generator.registerResourceFactories(set);
			generator.registerPackages(set);
			generator.doGenerate(new BasicMonitor());
			
			removeDirectory(targetFolder);
			
		}
		catch(IOException e) {
			e.printStackTrace();
		}
		*/
		
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

