package de.hub.mse.emf.serge.benchmarks;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.runner.RunWith;

import com.pholser.junit.quickcheck.From;

import de.hub.mse.emf.fuzz.JQFModelFuzzer;

import edu.berkeley.cs.jqf.fuzz.Fuzz;
import de.hub.mse.emf.generator.cgf.GuidedSERGeUMLGenerator;

import uml2owl_plugin.files.*;

@RunWith(JQFModelFuzzer.class)
public class UML2OWLTest {

    @Fuzz
    public void test(@From(GuidedSERGeUMLGenerator.class) Resource resource) {
    	try {
			UML2OWL uml2owl = new UML2OWL();
			uml2owl.loadModelByResource(resource);
			uml2owl.doUML2OWL(new NullProgressMonitor());
			//uml2owl.saveModels(path);
		}
		catch(Exception e) {
			throw new RuntimeException(e);
		}
    }  
}