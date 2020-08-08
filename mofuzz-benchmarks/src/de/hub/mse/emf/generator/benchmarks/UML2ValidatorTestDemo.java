package de.hub.mse.emf.generator.benchmarks;

import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.eclipse.uml2.uml.util.UMLValidator;
import org.junit.runner.RunWith;

import com.pholser.junit.quickcheck.From;

import de.hub.mse.emf.fuzz.JQFModelFuzzer;
import de.hub.mse.emf.generator.demo.DemoUMLGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;

@RunWith(JQFModelFuzzer.class)
public class UML2ValidatorTestDemo {

    @Fuzz
    public void test(@From(DemoUMLGenerator.class) Resource resource) {
    	BasicDiagnostic diagnosticChain = new BasicDiagnostic();
		for (EObject eObject : resource.getContents()) {
			Diagnostician.INSTANCE.validate(eObject, diagnosticChain);
			UMLValidator.INSTANCE.validate(eObject, diagnosticChain, null);
		}
    }
    
}