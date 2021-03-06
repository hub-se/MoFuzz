package nas.emf.repair.gen.benchmarks;

import java.io.File;
import java.io.IOException;
import java.util.*;

import nas.emf.repair.gen.uml.prototype.EMFModelGeneratorAdapter;
import de.hub.mse.emf.fuzz.JQFModelFuzzer;

import org.junit.runner.RunWith;
import org.eclipse.emf.common.util.BasicMonitor;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.util.EcoreUtil;

import org.eclipse.uml2.uml.Model;
import org.eclipse.uml2.uml.UMLPackage;

import com.pholser.junit.quickcheck.From;

import edu.berkeley.cs.jqf.fuzz.Fuzz;
import org.eclipse.acceleo.module.example.uml2java.helios.GenerateJava;

@RunWith(JQFModelFuzzer.class)
public class AcceleoUML2JavaHeliosTest {

    @Fuzz
    public void simpleGeneratorTest(@From(EMFModelGeneratorAdapter.class) Resource resource) {
    	Model model = (Model) EcoreUtil.getObjectByType(resource.getContents(), UMLPackage.Literals.MODEL);
    	ResourceSet set = resource.getResourceSet();
		File targetFolder = new File("workdirs/nasgen_uml2java_out");
		
		try {
			
	    	GenerateJava generator = new GenerateJava(model, targetFolder, new ArrayList<String>());
			generator.registerResourceFactories(set);
			generator.registerPackages(set);
			generator.doGenerate(new BasicMonitor());
			removeDirectory(targetFolder);
			
		}
		catch(IOException e) {
			e.printStackTrace();
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

