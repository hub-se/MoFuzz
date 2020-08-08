package de.hub.mse.emf.generator.zest.benchmarks;

import org.eclipse.uml2.uml.util.UMLValidator;
import org.eclipse.emf.common.util.BasicDiagnostic;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.util.Diagnostician;
import org.junit.runner.RunWith;

import com.pholser.junit.quickcheck.From;

import de.hub.mse.emf.generator.zest.ZestUMLGenerator;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;

@RunWith(JQF.class)
public class UML2ValidatorTest {

    @Fuzz
    public void test(@From(ZestUMLGenerator.class) Resource resource) {
    	BasicDiagnostic diagnosticChain = new BasicDiagnostic();
    	for (TreeIterator<EObject> it = resource.getAllContents(); it.hasNext();) {
			EObject eObject = it.next();
			Diagnostician.INSTANCE.validate(eObject, diagnosticChain);
			UMLValidator.INSTANCE.validate(eObject, diagnosticChain, null);
		}
    }
    
}