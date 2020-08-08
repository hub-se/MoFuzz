package de.hub.mse.emf.generator.zest.benchmarks;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.emf.ecore.resource.Resource;
import org.junit.runner.RunWith;

import com.pholser.junit.quickcheck.From;

import de.hub.mse.emf.generator.zest.ZestUMLGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import uml2owl_plugin.files.*;

@RunWith(JQF.class)
public class UML2OWLTest {

    @Fuzz
    public void test(@From(ZestUMLGenerator.class) Resource resource) {
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